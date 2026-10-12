package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.HeroHud;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.AbilityState;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Passive;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The HUD of a character of the engine (catalog, stage 14), the same for all of them: emblem and
 * name, the class energy bar with its name (blinking when the chosen ability costs more than is
 * left), the 6 abilities with their cost and a ring that fills during the cooldown, the gold Supreme
 * bar, the fight or world mode (sword, or pickaxe while crouching), and the keys.
 */
public final class EngineHud {
    private static final ItemStack FIGHT = new ItemStack(Items.IRON_SWORD);
    private static final ItemStack WORLD = new ItemStack(Items.IRON_PICKAXE);
    private static final int RING_SEGMENTS = 24;
    private static final int GOLD = 0xFFF2B705;

    private final EngineHero hero;

    public EngineHud(EngineHero hero) {
        this.hero = hero;
    }

    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        EngineIcons.ensure(hero);
        Player player = mc.player;
        if (player == null || mc.level == null || player.isSpectator() || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        boolean suited = hero.isSuited(player);
        if (hero.heldItem(player).isEmpty() && !suited) return;
        CharacterData data = hero.data();
        CharacterData.Colors colors = data.colors();
        HeroPowers<EngineAbility> powers = hero.powers();
        WheelStyle wheel = powers.wheel();
        RingEnergy energy = powers.energy().get(item);
        AbilityState state = EngineServer.state(item);
        long now = mc.level.getGameTime();
        int selected = powers.selectedIndex(item);
        int count = powers.set().count();
        int main = 0xFF000000 | colors.main();
        int light = 0xFF000000 | colors.light();
        int second = 0xFF000000 | colors.second();

        int x = 8;
        int y = HeroHud.top(player, hero);
        // Emblem and the energy bar, named after the class energy.
        graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), x, y - 2, wheel.iconsWidth() - 16, 0, 16, 16, wheel.iconsWidth(),
                WheelStyle.ICONS_HEIGHT, light);
        int barX = x + 19;
        int barY = y + 2;
        int barW = 100;
        boolean short_ = energy.stored() < powers.power(selected).cost() && !player.isCreative();
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, short_ && player.tickCount % 10 < 5 ? 0xFFFF4040 : main);
        graphics.fill(barX, barY, barX + barW, barY + 8, 0xD0101010);
        int fill = Math.round(barW * energy.fraction());
        for (int i = 0; i < fill; i++) {
            float t = (float) i / barW;
            int r = (int) Mth.lerp(t, main >> 16 & 0xFF, second >> 16 & 0xFF);
            int g = (int) Mth.lerp(t, main >> 8 & 0xFF, second >> 8 & 0xFF);
            int b = (int) Mth.lerp(t, main & 0xFF, second & 0xFF);
            graphics.fill(barX + i, barY, barX + i + 1, barY + 8, 0xFF000000 | r << 16 | g << 8 | b);
        }
        graphics.fill(barX, barY, barX + fill, barY + 1, 0x90FFFFFF);
        Component energyLine = Component.translatable("hud.greenlantern.engine_energy", data.characterClass().energyName(), energy.stored(), energy.capacity());
        graphics.drawString(mc.font, energyLine, barX + barW + 4, barY, energy.fraction() < 0.2F ? 0xFFFF5555 : light, true);

        // The 6 abilities: cost under each one, a ring filling during its cooldown.
        int iconsY = y + 15;
        for (int i = 0; i < count; i++) {
            EngineAbility ability = (EngineAbility) powers.power(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = i == selected;
            long left = state.remaining(i, now);
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, second);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), ix, iconsY, ability.iconIndex() * 16, 0, 16, 16, wheel.iconsWidth(),
                    WheelStyle.ICONS_HEIGHT, left > 0 ? 0x70FFFFFF : isSelected ? 0xFFFFFFFF : 0xB0FFFFFF);
            if (left > 0) cooldownRing(graphics, ix + 8, iconsY + 8, 1.0F - (float) left / ability.cooldownTicks(), light);
            String cost = ability.supreme() ? Math.round(state.supremeFraction() * 100) + "%" : String.valueOf(ability.cost());
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix + 16 - mc.font.width(cost) * 0.5F, iconsY + 12);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.drawString(mc.font, cost, 0, 0, ability.supreme() ? GOLD : 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
        // Fight or world: a sword, or a pickaxe while crouching.
        graphics.renderItem(player.isShiftKeyDown() ? WORLD : FIGHT, x + 19 + count * 20 + 2, iconsY);

        // The Supreme bar: thin and gold, shining when full.
        int supremeY = iconsY + 19;
        int supremeW = count * 20 - 4;
        graphics.fill(barX - 1, supremeY - 1, barX + supremeW + 1, supremeY + 3, 0xC0302000);
        int supremeFill = Math.round(supremeW * state.supremeFraction());
        int shine = state.supremeFull() ? (int) (200 + 55 * Mth.sin(player.tickCount * 0.3F)) : 255;
        graphics.fill(barX, supremeY, barX + supremeFill, supremeY + 2, shine << 24 | (GOLD & 0xFFFFFF));

        Component keys = Component.translatable(player.isShiftKeyDown() ? "hud.greenlantern.engine_world_key" : "hud.greenlantern.power_key",
                powers.power(selected).displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage());
        graphics.drawString(mc.font, keys, x + 19, supremeY + 5, light, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.engine_suit_hint", data.name(), KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, supremeY + 16, 0xFFA0A0A0, true);
        } else if (state.supremeFull()) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.engine_supreme_key", KeyBindings.SUPREME.getTranslatedKeyMessage()),
                    x + 19, supremeY + 16, GOLD, true);
        }
        if (suited && data.passive() == Passive.SEE_HEALTH) aimedHealth(graphics, mc, light);
    }

    /** A ring of dots around the icon that fills clockwise from the top as the cooldown runs out. */
    private static void cooldownRing(GuiGraphics graphics, int cx, int cy, float progress, int color) {
        int lit = Math.round(RING_SEGMENTS * Mth.clamp(progress, 0.0F, 1.0F));
        for (int s = 0; s < RING_SEGMENTS; s++) {
            float angle = (float) (s * Math.PI * 2 / RING_SEGMENTS - Math.PI / 2);
            int px = cx + Math.round(Mth.cos(angle) * 7.0F);
            int py = cy + Math.round(Mth.sin(angle) * 7.0F);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, s < lit ? color : 0x80000000);
        }
    }

    /** The inventor's passive: the health of the creature aimed at, under the crosshair. */
    private static void aimedHealth(GuiGraphics graphics, Minecraft mc, int color) {
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return;
        Component line = Component.translatable("hud.greenlantern.engine_target", target.getDisplayName(), Mth.ceil(target.getHealth()), Mth.ceil(target.getMaxHealth()));
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2 + 12;
        graphics.drawCenteredString(mc.font, line, cx, cy, color);
    }
}
