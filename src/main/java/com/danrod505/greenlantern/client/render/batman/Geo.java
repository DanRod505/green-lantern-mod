package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.GreenLantern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Lit, vertex-coloured geometry for Batman's gear (the cape, the ears, the Batmobile, the gadgets).
 * Everything is drawn on a near-white, slightly grainy texture and tinted per vertex, so shapes
 * can be built freely in code; the entity shader shades each face from its normal.
 */
public final class Geo {
    /** Near-white texture with a fine grain: the vertex colour gives the actual colour. */
    public static final Identifier PLAIN = GreenLantern.id("textures/entity/batman/plain.png");

    private Geo() {}

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, int light,
                              float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /** Quad through four points (counter-clockwise seen from its front); the normal is computed. */
    public static void quad(VertexConsumer vc, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float x2, float y2, float z2, float x3, float y3, float z3, int color, int light) {
        // Normal from the diagonals (robust for slightly non-planar quads).
        float ax = x2 - x0, ay = y2 - y0, az = z2 - z0;
        float bx = x3 - x1, by = y3 - y1, bz = z3 - z1;
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) {
            nx = 0;
            ny = 1;
            nz = 0;
        } else {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        vertex(vc, pose, x0, y0, z0, 0, 1, color, light, nx, ny, nz);
        vertex(vc, pose, x1, y1, z1, 1, 1, color, light, nx, ny, nz);
        vertex(vc, pose, x2, y2, z2, 1, 0, color, light, nx, ny, nz);
        vertex(vc, pose, x3, y3, z3, 0, 0, color, light, nx, ny, nz);
    }

    /** The same quad, seen from both sides (front colour and back colour). */
    public static void doubleQuad(VertexConsumer vc, PoseStack.Pose pose,
                                  float x0, float y0, float z0, float x1, float y1, float z1,
                                  float x2, float y2, float z2, float x3, float y3, float z3, int front, int back, int light) {
        quad(vc, pose, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, front, light);
        quad(vc, pose, x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0, back, light);
    }

    /** Axis-aligned box. */
    public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
        box(vc, pose, x0, y0, z0, x1, y1, z1, color, color, light);
    }

    /** Axis-aligned box with a different colour on top (lighter panels, a darker underside...). */
    public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                           int sides, int top, int light) {
        // -Z
        quad(vc, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, sides, light);
        // +Z
        quad(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, sides, light);
        // -X
        quad(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, sides, light);
        // +X
        quad(vc, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, sides, light);
        // -Y
        quad(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, sides, light);
        // +Y
        quad(vc, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, top, light);
    }

    /**
     * Box whose top face is narrower than its bottom (a wedge, for noses, fenders and canopies):
     * the top spans [tx0, tx1] x [tz0, tz1].
     */
    public static void taper(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float tx0, float tz0, float tx1, float tz1, int sides, int top, int light) {
        quad(vc, pose, x1, y0, z0, x0, y0, z0, tx0, y1, tz0, tx1, y1, tz0, sides, light);
        quad(vc, pose, x0, y0, z1, x1, y0, z1, tx1, y1, tz1, tx0, y1, tz1, sides, light);
        quad(vc, pose, x0, y0, z0, x0, y0, z1, tx0, y1, tz1, tx0, y1, tz0, sides, light);
        quad(vc, pose, x1, y0, z1, x1, y0, z0, tx1, y1, tz0, tx1, y1, tz1, sides, light);
        quad(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, sides, light);
        quad(vc, pose, tx0, y1, tz1, tx1, y1, tz1, tx1, y1, tz0, tx0, y1, tz0, top, light);
    }

    /**
     * Cylinder along the X axis (a wheel): centered on (cx, cy, cz), {@code sides} faces, turned by
     * {@code angle} radians. The caps get their own colour (the rim) and a lighter hub.
     */
    public static void wheel(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float radius, float halfWidth, int sides,
                             float angle, int tire, int rim, int hub, int light) {
        float x0 = cx - halfWidth;
        float x1 = cx + halfWidth;
        for (int i = 0; i < sides; i++) {
            float a0 = angle + i * Mth.TWO_PI / sides;
            float a1 = angle + (i + 1) * Mth.TWO_PI / sides;
            float y0 = cy + Mth.sin(a0) * radius, z0 = cz + Mth.cos(a0) * radius;
            float y1 = cy + Mth.sin(a1) * radius, z1 = cz + Mth.cos(a1) * radius;
            // Tread (alternating shade: you can see it roll).
            quad(vc, pose, x0, y0, z0, x1, y0, z0, x1, y1, z1, x0, y1, z1, i % 2 == 0 ? tire : darken(tire, 0.75F), light);
            // Caps: rim ring and hub.
            float ri = radius * 0.62F;
            float yi0 = cy + Mth.sin(a0) * ri, zi0 = cz + Mth.cos(a0) * ri;
            float yi1 = cy + Mth.sin(a1) * ri, zi1 = cz + Mth.cos(a1) * ri;
            float rh = radius * 0.22F;
            float yh0 = cy + Mth.sin(a0) * rh, zh0 = cz + Mth.cos(a0) * rh;
            float yh1 = cy + Mth.sin(a1) * rh, zh1 = cz + Mth.cos(a1) * rh;
            int spoke = i % 2 == 0 ? rim : darken(rim, 0.55F);
            // +X cap
            quad(vc, pose, x1 + 0.001F, y0, z0, x1 + 0.001F, yi0, zi0, x1 + 0.001F, yi1, zi1, x1 + 0.001F, y1, z1, tire, light);
            quad(vc, pose, x1 + 0.02F, yi0, zi0, x1 + 0.02F, yh0, zh0, x1 + 0.02F, yh1, zh1, x1 + 0.02F, yi1, zi1, spoke, light);
            quad(vc, pose, x1 + 0.03F, yh0, zh0, x1 + 0.03F, cy, cz, x1 + 0.03F, cy, cz, x1 + 0.03F, yh1, zh1, hub, light);
            // -X cap
            quad(vc, pose, x0 - 0.001F, y1, z1, x0 - 0.001F, yi1, zi1, x0 - 0.001F, yi0, zi0, x0 - 0.001F, y0, z0, tire, light);
            quad(vc, pose, x0 - 0.02F, yi1, zi1, x0 - 0.02F, yh1, zh1, x0 - 0.02F, yh0, zh0, x0 - 0.02F, yi0, zi0, spoke, light);
            quad(vc, pose, x0 - 0.03F, yh1, zh1, x0 - 0.03F, cy, cz, x0 - 0.03F, cy, cz, x0 - 0.03F, yh0, zh0, hub, light);
        }
    }

    public static int darken(int argb, float f) {
        int a = argb >>> 24;
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int g = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int rgb, float alpha) {
        return Mth.clamp((int) (alpha * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }
}
