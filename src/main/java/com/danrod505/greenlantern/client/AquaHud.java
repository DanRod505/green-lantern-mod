package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.aquaman.AquaPower;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.entity.KrakenEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** HUD of the Atlantean Emblem: the Power of the Seas bar (a rolling wave) and the selected power. */
public final class AquaHud {
    private static float displayedFraction = -1.0F;

    private AquaHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack emblem = AquamanHelper.findEmblem(player);
        if (emblem.isEmpty()) {
            displayedFraction = -1.0F;
            return;
        }
        boolean held = !AquamanHelper.heldEmblem(player).isEmpty();
        boolean suited = AquamanHelper.isSuited(player);
        if (!held && !suited) return;

        RingEnergy force = AquamanHero.SEA_FORCE.get(emblem);
        float fraction = force.fraction();
        if (displayedFraction < 0) displayedFraction = fraction;
        displayedFraction += (fraction - displayedFraction) * 0.15F;

        int x = 8;
        // Below the other heroes' HUDs when those are showing too.
        int y = 8;
        if (!RingHelper.findRing(player).isEmpty() && (!RingHelper.heldRing(player).isEmpty() || RingHelper.isSuited(player))) y += 58;
        if (!FlashHelper.findRing(player).isEmpty() && (!FlashHelper.heldRing(player).isEmpty() || FlashHelper.isSuited(player))) y += 58;

        // Emblem: the golden "A" (last icon of the strip).
        int emblemIcon = AquaPower.POWERS.count();
        int iconsW = AquamanHero.WHEEL.iconsWidth();
        graphics.blit(RenderPipelines.GUI_TEXTURED, AquamanHero.WHEEL.icons(), x, y - 2, emblemIcon * 16, 0, 16, 16, iconsW, WheelStyle.ICONS_HEIGHT);
        int barX = x + 19;
        int barY = y + 1;
        int border = 0xFFC8A02A;
        graphics.fill(barX, barY, barX + 102, barY + 12, 0xC0021418);
        graphics.fill(barX, barY, barX + 102, barY + 1, border);
        graphics.fill(barX, barY + 11, barX + 102, barY + 12, border);
        graphics.fill(barX, barY, barX + 1, barY + 12, border);
        graphics.fill(barX + 101, barY, barX + 102, barY + 12, border);
        int fill = Math.round(100 * displayedFraction);
        boolean low = fraction < 0.2F;
        int tick = player.tickCount;
        for (int i = 0; i < fill; i++) {
            // A rolling wave: deep teal with lighter crests, the top edge rising and falling.
            float wave = 0.5F + 0.5F * Mth.sin((i + tick * 1.5F) * 0.2F);
            int top = barY + 2 + Math.round(1.5F * (1.0F - wave));
            int g = (int) (140 + 80 * wave);
            int b = (int) (150 + 70 * wave);
            int alpha = low ? (int) (170 + 85 * Mth.sin(tick * 0.4F)) : 255;
            graphics.fill(barX + 1 + i, top, barX + 2 + i, barY + 10, Mth.clamp(alpha, 0, 255) << 24 | 0x18 << 16 | g << 8 | b);
            if (wave > 0.85F) graphics.fill(barX + 1 + i, top, barX + 2 + i, top + 1, 0xFFE0FFFA);
        }
        String text = force.stored() + " / " + force.capacity();
        graphics.drawString(mc.font, text, barX + 104, barY + 2, low ? 0xFFFF5555 : 0xFFF2C94A, true);

        AquaPower selected = AquaPower.POWERS.selected(emblem);
        int iconsY = y + 17;
        for (int i = 0; i < AquaPower.POWERS.count(); i++) {
            AquaPower power = AquaPower.POWERS.byIndex(i);
            int ix = x + 19 + i * 20;
            boolean isSelected = power == selected;
            if (isSelected) graphics.fill(ix - 1, iconsY - 1, ix + 17, iconsY + 17, 0xC03CE0D0);
            graphics.blit(RenderPipelines.GUI_TEXTURED, AquamanHero.WHEEL.icons(), ix, iconsY, power.iconIndex() * 16, 0, 16, 16,
                    iconsW, WheelStyle.ICONS_HEIGHT, isSelected ? 0xFFFFFFFF : 0x90FFFFFF);
        }
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.power_key", selected.displayName(), KeyBindings.HERO_POWER.getTranslatedKeyMessage()),
                x + 19, iconsY + 19, 0xFF8FF0E4, true);
        if (!suited) {
            graphics.drawString(mc.font, Component.translatable("hud.greenlantern.aquaman_suit_hint", KeyBindings.TOGGLE_UNIFORM.getTranslatedKeyMessage()),
                    x + 19, iconsY + 30, 0xFFA0A0A0, true);
        } else {
            renderKraken(graphics, mc, player, x + 19, iconsY + 32);
        }
    }

    /** The life of the Kraken Aquaman rides (or that waits for him nearby). */
    private static void renderKraken(GuiGraphics graphics, Minecraft mc, Player player, int x, int y) {
        KrakenEntity kraken = player.getVehicle() instanceof KrakenEntity ridden ? ridden : null;
        if (kraken == null) {
            List<KrakenEntity> near = player.level().getEntitiesOfClass(KrakenEntity.class, player.getBoundingBox().inflate(48),
                    k -> k.isOwnedBy(player) && !k.isRemoved());
            if (near.isEmpty()) return;
            kraken = near.getFirst();
        }
        float max = KrakenEntity.maxHealth();
        float fraction = Mth.clamp(kraken.getHealth() / max, 0.0F, 1.0F);
        graphics.drawString(mc.font, Component.translatable("hud.greenlantern.kraken"), x, y + 1, 0xFFF2A0A0, true);
        int barX = x + mc.font.width(Component.translatable("hud.greenlantern.kraken")) + 4;
        int width = 82;
        int border = 0xFF8A1C30;
        graphics.fill(barX, y, barX + width + 2, y + 9, 0xC0180408);
        graphics.fill(barX, y, barX + width + 2, y + 1, border);
        graphics.fill(barX, y + 8, barX + width + 2, y + 9, border);
        graphics.fill(barX, y, barX + 1, y + 9, border);
        graphics.fill(barX + width + 1, y, barX + width + 2, y + 9, border);
        int fill = Math.round(width * fraction);
        boolean low = fraction < 0.25F;
        int tick = player.tickCount;
        for (int i = 0; i < fill; i++) {
            float wave = 0.5F + 0.5F * Mth.sin((i + tick * 0.8F) * 0.35F);
            int r = (int) (170 + 60 * wave);
            int g = (int) (30 + 30 * wave);
            int alpha = low ? (int) (170 + 85 * Mth.sin(tick * 0.5F)) : 255;
            graphics.fill(barX + 1 + i, y + 1, barX + 2 + i, y + 8, Mth.clamp(alpha, 0, 255) << 24 | r << 16 | g << 8 | 0x40);
        }
        String text = Mth.ceil(kraken.getHealth()) + " / " + Mth.ceil(max);
        graphics.drawString(mc.font, text, barX + width + 5, y + 1, low ? 0xFFFF5555 : 0xFFF2C0C0, true);
    }
}
