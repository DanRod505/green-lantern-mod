package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.ring.LanternHero;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Anti-aliased textures of the construct wheel, generated at runtime for the current number of
 * constructs (so new constructs automatically get their own slice).
 * <p>
 * Radii are normalized: 1.0 is the outer edge of a slice; the textures extend to {@link #EXTENT}
 * so a highlighted slice can grow. Slice 0 is centered straight up; other slices are drawn by
 * rotating the slice textures.
 */
public final class WheelTextures {
    public static final int SIZE = 448;
    public static final float EXTENT = 1.12F;
    public static final float INNER = 0.44F;
    private static final float GAP_PIXELS = 2.2F;
    private static final int SAMPLES = 2;

    public static final Identifier BASE = LanternHero.WHEEL_THEME.base();
    public static final Identifier HIGHLIGHT = LanternHero.WHEEL_THEME.highlight();
    public static final Identifier CURRENT = LanternHero.WHEEL_THEME.currentLayer();

    private static final java.util.Map<String, Integer> GENERATED = new java.util.HashMap<>();

    private WheelTextures() {}

    public static void ensure(int slices) {
        ensure(LanternHero.WHEEL_THEME, slices);
    }

    public static void ensure(WheelTheme theme, int slices) {
        Integer done = GENERATED.get(theme.name());
        if (done != null && done == slices) return;
        GENERATED.put(theme.name(), slices);
        register(theme.base(), slices, Layer.BASE, theme);
        register(theme.highlight(), slices, Layer.HIGHLIGHT, theme);
        register(theme.currentLayer(), slices, Layer.CURRENT, theme);
    }

    private enum Layer { BASE, HIGHLIGHT, CURRENT }

    private static void register(Identifier id, int slices, Layer layer, WheelTheme theme) {
        NativeImage image = new NativeImage(SIZE, SIZE, true);
        float half = SIZE / 2.0F;
        float pixelsPerUnit = half / EXTENT;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float a = 0, r = 0, g = 0, b = 0;
                for (int sy = 0; sy < SAMPLES; sy++) {
                    for (int sx = 0; sx < SAMPLES; sx++) {
                        float px = (x + (sx + 0.5F) / SAMPLES - half) / pixelsPerUnit;
                        float py = (y + (sy + 0.5F) / SAMPLES - half) / pixelsPerUnit;
                        int c = sample(px, py, slices, layer, pixelsPerUnit, theme);
                        float ca = (c >>> 24) / 255.0F;
                        a += ca;
                        r += ((c >> 16) & 0xFF) * ca;
                        g += ((c >> 8) & 0xFF) * ca;
                        b += (c & 0xFF) * ca;
                    }
                }
                int n = SAMPLES * SAMPLES;
                int color = 0;
                if (a > 0) {
                    color = (int) (a / n * 255) << 24 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
                }
                image.setPixel(x, y, color);
            }
        }
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
    }

    /** Colour (ARGB) of the wheel at normalized position (x, y). */
    private static int sample(float x, float y, int slices, Layer layer, float pixelsPerUnit, WheelTheme theme) {
        float radius = Mth.sqrt(x * x + y * y);
        // Angle from straight up, clockwise, in [-180, 180).
        float angle = (float) Math.toDegrees(Math.atan2(y, x)) + 90.0F;
        float sliceSize = 360.0F / slices;
        float local = Mth.wrapDegrees(angle - Math.round(angle / sliceSize) * sliceSize);
        int index = Math.floorMod(Math.round(angle / sliceSize), slices);
        // Distance (in pixels) to the slice boundary: the gap between slices.
        float edgeDistance = (sliceSize / 2 - Math.abs(local)) * Mth.DEG_TO_RAD * radius * pixelsPerUnit;

        switch (layer) {
            case BASE -> {
                if (radius < INNER - 0.035F) {
                    float t = radius / INNER;
                    return argb(0.82F, mix(theme.centerInner(), theme.centerOuter(), t));
                }
                if (radius < INNER - 0.015F) return argb(0.95F, theme.centerRing());
                if (radius < INNER || radius > 1.0F || edgeDistance < GAP_PIXELS) return 0;
                if (radius > 0.975F) return argb(0.75F, theme.border());
                if (radius < INNER + 0.012F) return argb(0.6F, theme.border());
                float t = (radius - INNER) / (1.0F - INNER);
                return argb(0.72F + 0.1F * t, mix(theme.sliceInner(), theme.sliceOuter(), t));
            }
            case HIGHLIGHT -> {
                if (index != 0 || radius < INNER || radius > 1.0F || edgeDistance < GAP_PIXELS) return 0;
                float t = (radius - INNER) / (1.0F - INNER);
                if (radius > 0.965F) return argb(1.0F, theme.highlightRim());
                if (edgeDistance < GAP_PIXELS + 1.5F) return argb(0.9F, theme.highlightEdge());
                return argb(0.78F + 0.15F * t, mix(theme.highlightInner(), theme.highlightOuter(), t * t));
            }
            case CURRENT -> {
                if (index != 0 || radius < 0.95F || radius > 1.0F || edgeDistance < GAP_PIXELS) return 0;
                return argb(1.0F, theme.current());
            }
        }
        return 0;
    }

    private static int mix(int c0, int c1, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (c0 >> 16) & 0xFF, (c1 >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (c0 >> 8) & 0xFF, (c1 >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, c0 & 0xFF, c1 & 0xFF);
        return r << 16 | g << 8 | b;
    }

    private static int argb(float alpha, int rgb) {
        return Mth.clamp((int) (alpha * 255), 0, 255) << 24 | rgb;
    }
}
