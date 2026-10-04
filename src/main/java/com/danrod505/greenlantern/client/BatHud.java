package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.batman.BatCharge;
import com.danrod505.greenlantern.batman.BatPower;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.entity.BatmobileEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** HUD of the Utility Belt: the belt's charge (a row of yellow cells), the gadgets and the Batmobile's controls. */
public final class BatHud {
    private static final int CELLS = 20;
    private static float displayedFraction = -1.0F;

    private BatHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack belt = BatmanHelper.findBelt(player);
        if (belt.isEmpty()) {
            displayedFraction = -1.0F;
            return;
        }
        boolean held = !BatmanHelper.heldBelt(player).isEmpty();
        boolean suited = BatmanHelper.isSuited(player);
        if (!held && !suited) return;

        RingEnergy charge = BatCharge.get(belt);
        float fraction = charge.fraction();
        if (displayedFraction < 0) displayedFraction = fraction;
        displayedFraction += (fraction - displayedFraction) * 0.15F;

        int x = 8;
        // Below the other heroes' HUDs when those are showing too.
        int y = 8;
        if (!RingHelper.findRing(player).isEmpty() && (!RingHelper.heldRing(player).isEmpty() || RingHelper.isSuited(player))) y += 58;
        if (!FlashHelper.findRing(player).isEmpty() && (!FlashHelper.heldRing(player).isEmpty() || FlashHelper.isSuited(player))) y += 58;
        if (!AquamanHelper.findEmblem(player).isEmpty() && (!AquamanHelper.heldEmblem(player).isEmpty() || AquamanHelper.isSuited(player))) y += 70;

        // The belt buckle (last icon of the strip).
        int iconsW = PowerWheelScreen.BAT_ICONS_W;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PowerWheelScreen.BAT_ICONS, x, y - 2, BatPower.count() * 16, 0, 16, 16, iconsW, PowerWheelScreen.ICONS_H);
        int barX = x + 19;
        int barY = y + 1;
        int border = 0xFF5A5A62;
        graphics.fill(barX, barY, barX + 102, barY + 12, 0xD0060607);
        graphics.fill(barX, barY, barX + 102, barY + 1, border);
        graphics.fill(barX, barY + 11, barX + 102, barY + 12, border);
        graphics.fill(barX, barY, barX + 1, barY + 12, border);
        graphics.fill(barX + 101, barY, barX + 102, barY + 12, border);
        boolean low = fraction < 0.2F;
        int tick = player.tickCount;
        float lit = displayedFraction * CELLS;
        for (int i = 0; i < CELLS; i++) {
            int cx = barX + 2 + i * 5;
            float amount = Mth.clamp(lit - i, 0.0F, 1.0F);
            if (amount <= 0.0F) {
                graphics.fill(cx, barY + 2, cx + 4, barY + 10, 0xFF1A1A1E);
                continue;
            }
            int alpha = low ? (int) (170 + 85 * Mth.sin(tick * 0.4F)) : 255;
            int top = barY + 10 - Math.round(8 * amount);
            graphics.fill(cx, barY + 2, cx + 4, barY + 10, 0xFF1A1A1E);
            graphics.fill(cx, top, cx + 4, barY + 10, Mth.clamp(alpha, 0, 255) << 24 | 0xE8B820);
            graphics.fill(cx, top, cx + 4, top + 1, 0xFFFFF0A0);
        }
        String text = charge.stored() + " / " + charge.capacity();
        graphics.drawString(mc.font, text, barX + 104, barY + 2, low ? 0xFFFF5555 : 0xFFF2D03A, true);

        BatPower selected = BatPower.selected(belt);
        int iconsY = y + 17;
        for (int i = 0; i < BatPower.count(); i++) {
            BatPower power = BatPower.byIndex(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = power == selected;
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, 0xC0F2D03A);
            graphics.blit(RenderPipelines.GUI_TEXTURED, PowerWheelScreen.BAT_ICONS, ix, iconsY, power.iconIndex() * 16, 0, 16, 16,
                    iconsW, PowerWheelScreen.ICONS_H, isSelected ? 0xFFFFFFFF : 0x90FFFFFF);
        }
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.power_key", selected.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                x + 19, iconsY + 19, 0xFFE0E0E6, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.batman_suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 30, 0xFFA0A0A0, true);
        } else if (player.getVehicle() instanceof BatmobileEntity car) {
            Component boost = car.isBoosting() ? Component.translatable("hud.greenlantern.batmobile_boosting") : Component.translatable("hud.greenlantern.batmobile_controls",
                    mc.options.keySprint.getTranslatedKeyMessage(), mc.options.keyAttack.getTranslatedKeyMessage());
            graphics.drawString(mc.font, boost, x + 19, iconsY + 30, car.isBoosting() ? 0xFFFFA040 : 0xFFC8C8D0, true);
        }
    }
}
