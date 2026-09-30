package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Translucent hexagon-patterned sphere that ripples when it blocks a hit. */
public class BubbleConstructRenderer extends EntityRenderer<BubbleConstructEntity, BubbleConstructRenderer.State> {
    public BubbleConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BubbleConstructEntity bubble, State state, float partialTick) {
        super.extractRenderState(bubble, state, partialTick);
        state.flash = Math.max(0.0F, (bubble.hitFlash - partialTick) / 8.0F);
    }

    @Override
    protected int getBlockLightLevel(BubbleConstructEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.ageInTicks;
        float grow = Math.min(1.0F, age / 6.0F);
        float scale = BubbleConstructEntity.RADIUS * (grow * (2.0F - grow)) * (1.0F + 0.02F * Mth.sin(age * 0.25F) + 0.05F * state.flash);
        float alpha = 0.35F + 0.08F * Mth.sin(age * 0.15F) + 0.45F * state.flash;

        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 0.8F));
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.BUBBLE), (pose, vc) -> {
            HardLight.sphere(vc, pose, scale, 16, 32, HardLight.color(alpha, 0.2F + state.flash));
        });
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 1.6F));
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            HardLight.sphere(vc, pose, scale * 1.04F, 12, 24, HardLight.color(0.08F + 0.2F * state.flash, 0.0F));
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float flash;
    }
}
