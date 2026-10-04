package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.aqua.SwimVisuals;
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
 * Draws the energy trails of every flying Lantern (and the speedster and swimming trails) as camera-facing ribbons: a wide soft glow and a
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
    private static final int KIND_AQUA_WAKE = 4;
    private static final int KIND_AQUA_SPIRAL = 5;
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
        extractSwimTrails(state, mc, level, cam, origin, now, partialTick);
    }

    /**
     * Aquaman's water trail: a sea-green wake the width of the body, wrapped in two streams of
     * bubbles that spiral around it.
     */
    private static void extractSwimTrails(State state, Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, double now, float partialTick) {
        for (SwimVisuals.Visual visual : SwimVisuals.all()) {
            if (visual.trail.size() < 2) continue;
            int life = visual.trailLife();
            Entity owner = level.getEntity(visual.entityId);
            boolean skipNear = owner == mc.player && mc.options.getCameraType().isFirstPerson();
            List<Vec3> points = new ArrayList<>();
            List<Float> ages = new ArrayList<>();
            List<Long> times = new ArrayList<>();
            if (owner != null && !skipNear && visual.swimming()) {
                // Head of the trail: on the swimmer, at the height of the trail points.
                points.add(owner.getPosition(partialTick).add(0, visual.trail.peekFirst().y() - owner.getY(), 0));
                ages.add(0.0F);
                times.add(visual.trail.peekFirst().time() + 1);
            }
            for (SwimVisuals.TrailPoint p : visual.trail) {
                float age = (float) ((now - p.time()) / life);
                if (age >= 1.0F) break;
                Vec3 point = new Vec3(p.x(), p.y(), p.z());
                if (skipNear && points.isEmpty() && point.distanceToSqr(cam) < 2.5 * 2.5) continue;
                points.add(point);
                ages.add(Math.max(0.0F, age));
                times.add(p.time());
            }
            int n = points.size();
            if (n < 2) continue;
            List<float[]> attrs = new ArrayList<>();
            for (int i = 0; i < n; i++) attrs.add(new float[] {0.5F, 1.0F - ages.get(i)});
            float[] wake = build(points, attrs, cam, origin);
            if (wake != null) state.ribbons.add(new Ribbon(wake, n, KIND_AQUA_WAKE));

            // Two bubble streams twisting around the wake (the twist is fixed to the water, not the swimmer).
            for (int strand = 0; strand < 2; strand++) {
                List<Vec3> spiral = new ArrayList<>(n);
                List<float[]> spiralAttrs = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    Vec3 p = points.get(i);
                    Vec3 tangent = points.get(Math.min(n - 1, i + 1)).subtract(points.get(Math.max(0, i - 1)));
                    if (tangent.lengthSqr() < 1.0E-6) tangent = new Vec3(0, 0, 1);
                    tangent = tangent.normalize();
                    Vec3 a = Math.abs(tangent.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                    Vec3 u = tangent.cross(a).normalize();
                    Vec3 v = tangent.cross(u).normalize();
                    double phase = times.get(i) * 0.9 + strand * Math.PI;
                    double radius = i == 0 ? 0.0 : 0.35 + 0.35 * ages.get(i);
                    spiral.add(p.add(u.scale(Math.cos(phase) * radius)).add(v.scale(Math.sin(phase) * radius)));
                    spiralAttrs.add(new float[] {0.07F, 1.0F - ages.get(i)});
                }
                float[] data = build(spiral, spiralAttrs, cam, origin);
                if (data != null) state.ribbons.add(new Ribbon(data, n, KIND_AQUA_SPIRAL));
            }
        }
    }

