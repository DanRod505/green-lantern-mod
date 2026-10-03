package com.danrod505.greenlantern.client.render;

import com.danrod505.greenlantern.entity.MechaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The giant hard-light mecha, built from glowing panels on an articulated skeleton: legs with
 * knees and ankles, a torso that turns towards the pilot's aim, a chest cockpit with an open
 * canopy, shoulder missile pods, arms with fist cannons that aim at the laser target, a head with
 * a visor and V-fin, and back thrusters whose flames grow with the thrust.
 * <p>
 * Coordinates are in blocks, relative to the feet; +Z is forward and +X is the mecha's left.
 */
public class MechaRenderer extends EntityRenderer<MechaEntity, MechaRenderer.State> {
    private static final float HIP = MechaEntity.HIP_HEIGHT;
    private static final float THIGH = 2.0F;
    private static final float SHIN = 2.0F;
    private static final float FOOT = 0.4F;
    private static final float UPPER_ARM = 1.75F;
    private static final float FOREARM = 1.7F;

    public MechaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MechaEntity mecha, State s, float partialTick) {
        super.extractRenderState(mecha, s, partialTick);
        s.yaw = mecha.getYRot(partialTick);
        s.twist = mecha.twist(partialTick);
        Player pilot = mecha.getOwner();
        boolean piloted = pilot != null && pilot.getVehicle() == mecha;
        s.pitch = piloted ? pilot.getViewXRot(partialTick) : 0.0F;
        s.walkPhase = Mth.lerp(partialTick, mecha.walkPhaseO, mecha.walkPhase);
        s.walkAmount = Mth.lerp(partialTick, mecha.walkAmountO, mecha.walkAmount);
        s.flyPose = Mth.lerp(partialTick, mecha.flyPoseO, mecha.flyPose);
        s.thrust = Mth.lerp(partialTick, mecha.thrustO, mecha.thrust);
        s.aim = Mth.lerp(partialTick, mecha.aimBlendO, mecha.aimBlend);
        s.squash = Mth.lerp(partialTick, mecha.squashO, mecha.squash);
        s.recoil = Mth.lerp(partialTick, mecha.recoilO, mecha.recoil);
        s.laser = mecha.isFiringLaser();
        Vec3 end = mecha.laserEndO.lerp(mecha.laserEnd, partialTick);
        s.laserTarget = new Vector3f((float) (end.x - s.x), (float) (end.y - s.y), (float) (end.z - s.z));
        Minecraft mc = Minecraft.getInstance();
        s.firstPerson = piloted && pilot == mc.player && mc.options.getCameraType().isFirstPerson();
        pose(s);
    }

    /** Works out the skeleton's joint angles for this frame. */
    private static void pose(State s) {
        float a = s.walkAmount * (1.0F - s.flyPose);
        float f = s.flyPose;
        float sq = s.squash;
        for (int i = 0; i < 2; i++) {
            float phase = s.walkPhase + i * Mth.PI;
            float sin = Mth.sin(phase);
            float cos = Mth.cos(phase);
            float hip = -sin * 26.0F * a;
            float knee = a * (4.0F + 42.0F * Math.max(0.0F, cos) * Math.max(0.0F, cos));
            // Flight: legs trail behind with bent knees. Landing: deep crouch.
            hip += 16.0F * f + (i == 0 ? 0.0F : 10.0F * f) - 32.0F * sq;
            knee += (24.0F + (i == 0 ? 10.0F : 0.0F)) * f + 62.0F * sq;
            s.hip[i] = hip;
            s.knee[i] = knee;
            s.ankle[i] = -(hip + knee) * (1.0F - 0.6F * f) + (cos > 0 ? -10.0F * a * cos : 0.0F);
            s.armSwing[i] = sin * 20.0F * a;
        }
        // The body sinks so the planted foot stays on the ground.
        float lowest = 0.0F;
        for (int i = 0; i < 2; i++) {
            float h = s.hip[i] * Mth.DEG_TO_RAD;
            float k = (s.hip[i] + s.knee[i]) * Mth.DEG_TO_RAD;
            lowest = Math.max(lowest, THIGH * Mth.cos(h) + SHIN * Mth.cos(k) + FOOT);
        }
        float breathe = Mth.sin(s.ageInTicks * 0.07F) * 0.03F;
        s.bodyDrop = (HIP - lowest) * (1.0F - f) - breathe;
        s.lean = 3.0F * a + 12.0F * f + 10.0F * sq;
        s.roll = Mth.sin(s.walkPhase) * 2.5F * a;
        s.sway = Mth.sin(s.walkPhase) * 4.0F * a;
    }

    @Override
    protected AABB getBoundingBoxForCulling(MechaEntity mecha) {
        AABB box = mecha.getBoundingBox().inflate(3.0, 1.0, 3.0);
        if (mecha.isFiringLaser()) box = box.minmax(new AABB(mecha.laserEnd, mecha.laserEnd).inflate(1.0));
        return box;
    }

    @Override
    protected int getBlockLightLevel(MechaEntity entity, BlockPos pos) {
        return 15;
    }

    // ---- Colours ----------------------------------------------------------------------------------

    private static int argb(float alpha, int r, int g, int b) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | r << 16 | g << 8 | b;
    }

    /** Main armour: glowing panel with a soft shell. */
    private static void armor(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, float a) {
        HardLight.glowingBox(vc, p, x0, y0, z0, x1, y1, z1, a);
    }

    /** Raised plates and trims: brighter. */
    private static void plate(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, float a) {
        HardLight.box(vc, p, x0, y0, z0, x1, y1, z1, HardLight.color(0.9F * a, 0.6F));
    }

    /** Joints and inner frame: deep green, nearly solid. */
    private static void joint(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, float a) {
        HardLight.box(vc, p, x0, y0, z0, x1, y1, z1, argb(0.92F * a, 12, 120, 40));
    }

    /** Lights: visor, emblems, cannon muzzles. */
    private static void light(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, float a, float boost) {
        HardLight.box(vc, p, x0, y0, z0, x1, y1, z1, HardLight.color(a, boost));
    }

    /** Green Lantern emblem facing +Z (or -Z when {@code facing} is -1), centered on (cx, cy). */
    private static void emblem(VertexConsumer vc, PoseStack.Pose p, float cx, float cy, float z, float size, int facing, float a) {
        float z0 = facing > 0 ? z : z - 0.05F;
        float z1 = facing > 0 ? z + 0.05F : z;
        float outer = 0.6F * size;
        float inner = 0.36F * size;
        light(vc, p, cx - size, cy + 0.78F * size, z0, cx + size, cy + 0.98F * size, z1, a, 1.0F);
        light(vc, p, cx - size, cy - 0.98F * size, z0, cx + size, cy - 0.78F * size, z1, a, 1.0F);
        light(vc, p, cx - outer, cy + inner, z0, cx + outer, cy + outer, z1, a, 1.0F);
        light(vc, p, cx - outer, cy - outer, z0, cx + outer, cy - inner, z1, a, 1.0F);
        light(vc, p, cx - outer, cy - inner, z0, cx - inner, cy + inner, z1, a, 1.0F);
        light(vc, p, cx + inner, cy - inner, z0, cx + outer, cy + inner, z1, a, 1.0F);
    }

    // ---- Rendering --------------------------------------------------------------------------------

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        // Materializing: the frame builds up from the feet over the first second and a half.
        float build = Math.min(1.0F, s.ageInTicks / 30.0F) * 11.0F;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        ps.translate(0.0F, HIP - s.bodyDrop, 0.0F);

        float pelvisA = reveal(build, 4.4F);
        draw(collector, ps, (vc, p) -> pelvis(vc, p, pelvisA));
        for (int i = 0; i < 2; i++) {
            leg(collector, ps, s, i, build);
        }

        // Torso: turns towards the pilot's aim and leans into the movement.
        ps.pushPose();
        ps.translate(0.0F, MechaEntity.WAIST_HEIGHT - HIP, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(-s.twist - s.sway));
        ps.mulPose(Axis.XP.rotationDegrees(s.lean));
        ps.mulPose(Axis.ZP.rotationDegrees(s.roll));
        torso(collector, ps, s, build);
        for (int i = 0; i < 2; i++) {
            arm(collector, ps, s, i, build);
        }
        if (!s.firstPerson) head(collector, ps, s, build);
        ps.popPose();
        ps.popPose();

        // Glow where the lasers hit.
        if (s.laser && s.aim > 0.6F) {
            ps.pushPose();
            ps.translate(s.laserTarget.x(), s.laserTarget.y(), s.laserTarget.z());
            ps.mulPose(camera.orientation);
            float pulse = 0.8F + 0.2F * Mth.sin(s.ageInTicks * 2.1F);
            collector.submitCustomGeometry(ps, HardLight.type(HardLight.CORE), (pose, vc) -> {
                HardLight.billboard(vc, pose, 0.9F * pulse, HardLight.color(1.0F, 1.0F));
                HardLight.billboard(vc, pose, 1.8F * pulse, HardLight.color(0.35F, 0.2F));
            });
            ps.popPose();
        }
        super.submit(s, ps, collector, camera);
    }

    private static float reveal(float build, float height) {
        return Mth.clamp(build - height, 0.0F, 1.0F);
    }

    private interface Geometry {
        void draw(VertexConsumer vc, PoseStack.Pose pose);
    }

    private static void draw(SubmitNodeCollector collector, PoseStack ps, Geometry geometry) {
        collector.submitCustomGeometry(ps, HardLight.type(HardLight.PANEL), (pose, vc) -> geometry.draw(vc, pose));
    }

    private static void pelvis(VertexConsumer vc, PoseStack.Pose p, float a) {
        if (a <= 0) return;
        armor(vc, p, -1.0F, -0.35F, -0.6F, 1.0F, 0.65F, 0.6F, a);
        // Codpiece with the belt emblem.
        plate(vc, p, -0.38F, -0.65F, 0.42F, 0.38F, 0.45F, 0.72F, a);
        emblem(vc, p, 0.0F, 0.05F, 0.72F, 0.22F, 1, a);
        // Belt.
        plate(vc, p, -1.05F, 0.45F, -0.65F, 1.05F, 0.65F, 0.65F, a);
        // Hip skirts.
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side > 0 ? 1.0F : -1.3F;
            plate(vc, p, x0, -0.95F, -0.5F, x0 + 0.3F, 0.5F, 0.55F, a);
            joint(vc, p, side * 0.75F - 0.33F, -0.33F, -0.33F, side * 0.75F + 0.33F, 0.33F, 0.33F, a);
        }
        // Rear plate.
        plate(vc, p, -0.7F, -0.6F, -0.75F, 0.7F, 0.45F, -0.55F, a);
    }

    private static void leg(SubmitNodeCollector collector, PoseStack ps, State s, int i, float build) {
        float side = i == 0 ? 1.0F : -1.0F;
        float thighA = reveal(build, 2.8F);
        float shinA = reveal(build, 1.0F);
        float footA = reveal(build, 0.0F);
        ps.pushPose();
        ps.translate(side * 0.75F, 0.0F, 0.0F);
        ps.mulPose(Axis.XP.rotationDegrees(s.hip[i]));
        draw(collector, ps, (vc, p) -> {
            if (thighA <= 0) return;
            armor(vc, p, -0.42F, -THIGH, -0.48F, 0.42F, 0.0F, 0.48F, thighA);
            plate(vc, p, -0.46F, -1.7F, 0.42F, 0.46F, -0.4F, 0.58F, thighA);
            plate(vc, p, side > 0 ? 0.42F : -0.54F, -1.6F, -0.35F, side > 0 ? 0.54F : -0.42F, -0.5F, 0.35F, thighA);
        });

        ps.translate(0.0F, -THIGH, 0.0F);
        ps.mulPose(Axis.XP.rotationDegrees(s.knee[i]));
        float thrust = s.thrust;
        float age = s.ageInTicks;
        draw(collector, ps, (vc, p) -> {
            if (shinA <= 0) return;
            joint(vc, p, -0.36F, -0.3F, -0.36F, 0.36F, 0.3F, 0.36F, shinA);
            plate(vc, p, -0.4F, -0.35F, 0.3F, 0.4F, 0.45F, 0.72F, shinA);
            light(vc, p, -0.12F, -0.05F, 0.72F, 0.12F, 0.15F, 0.76F, shinA, 1.0F);
            armor(vc, p, -0.46F, -SHIN, -0.48F, 0.46F, -0.1F, 0.5F, shinA);
            plate(vc, p, -0.5F, -1.75F, 0.48F, 0.5F, -0.4F, 0.62F, shinA);
            // Calf thruster.
            joint(vc, p, -0.32F, -1.5F, -0.7F, 0.32F, -0.5F, -0.46F, shinA);
            light(vc, p, -0.2F, -1.55F, -0.62F, 0.2F, -1.45F, -0.5F, shinA, 0.6F + 0.4F * thrust);
            if (thrust > 0.05F) flame(vc, p, 0.0F, -1.5F, -0.58F, 0.16F, 0.4F + 1.4F * thrust, age, shinA * thrust);
        });

        ps.translate(0.0F, -SHIN, 0.0F);
        ps.mulPose(Axis.XP.rotationDegrees(s.ankle[i]));
        draw(collector, ps, (vc, p) -> {
            if (footA <= 0) return;
            joint(vc, p, -0.4F, -0.12F, -0.4F, 0.4F, 0.2F, 0.4F, footA);
            armor(vc, p, -0.55F, -FOOT, -0.65F, 0.55F, -0.05F, 0.95F, footA);
            plate(vc, p, -0.5F, -FOOT, 0.95F, 0.5F, -0.12F, 1.35F, footA);
            plate(vc, p, -0.3F, -FOOT, -0.95F, 0.3F, -0.15F, -0.65F, footA);
            light(vc, p, -0.45F, -0.2F, 1.35F, 0.45F, -0.14F, 1.38F, footA, 1.0F);
        });
        ps.popPose();
    }

    /** Hard-light exhaust along -Y of the current pose, starting at (x, y, z). */
    private static void flame(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float radius, float length, float age, float a) {
        float flicker = 0.85F + 0.15F * Mth.sin(age * 3.7F + x * 5.0F);
        float len = length * flicker;
        HardLight.box(vc, p, x - radius * 0.45F, y - len, z - radius * 0.45F, x + radius * 0.45F, y, z + radius * 0.45F, HardLight.color(0.95F * a, 1.0F));
        HardLight.box(vc, p, x - radius, y - len * 0.75F, z - radius, x + radius, y, z + radius, HardLight.color(0.5F * a, 0.7F));
        HardLight.box(vc, p, x - radius * 1.6F, y - len * 0.45F, z - radius * 1.6F, x + radius * 1.6F, y, z + radius * 1.6F, HardLight.color(0.2F * a, 0.2F));
    }

    private static void torso(SubmitNodeCollector collector, PoseStack ps, State s, float build) {
        float coreA = reveal(build, 5.2F);
        float chestA = reveal(build, 6.3F);
        float upperA = reveal(build, 7.4F);
        boolean fp = s.firstPerson;
        draw(collector, ps, (vc, p) -> {
            // Abdomen with segmented plates.
            if (coreA > 0) {
                armor(vc, p, -0.75F, 0.0F, -0.5F, 0.75F, 0.7F, 0.5F, coreA);
                light(vc, p, -0.6F, 0.18F, 0.5F, 0.6F, 0.24F, 0.54F, coreA, 0.8F);
                light(vc, p, -0.6F, 0.42F, 0.5F, 0.6F, 0.48F, 0.54F, coreA, 0.8F);
            }
            if (chestA <= 0) return;
            if (!fp) {
                // Cockpit shell: floor, back, side walls, lower front.
                armor(vc, p, -1.3F, 0.6F, -0.8F, 1.3F, 0.8F, 0.8F, chestA);
                armor(vc, p, -1.3F, 0.8F, -0.85F, 1.3F, 2.7F, -0.6F, chestA);
                armor(vc, p, 1.1F, 0.6F, -0.85F, 1.4F, 2.5F, 0.85F, chestA);
                armor(vc, p, -1.4F, 0.6F, -0.85F, -1.1F, 2.5F, 0.85F, chestA);
                armor(vc, p, -1.3F, 0.6F, 0.7F, 1.3F, 1.05F, 0.92F, chestA);
                // Backpack with the big emblem, and its two main thrusters.
                armor(vc, p, -1.0F, 0.9F, -1.4F, 1.0F, 2.6F, -0.85F, chestA);
                plate(vc, p, -1.1F, 2.3F, -1.5F, 1.1F, 2.65F, -0.8F, chestA);
                emblem(vc, p, 0.0F, 1.7F, -1.4F, 0.42F, -1, chestA);
            }
            // Canopy frame (also seen from inside the cockpit) and its faint glass.
            plate(vc, p, 1.0F, 1.05F, 0.8F, 1.15F, 2.2F, 0.94F, chestA);
            plate(vc, p, -1.15F, 1.05F, 0.8F, -1.0F, 2.2F, 0.94F, chestA);
            plate(vc, p, -1.15F, 2.08F, 0.8F, 1.15F, 2.22F, 0.94F, chestA);
            plate(vc, p, -1.15F, 1.0F, 0.8F, 1.15F, 1.1F, 0.94F, chestA);
            HardLight.box(vc, p, -1.0F, 1.1F, 0.86F, 1.0F, 2.08F, 0.88F, HardLight.color(0.12F * chestA, 0.9F));
            if (upperA <= 0 || fp) return;
            // Upper chest with its emblem, and the collar.
            armor(vc, p, -1.45F, 2.2F, -0.85F, 1.45F, 2.75F, 0.95F, upperA);
            plate(vc, p, -1.2F, 2.75F, -0.6F, 1.2F, 2.85F, 0.7F, upperA);
            emblem(vc, p, 0.0F, 2.48F, 0.95F, 0.2F, 1, upperA);
            joint(vc, p, -0.7F, 2.75F, -0.5F, 0.7F, 2.95F, 0.4F, upperA);
        });

        // Main thrusters, angled down and back.
        if (fp || chestA <= 0) return;
        float thrust = s.thrust;
        float age = s.ageInTicks;
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.6F, 0.75F, -1.3F);
            ps.mulPose(Axis.XP.rotationDegrees(35.0F));
            draw(collector, ps, (vc, p) -> {
                joint(vc, p, -0.3F, -0.35F, -0.3F, 0.3F, 0.25F, 0.3F, chestA);
                plate(vc, p, -0.34F, -0.5F, -0.34F, 0.34F, -0.3F, 0.34F, chestA);
                light(vc, p, -0.2F, -0.52F, -0.2F, 0.2F, -0.48F, 0.2F, chestA, 0.6F + 0.4F * thrust);
                if (thrust > 0.03F) flame(vc, p, 0.0F, -0.5F, 0.0F, 0.26F, 0.8F + 3.2F * thrust, age, chestA * Math.min(1.0F, thrust * 1.5F));
            });
            ps.popPose();
        }
    }

    private static void arm(SubmitNodeCollector collector, PoseStack ps, State s, int i, float build) {
        float side = i == 0 ? 1.0F : -1.0F;
        float a = reveal(build, 7.0F);
        if (a <= 0) return;
        float recoil = s.recoil;
        ps.pushPose();
        ps.translate(side * MechaEntity.SHOULDER_X, MechaEntity.SHOULDER_Y, 0.0F);

        // Pauldron and missile pod (fixed to the shoulder).
        draw(collector, ps, (vc, p) -> {
            joint(vc, p, -0.42F, -0.42F, -0.42F, 0.42F, 0.42F, 0.42F, a);
            float in = side > 0 ? -0.5F : -0.75F;
            float out = side > 0 ? 0.75F : 0.5F;
            armor(vc, p, in, -0.3F, -0.75F, out, 0.65F, 0.75F, a);
            plate(vc, p, in - 0.04F, -0.42F, -0.8F, out + 0.04F, -0.3F, 0.8F, a);
            plate(vc, p, side > 0 ? 0.75F : -0.88F, -0.2F, -0.5F, side > 0 ? 0.88F : -0.75F, 0.55F, 0.5F, a);
            // Pod with 2x3 launch tubes on top; they flash when a salvo goes off.
            float kick = recoil * 0.08F;
            armor(vc, p, -0.5F, 0.65F - kick, -0.55F, 0.5F, 1.15F - kick, 0.65F, a);
            plate(vc, p, -0.55F, 0.6F - kick, 0.55F, 0.55F, 1.2F - kick, 0.7F, a);
            for (int col = -1; col <= 1; col += 2) {
                for (int row = 0; row < 3; row++) {
                    float x = col * 0.22F;
                    float z = 0.3F - row * 0.35F;
                    light(vc, p, x - 0.1F, 1.15F - kick, z - 0.1F, x + 0.1F, 1.19F - kick, z + 0.1F, a * (0.6F + 0.4F * recoil), 0.5F + 0.5F * recoil);
                }
            }
        });

        // The arm: swings while walking, aims at the laser target while firing.
        float swingPitch = s.armSwing[i] * (1.0F - s.flyPose) + 18.0F * s.flyPose;
        float aimYaw = 0.0F;
        float aimPitch = swingPitch;
        float elbow = 14.0F + 10.0F * s.walkAmount + 20.0F * s.flyPose;
        float splay = 6.0F + 10.0F * s.flyPose;
        if (s.aim > 0.0F) {
            Vector3f d = shoulderToTarget(s, side);
            float yawTo = (float) (Mth.atan2(d.x, d.z) * Mth.RAD_TO_DEG);
            float elev = (float) (Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
            aimYaw = Mth.lerp(s.aim, 0.0F, yawTo);
            aimPitch = Mth.lerp(s.aim, swingPitch, -(90.0F + elev));
            elbow = Mth.lerp(s.aim, elbow, 0.0F);
            splay = Mth.lerp(s.aim, splay, 0.0F);
            s.beamLength[i] = d.length();
        }
        ps.mulPose(Axis.YP.rotationDegrees(aimYaw));
        ps.mulPose(Axis.XP.rotationDegrees(aimPitch));
        ps.mulPose(Axis.ZP.rotationDegrees(side * splay));
        draw(collector, ps, (vc, p) -> {
            armor(vc, p, -0.36F, -UPPER_ARM, -0.36F, 0.36F, -0.2F, 0.36F, a);
            plate(vc, p, side > 0 ? 0.36F : -0.48F, -1.45F, -0.3F, side > 0 ? 0.48F : -0.36F, -0.55F, 0.3F, a);
        });
        ps.translate(0.0F, -UPPER_ARM, 0.0F);
        ps.mulPose(Axis.XP.rotationDegrees(-elbow));
        boolean firing = s.laser && s.aim > 0.6F;
        float pulse = 0.75F + 0.25F * Mth.sin(s.ageInTicks * 1.9F);
        draw(collector, ps, (vc, p) -> {
            joint(vc, p, -0.32F, -0.32F, -0.32F, 0.32F, 0.32F, 0.32F, a);
            armor(vc, p, -0.46F, -FOREARM, -0.46F, 0.46F, -0.15F, 0.46F, a);
            plate(vc, p, side > 0 ? 0.46F : -0.62F, -1.5F, -0.4F, side > 0 ? 0.62F : -0.46F, -0.3F, 0.4F, a);
            plate(vc, p, -0.5F, -0.5F, -0.5F, 0.5F, -0.35F, 0.5F, a);
            // Fist with the cannon muzzle.
            armor(vc, p, -0.38F, -2.2F, -0.36F, 0.38F, -FOREARM, 0.4F, a);
            light(vc, p, -0.24F, -2.26F, -0.24F, 0.24F, -2.18F, 0.24F, a, firing ? pulse : 0.55F);
        });
        if (firing) {
            float length = s.beamLength[i] - (UPPER_ARM + 2.2F);
            float width = 0.9F + 0.1F * Mth.sin(s.ageInTicks * 3.3F + side);
            draw(collector, ps, (vc, p) -> {
                float y0 = -2.2F - length;
                HardLight.box(vc, p, -0.07F * width, y0, -0.07F * width, 0.07F * width, -2.2F, 0.07F * width, HardLight.color(1.0F, 1.0F));
                HardLight.box(vc, p, -0.17F * width, y0, -0.17F * width, 0.17F * width, -2.2F, 0.17F * width, HardLight.color(0.45F, 0.7F));
                HardLight.box(vc, p, -0.32F * width, y0, -0.32F * width, 0.32F * width, -2.2F, 0.32F * width, HardLight.color(0.16F, 0.2F));
            });
        }
        ps.popPose();
    }

    /** Vector from a shoulder joint to the laser target, in the torso's frame. */
    private static Vector3f shoulderToTarget(State s, float side) {
        Vector3f t = new Vector3f(s.laserTarget);
        // Undo the body yaw, then move to the waist and undo the torso twist.
        Axis.YP.rotationDegrees(s.yaw).transform(t);
        t.sub(0.0F, MechaEntity.WAIST_HEIGHT - s.bodyDrop, 0.0F);
        Axis.YP.rotationDegrees(s.twist + s.sway).transform(t);
        t.sub(side * MechaEntity.SHOULDER_X, MechaEntity.SHOULDER_Y, 0.0F);
        return t;
    }

    private static void head(SubmitNodeCollector collector, PoseStack ps, State s, float build) {
        float a = reveal(build, 8.3F);
        if (a <= 0) return;
        float lookPitch = Mth.clamp(s.pitch, -40.0F, 40.0F) * 0.7F - s.lean * 0.5F;
        ps.pushPose();
        ps.translate(0.0F, 2.85F, 0.05F);
        ps.mulPose(Axis.XP.rotationDegrees(lookPitch));
        boolean firing = s.laser;
        draw(collector, ps, (vc, p) -> {
            joint(vc, p, -0.3F, 0.0F, -0.3F, 0.3F, 0.3F, 0.3F, a);
            armor(vc, p, -0.52F, 0.25F, -0.5F, 0.52F, 1.2F, 0.5F, a);
            // Visor, face plate, crest.
            light(vc, p, -0.42F, 0.66F, 0.5F, 0.42F, 0.84F, 0.58F, a, firing ? 1.0F : 0.85F);
            plate(vc, p, -0.3F, 0.28F, 0.45F, 0.3F, 0.56F, 0.62F, a);
            plate(vc, p, -0.56F, 1.1F, -0.55F, 0.56F, 1.22F, 0.55F, a);
            light(vc, p, -0.1F, 0.92F, 0.5F, 0.1F, 1.12F, 0.6F, a, 1.0F);
            // Side "ears" and the rear antenna.
            for (int side = -1; side <= 1; side += 2) {
                float x0 = side > 0 ? 0.52F : -0.68F;
                plate(vc, p, x0, 0.5F, -0.25F, x0 + 0.16F, 0.95F, 0.25F, a);
                light(vc, p, side > 0 ? 0.68F : -0.7F, 0.66F, -0.08F, side > 0 ? 0.7F : -0.68F, 0.8F, 0.08F, a, 0.9F);
            }
            plate(vc, p, -0.06F, 1.2F, -0.45F, 0.06F, 1.75F, -0.35F, a);
        });
        // V-fin.
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.08F, 1.02F, 0.55F);
            ps.mulPose(Axis.ZP.rotationDegrees(-side * 38.0F));
            draw(collector, ps, (vc, p) -> {
                plate(vc, p, -0.07F, 0.0F, -0.05F, 0.07F, 1.3F, 0.06F, a);
                light(vc, p, -0.03F, 0.1F, 0.06F, 0.03F, 1.2F, 0.08F, a, 1.0F);
            });
            ps.popPose();
        }
        ps.popPose();
    }

    public static class State extends EntityRenderState {
        float yaw;
        float twist;
        float pitch;
        float walkPhase;
        float walkAmount;
        float flyPose;
        float thrust;
        float aim;
        float squash;
        float recoil;
        boolean laser;
        boolean firstPerson;
        Vector3f laserTarget = new Vector3f();
        final float[] hip = new float[2];
        final float[] knee = new float[2];
        final float[] ankle = new float[2];
        final float[] armSwing = new float[2];
        final float[] beamLength = new float[2];
        float bodyDrop;
        float lean;
        float roll;
        float sway;
    }
}
