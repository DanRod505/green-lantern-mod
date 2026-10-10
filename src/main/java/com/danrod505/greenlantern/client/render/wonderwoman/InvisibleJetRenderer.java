package com.danrod505.greenlantern.client.render.wonderwoman;

import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * The Invisible Jet: a sleek delta-winged fighter made of something like glass. The panels are
 * barely there (a faint blue-white tint) and only the edges catch the light; the engines glow gold.
 * With the cloak on, everything fades to a faint shimmer. Built in code (+Z is the nose, +X its left).
 */
public class InvisibleJetRenderer extends EntityRenderer<InvisibleJetEntity, InvisibleJetRenderer.State> {
    private static final int PANEL = 0xC8E6FF;
    private static final int EDGE = 0xF0FAFF;
    private static final int ENGINE = 0xFFC850;

    public InvisibleJetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InvisibleJetEntity jet, State state, float partialTick) {
        super.extractRenderState(jet, state, partialTick);
        state.yaw = jet.getYRot(partialTick);
        state.pitch = jet.getXRot(partialTick);
        state.bank = Mth.lerp(partialTick, jet.bankO, jet.bank);
        state.thrust = Mth.lerp(partialTick, jet.thrustO, jet.thrust);
        state.cloak = Mth.lerp(partialTick, jet.cloakO, jet.cloak);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.55F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.bank * 30.0F));
        poseStack.translate(0.0F, -0.55F, 0.0F);
        // Cloaked: only a faint shimmer runs over the edges.
        float shimmer = 0.5F + 0.5F * Mth.sin(state.ageInTicks * 0.3F);
        float visible = Mth.lerp(state.cloak, 1.0F, 0.06F + 0.06F * shimmer);
        collector.submitCustomGeometry(poseStack, HardLight.type(Geo.PLAIN), (pose, vc) -> {
            hull(vc, pose, Geo.withAlpha(PANEL, 0.16F * visible), Geo.withAlpha(EDGE, 0.75F * visible));
            engines(vc, pose, Geo.withAlpha(ENGINE, (0.35F + 0.65F * state.thrust) * Mth.lerp(state.cloak, 1.0F, 0.1F)));
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void hull(VertexConsumer vc, PoseStack.Pose pose, int panel, int edge) {
        // Fuselage: a long wedge from the tail to the pointed nose.
        Geo.taper(vc, pose, -0.42F, 0.3F, -1.9F, 0.42F, 0.72F, 1.2F, -0.26F, -1.6F, 0.26F, 0.9F, panel, panel, LightTexture.FULL_BRIGHT);
        Geo.taper(vc, pose, -0.36F, 0.34F, 1.2F, 0.36F, 0.66F, 2.7F, -0.04F, 2.65F, 0.04F, 2.7F, panel, panel, LightTexture.FULL_BRIGHT);
        // Canopy bubble over the seat.
        Geo.taper(vc, pose, -0.34F, 0.72F, -0.5F, 0.34F, 1.15F, 1.1F, -0.18F, -0.2F, 0.18F, 0.7F, panel, panel, LightTexture.FULL_BRIGHT);
        // Delta wings.
        for (int side = -1; side <= 1; side += 2) {
            float root = side * 0.4F;
            float tip = side * 1.6F;
            Geo.doubleQuad(vc, pose, root, 0.45F, 0.9F, root, 0.45F, -1.6F, tip, 0.42F, -1.5F, tip, 0.42F, -0.9F, panel, panel, LightTexture.FULL_BRIGHT);
            // Tail fins, canted out.
            float fin = side * 0.3F;
            Geo.doubleQuad(vc, pose, fin, 0.7F, -1.0F, fin, 0.7F, -1.9F, fin + side * 0.25F, 1.45F, -2.0F, fin + side * 0.18F, 1.4F, -1.55F,
                    panel, panel, LightTexture.FULL_BRIGHT);
            // Edges that catch the light: wing leading and trailing edges, fin edges.
            line(vc, pose, root, 0.45F, 0.9F, tip, 0.42F, -0.9F, edge);
            line(vc, pose, tip, 0.42F, -0.9F, tip, 0.42F, -1.5F, edge);
            line(vc, pose, tip, 0.42F, -1.5F, root, 0.45F, -1.6F, edge);
            line(vc, pose, fin, 0.7F, -1.0F, fin + side * 0.18F, 1.4F, -1.55F, edge);
            line(vc, pose, fin + side * 0.18F, 1.4F, -1.55F, fin + side * 0.25F, 1.45F, -2.0F, edge);
            // Fuselage outline down each side to the nose.
            line(vc, pose, side * 0.42F, 0.72F, -1.9F, side * 0.36F, 0.66F, 1.2F, edge);
            line(vc, pose, side * 0.36F, 0.66F, 1.2F, 0.0F, 0.6F, 2.7F, edge);
            line(vc, pose, side * 0.34F, 0.72F, -0.5F, side * 0.18F, 1.15F, 0.1F, edge);
        }
        line(vc, pose, 0.0F, 1.15F, -0.2F, 0.0F, 1.15F, 0.7F, edge);
    }

    private static void engines(VertexConsumer vc, PoseStack.Pose pose, int glow) {
        for (int side = -1; side <= 1; side += 2) {
            float cx = side * 0.22F;
            Geo.box(vc, pose, cx - 0.13F, 0.38F, -1.98F, cx + 0.13F, 0.62F, -1.9F, glow, LightTexture.FULL_BRIGHT);
        }
    }

    /** A thin bright bar between two points (an edge of the glass catching the light). */
    private static void line(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float w = 0.018F;
        Geo.quad(vc, pose, x0 - w, y0, z0, x1 - w, y1, z1, x1 + w, y1, z1, x0 + w, y0, z0, color, LightTexture.FULL_BRIGHT);
        Geo.quad(vc, pose, x0 + w, y0, z0, x1 + w, y1, z1, x1 - w, y1, z1, x0 - w, y0, z0, color, LightTexture.FULL_BRIGHT);
        Geo.quad(vc, pose, x0, y0 - w, z0, x1, y1 - w, z1, x1, y1 + w, z1, x0, y0 + w, z0, color, LightTexture.FULL_BRIGHT);
        Geo.quad(vc, pose, x0, y0 + w, z0, x1, y1 + w, z1, x1, y1 - w, z1, x0, y0 - w, z0, color, LightTexture.FULL_BRIGHT);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        float bank;
        float thrust;
        float cloak;
    }
}
