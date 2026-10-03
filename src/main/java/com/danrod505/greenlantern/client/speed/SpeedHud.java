package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.GLClientConfig;
import com.danrod505.greenlantern.flash.PhaseState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Running HUD: red and gold speed lines that close in as speed builds up, a flash at the sound
 * barrier, the speedometer (km/h, scaled so the sound barrier reads as the real 1235 km/h) and a
 * shimmering frame while the molecules vibrate.
 */
public final class SpeedHud {
    private static final RandomSource RANDOM = RandomSource.create();
    /** km/h shown per block/tick of speed: 2.4 blocks/tick (the sound barrier) reads as 1235 km/h. */
    private static final float KMH_PER_SPEED = 1235.0F / 2.4F;
    private static float shownKmh;

    private SpeedHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        if (PhaseState.isPhasing(mc.player)) phaseFrame(graphics, mc, w, h);
        boolean running = SpeedController.isRunning();
        float mach = SpeedController.mach();
        shownKmh += ((float) SpeedController.speed() * KMH_PER_SPEED - shownKmh) * 0.3F;
        if (!running && shownKmh < 5) {
            shownKmh = 0;
            return;
        }
        if (GLClientConfig.SPEED_LINES.get()) {
            speedLines(graphics, w, h, Mth.clamp((mach - 0.2F) / 0.9F, 0.0F, 1.0F), mc.player.tickCount);
        }
        int flash = SpeedController.boomFlash();
        if (flash > 0) {
            int a = (int) (140 * flash / 6.0F);
            graphics.fill(0, 0, w, h, a << 24 | 0xFFF0C0);
        }
        speedometer(graphics, mc, w, h, mach);
    }

    private static void speedLines(GuiGraphics graphics, int w, int h, float intensity, int tick) {
        if (intensity <= 0.01F) return;
        float cx = w / 2.0F;
        float cy = h / 2.0F;
        float radius = (float) Math.sqrt(cx * cx + cy * cy);
        int count = (int) (18 + 50 * intensity);
        RANDOM.setSeed(tick * 341873128712L + 77);
        for (int i = 0; i < count; i++) {
            float angle = RANDOM.nextFloat() * Mth.TWO_PI;
            float start = radius * (0.4F + 0.4F * RANDOM.nextFloat() * (1.1F - intensity));
            float length = radius * (0.2F + 0.4F * RANDOM.nextFloat()) * (0.5F + intensity);
            int alpha = (int) ((40 + 100 * RANDOM.nextFloat()) * intensity);
            float pick = RANDOM.nextFloat();
            int rgb = pick < 0.35F ? 0xFFD040 : pick < 0.6F ? 0xFF5A28 : 0xFFF4E0;
            float thickness = 0.6F + 1.4F * RANDOM.nextFloat() * intensity;
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx, cy);
            graphics.pose().rotate(angle);
            graphics.pose().translate(start, 0);
            graphics.pose().scale(length, thickness);
            graphics.fill(0, 0, 1, 1, alpha << 24 | rgb);
            graphics.pose().popMatrix();
        }
        if (SpeedController.isSupersonic()) {
            // Crackling Speed Force at the edges of the screen.
            for (int i = 0; i < 6; i++) {
                boolean vertical = RANDOM.nextBoolean();
                int x = vertical ? (RANDOM.nextBoolean() ? 0 : w - 3) : RANDOM.nextInt(w);
                int y = vertical ? RANDOM.nextInt(h) : (RANDOM.nextBoolean() ? 0 : h - 3);
                int len = 10 + RANDOM.nextInt(30);
                graphics.fill(x, y, vertical ? x + 3 : x + len, vertical ? y + len : y + 3, 0xB0FFD84A);
            }
        }
    }

    private static void speedometer(GuiGraphics graphics, Minecraft mc, int w, int h, float mach) {
        int y = h - 78;
        int kmh = Math.round(shownKmh);
        boolean supersonic = SpeedController.isSupersonic();
        graphics.pose().pushMatrix();
        graphics.pose().translate(w / 2.0F, y - 14);
        graphics.pose().scale(1.6F, 1.6F);
        graphics.drawCenteredString(mc.font, Component.literal(kmh + " KM/H"), 0, 0, supersonic ? 0xFFFFF0B0 : 0xFFFFC040);
        graphics.pose().popMatrix();
        String tier = mach >= 1.5F ? "hypersonic" : supersonic ? "supersonic" : mach >= 0.5F ? "superspeed" : "running";
        int barW = 130;
        int x = (w - barW) / 2;
        float max = 1.8F;
        graphics.fill(x - 1, y - 1, x + barW + 1, y + 5, 0x90000000);
        int fill = (int) (barW * Mth.clamp(mach / max, 0, 1));
        // Gradient red -> gold as speed builds up.
        for (int i = 0; i < fill; i += 2) {
            float t = (float) i / barW;
            int r = 255;
            int g = (int) (40 + 200 * t);
            int b = (int) (20 + 60 * t);
            graphics.fill(x + i, y, Math.min(x + fill, x + i + 2), y + 4, 0xFF000000 | r << 16 | g << 8 | b);
        }
        int mark = x + (int) (barW / max);
        graphics.fill(mark, y - 3, mark + 1, y + 7, 0xFFFFFFFF);
        graphics.drawCenteredString(mc.font, Component.translatable("hud.greenlantern.speed." + tier).append(String.format(" · MACH %.2f", mach)),
                w / 2, y + 8, supersonic ? 0xFFFFE6A0 : 0xFFFFB070);
    }

    private static void phaseFrame(GuiGraphics graphics, Minecraft mc, int w, int h) {
        int tick = mc.player.tickCount;
        RANDOM.setSeed(tick * 9137L);
        int depth = Math.max(10, h / 8);
        for (int i = 0; i < 8; i++) {
            float pulse = 0.6F + 0.4F * RANDOM.nextFloat();
            int a = (int) (60 * pulse * (1.0F - i / 8.0F));
            int color = a << 24 | 0xFFE070;
            int d = depth * i / 8;
            int d2 = depth * (i + 1) / 8;
            int jitter = RANDOM.nextInt(3) - 1;
            graphics.fill(0, d + jitter, w, d2 + jitter, color);
            graphics.fill(0, h - d2 - jitter, w, h - d - jitter, color);
            graphics.fill(d + jitter, 0, d2 + jitter, h, color);
            graphics.fill(w - d2 - jitter, 0, w - d - jitter, h, color);
        }
        int offset = RANDOM.nextInt(3) - 1;
        graphics.drawCenteredString(mc.font, Component.translatable("hud.greenlantern.phasing"), w / 2 + offset, 24, 0xFFFFE680);
    }
}
