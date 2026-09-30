package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.SawConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;

/** Hover board with a fork holding the giant spinning saw blade in front. */
public class SawConstructRenderer extends EntityRenderer<SawConstructEntity, SawConstructRenderer.State> {
    public SawConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SawConstructEntity saw, State state, float partialTick) {
        super.extractRenderState(saw, state, partialTick);
        state.yaw = saw.getYRot(partialTick);
        state.bladeAngle = Mth.lerp(partialTick, saw.bladeAngleO, saw.bladeAngle);
    }

    @Override
    protected AABB getBoundingBoxForCulling(SawConstructEntity entity) {
        return entity.getBoundingBox().inflate(SawConstructEntity.BLADE_OFFSET + SawConstructEntity.BLADE_RADIUS, 2.5, SawConstructEntity.BLADE_OFFSET + SawConstructEntity.BLADE_RADIUS);
    }

    @Override
    protected int getBlockLightLevel(SawConstructEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.ageInTicks;
        float appear = Math.min(1.0F, age / 5.0F);
        float h = state.boundingBoxHeight;
        float bob = 0.04F * Mth.sin(age * 0.3F);

        poseStack.pushPose();
        poseStack.translate(0.0F, bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.scale(appear, appear, appear);

        // Board, rails, thrusters and the fork holding the blade.
        float bladeZ = (float) SawConstructEntity.BLADE_OFFSET;
        float bladeY = SawConstructEntity.BLADE_RADIUS - 0.25F;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            HardLight.glowingBox(vc, pose, -0.75F, h - 0.18F, -1.1F, 0.75F, h, 0.85F, 1.0F);
            HardLight.glowingBox(vc, pose, -0.85F, h - 0.28F, -1.2F, -0.7F, h + 0.05F, 0.95F, 0.9F);
            HardLight.glowingBox(vc, pose, 0.7F, h - 0.28F, -1.2F, 0.85F, h + 0.05F, 0.95F, 0.9F);
            HardLight.glowingBox(vc, pose, -0.55F, 0.05F, -0.9F, -0.25F, h - 0.18F, -0.6F, 0.8F);
            HardLight.glowingBox(vc, pose, 0.25F, 0.05F, -0.9F, 0.55F, h - 0.18F, -0.6F, 0.8F);
            // Fork arms reaching the blade axle.
            HardLight.glowingBox(vc, pose, -0.32F, bladeY - 0.08F, 0.6F, -0.2F, h, bladeZ + 0.1F, 1.0F);
            HardLight.glowingBox(vc, pose, 0.2F, bladeY - 0.08F, 0.6F, 0.32F, h, bladeZ + 0.1F, 1.0F);
            // Axle.
            HardLight.box(vc, pose, -0.36F, bladeY - 0.1F, bladeZ - 0.1F, 0.36F, bladeY + 0.1F, bladeZ + 0.1F, HardLight.color(0.95F, 1.0F));
            // Handle bar for the rider.
            HardLight.glowingBox(vc, pose, -0.05F, h, 0.55F, 0.05F, h + 1.0F, 0.65F, 0.9F);
            HardLight.glowingBox(vc, pose, -0.45F, h + 0.95F, 0.5F, 0.45F, h + 1.05F, 0.7F, 0.9F);
        });

        // Blade: textured disc spinning around the axle (X axis).
        poseStack.translate(0.0F, bladeY, bladeZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.bladeAngle));
        float r = SawConstructEntity.BLADE_RADIUS + 0.2F;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.SAW_BLADE), (pose, vc) -> {
            int main = HardLight.color(0.95F, 0.5F);
            int ghost = HardLight.color(0.35F, 0.2F);
            HardLight.quad(vc, pose, 0.03F, -r, -r, 0.03F, -r, r, 0.03F, r, r, 0.03F, r, -r, main, 1, 0, 0);
            HardLight.quad(vc, pose, -0.03F, -r, r, -0.03F, -r, -r, -0.03F, r, -r, -0.03F, r, r, main, -1, 0, 0);
            float r2 = r * 1.04F;
            HardLight.quad(vc, pose, 0.0F, -r2, -r2, 0.0F, -r2, r2, 0.0F, r2, r2, 0.0F, r2, -r2, ghost, 1, 0, 0);
        });
        // Motion blur copy, slightly behind in rotation.
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.SAW_BLADE), (pose, vc) ->
                HardLight.quad(vc, pose, 0.0F, -r, -r, 0.0F, -r, r, 0.0F, r, r, 0.0F, r, -r, HardLight.color(0.25F, 0.0F), 1, 0, 0));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float yaw;
        public float bladeAngle;
    }
}
