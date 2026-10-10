package com.danrod505.greenlantern.client.render.speed;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.entity.SpeedTornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Flash's tornado: a funnel of wind that widens upwards, made of stacked rings whose streaky
 * texture scrolls around the axis (faster near the ground, so the walls look sheared), wrapped
 * in a few golden lightning streaks of the Speed Force.
 */
public class TornadoRenderer extends EntityRenderer<SpeedTornadoEntity, TornadoRenderer.State> {
    private static final Identifier WIND = GreenLantern.id("textures/entity/speed_wind.png");
    private static final Identifier TRAIL = GreenLantern.id("textures/entity/speed_trail.png");
    private static final int RINGS = 12;
    private static final int SEGMENTS = 28;

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected boolean affectedByCulling(SpeedTornadoEntity entity) {
        return false;
    }

    @Override
    public void extractRenderState(SpeedTornadoEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.strength = entity.strength(partialTick);
        state.collapsing = entity.isCollapsing();
    }

    @Override
    protected int getBlockLightLevel(SpeedTornadoEntity entity, BlockPos pos) {
        return 15;
    }

    /** Funnel radius at a height (0-1 of the full height). */
    private static float radius(float h, float strength) {
        return (0.7F + 4.6F * h * h + 0.8F * h) * (0.35F + 0.65F * strength);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float strength = state.strength;
        if (strength <= 0.01F) return;
        float t = state.ageInTicks;
        float height = (float) SpeedTornadoEntity.HEIGHT * (0.4F + 0.6F * strength);
        collector.submitCustomGeometry(poseStack, HardLight.type(WIND), (pose, vc) -> {
            // Outer dusty wall and a brighter, faster inner wall.
            funnel(vc, pose, t, height, strength, 1.0F, 0.09F, 0xD8D2C8, 0.55F);
            funnel(vc, pose, t * 1.6F, height, strength, 0.72F, 0.13F, 0xFFE6B0, 0.4F);
            funnel(vc, pose, t * 2.3F, height * 0.8F, strength, 0.42F, 0.2F, 0xFFC860, 0.3F);
        });
        collector.submitCustomGeometry(poseStack, HardLight.type(TRAIL), (pose, vc) -> {
            for (int i = 0; i < 4; i++) streak(vc, pose, t, height, strength, i);
        });
        super.submit(state, poseStack, collector, camera);
    }

    private static void funnel(VertexConsumer vc, PoseStack.Pose pose, float t, float height, float strength, float scale, float spin, int rgb, float alpha) {
        for (int ring = 0; ring < RINGS; ring++) {
            float h0 = (float) ring / RINGS;
            float h1 = (float) (ring + 1) / RINGS;
            float y0 = h0 * height;
            float y1 = h1 * height;
            // Wobble: the funnel snakes a little.
            float wx0 = Mth.sin(t * 0.07F + h0 * 3.0F) * 0.4F * h0;
            float wz0 = Mth.cos(t * 0.06F + h0 * 2.6F) * 0.4F * h0;
            float wx1 = Mth.sin(t * 0.07F + h1 * 3.0F) * 0.4F * h1;
            float wz1 = Mth.cos(t * 0.06F + h1 * 2.6F) * 0.4F * h1;
            float r0 = radius(h0, strength) * scale;
            float r1 = radius(h1, strength) * scale;
            // Lower rings spin faster: shear.
            float scroll0 = t * spin * (1.6F - h0);
            float scroll1 = t * spin * (1.6F - h1);
            float fade0 = edgeFade(h0) * alpha * strength;
            float fade1 = edgeFade(h1) * alpha * strength;
            int c0 = argb(rgb, fade0);
            int c1 = argb(rgb, fade1);
            for (int s = 0; s < SEGMENTS; s++) {
                float a0 = Mth.TWO_PI * s / SEGMENTS;
                float a1 = Mth.TWO_PI * (s + 1) / SEGMENTS;
                float u0 = (float) s / SEGMENTS * 3.0F;
                float u1 = (float) (s + 1) / SEGMENTS * 3.0F;
                float x00 = wx0 + Mth.cos(a0) * r0, z00 = wz0 + Mth.sin(a0) * r0;
                float x01 = wx0 + Mth.cos(a1) * r0, z01 = wz0 + Mth.sin(a1) * r0;
                float x10 = wx1 + Mth.cos(a0) * r1, z10 = wz1 + Mth.sin(a0) * r1;
                float x11 = wx1 + Mth.cos(a1) * r1, z11 = wz1 + Mth.sin(a1) * r1;
                float nx = Mth.cos((a0 + a1) * 0.5F);
                float nz = Mth.sin((a0 + a1) * 0.5F);
                // Front faces.
                HardLight.vertex(vc, pose, x00, y0, z00, u0 + scroll0, h0, c0, nx, 0, nz);
                HardLight.vertex(vc, pose, x10, y1, z10, u0 + scroll1, h1, c1, nx, 0, nz);
                HardLight.vertex(vc, pose, x11, y1, z11, u1 + scroll1, h1, c1, nx, 0, nz);
                HardLight.vertex(vc, pose, x01, y0, z01, u1 + scroll0, h0, c0, nx, 0, nz);
                // Back faces, so the inside of the funnel shows too.
                HardLight.vertex(vc, pose, x01, y0, z01, u1 + scroll0, h0, c0, -nx, 0, -nz);
                HardLight.vertex(vc, pose, x11, y1, z11, u1 + scroll1, h1, c1, -nx, 0, -nz);
                HardLight.vertex(vc, pose, x10, y1, z10, u0 + scroll1, h1, c1, -nx, 0, -nz);
                HardLight.vertex(vc, pose, x00, y0, z00, u0 + scroll0, h0, c0, -nx, 0, -nz);
            }
        }
    }

    /** A golden lightning streak spiralling up the funnel (cross-shaped ribbon). */
    private static void streak(VertexConsumer vc, PoseStack.Pose pose, float t, float height, float strength, int index) {
        int steps = 18;
        float phase = index * 1.7F + t * 0.45F;
        // Re-strike the jitter every 2 ticks.
        int strike = (int) (t / 2) * 31 + index * 7;
        float w = 0.07F;
        int color = argb(0xFFE070, 0.85F * strength);
        for (int i = 0; i < steps; i++) {
            float h0 = (float) i / steps;
            float h1 = (float) (i + 1) / steps;
            float[] p0 = streakPoint(h0, phase, height, strength, strike + i);
            float[] p1 = streakPoint(h1, phase, height, strength, strike + i + 1);
            HardLight.vertex(vc, pose, p0[0] - w, p0[1], p0[2], 0, 0, color, 0, 1, 0);
            HardLight.vertex(vc, pose, p1[0] - w, p1[1], p1[2], 0, 1, color, 0, 1, 0);
            HardLight.vertex(vc, pose, p1[0] + w, p1[1], p1[2], 1, 1, color, 0, 1, 0);
            HardLight.vertex(vc, pose, p0[0] + w, p0[1], p0[2], 1, 0, color, 0, 1, 0);
            HardLight.vertex(vc, pose, p0[0], p0[1], p0[2] - w, 0, 0, color, 1, 0, 0);
            HardLight.vertex(vc, pose, p1[0], p1[1], p1[2] - w, 0, 1, color, 1, 0, 0);
            HardLight.vertex(vc, pose, p1[0], p1[1], p1[2] + w, 1, 1, color, 1, 0, 0);
            HardLight.vertex(vc, pose, p0[0], p0[1], p0[2] + w, 1, 0, color, 1, 0, 0);
        }
    }

    private static float[] streakPoint(float h, float phase, float height, float strength, int seed) {
        float a = phase + h * 9.0F;
        float r = radius(h, strength) * 0.85F;
        float j = ((seed * 1103515245 + 12345) >>> 16 & 0xFF) / 255.0F - 0.5F;
        float k = ((seed * 214013 + 2531011) >>> 16 & 0xFF) / 255.0F - 0.5F;
        return new float[] {Mth.cos(a) * r + j * 0.5F, h * height + k * 0.3F, Mth.sin(a) * r + k * 0.5F};
    }

    private static float edgeFade(float h) {
        return Mth.clamp(h * 6.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - h) * 3.0F, 0.0F, 1.0F) * 0.6F + 0.4F * Mth.clamp((1.0F - h) * 3.0F, 0.0F, 1.0F);
    }

    private static int argb(int rgb, float alpha) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | (rgb & 0xFFFFFF);
    }

    public static class State extends EntityRenderState {
        public float strength;
        public boolean collapsing;
    }
}
