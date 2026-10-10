package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GLClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Speed lines, supersonic vignette, sound-barrier flash and the Mach meter. */
public final class FlightHud {
    private static final RandomSource RANDOM = RandomSource.create();

    private FlightHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !FlightController.isPowerFlying()) return;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        float mach = FlightController.mach();
        boolean supersonic = FlightController.isSupersonic();

        if (GLClientConfig.SPEED_LINES.get()) {
            speedLines(graphics, w, h, Mth.clamp((mach - 0.3F) / 0.9F, 0.0F, 1.0F), mc.player.tickCount);
            if (supersonic) vignette(graphics, w, h, mc.player.tickCount);
        }
        int flash = FlightController.boomFlash();
        if (flash > 0) {
            int a = (int) (150 * flash / 6.0F);
            graphics.fill(0, 0, w, h, a << 24 | (FlightController.isSuperman() ? 0xF0F4FF : 0xD8FFE0));
        }
        machMeter(graphics, mc, w, h, mach, supersonic);
    }

    private static void speedLines(GuiGraphics graphics, int w, int h, float intensity, int tick) {
        if (intensity <= 0.01F) return;
        float cx = w / 2.0F;
        float cy = h / 2.0F;
        float radius = (float) Math.sqrt(cx * cx + cy * cy);
        int count = (int) (14 + 40 * intensity);
        RANDOM.setSeed(tick * 341873128712L);
        for (int i = 0; i < count; i++) {
            float angle = RANDOM.nextFloat() * Mth.TWO_PI;
            float start = radius * (0.45F + 0.35F * RANDOM.nextFloat() * (1.1F - intensity));
            float length = radius * (0.15F + 0.35F * RANDOM.nextFloat()) * (0.5F + intensity);
            int alpha = (int) ((40 + 90 * RANDOM.nextFloat()) * intensity);
            int color = alpha << 24 | (RANDOM.nextFloat() < 0.3F ? (FlightController.isSuperman() ? 0x9BC0FF : 0x9BFFB0) : 0xFFFFFF);
            float thickness = 0.6F + 1.2F * RANDOM.nextFloat() * intensity;
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx, cy);
            graphics.pose().rotate(angle);
            graphics.pose().translate(start, 0);
            graphics.pose().scale(length, thickness);
            graphics.fill(0, 0, 1, 1, color);
            graphics.pose().popMatrix();
        }
    }

    private static void vignette(GuiGraphics graphics, int w, int h, int tick) {
        float pulse = 0.75F + 0.25F * Mth.sin(tick * 0.5F);
        int steps = 10;
        int depth = Math.max(8, h / 6);
        for (int i = 0; i < steps; i++) {
            int a = (int) (70 * pulse * (1.0F - (float) i / steps));
            // Superman's edges alternate blue and red; the Lantern's glow green.
            int color = a << 24 | (FlightController.isSuperman() ? (i % 2 == 0 ? 0x2E6BFF : 0xE8303A) : 0x2CFF5A);
            int d = depth * i / steps;
            int d2 = depth * (i + 1) / steps;
            graphics.fill(0, d, w, d2, color);
            graphics.fill(0, h - d2, w, h - d, color);
            graphics.fill(d, 0, d2, h, color);
            graphics.fill(w - d2, 0, w - d, h, color);
        }
    }

    private static void machMeter(GuiGraphics graphics, Minecraft mc, int w, int h, float mach, boolean supersonic) {
        int barW = 120;
        int x = (w - barW) / 2;
        int y = h - 62;
        String text = String.format("MACH %.2f", mach);
        boolean superman = FlightController.isSuperman();
        int color = superman ? (supersonic ? 0xFFFFE070 : 0xFF8FB4FF) : (supersonic ? 0xFFB6FFC6 : 0xFF6CFF86);
        graphics.pose().pushMatrix();
        graphics.pose().translate(w / 2.0F, y - 12);
        graphics.pose().scale(1.5F, 1.5F);
        graphics.drawCenteredString(mc.font, Component.literal(text), 0, 0, color);
        graphics.pose().popMatrix();
        // Bar from Mach 0 to Mach 1.6 (3 for Superman) with the sound barrier marked.
        float max = superman ? 3.0F : 1.6F;
        graphics.fill(x - 1, y - 1, x + barW + 1, y + 5, 0x90000000);
        int fill = (int) (barW * Mth.clamp(mach / max, 0, 1));
        graphics.fill(x, y, x + fill, y + 4, superman ? (supersonic ? 0xFFFFD040 : 0xFF2E6BFF) : (supersonic ? 0xFFC8FFD2 : 0xFF2EE65A));
        int mark = x + (int) (barW / max);
        graphics.fill(mark, y - 3, mark + 1, y + 7, 0xFFFFFFFF);
        if (supersonic) {
            graphics.drawCenteredString(mc.font, Component.translatable("hud.greenlantern.supersonic"), w / 2, y + 8, superman ? 0xFFFFF0C0 : 0xFFE0FFE6);
        }
    }
}
