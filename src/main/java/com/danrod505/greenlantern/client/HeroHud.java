package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.function.BiPredicate;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The standard hero HUD, for heroes made with the hero kit: the hero's emblem (the icon after the
 * last power in the wheel icon sheet), the energy bar with its numbers, the powers (selected and
 * active ones lit), the power key and, while not suited, how to suit up. It shows while the hero's
 * item is in hand or the suit is worn, below the HUDs of the heroes listed before it.
 *
 * @param barEmpty colour (RGB) of the bar's start
 * @param barFull  colour (RGB) of the bar's end
 * @param border   colour (ARGB) of the frame and of the selected power
 * @param text     colour (ARGB) of the numbers and the power line
 * @param active   which powers are running right now (lit with a line under the icon), or null
 */
public record HeroHud(HeroDefinition hero, int barEmpty, int barFull, int border, int text, BiPredicate<Player, HeroPower> active) {
    /** Where the HUD of {@code hero} starts: below every HUD of an earlier hero that is showing. */
    public static int top(Player player, HeroDefinition hero) {
        int y = 8;
        for (HeroDefinition other : HeroRegistry.all()) {
            if (other == hero) break;
            if (!other.findItem(player).isEmpty() && (!other.heldItem(player).isEmpty() || other.isSuited(player))) y += other.hudHeight();
        }
        return y;
    }

    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        boolean suited = hero.isSuited(player);
        if (hero.heldItem(player).isEmpty() && !suited) return;
        HeroPowers<?> powers = hero.powers();
        WheelStyle wheel = powers.wheel();
        RingEnergy energy = powers.energy().get(item);
        float fraction = energy.fraction();
        int count = powers.set().count();

        int x = 8;
        int y = top(player, hero);
        graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), x, y - 2, count * 16, 0, 16, 16, wheel.iconsWidth(), WheelStyle.ICONS_HEIGHT);
        int barX = x + 19;
        int barY = y + 2;
        int barW = 100;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, border);
        graphics.fill(barX, barY, barX + barW, barY + 8, 0xD0101010);
        boolean low = fraction < 0.2F;
        int fill = Math.round(barW * fraction);
        for (int i = 0; i < fill; i++) {
            float t = (float) i / barW;
            int r = (int) Mth.lerp(t, barEmpty >> 16 & 0xFF, barFull >> 16 & 0xFF);
            int g = (int) Mth.lerp(t, barEmpty >> 8 & 0xFF, barFull >> 8 & 0xFF);
            int b = (int) Mth.lerp(t, barEmpty & 0xFF, barFull & 0xFF);
            int alpha = low ? Mth.clamp((int) (170 + 85 * Mth.sin(player.tickCount * 0.4F)), 0, 255) : 255;
            graphics.fill(barX + i, barY, barX + i + 1, barY + 8, alpha << 24 | r << 16 | g << 8 | b);
        }
        graphics.fill(barX, barY, barX + fill, barY + 1, 0x90FFFFFF);
        graphics.drawString(mc.font, energy.stored() + " / " + energy.capacity(), barX + barW + 4, barY, low ? 0xFFFF5555 : text, true);

        int selected = powers.selectedIndex(item);
        int iconsY = y + 15;
        for (int i = 0; i < count; i++) {
            HeroPower power = powers.power(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = i == selected;
            boolean running = active != null && active.test(player, power);
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, border);
            if (running) graphics.fill(ix - 1, iconsY + 17, ix + 17, iconsY + 19, text);
            graphics.blit(RenderPipelines.GUI_TEXTURED, wheel.icons(), ix, iconsY, power.iconIndex() * 16, 0, 16, 16,
                    wheel.iconsWidth(), WheelStyle.ICONS_HEIGHT, isSelected || running ? 0xFFFFFFFF : 0x90FFFFFF);
        }
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.power_key", powers.power(selected).displayName(),
                KeyBindings.HERO_POWER.getTranslatedKeyMessage()), x + 19, iconsY + 21, text, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern." + hero.id() + "_suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 32, 0xFFA0A0A0, true);
        }
    }
}
