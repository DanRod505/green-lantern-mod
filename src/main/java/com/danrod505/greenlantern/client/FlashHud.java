package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.SpeedForce;
import com.danrod505.greenlantern.flash.SpeedsterPower;
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

/** HUD of the Flash ring: Speed Force bar with the lightning emblem and the selected power. */
public final class FlashHud {
    private static float displayedFraction = -1.0F;

    private FlashHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack ring = FlashHelper.findRing(player);
        if (ring.isEmpty()) {
            displayedFraction = -1.0F;
            return;
        }
        boolean held = !FlashHelper.heldRing(player).isEmpty();
        boolean suited = FlashHelper.isSuited(player);
        if (!held && !suited) return;

        RingEnergy force = SpeedForce.get(ring);
        float fraction = force.fraction();
        if (displayedFraction < 0) displayedFraction = fraction;
        displayedFraction += (fraction - displayedFraction) * 0.15F;

        int x = 8;
        // Below the Lantern's HUD when that one is showing too.
        boolean lanternHud = !RingHelper.findRing(player).isEmpty() && (!RingHelper.heldRing(player).isEmpty() || RingHelper.isSuited(player));
        int y = lanternHud ? 66 : 8;

        // Emblem: the lightning bolt in a white circle.
        graphics.blit(RenderPipelines.GUI_TEXTURED, PowerWheelScreen.ICONS, x, y - 2, 48, 0, 16, 16, PowerWheelScreen.ICONS_W, PowerWheelScreen.ICONS_H);
        int barX = x + 19;
        int barY = y + 1;
        graphics.fill(barX, barY, barX + 102, barY + 12, 0xC0200806);
        graphics.fill(barX, barY, barX + 102, barY + 1, 0xFFB8862A);
        graphics.fill(barX, barY + 11, barX + 102, barY + 12, 0xFFB8862A);
        graphics.fill(barX, barY, barX + 1, barY + 12, 0xFFB8862A);
        graphics.fill(barX + 101, barY, barX + 102, barY + 12, 0xFFB8862A);
        int fill = Math.round(100 * displayedFraction);
        boolean low = fraction < 0.2F;
        int tick = player.tickCount;
        for (int i = 0; i < fill; i++) {
            // Flowing Speed Force: a gold wave over red.
            float wave = 0.5F + 0.5F * Mth.sin((i - tick * 3) * 0.25F);
            int g = (int) (110 + 120 * wave);
            int alpha = low ? (int) (170 + 85 * Mth.sin(tick * 0.4F)) : 255;
            graphics.fill(barX + 1 + i, barY + 2, barX + 2 + i, barY + 10, Mth.clamp(alpha, 0, 255) << 24 | 0xFF << 16 | g << 8 | 0x20);
        }
        String text = force.stored() + " / " + force.capacity();
        graphics.drawString(mc.font, text, barX + 104, barY + 2, low ? 0xFFFF5555 : 0xFFFFD24A, true);

        SpeedsterPower selected = SpeedsterPower.selected(ring);
        int iconsY = y + 17;
        for (int i = 0; i < SpeedsterPower.count(); i++) {
            SpeedsterPower power = SpeedsterPower.byIndex(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = power == selected;
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, 0xC0FFC830);
            graphics.blit(RenderPipelines.GUI_TEXTURED, PowerWheelScreen.ICONS, ix, iconsY, power.iconIndex() * 16, 0, 16, 16,
                    PowerWheelScreen.ICONS_W, PowerWheelScreen.ICONS_H, isSelected ? 0xFFFFFFFF : 0x90FFFFFF);
        }
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.power_key", selected.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                x + 19, iconsY + 19, 0xFFFFD27A, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.flash_suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 30, 0xFFA0A0A0, true);
        }
    }
}
