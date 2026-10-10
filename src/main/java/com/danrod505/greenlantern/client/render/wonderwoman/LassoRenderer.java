package com.danrod505.greenlantern.client.render.wonderwoman;

import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The Lasso of Truth: a glowing golden rope from Wonder Woman's hand to its loop, which is wide open
 * in flight and closes tight around the creature it catches.
 */
public class LassoRenderer extends EntityRenderer<LassoEntity, LassoRenderer.State> {
    private static final int SEGMENTS = 18;

    public LassoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(LassoEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        // The rope can cross the screen even when the loop itself is out of view.
        return entity.getOwner() != null || super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public void extractRenderState(LassoEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        LivingEntity held = entity.boundTarget();
        state.bound = held != null;
        if (held != null) {
            // Around the creature's middle, wherever it is this frame.
            Vec3 at = held.getPosition(partialTick).add(0, held.getBbHeight() * 0.5, 0);
            state.loopOffset = at.subtract(state.x, state.y, state.z);
            state.loopRadius = held.getBbWidth() * 0.62F + 0.06F;
        } else {
            state.loopOffset = Vec3.ZERO;
            state.loopRadius = entity.state() == LassoEntity.STATE_RETURNING ? 0.25F : 0.55F;
        }
        Entity owner = entity.getOwner();
        if (owner instanceof Player player) {
            Vec3 pos = player.getPosition(partialTick);
            float body = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(body), 0, Mth.cos(body));
            Vec3 right = new Vec3(-Mth.cos(body), 0, -Mth.sin(body));
            Vec3 hand = pos.add(0, player.isCrouching() ? 0.95 : 1.25, 0).add(right.scale(0.38)).add(forward.scale(0.3));
            state.rope = hand.subtract(state.x, state.y, state.z);
        } else {
            state.rope = null;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 rope = state.rope;
        Vec3 loop = state.loopOffset;
        float spin = state.ageInTicks * 0.35F;
        collector.submitCustomGeometry(poseStack, HardLight.type(Geo.PLAIN), (pose, vc) -> {
            if (rope != null && rope.subtract(loop).lengthSqr() > 0.01) {
                // Taut when it holds a creature, a light sag in flight.
                Vec3 span = loop.subtract(rope);
                double sag = state.bound ? Math.min(0.4, span.length() * 0.02) : Math.min(1.0, span.length() * 0.05);
                for (int i = 0; i < SEGMENTS; i++) {
                    float t0 = (float) i / SEGMENTS;
                    float t1 = (float) (i + 1) / SEGMENTS;
                    Vec3 p0 = rope.add(span.scale(t0)).add(0, -Math.sin(t0 * Math.PI) * sag, 0);
                    Vec3 p1 = rope.add(span.scale(t1)).add(0, -Math.sin(t1 * Math.PI) * sag, 0);
                    Rope.segment(vc, pose, p0, p1, 0.028F, Rope.GOLD, Rope.GLOW);
                }
            }
            // The loop (two turns when it binds).
            Rope.loop(vc, pose, loop, state.loopRadius, 16, spin, 0.035F, Rope.GOLD, Rope.GLOW);
            if (state.bound) {
                Rope.loop(vc, pose, loop.add(0, 0.18, 0), state.loopRadius * 1.02, 16, -spin, 0.03F, Rope.GOLD, Rope.GLOW);
            }
        });
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        boolean bound;
        float loopRadius;
        Vec3 loopOffset = Vec3.ZERO;
        Vec3 rope;
    }
}
