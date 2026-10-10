package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.MechaMissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Slim hard-light missile with tail fins and a bright exhaust once the motor is lit. */
public class MechaMissileRenderer extends EntityRenderer<MechaMissileEntity, MechaMissileRenderer.State> {
    public MechaMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MechaMissileEntity missile, State state, float partialTick) {
        super.extractRenderState(missile, state, partialTick);
        state.yaw = missile.getYRot(partialTick);
        state.pitch = missile.getXRot(partialTick);
        state.ignited = missile.ignited();
    }

    @Override
    protected int getBlockLightLevel(MechaMissileEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.pushPose();
        // Same orientation convention as the projectile rotation (+Z along the flight direction).
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 12.0F));
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            HardLight.box(vc, pose, -0.11F, -0.11F, -0.45F, 0.11F, 0.11F, 0.35F, HardLight.color(0.9F, 0.4F));
            HardLight.box(vc, pose, -0.07F, -0.07F, 0.35F, 0.07F, 0.07F, 0.55F, HardLight.color(1.0F, 1.0F));
            HardLight.box(vc, pose, -0.28F, -0.02F, -0.45F, 0.28F, 0.02F, -0.2F, HardLight.color(0.85F, 0.6F));
            HardLight.box(vc, pose, -0.02F, -0.28F, -0.45F, 0.02F, 0.28F, -0.2F, HardLight.color(0.85F, 0.6F));
            HardLight.box(vc, pose, -0.16F, -0.16F, -0.5F, 0.16F, 0.16F, 0.45F, HardLight.color(0.2F, 0.0F));
        });
        poseStack.popPose();
        if (state.ignited) {
            float pulse = 0.8F + 0.2F * Mth.sin(state.ageInTicks * 2.7F);
            poseStack.mulPose(camera.orientation);
            collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.CORE), (pose, vc) -> {
                HardLight.billboard(vc, pose, 0.35F * pulse, HardLight.color(1.0F, 1.0F));
                HardLight.billboard(vc, pose, 0.7F * pulse, HardLight.color(0.3F, 0.2F));
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        boolean ignited;
    }
}
