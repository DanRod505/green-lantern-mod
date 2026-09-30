package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.GreenLantern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Geometry helpers used by every construct renderer. Constructs are drawn with a translucent,
 * emissive render type so they always glow, even in complete darkness.
 */
public final class HardLight {
    public static final Identifier PANEL = GreenLantern.id("textures/entity/construct/hard_light.png");
    public static final Identifier PANEL_SOLID = GreenLantern.id("textures/entity/construct/hard_light_solid.png");
    public static final Identifier CORE = GreenLantern.id("textures/entity/construct/energy_core.png");
    public static final Identifier BUBBLE = GreenLantern.id("textures/entity/construct/bubble.png");
    public static final Identifier SAW_BLADE = GreenLantern.id("textures/entity/construct/saw_blade.png");

    public static final int LIGHT = LightTexture.FULL_BRIGHT;

    private HardLight() {}

    public static RenderType type(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture, false);
    }

    /** ARGB colour of hard light with the given opacity (0-1) and brightness boost (0-1). */
    public static int color(float alpha, float boost) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        int r = Mth.clamp((int) (35 + 170 * boost), 0, 255);
        int g = 255;
        int b = Mth.clamp((int) (70 + 150 * boost), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    /** Textured quad (corners in counter-clockwise order). */
    public static void quad(VertexConsumer vc, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float x2, float y2, float z2, float x3, float y3, float z3,
                            int color, float nx, float ny, float nz) {
        vertex(vc, pose, x0, y0, z0, 0, 1, color, nx, ny, nz);
        vertex(vc, pose, x1, y1, z1, 1, 1, color, nx, ny, nz);
        vertex(vc, pose, x2, y2, z2, 1, 0, color, nx, ny, nz);
        vertex(vc, pose, x3, y3, z3, 0, 0, color, nx, ny, nz);
    }

    /** Axis-aligned box; every face shows the full panel texture (bright border, soft center). */
    public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        // -Z
        quad(vc, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, color, 0, 0, -1);
        // +Z
        quad(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color, 0, 0, 1);
        // -X
        quad(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color, -1, 0, 0);
        // +X
        quad(vc, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, color, 1, 0, 0);
        // -Y
        quad(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color, 0, -1, 0);
        // +Y
        quad(vc, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, color, 0, 1, 0);
    }

    /** Box centered on the origin of the current pose. */
    public static void centeredBox(VertexConsumer vc, PoseStack.Pose pose, float hx, float hy, float hz, int color) {
        box(vc, pose, -hx, -hy, -hz, hx, hy, hz, color);
    }

    /** Box plus a slightly larger, fainter shell that gives constructs their glow. */
    public static void glowingBox(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float alpha) {
        box(vc, pose, x0, y0, z0, x1, y1, z1, color(0.85F * alpha, 0.3F));
        float g = 0.06F;
        box(vc, pose, x0 - g, y0 - g, z0 - g, x1 + g, y1 + g, z1 + g, color(0.22F * alpha, 0.0F));
    }

    /** Nearly opaque box with a faint glow shell, for small constructs. */
    public static void solidBox(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float alpha) {
        box(vc, pose, x0, y0, z0, x1, y1, z1, color(alpha, 0.1F));
        float g = 0.04F;
        box(vc, pose, x0 - g, y0 - g, z0 - g, x1 + g, y1 + g, z1 + g, color(0.18F * alpha, 0.0F));
    }

    /** UV sphere centered on the origin. */
    public static void sphere(VertexConsumer vc, PoseStack.Pose pose, float radius, int stacks, int slices, int color) {
        for (int i = 0; i < stacks; i++) {
            float v0 = (float) i / stacks;
            float v1 = (float) (i + 1) / stacks;
            float phi0 = Mth.PI * v0 - Mth.HALF_PI;
            float phi1 = Mth.PI * v1 - Mth.HALF_PI;
            for (int j = 0; j < slices; j++) {
                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;
                float th0 = Mth.TWO_PI * u0;
                float th1 = Mth.TWO_PI * u1;
                spherePoint(vc, pose, radius, phi0, th0, u0, v0, color);
                spherePoint(vc, pose, radius, phi0, th1, u1, v0, color);
                spherePoint(vc, pose, radius, phi1, th1, u1, v1, color);
                spherePoint(vc, pose, radius, phi1, th0, u0, v1, color);
            }
        }
    }

    private static void spherePoint(VertexConsumer vc, PoseStack.Pose pose, float r, float phi, float theta, float u, float v, int color) {
        float cp = Mth.cos(phi);
        float nx = cp * Mth.cos(theta);
        float ny = Mth.sin(phi);
        float nz = cp * Mth.sin(theta);
        vertex(vc, pose, nx * r, ny * r, nz * r, u, v, color, nx, ny, nz);
    }

    /** Camera-facing square of the given half size (call after applying the camera orientation). */
    public static void billboard(VertexConsumer vc, PoseStack.Pose pose, float half, int color) {
        quad(vc, pose, -half, -half, 0, half, -half, 0, half, half, 0, -half, half, 0, color, 0, 0, 1);
    }
}
