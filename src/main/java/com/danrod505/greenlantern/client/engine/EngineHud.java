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

        AbilityRow.render(graphics, mc, hero, item, x + 19, y + 15, light, second, suited, i -> false);
        if (!suited) {
            int keysY = AbilityRow.keysY(hero, y + 15);
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.engine_suit_hint", data.name(), KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, keysY + 11, 0xFFA0A0A0, true);
        }
        if (suited && data.passive() == Passive.SEE_HEALTH) aimedHealth(graphics, mc, light);
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
