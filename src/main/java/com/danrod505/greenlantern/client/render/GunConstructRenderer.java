package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.GunConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Six-barrel hard-light gatling gun. */
public class GunConstructRenderer extends EntityRenderer<GunConstructEntity, GunConstructRenderer.State> {
    private static final int BARRELS = 6;

    public GunConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GunConstructEntity gun, State state, float partialTick) {
        super.extractRenderState(gun, state, partialTick);
        state.yaw = gun.getYRot(partialTick);
        state.pitch = gun.getXRot(partialTick);
        state.barrelAngle = Mth.lerp(partialTick, gun.barrelAngleO, gun.barrelAngle);
        state.firing = gun.getSpinTicks() >= 6;
    }

    @Override
    protected int getBlockLightLevel(GunConstructEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.ageInTicks;
        float appear = Math.min(1.0F, age / 4.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        poseStack.scale(appear, appear, appear);

        // Body, grip and ammo drum.
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            HardLight.glowingBox(vc, pose, -0.2F, -0.2F, -0.6F, 0.2F, 0.2F, 0.05F, 1.0F);
            HardLight.glowingBox(vc, pose, -0.07F, -0.5F, -0.45F, 0.07F, -0.2F, -0.3F, 1.0F);
            HardLight.glowingBox(vc, pose, 0.2F, -0.18F, -0.5F, 0.42F, 0.12F, -0.1F, 0.9F);
            HardLight.glowingBox(vc, pose, -0.06F, 0.2F, -0.4F, 0.06F, 0.3F, -0.05F, 0.8F);
        });

        // Rotating barrel assembly.
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.barrelAngle));
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            for (int i = 0; i < BARRELS; i++) {
                float a = Mth.TWO_PI * i / BARRELS;
                float cx = Mth.cos(a) * 0.12F;
                float cy = Mth.sin(a) * 0.12F;
                HardLight.box(vc, pose, cx - 0.035F, cy - 0.035F, 0.05F, cx + 0.035F, cy + 0.035F, 1.05F, HardLight.color(0.8F, 0.45F));
            }
            HardLight.glowingBox(vc, pose, -0.19F, -0.19F, 0.45F, 0.19F, 0.19F, 0.5F, 0.8F);
            HardLight.glowingBox(vc, pose, -0.19F, -0.19F, 0.95F, 0.19F, 0.19F, 1.0F, 0.8F);
            HardLight.box(vc, pose, -0.03F, -0.03F, 0.05F, 0.03F, 0.03F, 1.0F, HardLight.color(0.9F, 1.0F));
        });
        poseStack.popPose();

        if (state.firing) {
            // Muzzle flash.
            poseStack.translate(0.0F, 0.0F, 1.15F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 47.0F));
            float flash = 0.18F + 0.1F * Mth.sin(age * 3.1F);
            collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.CORE), (pose, vc) -> {
                HardLight.billboard(vc, pose, flash, HardLight.color(0.9F, 1.0F));
                HardLight.quad(vc, pose, 0, -flash, -flash, 0, flash, -flash, 0, flash, flash, 0, -flash, flash, HardLight.color(0.7F, 1.0F), 1, 0, 0);
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float barrelAngle;
        public boolean firing;
    }
}
