package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Glowing core with a hard-light trail pointing backwards along the flight direction. */
public class EnergyBoltRenderer extends EntityRenderer<EnergyBoltEntity, EnergyBoltRenderer.State> {
    public EnergyBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EnergyBoltEntity bolt, State state, float partialTick) {
        super.extractRenderState(bolt, state, partialTick);
        state.heavy = bolt.isHeavy();
        Vec3 motion = bolt.getDeltaMovement();
        double horizontal = motion.horizontalDistance();
        state.yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        state.pitch = (float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG);
    }

    @Override
    protected int getBlockLightLevel(EnergyBoltEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean heavy = state.heavy;
        float age = state.ageInTicks;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 1.7F);
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);

        // Trail, aligned with the direction of travel.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        float w = heavy ? 0.22F : 0.06F;
        float len = heavy ? 1.6F : 1.1F;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            HardLight.box(vc, pose, -w, -w, -len, w, w, 0.1F, HardLight.color(0.7F, 0.6F));
            HardLight.box(vc, pose, -w * 1.8F, -w * 1.8F, -len * 0.6F, w * 1.8F, w * 1.8F, 0.15F, HardLight.color(0.2F, 0.0F));
        });
        if (heavy) {
            // Spinning inner cube.
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 25.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 18.0F));
            collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL),
                    (pose, vc) -> HardLight.centeredBox(vc, pose, 0.22F, 0.22F, 0.22F, HardLight.color(0.9F, 0.8F)));
        }
        poseStack.popPose();

        // Camera facing glow.
        poseStack.mulPose(camera.orientation);
        float size = (heavy ? 0.75F : 0.28F) * pulse;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.CORE), (pose, vc) -> {
            HardLight.billboard(vc, pose, size, HardLight.color(1.0F, 1.0F));
            HardLight.billboard(vc, pose, size * 1.9F, HardLight.color(0.35F, 0.2F));
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public boolean heavy;
        public float yaw;
        public float pitch;
    }
}
