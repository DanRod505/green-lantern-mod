package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.client.superman.SupermanVisuals;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.superman.SuperPower;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.superman.SupermanHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * HUD of the Kryptonian Crystal: Superman's solar energy (a golden bar that shimmers while the sun
 * charges it), the powers, and the screen effects of X-ray vision and heat vision.
 */
public final class SupermanHud {
    private static float displayedFraction = -1.0F;
    private static int lastStored = -1;
    private static int chargingTicks;

    private SupermanHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) return;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        screenEffects(graphics, mc, w, h);
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack crystal = SupermanHelper.findCrystal(player);
        if (crystal.isEmpty()) {
            displayedFraction = -1.0F;
            return;
        }
        boolean held = !SupermanHelper.heldCrystal(player).isEmpty();
        boolean suited = SupermanHelper.isSuited(player);
        if (!held && !suited) return;

        RingEnergy energy = SupermanHero.SOLAR_ENERGY.get(crystal);
        float fraction = energy.fraction();
        if (displayedFraction < 0) displayedFraction = fraction;
        displayedFraction += (fraction - displayedFraction) * 0.15F;
        if (lastStored >= 0 && energy.stored() > lastStored) chargingTicks = 30;
        else if (chargingTicks > 0) chargingTicks--;
        lastStored = energy.stored();

        int x = 8;
        // Below the other heroes' HUDs when those are showing too.
        int y = 8;
        if (!RingHelper.findRing(player).isEmpty() && (!RingHelper.heldRing(player).isEmpty() || RingHelper.isSuited(player))) y += 58;
        if (!FlashHelper.findRing(player).isEmpty() && (!FlashHelper.heldRing(player).isEmpty() || FlashHelper.isSuited(player))) y += 58;
        if (!AquamanHelper.findEmblem(player).isEmpty() && (!AquamanHelper.heldEmblem(player).isEmpty() || AquamanHelper.isSuited(player))) y += 70;
        if (!BatmanHelper.findBelt(player).isEmpty() && (!BatmanHelper.heldBelt(player).isEmpty() || BatmanHelper.isSuited(player))) y += 58;

        // The S shield (last icon of the strip).
        int iconsW = SupermanHero.WHEEL.iconsWidth();
        graphics.blit(RenderPipelines.GUI_TEXTURED, SupermanHero.WHEEL.icons(), x, y - 2, SuperPower.POWERS.count() * 16, 0, 16, 16, iconsW, WheelStyle.ICONS_HEIGHT);
        int barX = x + 19;
        int barY = y + 2;
        int barW = 100;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF1A2A6A);
        graphics.fill(barX, barY, barX + barW, barY + 8, 0xD0080C1C);
        boolean low = fraction < 0.2F;
        int tick = player.tickCount;
        int fill = Math.round(barW * Mth.clamp(displayedFraction, 0.0F, 1.0F));
        for (int i = 0; i < fill; i++) {
            // A warm gradient (orange to bright gold) with a light band sweeping along while charging.
            float t = (float) i / barW;
            int r = 255;
            int g = (int) Mth.lerp(t, 150, 222);
            int b = (int) Mth.lerp(t, 20, 70);
            if (chargingTicks > 0) {
                float sweep = Mth.frac((tick + delta.getGameTimeDeltaPartialTick(false)) * 0.04F);
                float d = Math.abs(t - sweep);
                if (d < 0.08F) {
                    float k = 1.0F - d / 0.08F;
                    g = (int) Mth.lerp(k, g, 255);
                    b = (int) Mth.lerp(k, b, 220);
                }
            }
            int alpha = low ? (int) (170 + 85 * Mth.sin(tick * 0.4F)) : 255;
            graphics.fill(barX + i, barY, barX + i + 1, barY + 8, Mth.clamp(alpha, 0, 255) << 24 | r << 16 | g << 8 | b);
        }
        graphics.fill(barX, barY, barX + fill, barY + 1, 0x90FFFFFF);
        String text = energy.stored() + " / " + energy.capacity();
        graphics.drawString(mc.font, text, barX + barW + 4, barY, low ? 0xFFFF5555 : 0xFFFFD84A, true);

        SuperPower selected = SuperPower.POWERS.selected(crystal);
        int iconsY = y + 15;
        int flags = SupermanVisuals.flags(player.getId());
        for (int i = 0; i < SuperPower.POWERS.count(); i++) {
            SuperPower power = SuperPower.POWERS.byIndex(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = power == selected;
            boolean active = switch (power) {
                case HEAT_VISION -> (flags & com.danrod505.greenlantern.superman.SuperFlags.HEAT_VISION) != 0;
                case SUPER_BREATH -> (flags & com.danrod505.greenlantern.superman.SuperFlags.SUPER_BREATH) != 0;
                case XRAY_VISION -> (flags & com.danrod505.greenlantern.superman.SuperFlags.XRAY) != 0;
                case SUPER_PUNCH -> false;
            };
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, 0xC02E6BFF);
            if (active) graphics.fill(ix - 1, iconsY + 17, ix + 17, iconsY + 19, 0xFFE8303A);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SupermanHero.WHEEL.icons(), ix, iconsY, power.iconIndex() * 16, 0, 16, 16,
                    iconsW, WheelStyle.ICONS_HEIGHT, isSelected || active ? 0xFFFFFFFF : 0x90FFFFFF);
        }
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.power_key", selected.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                x + 19, iconsY + 21, 0xFFE6ECFF, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.superman_suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 32, 0xFFA0A0A0, true);
        } else if (chargingTicks > 0 && !energy.isFull()) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.solar_charging"), x + 19, iconsY + 32, 0xFFFFD84A, true);
        }
    }

    /** X-ray vision tints the view blue with scan lines; heat vision reddens the edges of the screen. */
    private static void screenEffects(GuiGraphics graphics, Minecraft mc, int w, int h) {
        if (!mc.options.getCameraType().isFirstPerson()) return;
        float xray = SupermanVisuals.xrayFade();
        if (xray > 0.0F) {
            graphics.fill(0, 0, w, h, (int) (40 * xray) << 24 | 0x2050FF);
            int lineAlpha = (int) (22 * xray);
            int offset = (mc.player.tickCount / 2) % 4;
            for (int y = offset; y < h; y += 4) graphics.fill(0, y, w, y + 1, lineAlpha << 24 | 0xA0C8FF);
            edges(graphics, w, h, (int) (90 * xray), 0x2E6BFF);
        }
        if (SupermanVisuals.localHeatVision()) {
            float pulse = 0.8F + 0.2F * Mth.sin(mc.player.tickCount * 0.9F);
            edges(graphics, w, h, (int) (110 * pulse), 0xFF2A10);
        }
    }

    private static void edges(GuiGraphics graphics, int w, int h, int alpha, int rgb) {
        int steps = 10;
        int depth = Math.max(8, h / 7);
        for (int i = 0; i < steps; i++) {
            int a = (int) (alpha * (1.0F - (float) i / steps));
            int color = Mth.clamp(a, 0, 255) << 24 | rgb;
            int d = depth * i / steps;
            int d2 = depth * (i + 1) / steps;
            graphics.fill(0, d, w, d2, color);
            graphics.fill(0, h - d2, w, h - d, color);
            graphics.fill(d, 0, d2, h, color);
            graphics.fill(w - d2, 0, w - d, h, color);
        }
    }
}
