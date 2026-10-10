package com.danrod505.greenlantern.client.render.wonderwoman;

import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.entity.AmazonShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * The thrown Amazon shield: a round, slightly domed shield of red-lacquered bronze with a silver rim
 * and a golden star, spinning flat like a discus.
 */
public class AmazonShieldRenderer extends EntityRenderer<AmazonShieldEntity, AmazonShieldRenderer.State> {
    private static final int RIM = 0xFFC9CED6;
    private static final int FACE = 0xFFB51E25;
    private static final int RING = 0xFFE3B23C;
    private static final int STAR = 0xFFFFD84A;
    private static final int BACK = 0xFF7A5A2E;
    private static final int SIDES = 20;
    private static final float RADIUS = 0.5F;

    public AmazonShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AmazonShieldEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spin = Mth.lerp(partialTick, entity.spinO, entity.spin);
        state.yaw = entity.getYRot(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        // Tilted a little, like a thrown discus.
        poseStack.mulPose(Axis.XP.rotationDegrees(12.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> disc(vc, pose, light));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** The shield lying flat (face up): rim, red face, golden ring and the star in the middle. */
    static void disc(VertexConsumer vc, PoseStack.Pose pose, int light) {
        float t = 0.05F;
        for (int i = 0; i < SIDES; i++) {
            float a0 = i * Mth.TWO_PI / SIDES;
            float a1 = (i + 1) * Mth.TWO_PI / SIDES;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float r = RADIUS;
            float ri = RADIUS * 0.86F;
            float rr = RADIUS * 0.55F;
            float rs = RADIUS * 0.45F;
            // Edge.
            Geo.quad(vc, pose, c0 * r, -t, s0 * r, c1 * r, -t, s1 * r, c1 * r, t, s1 * r, c0 * r, t, s0 * r, RIM, light);
            // Top: silver rim, red face (a gentle dome), golden ring.
            Geo.quad(vc, pose, c0 * ri, t + 0.01F, s0 * ri, c1 * ri, t + 0.01F, s1 * ri, c1 * r, t, s1 * r, c0 * r, t, s0 * r, RIM, light);
            Geo.quad(vc, pose, c0 * rr, t + 0.04F, s0 * rr, c1 * rr, t + 0.04F, s1 * rr, c1 * ri, t + 0.01F, s1 * ri, c0 * ri, t + 0.01F, s0 * ri, FACE, light);
            Geo.quad(vc, pose, c0 * rs, t + 0.05F, s0 * rs, c1 * rs, t + 0.05F, s1 * rs, c1 * rr, t + 0.04F, s1 * rr, c0 * rr, t + 0.04F, s0 * rr, RING, light);
            Geo.quad(vc, pose, 0, t + 0.06F, 0, 0, t + 0.06F, 0, c1 * rs, t + 0.05F, s1 * rs, c0 * rs, t + 0.05F, s0 * rs, FACE, light);
            // Back: plain bronze.
            Geo.quad(vc, pose, c0 * r, -t, s0 * r, c1 * r, -t, s1 * r, 0, -t, 0, 0, -t, 0, BACK, light);
        }
        // Five-pointed star in the middle.
        float y = t + 0.07F;
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 - Mth.HALF_PI;
            float al = a - Mth.TWO_PI / 10;
            float ar = a + Mth.TWO_PI / 10;
            float outer = RADIUS * 0.4F;
            float inner = RADIUS * 0.16F;
            Geo.quad(vc, pose, 0, y, 0, Mth.cos(ar) * inner, y, Mth.sin(ar) * inner, Mth.cos(a) * outer, y, Mth.sin(a) * outer,
                    Mth.cos(al) * inner, y, Mth.sin(al) * inner, STAR, light);
        }
    }

    public static class State extends EntityRenderState {
        float spin;
        float yaw;
    }
}
