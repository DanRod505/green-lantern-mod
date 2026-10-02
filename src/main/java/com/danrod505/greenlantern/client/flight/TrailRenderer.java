package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.entity.FlightTrailEntity;
import com.danrod505.greenlantern.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the energy trails of every flying Lantern as camera-facing ribbons: a wide soft glow and a
 * bright core that taper and fade out with age. The geometry is attached to a client-only entity
 * that follows the camera, so trails are visible even when their owner is off screen.
 */
public class TrailRenderer extends EntityRenderer<FlightTrailEntity, TrailRenderer.State> {
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/trail.png");
    /** Floats per ribbon point: center xyz, side xyz, u, alpha. */
    private static final int STRIDE = 8;
    private static FlightTrailEntity holder;

    public TrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Keeps the client-only trail holder entity alive in the current level, at the camera. */
    public static void tickHolder() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            holder = null;
            return;
        }
        if (holder == null || holder.isRemoved() || holder.level() != level) {
            holder = new FlightTrailEntity(ModEntities.FLIGHT_TRAIL.get(), level);
            holder.setId(-2814);
            Vec3 cam = mc.gameRenderer.getMainCamera().position();
            holder.setPos(cam.x, cam.y, cam.z);
            level.addEntity(holder);
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        holder.setPos(cam.x, cam.y, cam.z);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected boolean affectedByCulling(FlightTrailEntity entity) {
        return false;
    }

    @Override
    public void extractRenderState(FlightTrailEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ribbons.clear();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double now = level.getGameTime() + partialTick;
        Vec3 origin = new Vec3(state.x, state.y, state.z);

        for (FlightVisuals.Visual visual : FlightVisuals.all()) {
            if (visual.trail.size() < 2) continue;
            int life = visual.supersonic() ? 34 : 22;
            List<Vec3> points = new ArrayList<>();
            List<float[]> attrs = new ArrayList<>();
            // Head of the ribbon: the owner's interpolated position (keeps it attached while moving).
            Entity owner = level.getEntity(visual.entityId);
            // In first person your own trail starts a few blocks behind you so it never covers the view.
            boolean skipNear = owner == mc.player && mc.options.getCameraType().isFirstPerson();
            if (owner != null && !skipNear) {
                Vec3 head = owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.45, 0);
                points.add(head);
                attrs.add(new float[] {visual.trail.peekFirst().width(), 1.0F});
            }
            for (FlightVisuals.TrailPoint p : visual.trail) {
                float age = (float) ((now - p.time()) / life);
                if (age >= 1.0F) break;
                Vec3 point = new Vec3(p.x(), p.y(), p.z());
                if (skipNear && points.isEmpty() && point.distanceToSqr(cam) < 3.5 * 3.5) continue;
                points.add(point);
                attrs.add(new float[] {p.width(), 1.0F - age});
            }
            int n = points.size();
            if (n < 2) continue;
            float[] data = new float[n * STRIDE];
            float length = 0;
            for (int i = 0; i < n; i++) {
                Vec3 p = points.get(i);
                Vec3 prev = points.get(Math.max(0, i - 1));
                Vec3 next = points.get(Math.min(n - 1, i + 1));
                Vec3 tangent = next.subtract(prev);
                Vec3 toCam = cam.subtract(p);
                Vec3 side = tangent.cross(toCam);
                if (side.lengthSqr() < 1.0E-8) side = new Vec3(0, 1, 0);
                float taper = 1.0F - 0.75F * i / (n - 1);
                float alpha = attrs.get(i)[1];
                side = side.normalize().scale(attrs.get(i)[0] * taper * (0.4F + 0.6F * alpha));
                if (i > 0) length += (float) p.distanceTo(prev);
                Vec3 rel = p.subtract(origin);
                int o = i * STRIDE;
                data[o] = (float) rel.x;
                data[o + 1] = (float) rel.y;
                data[o + 2] = (float) rel.z;
                data[o + 3] = (float) side.x;
                data[o + 4] = (float) side.y;
                data[o + 5] = (float) side.z;
                data[o + 6] = length * 0.25F;
                data[o + 7] = alpha * alpha;
            }
            state.ribbons.add(new Ribbon(data, n, visual.supersonic()));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.ribbons.isEmpty()) return;
        List<Ribbon> ribbons = List.copyOf(state.ribbons);
        collector.submitCustomGeometry(poseStack, HardLight.type(TEXTURE), (pose, vc) -> {
            for (Ribbon ribbon : ribbons) {
                // Wide, soft outer glow, then the bright core.
                emit(vc, pose, ribbon, 2.4F, ribbon.supersonic ? 0.45F : 0.32F, 0.0F);
                emit(vc, pose, ribbon, 1.0F, 0.95F, ribbon.supersonic ? 1.0F : 0.6F);
            }
        });
    }

    private static void emit(VertexConsumer vc, PoseStack.Pose pose, Ribbon ribbon, float widthScale, float alphaScale, float boost) {
        float[] d = ribbon.data;
        for (int i = 0; i < ribbon.count - 1; i++) {
            int a = i * STRIDE;
            int b = (i + 1) * STRIDE;
            int ca = HardLight.color(Mth.clamp(d[a + 7] * alphaScale, 0, 1), boost);
            int cb = HardLight.color(Mth.clamp(d[b + 7] * alphaScale, 0, 1), boost);
            vertex(vc, pose, d, a, -widthScale, 0.0F, ca);
            vertex(vc, pose, d, a, widthScale, 1.0F, ca);
            vertex(vc, pose, d, b, widthScale, 1.0F, cb);
            vertex(vc, pose, d, b, -widthScale, 0.0F, cb);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float[] d, int o, float side, float v, int color) {
        HardLight.vertex(vc, pose, d[o] + d[o + 3] * side, d[o + 1] + d[o + 4] * side, d[o + 2] + d[o + 5] * side,
                d[o + 6], v, color, 0, 1, 0);
    }

    record Ribbon(float[] data, int count, boolean supersonic) {}

    public static class State extends EntityRenderState {
        final List<Ribbon> ribbons = new ArrayList<>();
    }
}
