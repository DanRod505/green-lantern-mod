package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.entity.BatarangEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;

/** The batarang: a flat, bat-shaped blade of dark steel, spinning as it flies. */
public class BatarangRenderer extends EntityRenderer<BatarangEntity, BatarangRenderer.State> {
    private static final int BLADE = 0xFF3A3B42;
    private static final int EDGE = 0xFF8A8C96;
    private static final int BODY = 0xFF1C1D22;

    public BatarangRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BatarangEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = entity.getYRot(partialTick);
        state.pitch = entity.getXRot(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch * 0.5F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 55.0F));
        poseStack.scale(1.8F, 1.8F, 1.8F);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            Geo.box(vc, pose, -0.04F, -0.018F, -0.07F, 0.04F, 0.018F, 0.09F, BODY, light);
            // Little ears at the front of the body.
            Geo.box(vc, pose, -0.035F, -0.012F, 0.09F, -0.012F, 0.012F, 0.13F, BODY, light);
            Geo.box(vc, pose, 0.012F, -0.012F, 0.09F, 0.035F, 0.012F, 0.13F, BODY, light);
            for (int side = -1; side <= 1; side += 2) wing(vc, pose, side, light);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void wing(VertexConsumer vc, PoseStack.Pose pose, float s, int light) {
        // Outline of one wing (x, z), from the body out to the tip, with a scalloped trailing edge.
        Geo.doubleQuad(vc, pose, s * 0.03F, 0.0F, 0.07F, s * 0.30F, 0.0F, 0.12F, s * 0.22F, 0.0F, -0.01F, s * 0.03F, 0.0F, -0.06F,
                BLADE, BLADE, light);
        Geo.doubleQuad(vc, pose, s * 0.22F, 0.0F, -0.01F, s * 0.30F, 0.0F, 0.12F, s * 0.34F, 0.0F, -0.08F, s * 0.25F, 0.0F, -0.05F,
                BLADE, BLADE, light);
        Geo.doubleQuad(vc, pose, s * 0.03F, 0.0F, -0.06F, s * 0.14F, 0.0F, -0.02F, s * 0.12F, 0.0F, -0.10F, s * 0.03F, 0.0F, -0.08F,
                BLADE, BLADE, light);
        // Sharpened leading edge.
        float y = 0.004F;
        Geo.doubleQuad(vc, pose, s * 0.03F, y, 0.07F, s * 0.30F, y, 0.12F, s * 0.29F, y, 0.105F, s * 0.03F, y, 0.055F, EDGE, EDGE, light);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
    }
}
