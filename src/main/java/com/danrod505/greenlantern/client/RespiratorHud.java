package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.Respirator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Air left in the Atlantean Respirator, above the hunger bar (where the vanilla air bubbles would
 * be) while it breathes for you underwater.
 */
public final class RespiratorHud {
    private RespiratorHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || player.isCreative()) return;
        if (!player.isEyeInFluid(FluidTags.WATER) || AquamanHelper.isSuited(player)) return;
        ItemStack respirator = Respirator.find(player);
        if (respirator.isEmpty()) return;
        float fraction = Respirator.fraction(respirator);
        int seconds = Respirator.air(respirator) / 20;
        int width = 81;
        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - 49;
        boolean low = fraction < 0.15F;
        graphics.fill(x - 1, y - 1, x + width + 1, y + 5, 0xC0021418);
        int fill = Math.round(width * fraction);
        int tick = player.tickCount;
        for (int i = 0; i < fill; i++) {
            float wave = 0.5F + 0.5F * Mth.sin((i + tick) * 0.3F);
            int g = (int) (170 + 70 * wave);
            int alpha = low ? (int) (150 + 100 * Mth.sin(tick * 0.5F)) : 255;
            graphics.fill(x + i, y, x + i + 1, y + 4, Mth.clamp(alpha, 0, 255) << 24 | 0x40 << 16 | g << 8 | 0xFF);
        }
        Component label = Component.translatable("hud.greenlantern.respirator", seconds / 60, String.format("%02d", seconds % 60));
        graphics.drawString(mc.font, label, x + width - mc.font.width(label), y - 10, low ? 0xFFFF5555 : 0xFF9FF0FF, true);
    }
}
