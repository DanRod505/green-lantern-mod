package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.DrillConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Cockpit on tracks with an arm holding the giant spinning drill bit, aimed where the rider looks. */
public class DrillConstructRenderer extends EntityRenderer<DrillConstructEntity, DrillConstructRenderer.State> {
    private static final int SIDES = 12;
    private static final int FLUTE_STEPS = 18;

    public DrillConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DrillConstructEntity drill, State state, float partialTick) {
        super.extractRenderState(drill, state, partialTick);
        state.yaw = drill.getYRot(partialTick);
        state.pitch = DrillConstructEntity.effectivePitch(drill.getXRot(partialTick));
        state.bitAngle = Mth.lerp(partialTick, drill.bitAngleO, drill.bitAngle);
    }

    @Override
    protected AABB getBoundingBoxForCulling(DrillConstructEntity entity) {
        double reach = DrillConstructEntity.PIVOT_FORWARD + DrillConstructEntity.BIT_START + DrillConstructEntity.BIT_LENGTH + 0.3;
        return entity.getBoundingBox().inflate(reach, reach, reach);
    }

    @Override
    protected int getBlockLightLevel(DrillConstructEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.ageInTicks;
        float appear = Math.min(1.0F, age / 5.0F);
        float h = state.boundingBoxHeight;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.scale(appear, appear, appear);

        float pz = DrillConstructEntity.PIVOT_FORWARD;
        float py = DrillConstructEntity.PIVOT_HEIGHT;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            // Tracks on both sides.
            HardLight.glowingBox(vc, pose, -0.75F, 0.0F, -0.8F, -0.45F, 0.4F, 0.75F, 1.0F);
            HardLight.glowingBox(vc, pose, 0.45F, 0.0F, -0.8F, 0.75F, 0.4F, 0.75F, 1.0F);
            float tread = (age * 0.08F) % 0.3F;
            for (float z = -0.8F + tread; z < 0.75F; z += 0.3F) {
                HardLight.box(vc, pose, -0.78F, 0.38F, z, -0.42F, 0.44F, z + 0.08F, HardLight.color(0.9F, 0.8F));
                HardLight.box(vc, pose, 0.42F, 0.38F, z, 0.78F, 0.44F, z + 0.08F, HardLight.color(0.9F, 0.8F));
            }
            // Hull and seat.
            HardLight.glowingBox(vc, pose, -0.45F, 0.1F, -0.7F, 0.45F, 0.35F, 0.6F, 1.0F);
            HardLight.glowingBox(vc, pose, -0.4F, 0.35F, -0.75F, 0.4F, h, -0.6F, 0.85F);
            // Windshield frame around the rider.
            HardLight.glowingBox(vc, pose, -0.45F, 0.35F, 0.35F, -0.35F, h + 0.35F, 0.45F, 0.8F);
            HardLight.glowingBox(vc, pose, 0.35F, 0.35F, 0.35F, 0.45F, h + 0.35F, 0.45F, 0.8F);
            HardLight.glowingBox(vc, pose, -0.45F, h + 0.3F, 0.35F, 0.45F, h + 0.4F, 0.45F, 0.8F);
        });

        // Arm and bit, tilted around the pivot.
        poseStack.translate(0.0F, py, pz);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        float start = DrillConstructEntity.BIT_START;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            HardLight.glowingBox(vc, pose, -0.2F, -0.2F, -0.3F, 0.2F, 0.2F, start, 1.0F);
            HardLight.box(vc, pose, -0.5F, -0.5F, start - 0.1F, 0.5F, 0.5F, start, HardLight.color(0.9F, 0.9F));
        });

        poseStack.translate(0.0F, 0.0F, start);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.bitAngle));
        float len = DrillConstructEntity.BIT_LENGTH;
        float radius = DrillConstructEntity.BIT_RADIUS;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            cone(vc, pose, radius, len, HardLight.color(0.6F, 0.3F));
            cone(vc, pose, radius + 0.06F, len + 0.1F, HardLight.color(0.18F, 0.0F));
            // Two helical flutes winding to the tip.
            for (int flute = 0; flute < 2; flute++) {
                for (int i = 0; i < FLUTE_STEPS; i++) {
                    float t = (i + 0.5F) / FLUTE_STEPS;
                    float r = radius * (1.0F - t) + 0.03F;
                    float a = t * Mth.TWO_PI * 2.0F + flute * Mth.PI;
                    float x = Mth.cos(a) * r;
                    float y = Mth.sin(a) * r;
                    float z = t * len;
                    float s = 0.06F + 0.1F * (1.0F - t);
                    HardLight.box(vc, pose, x - s, y - s, z - 0.07F, x + s, y + s, z + 0.07F, HardLight.color(0.95F, 1.0F));
                }
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** Cone along +Z: base circle of the given radius at z=0, tip at z=length. */
    private static void cone(VertexConsumer vc, PoseStack.Pose pose, float radius, float length, int color) {
        for (int i = 0; i < SIDES; i++) {
            float a0 = Mth.TWO_PI * i / SIDES;
            float a1 = Mth.TWO_PI * (i + 1) / SIDES;
            float x0 = Mth.cos(a0) * radius;
            float y0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float y1 = Mth.sin(a1) * radius;
            float am = (a0 + a1) * 0.5F;
            float nx = Mth.cos(am);
            float ny = Mth.sin(am);
            float nz = radius / length;
            HardLight.quad(vc, pose, x1, y1, 0, x0, y0, 0, 0, 0, length, 0, 0, length, color, nx, ny, nz);
            // Back cap.
            HardLight.quad(vc, pose, x0, y0, 0, x1, y1, 0, 0, 0, 0, 0, 0, 0, color, 0, 0, -1);
        }
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        float bitAngle;
    }
}
