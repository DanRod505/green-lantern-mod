package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.entity.BatmobileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * The Batmobile: a long, low armored car, black with dark-gray panels, a smoked canopy, bat fins at
 * the back, a jet nozzle that flares orange on boost, glowing headlights and a missile pod on each
 * side of the hood. Built in code from boxes and wedges (+Z is the front, +X its left).
 */
public class BatmobileRenderer extends EntityRenderer<BatmobileEntity, BatmobileRenderer.State> {
    private static final int BODY = 0xFF17181C;
    private static final int PANEL = 0xFF25262C;
    private static final int TRIM = 0xFF3B3D45;
    private static final int UNDER = 0xFF0C0C0E;
    private static final int TIRE = 0xFF141414;
    private static final int RIM = 0xFF4A4C55;
    private static final int HUB = 0xFFB8962E;
    private static final int GLASS = 0x8C2C3446;
    private static final int LAMP = 0xFFFFF2C4;
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    public BatmobileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.3F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BatmobileEntity car, State state, float partialTick) {
        super.extractRenderState(car, state, partialTick);
        state.yaw = car.getYRot(partialTick);
        state.wheelSpin = Mth.lerp(partialTick, car.wheelSpinO, car.wheelSpin);
        state.steer = Mth.lerp(partialTick, car.steerO, car.steer);
        state.flame = Mth.lerp(partialTick, car.flameO, car.flame);
        state.recoil = Mth.lerp(partialTick, car.recoilO, car.recoil);
        state.driven = car.getControllingPassenger() != null;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> body(vc, pose, state, light));
        submitWheels(poseStack, collector, state, light);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(Geo.PLAIN), (pose, vc) -> glow(vc, pose, state));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(Geo.PLAIN), (pose, vc) ->
                Geo.taper(vc, pose, -0.72F, 0.95F, -0.95F, 0.72F, 1.62F, 0.65F, -0.42F, -0.7F, 0.42F, 0.1F, GLASS, GLASS, light));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void body(VertexConsumer vc, PoseStack.Pose pose, State state, int light) {
        // Chassis and underside.
        Geo.box(vc, pose, -1.0F, 0.28F, -2.3F, 1.0F, 0.72F, 2.2F, BODY, PANEL, light);
        Geo.box(vc, pose, -0.9F, 0.18F, -2.1F, 0.9F, 0.28F, 2.0F, UNDER, light);
        // Long sloping nose, and the hood over it.
        Geo.taper(vc, pose, -0.98F, 0.28F, 2.2F, 0.98F, 0.64F, 3.15F, -0.7F, 2.2F, 0.7F, 2.75F, BODY, PANEL, light);
        Geo.taper(vc, pose, -0.9F, 0.72F, 0.6F, 0.9F, 0.92F, 2.2F, -0.68F, 0.8F, 0.68F, 1.95F, BODY, PANEL, light);
        Geo.box(vc, pose, -0.06F, 0.92F, 0.85F, 0.06F, 0.96F, 1.9F, TRIM, light);
        // Wheel arches: slim at the front, big and muscular at the back.
        for (int side = -1; side <= 1; side += 2) {
            float in = side * 0.82F;
            float out = side * 1.28F;
            float x0 = Math.min(in, out);
            float x1 = Math.max(in, out);
            Geo.taper(vc, pose, x0, 0.62F, 1.05F, x1, 0.9F, 2.15F, x0 + 0.05F, 1.2F, x1 - 0.05F, 2.0F, BODY, PANEL, light);
            float rin = side * 0.8F;
            float rout = side * 1.38F;
            float rx0 = Math.min(rin, rout);
            float rx1 = Math.max(rin, rout);
            Geo.taper(vc, pose, rx0, 0.62F, -2.3F, rx1, 1.12F, -0.9F, rx0 + 0.06F, -2.1F, rx1 - 0.06F, -1.1F, BODY, PANEL, light);
            // Side skirt and intake.
            Geo.box(vc, pose, Math.min(side * 1.0F, side * 1.12F), 0.3F, -0.9F, Math.max(side * 1.0F, side * 1.12F), 0.62F, 1.05F, PANEL, light);
            Geo.box(vc, pose, Math.min(side * 1.12F, side * 1.16F), 0.38F, -0.7F, Math.max(side * 1.12F, side * 1.16F), 0.54F, 0.4F, UNDER, light);
        }
        // Cockpit tub (the smoked canopy is drawn translucent on top of it).
        Geo.box(vc, pose, -0.78F, 0.72F, -0.95F, 0.78F, 0.95F, 0.65F, PANEL, TRIM, light);
        // Rear deck sloping back to the jet.
        Geo.taper(vc, pose, -0.95F, 0.72F, -2.35F, 0.95F, 1.02F, -0.95F, -0.8F, -2.2F, 0.8F, -1.2F, BODY, PANEL, light);
        // Bat fins.
        for (int side = -1; side <= 1; side += 2) {
            float x = side * 0.84F;
            Geo.doubleQuad(vc, pose, x, 1.0F, -1.0F, x, 1.0F, -2.3F, x, 1.85F, -2.75F, x, 1.2F, -1.55F, BODY, BODY, light);
            Geo.doubleQuad(vc, pose, x, 1.2F, -1.55F, x, 1.85F, -2.75F, x, 1.62F, -2.3F, x, 1.36F, -1.9F, PANEL, PANEL, light);
            Geo.box(vc, pose, x - 0.04F, 1.0F, -2.3F, x + 0.04F, 1.08F, -1.0F, TRIM, light);
        }
        // Jet nozzle.
        Geo.box(vc, pose, -0.36F, 0.32F, -2.6F, 0.36F, 0.8F, -2.3F, TRIM, light);
        Geo.box(vc, pose, -0.26F, 0.4F, -2.62F, 0.26F, 0.72F, -2.58F, UNDER, light);
        // Missile pods on the hood, kicked back by each salvo.
        float kick = -state.recoil * 0.18F;
        for (int side = -1; side <= 1; side += 2) {
            float cx = side * 0.62F;
            Geo.box(vc, pose, cx - 0.17F, 0.92F, 1.2F + kick, cx + 0.17F, 1.18F, 2.05F + kick, TRIM, PANEL, light);
            Geo.box(vc, pose, cx - 0.1F, 0.98F, 2.05F + kick, cx - 0.01F, 1.08F, 2.1F + kick, UNDER, light);
            Geo.box(vc, pose, cx + 0.01F, 0.98F, 2.05F + kick, cx + 0.1F, 1.08F, 2.1F + kick, UNDER, light);
            // Headlight housings.
            Geo.box(vc, pose, side * 0.55F - 0.16F, 0.42F, 2.98F, side * 0.55F + 0.16F, 0.56F, 3.04F, TRIM, light);
        }
    }

    private void submitWheels(PoseStack poseStack, SubmitNodeCollector collector, State state, int light) {
        float spin = -state.wheelSpin;
        float steer = state.steer * 25.0F;
        for (int side = -1; side <= 1; side += 2) {
            float front = side * 1.05F;
            poseStack.pushPose();
            poseStack.translate(front, 0.38F, 1.6F);
            poseStack.mulPose(Axis.YP.rotationDegrees(steer));
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) ->
                    Geo.wheel(vc, pose, 0.0F, 0.0F, 0.0F, 0.38F, 0.17F, 14, spin, TIRE, RIM, HUB, light));
            poseStack.popPose();
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            for (int side = -1; side <= 1; side += 2) {
                Geo.wheel(vc, pose, side * 1.1F, 0.45F, -1.6F, 0.45F, 0.23F, 16, spin * 0.84F, TIRE, RIM, HUB, light);
            }
        });
    }

    private static void glow(VertexConsumer vc, PoseStack.Pose pose, State state) {
        int light = FULL_BRIGHT;
        // Headlights (and a thin red strip at the back).
        for (int side = -1; side <= 1; side += 2) {
            float cx = side * 0.55F;
            Geo.box(vc, pose, cx - 0.12F, 0.45F, 3.04F, cx + 0.12F, 0.53F, 3.06F, LAMP, light);
            float rx = side * 0.7F;
            Geo.box(vc, pose, rx - 0.18F, 0.6F, -2.37F, rx + 0.18F, 0.66F, -2.35F, state.driven ? 0xFFE0242A : 0xFF701216, light);
        }
        // The jet: a dull ember idling, a roaring flame on boost.
        float f = state.flame;
        int core = Geo.withAlpha(0xFFB040, 0.45F + 0.55F * Math.max(f, state.driven ? 0.25F : 0.0F));
        Geo.box(vc, pose, -0.24F, 0.42F, -2.635F, 0.24F, 0.7F, -2.625F, core, light);
        if (f > 0.02F) {
            float flicker = 0.85F + 0.15F * Mth.sin(state.ageInTicks * 3.1F);
            float len = (0.6F + 1.4F * f) * flicker;
            int outer = Geo.withAlpha(0xFF7A20, 0.55F * f);
            int inner = Geo.withAlpha(0xFFE6A0, 0.8F * f);
            flame(vc, pose, 0.24F, len, outer, light);
            flame(vc, pose, 0.12F, len * 0.6F, inner, light);
        }
    }

    /** Two crossed tapering quads trailing back from the nozzle. */
    private static void flame(VertexConsumer vc, PoseStack.Pose pose, float r, float len, int color, int light) {
        float z0 = -2.64F;
        float z1 = z0 - len;
        float cy = 0.56F;
        Geo.doubleQuad(vc, pose, -r, cy, z0, r, cy, z0, r * 0.2F, cy, z1, -r * 0.2F, cy, z1, color, color, light);
        Geo.doubleQuad(vc, pose, 0.0F, cy - r, z0, 0.0F, cy + r, z0, 0.0F, cy + r * 0.2F, z1, 0.0F, cy - r * 0.2F, z1, color, color, light);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float wheelSpin;
        float steer;
        float flame;
        float recoil;
        boolean driven;
    }
}
