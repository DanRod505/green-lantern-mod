package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.entity.GrappleHookEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The grappling hook: a three-pronged steel claw, and the cable that runs back to Batman's hand. */
public class GrappleHookRenderer extends EntityRenderer<GrappleHookEntity, GrappleHookRenderer.State> {
    private static final int STEEL = 0xFF6E717A;
    private static final int DARK = 0xFF2A2B30;
    private static final int CABLE = 0xFF111215;
    private static final int SEGMENTS = 16;

    public GrappleHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(GrappleHookEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        // The cable can cross the screen even when the claw itself is out of view.
        return entity.getOwner() != null || super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public void extractRenderState(GrappleHookEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = entity.getYRot(partialTick);
        state.pitch = entity.getXRot(partialTick);
        state.attached = entity.isAttached();
        Entity owner = entity.getOwner();
        if (owner instanceof Player player) {
            Vec3 pos = player.getPosition(partialTick);
            float body = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(body), 0, Mth.cos(body));
            Vec3 right = new Vec3(-Mth.cos(body), 0, -Mth.sin(body));
            Vec3 hand = pos.add(0, player.isCrouching() ? 0.95 : 1.25, 0).add(right.scale(0.38)).add(forward.scale(0.25));
            state.cable = hand.subtract(state.x, state.y, state.z);
        } else {
            state.cable = null;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        Vec3 cable = state.cable;
        if (cable != null && cable.lengthSqr() > 0.01) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
                Vec3 dir = cable.normalize();
                Vec3 a = dir.cross(new Vec3(0, 1, 0));
                if (a.lengthSqr() < 1.0E-4) a = new Vec3(1, 0, 0);
                a = a.normalize().scale(0.025);
                Vec3 b = dir.cross(a).normalize().scale(0.025);
                // A taut cable once the hook bites; a slack, sagging one in flight.
                double sag = state.attached ? 0.0 : Math.min(1.5, cable.length() * 0.04);
                for (int i = 0; i < SEGMENTS; i++) {
                    Vec3 p0 = point(cable, (float) i / SEGMENTS, sag);
                    Vec3 p1 = point(cable, (float) (i + 1) / SEGMENTS, sag);
                    ribbon(vc, pose, p0, p1, a, light);
                    ribbon(vc, pose, p0, p1, b, light);
                }
            });
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            // Shaft along +Z (the flight direction), claws at the front.
            Geo.box(vc, pose, -0.05F, -0.05F, -0.2F, 0.05F, 0.05F, 0.12F, DARK, light);
            Geo.box(vc, pose, -0.07F, -0.07F, -0.26F, 0.07F, 0.07F, -0.18F, STEEL, light);
            for (int k = 0; k < 3; k++) {
                float angle = k * Mth.TWO_PI / 3.0F;
                float cx = Mth.cos(angle);
                float cy = Mth.sin(angle);
                PoseStack.Pose p = pose;
                Geo.quad(vc, p, cx * 0.03F, cy * 0.03F, 0.08F, cx * 0.16F, cy * 0.16F, 0.2F, cx * 0.13F, cy * 0.13F, 0.1F,
                        cx * 0.03F, cy * 0.03F, 0.0F, STEEL, light);
                Geo.quad(vc, p, cx * 0.03F, cy * 0.03F, 0.0F, cx * 0.13F, cy * 0.13F, 0.1F, cx * 0.16F, cy * 0.16F, 0.2F,
                        cx * 0.03F, cy * 0.03F, 0.08F, STEEL, light);
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static Vec3 point(Vec3 cable, float t, double sag) {
        return cable.scale(t).add(0, -Math.sin(t * Math.PI) * sag, 0);
    }

    private static void ribbon(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 w, int light) {
        Geo.quad(vc, pose, (float) (p0.x - w.x), (float) (p0.y - w.y), (float) (p0.z - w.z),
                (float) (p0.x + w.x), (float) (p0.y + w.y), (float) (p0.z + w.z),
                (float) (p1.x + w.x), (float) (p1.y + w.y), (float) (p1.z + w.z),
                (float) (p1.x - w.x), (float) (p1.y - w.y), (float) (p1.z - w.z), CABLE, light);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        boolean attached;
        Vec3 cable;
    }
}
