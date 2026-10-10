package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.client.speed.SpeedVisuals;
import com.danrod505.greenlantern.entity.FlightTrailEntity;
import com.danrod505.greenlantern.flash.SpeedFlags;
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
    /** Neutral (white) trail texture, tinted per vertex for the speedster trails. */
    private static final Identifier SPEED_TEXTURE = GreenLantern.id("textures/entity/speed_trail.png");
    private static final int KIND_LANTERN = 0;
    private static final int KIND_LANTERN_SUPERSONIC = 1;
    private static final int KIND_SPEED_BLUR = 2;
    private static final int KIND_SPEED_BOLT = 3;
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
            float[] data = build(points, attrs, cam, origin);
            if (data == null) continue;
            state.ribbons.add(new Ribbon(data, points.size(), visual.supersonic() ? KIND_LANTERN_SUPERSONIC : KIND_LANTERN));
        }
        extractSpeedTrails(state, mc, level, cam, origin, now, partialTick);
    }

    /**
     * Speedster trails: a red-orange motion blur the width of the body, wrapped in crackling
     * yellow Speed Force lightning that jumps around every couple of ticks.
     */
    private static void extractSpeedTrails(State state, Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, double now, float partialTick) {
        for (SpeedVisuals.Visual visual : SpeedVisuals.all()) {
            if (visual.trail.size() < 2) continue;
            int life = visual.trailLife();
            boolean supersonic = visual.has(SpeedFlags.SUPERSONIC);
            Entity owner = level.getEntity(visual.entityId);
            boolean skipNear = owner == mc.player && mc.options.getCameraType().isFirstPerson();
            List<Vec3> points = new ArrayList<>();
            List<Float> ages = new ArrayList<>();
            List<Long> seeds = new ArrayList<>();
            if (owner != null && !skipNear && visual.running()) {
                points.add(owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.5, 0));
                ages.add(0.0F);
                seeds.add(visual.trail.peekFirst().time() + 1);
            }
            for (SpeedVisuals.TrailPoint p : visual.trail) {
                float age = (float) ((now - p.time()) / life);
                if (age >= 1.0F) break;
                Vec3 point = new Vec3(p.x(), p.y(), p.z());
                if (skipNear && points.isEmpty() && point.distanceToSqr(cam) < 2.5 * 2.5) continue;
                points.add(point);
                ages.add(Math.max(0.0F, age));
                seeds.add(p.time());
            }
            int n = points.size();
            if (n < 2) continue;
            float width = supersonic ? 0.55F : 0.45F;
            List<float[]> attrs = new ArrayList<>();
            for (int i = 0; i < n; i++) attrs.add(new float[] {width, 1.0F - ages.get(i)});
            float[] blur = build(points, attrs, cam, origin);
            if (blur != null) state.ribbons.add(new Ribbon(blur, n, KIND_SPEED_BLUR));

            // Lightning: jittered copies of the path that re-strike every other tick.
            long strike = (long) now / 2;
            int bolts = supersonic ? 3 : 2;
            for (int b = 0; b < bolts; b++) {
                List<Vec3> bolt = new ArrayList<>(n);
                List<float[]> boltAttrs = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    long seed = seeds.get(i) * 341873128712L + b * 132897987541L + strike * 2654435761L;
                    double amp = i == 0 ? 0.0 : (0.25 + 0.2 * b) * (supersonic ? 1.3 : 1.0);
                    bolt.add(points.get(i).add(jitter(seed) * amp, jitter(seed >>> 7) * amp, jitter(seed >>> 13) * amp));
                    boltAttrs.add(new float[] {0.05F, 1.0F - ages.get(i)});
                }
                float[] data = build(bolt, boltAttrs, cam, origin);
                if (data != null) state.ribbons.add(new Ribbon(data, n, KIND_SPEED_BOLT));
            }
        }
    }

    private static double jitter(long seed) {
        seed = (seed ^ (seed >>> 33)) * 0xff51afd7ed558ccdL;
        seed = (seed ^ (seed >>> 33)) * 0xc4ceb9fe1a85ec53L;
        seed ^= seed >>> 33;
        return ((seed & 0xFFFF) / 65535.0) * 2.0 - 1.0;
    }

    /** Camera-facing ribbon data from points with {width, alpha} attributes; null if too short. */
    private static float[] build(List<Vec3> points, List<float[]> attrs, Vec3 cam, Vec3 origin) {
        int n = points.size();
        if (n < 2) return null;
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
        return data;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.ribbons.isEmpty()) return;
        List<Ribbon> ribbons = List.copyOf(state.ribbons);
        if (ribbons.stream().anyMatch(r -> r.kind <= KIND_LANTERN_SUPERSONIC)) {
            collector.submitCustomGeometry(poseStack, HardLight.type(TEXTURE), (pose, vc) -> {
                for (Ribbon ribbon : ribbons) {
                    if (ribbon.kind > KIND_LANTERN_SUPERSONIC) continue;
                    boolean supersonic = ribbon.kind == KIND_LANTERN_SUPERSONIC;
                    // Wide, soft outer glow, then the bright core.
                    emit(vc, pose, ribbon, 2.4F, supersonic ? 0.45F : 0.32F, HardLight.color(1, 0.0F));
                    emit(vc, pose, ribbon, 1.0F, 0.95F, HardLight.color(1, supersonic ? 1.0F : 0.6F));
                }
            });
        }
        if (ribbons.stream().anyMatch(r -> r.kind > KIND_LANTERN_SUPERSONIC)) {
            collector.submitCustomGeometry(poseStack, HardLight.type(SPEED_TEXTURE), (pose, vc) -> {
                for (Ribbon ribbon : ribbons) {
                    if (ribbon.kind == KIND_SPEED_BLUR) {
                        emit(vc, pose, ribbon, 1.6F, 0.28F, 0xFFC81E0A);
                        emit(vc, pose, ribbon, 0.8F, 0.55F, 0xFFFF7A1C);
                    } else if (ribbon.kind == KIND_SPEED_BOLT) {
                        emit(vc, pose, ribbon, 3.0F, 0.45F, 0xFFFFB800);
                        emit(vc, pose, ribbon, 1.0F, 1.0F, 0xFFFFF8C8);
                    }
                }
            });
        }
    }

    private static void emit(VertexConsumer vc, PoseStack.Pose pose, Ribbon ribbon, float widthScale, float alphaScale, int rgb) {
        float[] d = ribbon.data;
        for (int i = 0; i < ribbon.count - 1; i++) {
            int a = i * STRIDE;
            int b = (i + 1) * STRIDE;
            int ca = withAlpha(rgb, d[a + 7] * alphaScale);
            int cb = withAlpha(rgb, d[b + 7] * alphaScale);
            vertex(vc, pose, d, a, -widthScale, 0.0F, ca);
            vertex(vc, pose, d, a, widthScale, 1.0F, ca);
            vertex(vc, pose, d, b, widthScale, 1.0F, cb);
            vertex(vc, pose, d, b, -widthScale, 0.0F, cb);
        }
    }

    private static int withAlpha(int rgb, float alpha) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | (rgb & 0xFFFFFF);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float[] d, int o, float side, float v, int color) {
        HardLight.vertex(vc, pose, d[o] + d[o + 3] * side, d[o + 1] + d[o + 4] * side, d[o + 2] + d[o + 5] * side,
                d[o + 6], v, color, 0, 1, 0);
    }

    record Ribbon(float[] data, int count, int kind) {}

    public static class State extends EntityRenderState {
        final List<Ribbon> ribbons = new ArrayList<>();
    }
}
