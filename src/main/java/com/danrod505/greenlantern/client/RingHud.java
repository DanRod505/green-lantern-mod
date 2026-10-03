package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * HUD shown while the player carries a Power Ring: energy bar with the Corps emblem plus the
 * construct selector (current construct highlighted).
 */
public final class RingHud {
    private static final Identifier ENERGY_BAR = GreenLantern.id("textures/gui/energy_bar.png");
    private static final Identifier CONSTRUCTS = GreenLantern.id("textures/gui/constructs.png");
    private static final int BAR_TEX_W = 128;
    private static final int BAR_TEX_H = 32;
    private static final int ICON_TEX_W = 256;
    private static final int ICON_TEX_H = 16;

    private static float displayedFraction = -1.0F;

    private RingHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty()) {
            displayedFraction = -1.0F;
            return;
        }
        boolean held = !RingHelper.heldRing(player).isEmpty();
        boolean suited = RingHelper.isSuited(player);
        if (!held && !suited) return;

        RingEnergy energy = RingEnergy.get(ring);
        float fraction = energy.fraction();
        if (displayedFraction < 0) displayedFraction = fraction;
        displayedFraction += (fraction - displayedFraction) * 0.15F;

        int x = 8;
        int y = 8;
        // Emblem + frame.
        graphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, x, y - 2, 104, 0, 16, 16, BAR_TEX_W, BAR_TEX_H);
        int barX = x + 19;
        int barY = y + 1;
        graphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, barX, barY, 0, 0, 102, 12, BAR_TEX_W, BAR_TEX_H);
        int fill = Math.round(100 * displayedFraction);
        boolean low = fraction < 0.2F;
        if (fill > 0) {
            int alpha = low ? (int) (180 + 75 * Mth.sin(player.tickCount * 0.4F)) : 255;
            int color = (Mth.clamp(alpha, 0, 255) << 24) | 0xFFFFFF;
            graphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, barX + 1, barY + 2, 0, low ? 24 : 16, fill, 8, BAR_TEX_W, BAR_TEX_H, color);
        }
        String text = energy.stored() + " / " + energy.capacity();
        graphics.drawString(mc.font, text, barX + 104, barY + 2, low ? 0xFFFF5555 : 0xFF7CFF8A, true);

        // Construct selector.
        List<Construct> all = ConstructRegistry.all();
        Construct selected = ConstructRegistry.selected(ring);
        int iconsY = y + 17;
        for (int i = 0; i < all.size(); i++) {
            Construct construct = all.get(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = construct == selected;
            int tint = isSelected ? 0xFFFFFFFF : 0x90FFFFFF;
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONSTRUCTS, ix, iconsY, construct.iconIndex() * 16, 0, 16, 16, ICON_TEX_W, ICON_TEX_H, tint);
            if (isSelected) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, CONSTRUCTS, ix, iconsY, 80, 0, 16, 16, ICON_TEX_W, ICON_TEX_H);
            }
        }
        Component name = selected.name();
        graphics.drawString(mc.font, name, x + 19, iconsY + 19, 0xFFB8FFC4, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 30, 0xFFA0A0A0, true);
        }
    }
}
