package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.HeroHud;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityIcons;
import com.danrod505.greenlantern.engine.AbilityState;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Passive;
import com.danrod505.greenlantern.engine.Traits;
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
import net.minecraft.world.phys.EntityHitResult;

/**
 * The HUD of a character of the engine (catalog, stage 14), the same for all of them: emblem and
 * name, the class energy bar with its name (blinking when the chosen ability costs more than is
 * left), the 5 to 8 abilities with their cost and a ring that fills during the cooldown (the supreme
 * one with a gold border), the icon of the function that goes off now (it changes with the aim, the
 * place and holding the key), the traits (lit while they are on) and the keys.
 */
public final class EngineHud {
    private static final int RING_SEGMENTS = 24;
    private static final int GOLD = 0xFF000000 | com.danrod505.greenlantern.engine.AbilityIcons.GOLD;

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
            String cost = String.valueOf(ability.cost());
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix + 16 - mc.font.width(cost) * 0.5F, iconsY + 12);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.drawString(mc.font, cost, 0, 0, ability.supreme() ? GOLD : 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
        // The function that goes off now: an arrow and its icon, a bar filling while the key is held.
        EngineAbility chosen = (EngineAbility) powers.power(selected);
        AbilityFunctions.Chosen function = AbilityFunctions.choose(chosen.data(), EngineKeys.press(chosen), player);
        int fx = x + 19 + count * 20 + 2;
        graphics.drawString(mc.font, "\u203a", fx, iconsY + 4, light, true);
        int cell = AbilityIcons.functionCell(data, selected, function.index());
        graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), fx + 7, iconsY, cell * 16, 0, 16, 16, wheel.iconsWidth(), WheelStyle.ICONS_HEIGHT, 0xFFFFFFFF);
        int held = EngineKeys.held();
        if (held > 0 && AbilityFunctions.usesHold(chosen.data())) {
            int w = Math.round(16 * Math.min(1.0F, (float) held / AbilityFunctions.HOLD_TICKS));
            graphics.fill(fx + 7, iconsY + 17, fx + 7 + w, iconsY + 19, held >= AbilityFunctions.HOLD_TICKS ? GOLD : light);
        }

        // The traits: small icons, lit while they are on.
        int traitsY = iconsY + 20;
        for (int i = 0; i < data.traits().size(); i++) {
            boolean on = suited && Traits.on(data.traits().get(i), player);
            graphics.pose().pushMatrix();
            graphics.pose().translate(barX + i * 12, traitsY);
            graphics.pose().scale(0.625F, 0.625F);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), 0, 0, AbilityIcons.traitCell(data, i) * 16, 0, 16, 16, wheel.iconsWidth(),
                    WheelStyle.ICONS_HEIGHT, on ? 0xFFFFFFFF : 0x50FFFFFF);
            graphics.pose().popMatrix();
        }

        int keysY = traitsY + (data.traits().isEmpty() ? 0 : 12);
        Component keys = Component.translatable("hud.greenlantern.power_key", chosen.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage());
        graphics.drawString(mc.font, keys, x + 19, keysY, chosen.supreme() ? GOLD : light, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.engine_suit_hint", data.name(), KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, keysY + 11, 0xFFA0A0A0, true);
        } else if (AbilityFunctions.usesHold(chosen.data())) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.engine_hold_hint", KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                    x + 19, keysY + 11, 0xFFA0A0A0, true);
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
