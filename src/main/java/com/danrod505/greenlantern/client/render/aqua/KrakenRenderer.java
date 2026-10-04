package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.KrakenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders the Kraken: the model (with glowing eyes on top), the heavy body motion (slumping after
 * each step, swaying, leaning into the slam, rising out of the deep, sinking when it dies) and the
 * water jet, a scrolling stream of water from its siphon to where it lands.
 */
public class KrakenRenderer extends EntityRenderer<KrakenEntity, KrakenRenderState> {
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/kraken.png");
    private static final Identifier EYES = GreenLantern.id("textures/entity/kraken_eyes.png");
    private static final Identifier JET = GreenLantern.id("textures/entity/kraken_jet.png");
    private final KrakenModel model;

    public KrakenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new KrakenModel(context.bakeLayer(KrakenModel.LAYER));
        this.shadowRadius = 3.5F;
    }

    @Override
    public KrakenRenderState createRenderState() {
        return new KrakenRenderState();
    }

    @Override
    protected boolean affectedByCulling(KrakenEntity entity) {
        return false;
    }

    @Override
    public void extractRenderState(KrakenEntity kraken, KrakenRenderState s, float partialTick) {
        super.extractRenderState(kraken, s, partialTick);
        s.yaw = kraken.getYRot(partialTick);
        s.pitch = kraken.bodyPitch(partialTick);
        s.swim = kraken.swimBlend(partialTick);
        s.walkPhase = Mth.lerp(partialTick, kraken.walkPhaseO, kraken.walkPhase);
        s.walkAmount = Mth.lerp(partialTick, kraken.walkAmountO, kraken.walkAmount);
        s.swimWave = Mth.lerp(partialTick, kraken.swimWaveO, kraken.swimWave);
        s.swimPower = Mth.lerp(partialTick, kraken.swimPowerO, kraken.swimPower);
        s.jet = Mth.lerp(partialTick, kraken.jetBlendO, kraken.jetBlend);
        s.bob = Mth.lerp(partialTick, kraken.bobO, kraken.bob);
        s.squash = Mth.lerp(partialTick, kraken.squashO, kraken.squash);
        s.slamTime = kraken.slamTime(partialTick);
        s.slamLeft = kraken.slamLeftClient;
        s.hurt = kraken.hurtTicks > 0;
        s.death = kraken.clientDeathTicks < 0 ? 0.0F : Math.min(1.0F, (kraken.clientDeathTicks + partialTick) / KrakenEntity.DEATH_TICKS);
        s.emerge = Math.min(1.0F, (kraken.tickCount + partialTick) / KrakenEntity.EMERGE_TICKS);
        s.jetting = kraken.isJetting() && s.jet > 0.25F;
        Vec3 pos = kraken.getPosition(partialTick);
        Vec3 siphon = kraken.bodyPoint(KrakenEntity.SIPHON_FORWARD, KrakenEntity.SIPHON_UP, 0.0, partialTick).subtract(pos);
        Vec3 end = kraken.jetEnd(partialTick).subtract(pos);
        s.siphon.set((float) siphon.x, (float) siphon.y, (float) siphon.z);
        s.jetEnd.set((float) end.x, (float) end.y, (float) end.z);
    }

    private static float ease(float k) {
        k = Mth.clamp(k, 0.0F, 1.0F);
        return k * k * (3.0F - 2.0F * k);
    }

    @Override
    public void submit(KrakenRenderState s, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float land = 1.0F - s.swim;
        float alive = 1.0F - s.death;
        float walk = s.walkAmount * land * alive;
        float headY = KrakenEntity.headHeight(s.swim);
        // Rising out of the deep, sinking when it dies.
        float emergeDrop = (1.0F - ease(s.emerge)) * (headY + 3.0F);
        float sink = s.death * s.death * headY * 0.8F;

        // Leaning: into the walk, back when winding up the slam, forward on the blow, back from the jet's push.
        float lean = 4.0F * walk;
        if (s.slamTime < KrakenEntity.SLAM_TICKS) {
            float st = s.slamTime;
            lean += st < 6.0F ? -6.0F * ease(st / 6.0F)
                    : st < 12.0F ? Mth.lerp(ease((st - 6.0F) / 4.0F), -6.0F, 9.0F)
                    : 9.0F * (1.0F - ease((st - 12.0F) / (KrakenEntity.SLAM_TICKS - 12.0F)));
        }
        lean -= 5.0F * s.jet * land;
        lean += 18.0F * s.death * land;
        float roll = Mth.sin(s.walkPhase) * 3.0F * walk + 24.0F * ease(s.death) + (s.jet > 0 ? 0.6F * s.jet * Mth.sin(s.ageInTicks * 2.1F) : 0.0F);

        poseStack.pushPose();
        poseStack.translate(0.0F, headY - s.bob - emergeDrop - sink, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - s.yaw));
        poseStack.translate(0.0F, 0.0F, -KrakenEntity.HEAD_FORWARD_SWIM * s.swim);
        poseStack.mulPose(Axis.XP.rotationDegrees(-(s.pitch + lean)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        float squash = s.squash * land;
        float k = KrakenModel.SCALE;
        poseStack.scale(-k * (1.0F + 0.025F * squash), -k * (1.0F - 0.04F * squash), k * (1.0F + 0.025F * squash));
        model.setupAnim(s);
        int overlay = OverlayTexture.pack(0.0F, s.hurt);
        collector.submitModel(model, s, poseStack, model.renderType(TEXTURE), s.lightCoords, overlay, s.outlineColor, null);
        if (s.death < 0.8F) {
            collector.submitModel(model, s, poseStack, RenderTypes.eyes(EYES), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0, null);
        }
        poseStack.popPose();

        if (s.jetting) submitJet(s, poseStack, collector);
        super.submit(s, poseStack, collector, camera);
    }

    // ---- the water jet ----------------------------------------------------------------------------------

    private static void submitJet(KrakenRenderState s, PoseStack poseStack, SubmitNodeCollector collector) {
        Vector3f from = new Vector3f(s.siphon);
        Vector3f to = new Vector3f(s.jetEnd);
        Vector3f dir = new Vector3f(to).sub(from);
        float length = dir.length();
        if (length < 0.5F) return;
        dir.div(length);
        // Two directions across the stream.
        Vector3f a = Math.abs(dir.y) < 0.95F ? new Vector3f(0, 1, 0).cross(dir).normalize() : new Vector3f(1, 0, 0).cross(dir).normalize();
        Vector3f b = new Vector3f(dir).cross(a).normalize();
        float age = s.ageInTicks;
        float strength = Mth.clamp(s.jet, 0.0F, 1.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(JET), (pose, vc) -> {
            // Outer spray, then the dense core.
            stream(vc, pose, from, dir, a, b, length, 0.55F, 1.9F, age * 1.6F, 0x5AA8E890, strength, 1.3F);
            stream(vc, pose, from, dir, a, b, length, 0.3F, 0.85F, age * 2.4F, 0xD0F2FCD8, strength, 0.0F);
        });
    }

    /**
     * A tapering stream made of three crossed ribbons, in segments that wobble a little; the texture
     * scrolls along it (quads are split where the texture repeats, so it never relies on wrapping).
     */
    private static void stream(VertexConsumer vc, PoseStack.Pose pose, Vector3f from, Vector3f dir, Vector3f a, Vector3f b,
                               float length, float r0, float r1, float scroll, int rgba, float strength, float wobble) {
        int segments = Math.max(2, Mth.ceil(length / 1.5F));
        int alpha = (int) ((rgba & 0xFF) * strength);
        int color = alpha << 24 | (rgba >>> 8);
        for (int ribbon = 0; ribbon < 3; ribbon++) {
            float angle = ribbon * Mth.PI / 3.0F;
            Vector3f across = new Vector3f(a).mul(Mth.cos(angle)).add(new Vector3f(b).mul(Mth.sin(angle)));
            for (int i = 0; i < segments; i++) {
                float t0 = (float) i / segments;
                float t1 = (float) (i + 1) / segments;
                float va = t0 * length / 3.0F - scroll * 0.1F;
                float vb = t1 * length / 3.0F - scroll * 0.1F;
                float base = Mth.floor(va);
                if (vb - base <= 1.0F) {
                    ribbon(vc, pose, from, dir, across, length, r0, r1, wobble, scroll, ribbon, segments, t0, t1, va - base, vb - base, color);
                } else {
                    float tm = Mth.lerp((base + 1.0F - va) / (vb - va), t0, t1);
                    ribbon(vc, pose, from, dir, across, length, r0, r1, wobble, scroll, ribbon, segments, t0, tm, va - base, 1.0F, color);
                    ribbon(vc, pose, from, dir, across, length, r0, r1, wobble, scroll, ribbon, segments, tm, t1, 0.0F, vb - base - 1.0F, color);
                }
            }
        }
    }

    private static float width(float t, float r0, float r1, float wobble, float scroll, int ribbon, int segments) {
        return Mth.lerp(t, r0, r1) * (1.0F + wobble * 0.12F * Mth.sin(scroll * 0.7F + t * segments * 1.3F + ribbon));
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, Vector3f from, Vector3f dir, Vector3f across, float length,
                               float r0, float r1, float wobble, float scroll, int ribbon, int segments,
                               float t0, float t1, float v0, float v1, int color) {
        float w0 = width(t0, r0, r1, wobble, scroll, ribbon, segments);
        float w1 = width(t1, r0, r1, wobble, scroll, ribbon, segments);
        Vector3f p0 = new Vector3f(dir).mul(t0 * length).add(from);
        Vector3f p1 = new Vector3f(dir).mul(t1 * length).add(from);
        Vector3f p0a = new Vector3f(across).mul(w0).add(p0);
        Vector3f p0b = new Vector3f(across).mul(-w0).add(p0);
        Vector3f p1a = new Vector3f(across).mul(w1).add(p1);
        Vector3f p1b = new Vector3f(across).mul(-w1).add(p1);
        // Both sides of the ribbon.
        quad(vc, pose, p0b, p0a, p1a, p1b, v0, v1, color);
        quad(vc, pose, p0a, p0b, p1b, p1a, v0, v1, color);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float v0, float v1, int color) {
        vertex(vc, pose, p0, 0.0F, v0, color);
        vertex(vc, pose, p1, 1.0F, v0, color);
        vertex(vc, pose, p2, 1.0F, v1, color);
        vertex(vc, pose, p3, 0.0F, v1, color);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, float u, float v, int color) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
