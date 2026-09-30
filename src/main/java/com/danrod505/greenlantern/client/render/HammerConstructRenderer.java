package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.HammerConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Giant mallet. The entity sits on the impact point; the handle pivots around a point behind it
 * (towards the player) and swings the head down onto the target.
 */
public class HammerConstructRenderer extends EntityRenderer<HammerConstructEntity, HammerConstructRenderer.State> {
    private static final float HANDLE_LENGTH = 3.4F;
    private static final float HEAD_HALF_LENGTH = 1.3F;

    public HammerConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HammerConstructEntity hammer, State state, float partialTick) {
        super.extractRenderState(hammer, state, partialTick);
        state.yaw = hammer.getYRot();
    }

    @Override
    protected AABB getBoundingBoxForCulling(HammerConstructEntity entity) {
        return entity.getBoundingBox().inflate(6.0, 7.0, 6.0);
    }

    @Override
    protected int getBlockLightLevel(HammerConstructEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.ageInTicks;
        float alpha = HammerConstructEntity.alpha(age);
        if (alpha <= 0.01F) return;
        float angle = HammerConstructEntity.swingAngle(age);
        float grow = Math.min(1.0F, 0.6F + age / 12.0F);
        boolean impact = age >= HammerConstructEntity.IMPACT_TICK && age < HammerConstructEntity.IMPACT_TICK + 4;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        // Pivot (the "hand") behind the target.
        poseStack.translate(0.0F, HEAD_HALF_LENGTH, -HANDLE_LENGTH);
        poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
        poseStack.scale(grow, grow, grow);

        float boost = impact ? 1.0F : 0.35F;
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            // Handle with grip bands and pommel.
            HardLight.glowingBox(vc, pose, -0.16F, -0.16F, -0.2F, 0.16F, 0.16F, HANDLE_LENGTH - 0.8F, alpha);
            HardLight.glowingBox(vc, pose, -0.24F, -0.24F, -0.45F, 0.24F, 0.24F, -0.15F, alpha);
            HardLight.glowingBox(vc, pose, -0.2F, -0.2F, 0.4F, 0.2F, 0.2F, 0.55F, alpha);
            HardLight.glowingBox(vc, pose, -0.2F, -0.2F, 0.9F, 0.2F, 0.2F, 1.05F, alpha);
            // Head, long along Y so its faces strike the ground.
            float z0 = HANDLE_LENGTH - 0.8F;
            float z1 = HANDLE_LENGTH + 0.8F;
            HardLight.box(vc, pose, -0.85F, -HEAD_HALF_LENGTH, z0, 0.85F, HEAD_HALF_LENGTH, z1, HardLight.color(0.8F * alpha, boost));
            HardLight.box(vc, pose, -0.95F, -HEAD_HALF_LENGTH - 0.05F, z0 - 0.1F, 0.95F, HEAD_HALF_LENGTH + 0.05F, z1 + 0.1F, HardLight.color(0.2F * alpha, 0.0F));
            // Rims on both striking faces and a central emblem band.
            HardLight.box(vc, pose, -0.95F, -HEAD_HALF_LENGTH, z0 - 0.08F, 0.95F, -HEAD_HALF_LENGTH + 0.3F, z1 + 0.08F, HardLight.color(0.9F * alpha, 0.8F));
            HardLight.box(vc, pose, -0.95F, HEAD_HALF_LENGTH - 0.3F, z0 - 0.08F, 0.95F, HEAD_HALF_LENGTH, z1 + 0.08F, HardLight.color(0.9F * alpha, 0.8F));
            HardLight.box(vc, pose, -0.9F, -0.2F, z0 - 0.05F, 0.9F, 0.2F, z1 + 0.05F, HardLight.color(0.95F * alpha, 1.0F));
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float yaw;
    }
}
