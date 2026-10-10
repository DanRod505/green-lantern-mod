package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.client.wonderwoman.WonderWomanVisuals;
import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.wonderwoman.AmazonFlags;
import com.danrod505.greenlantern.wonderwoman.AmazonPower;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * HUD of the Tiara of Themyscira: Wonder Woman's divine power (a crimson-to-gold bar), her powers,
 * and the Invisible Jet's controls while she flies it.
 */
public final class WonderWomanHud {
    private static float displayedFraction = -1.0F;

    private WonderWomanHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack tiara = WonderWomanHelper.findTiara(player);
        if (tiara.isEmpty()) {
            displayedFraction = -1.0F;
            return;
        }
        boolean held = !WonderWomanHelper.heldTiara(player).isEmpty();
        boolean suited = WonderWomanHelper.isSuited(player);
        if (!held && !suited) return;

        RingEnergy energy = WonderWomanHero.DIVINE_POWER.get(tiara);
        float fraction = energy.fraction();
        if (displayedFraction < 0) displayedFraction = fraction;
        displayedFraction += (fraction - displayedFraction) * 0.15F;

        int x = 8;
        // Below the other heroes' HUDs when those are showing too.
        int y = 8;
        if (!RingHelper.findRing(player).isEmpty() && (!RingHelper.heldRing(player).isEmpty() || RingHelper.isSuited(player))) y += 58;
        if (!FlashHelper.findRing(player).isEmpty() && (!FlashHelper.heldRing(player).isEmpty() || FlashHelper.isSuited(player))) y += 58;
        if (!AquamanHelper.findEmblem(player).isEmpty() && (!AquamanHelper.heldEmblem(player).isEmpty() || AquamanHelper.isSuited(player))) y += 70;
        if (!BatmanHelper.findBelt(player).isEmpty() && (!BatmanHelper.heldBelt(player).isEmpty() || BatmanHelper.isSuited(player))) y += 58;
        if (!SupermanHelper.findCrystal(player).isEmpty() && (!SupermanHelper.heldCrystal(player).isEmpty() || SupermanHelper.isSuited(player))) y += 58;

        // The golden eagle emblem (last icon of the strip).
        int iconsW = WonderWomanHero.WHEEL.iconsWidth();
        graphics.blit(RenderPipelines.GUI_TEXTURED, WonderWomanHero.WHEEL.icons(), x, y - 2, AmazonPower.POWERS.count() * 16, 0, 16, 16, iconsW, WheelStyle.ICONS_HEIGHT);
        int barX = x + 19;
        int barY = y + 2;
        int barW = 100;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF7A0E1C);
        graphics.fill(barX, barY, barX + barW, barY + 8, 0xD0140408);
        boolean low = fraction < 0.2F;
        int tick = player.tickCount;
        int fill = Math.round(barW * Mth.clamp(displayedFraction, 0.0F, 1.0F));
        for (int i = 0; i < fill; i++) {
            // Crimson into gold.
            float t = (float) i / barW;
            int r = (int) Mth.lerp(t, 200, 255);
            int g = (int) Mth.lerp(t, 24, 205);
            int b = (int) Mth.lerp(t, 46, 60);
            int alpha = low ? (int) (170 + 85 * Mth.sin(tick * 0.4F)) : 255;
            graphics.fill(barX + i, barY, barX + i + 1, barY + 8, Mth.clamp(alpha, 0, 255) << 24 | r << 16 | g << 8 | b);
        }
        graphics.fill(barX, barY, barX + fill, barY + 1, 0x90FFFFFF);
        graphics.drawString(mc.font, energy.stored() + " / " + energy.capacity(), barX + barW + 4, barY, low ? 0xFFFF5555 : 0xFFFFD24A, true);

        AmazonPower selected = AmazonPower.POWERS.selected(tiara);
        int iconsY = y + 15;
        int flags = WonderWomanVisuals.flags(player.getId());
        LassoEntity lasso = LassoEntity.find(player);
        for (int i = 0; i < AmazonPower.POWERS.count(); i++) {
            AmazonPower power = AmazonPower.POWERS.byIndex(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = power == selected;
            boolean active = switch (power) {
                case LASSO_CAPTURE -> lasso != null && lasso.boundTarget() != null;
                case LASSO_SPIN -> AmazonFlags.has(flags, AmazonFlags.SPIN);
                case BRACELET_GUARD -> AmazonFlags.has(flags, AmazonFlags.GUARD);
                case SWORD_AND_SHIELD -> WonderWomanHelper.swordSlot(player) >= 0;
                case INVISIBLE_JET -> player.getVehicle() instanceof InvisibleJetEntity;
                default -> false;
            };
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, 0xC0C8102E);
            if (active) graphics.fill(ix - 1, iconsY + 17, ix + 17, iconsY + 19, 0xFFF2B71C);
            graphics.blit(RenderPipelines.GUI_TEXTURED, WonderWomanHero.WHEEL.icons(), ix, iconsY, power.iconIndex() * 16, 0, 16, 16,
                    iconsW, WheelStyle.ICONS_HEIGHT, isSelected || active ? 0xFFFFFFFF : 0x90FFFFFF);
        }
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.power_key", selected.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                x + 19, iconsY + 21, 0xFFFFECE0, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.wonder_woman_suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 32, 0xFFA0A0A0, true);
        } else if (player.getVehicle() instanceof InvisibleJetEntity jet) {
            Component controls = jet.isBoosting() ? Component.translatable("hud.greenlantern.jet_afterburner")
                    : Component.translatable(jet.isCloaked() ? "hud.greenlantern.jet_controls_cloaked" : "hud.greenlantern.jet_controls");
            graphics.drawString(mc.font, controls, x + 19, iconsY + 32, 0xFFFFD24A, true);
        }
    }
}
