package com.danrod505.greenlantern.client.render.speed;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.entity.SpeedLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Thrown Speed Force lightning: a jagged golden bolt that re-strikes every tick, with a hot white core and glow. */
public class SpeedLightningRenderer extends EntityRenderer<SpeedLightningEntity, SpeedLightningRenderer.State> {
    private static final Identifier TRAIL = GreenLantern.id("textures/entity/speed_trail.png");
    private static final Identifier GLOW = GreenLantern.id("textures/entity/speed_glow.png");
    private static final int SEGMENTS = 10;

    public SpeedLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpeedLightningEntity bolt, State state, float partialTick) {
        super.extractRenderState(bolt, state, partialTick);
        Vec3 motion = bolt.getDeltaMovement();
        state.yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        state.pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
        state.length = (float) Math.min(4.5, motion.length() * 1.4 + 0.5);
        state.seed = bolt.getId() * 7919 + bolt.tickCount * 104729;
    }

    @Override
    protected int getBlockLightLevel(SpeedLightningEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        float[][] points = new float[SEGMENTS + 1][];
        for (int i = 0; i <= SEGMENTS; i++) {
            float f = (float) i / SEGMENTS;
            float amp = i == 0 ? 0.0F : 0.12F + 0.2F * f;
            points[i] = new float[] {rand(state.seed, i * 2) * amp, rand(state.seed, i * 2 + 1) * amp, -f * state.length};
        }
        collector.submitCustomGeometry(poseStack, HardLight.type(TRAIL), (pose, vc) -> {
            bolt(vc, pose, points, 0.16F, 0x80FFA000);
            bolt(vc, pose, points, 0.05F, 0xFFFFF6C8);
            // A forked branch.
            int from = 3 + Math.abs(state.seed % 4);
            float[][] fork = new float[4][];
            fork[0] = points[from];
            for (int i = 1; i < 4; i++) {
                fork[i] = new float[] {fork[i - 1][0] + rand(state.seed, 40 + i) * 0.35F, fork[i - 1][1] + rand(state.seed, 50 + i) * 0.35F, fork[i - 1][2] - 0.3F};
            }
            bolt(vc, pose, fork, 0.03F, 0xC0FFE680);
        });
        poseStack.popPose();
        poseStack.mulPose(camera.orientation);
        float pulse = 0.85F + 0.3F * rand(state.seed, 99);
        collector.submitCustomGeometry(poseStack, HardLight.type(GLOW), (pose, vc) -> {
            HardLight.billboard(vc, pose, 0.35F * pulse, 0xFFFFF8E0);
            HardLight.billboard(vc, pose, 0.9F * pulse, 0x90FFB020);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void bolt(VertexConsumer vc, PoseStack.Pose pose, float[][] p, float w, int color) {
        for (int i = 0; i < p.length - 1; i++) {
            float[] a = p[i];
            float[] b = p[i + 1];
            HardLight.vertex(vc, pose, a[0] - w, a[1], a[2], 0, 0, color, 0, 1, 0);
            HardLight.vertex(vc, pose, b[0] - w, b[1], b[2], 0, 1, color, 0, 1, 0);
            HardLight.vertex(vc, pose, b[0] + w, b[1], b[2], 1, 1, color, 0, 1, 0);
            HardLight.vertex(vc, pose, a[0] + w, a[1], a[2], 1, 0, color, 0, 1, 0);
            HardLight.vertex(vc, pose, a[0], a[1] - w, a[2], 0, 0, color, 1, 0, 0);
            HardLight.vertex(vc, pose, b[0], b[1] - w, b[2], 0, 1, color, 1, 0, 0);
            HardLight.vertex(vc, pose, b[0], b[1] + w, b[2], 1, 1, color, 1, 0, 0);
            HardLight.vertex(vc, pose, a[0], a[1] + w, a[2], 1, 0, color, 1, 0, 0);
        }
    }

    /** Deterministic noise in [-1, 1]. */
    private static float rand(int seed, int i) {
        int h = seed * 0x9E3779B1 + i * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return ((h & 0xFFFF) / 65535.0F) * 2.0F - 1.0F;
    }

    public static class State extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float length;
        public int seed;
    }
}
