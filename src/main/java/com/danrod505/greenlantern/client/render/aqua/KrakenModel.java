package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.KrakenEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The Kraken: a big head with glowing eyes and a siphon, a long mantle rising behind it (three
 * bending sections, a pointed tip and two fins), eight walking arms of six segments each and two
 * long hunting tentacles ending in toothed clubs.
 * <p>
 * Model space as in vanilla (facing -Z, Y pointing down, +X is the Kraken's left once rendered),
 * with the origin at the head center. The renderer scales it up four times, so one model unit is a
 * quarter of a block. The texture (256x128) is painted by {@code tools/generate_aquaman_textures.py}
 * using the same box layout.
 * <p>
 * Poses: on land the mantle leans back and the arms are legs, walking in two alternating groups
 * of four; in the water the mantle lies back flat, the arms trail behind in a rippling bundle and
 * the mantle pumps at every stroke. The hunting tentacles curl up in front of the head, rise over
 * it and slam down for the attack.
 */
public class KrakenModel extends EntityModel<KrakenRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("kraken"), "main");
    /** Blocks per model unit (the renderer scales the model by 4). */
    public static final float SCALE = 4.0F;

    private static final int ARMS = 8;
    private static final int[] ARM_WIDTH = {6, 5, 4, 3, 3, 2};
    private static final int[] ARM_LENGTH = {6, 6, 6, 5, 5, 4};
    private static final int[] ARM_U = {0, 24, 44, 60, 72, 84};
    /** Standing on land: angle of each segment relative to the previous one (the first from straight down). */
    private static final float[] ARM_LAND = rad(80, -15, -20, -25, -20, -40);

    private static final int CLUB_SEGMENTS = 8;
    private static final int[] CLUB_WIDTH = {4, 4, 3, 3, 3, 2, 2, 6};
    private static final float CLUB_LENGTH = 5.0F;
    private static final float[] CLUB_IDLE = rad(100, 20, 20, 15, 10, 10, 10, 15);
    private static final float[] CLUB_RAISE = rad(160, 8, 8, 8, 8, 8, 8, 8);
    private static final float[] CLUB_SLAM = rad(52, 2, 2, 2, 2, 2, 2, 8);
    private static final float CLUB_SPREAD = 0.3F;

    private final ModelPart head;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart siphon;
    private final ModelPart mantle;
    private final ModelPart mantleMid;
    private final ModelPart mantleTop;
    private final ModelPart mantleTip;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[][] arms = new ModelPart[ARMS][ARM_WIDTH.length];
    private final ModelPart[][] clubs = new ModelPart[2][CLUB_SEGMENTS];

    public KrakenModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.head = root.getChild("head");
        this.leftEye = head.getChild("left_eye");
        this.rightEye = head.getChild("right_eye");
        this.siphon = head.getChild("siphon");
        this.mantle = head.getChild("mantle");
        this.mantleMid = mantle.getChild("mantle_mid");
        this.mantleTop = mantleMid.getChild("mantle_top");
        this.mantleTip = mantleTop.getChild("mantle_tip");
        this.leftFin = mantleTop.getChild("left_fin");
        this.rightFin = mantleTop.getChild("right_fin");
        for (int i = 0; i < ARMS; i++) {
            ModelPart part = head.getChild("arm" + i);
            for (int j = 0; j < ARM_WIDTH.length; j++) {
                if (j > 0) part = part.getChild("s" + j);
                arms[i][j] = part;
            }
        }
        for (int c = 0; c < 2; c++) {
            ModelPart part = head.getChild(c == 0 ? "club_left" : "club_right");
            for (int j = 0; j < CLUB_SEGMENTS; j++) {
                if (j > 0) part = part.getChild("s" + j);
                clubs[c][j] = part;
            }
        }
    }

    private static float[] rad(float... degrees) {
        float[] out = new float[degrees.length];
        for (int i = 0; i < degrees.length; i++) out[i] = degrees[i] * Mth.DEG_TO_RAD;
        return out;
    }

    /** Heading of arm {@code i} around the body, in (-PI, PI]: 0 points back, PI forward. */
    private static float armHeading(int i) {
        return Mth.wrapDegrees((i + 0.5F) * 45.0F) * Mth.DEG_TO_RAD;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.0F, -7.0F, -8.0F, 16, 14, 16)
                        .texOffs(64, 0).addBox(-7.0F, 5.0F, -7.0F, 14, 3, 14),
                PartPose.ZERO);
        head.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(120, 0).addBox(0.0F, -2.5F, -2.5F, 2, 5, 5),
                PartPose.offset(7.5F, -1.0F, -4.0F));
        head.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(134, 0).addBox(-2.0F, -2.5F, -2.5F, 2, 5, 5),
                PartPose.offset(-7.5F, -1.0F, -4.0F));
        head.addOrReplaceChild("siphon",
                CubeListBuilder.create()
                        .texOffs(148, 0).addBox(-2.5F, -2.5F, -5.0F, 5, 5, 5)
                        .texOffs(168, 0).addBox(-3.5F, -3.5F, -6.0F, 7, 7, 1),
                PartPose.offset(0.0F, 4.0F, -7.5F));

        // The mantle rises from the back of the head: three sections and a pointed tip.
        PartDefinition mantle = head.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 30).addBox(-9.0F, -10.0F, -7.0F, 18, 10, 14),
                PartPose.offset(0.0F, -6.0F, 7.0F));
        PartDefinition mid = mantle.addOrReplaceChild("mantle_mid",
                CubeListBuilder.create().texOffs(0, 56).addBox(-10.0F, -8.0F, -9.0F, 20, 8, 18),
                PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition top = mid.addOrReplaceChild("mantle_top",
                CubeListBuilder.create().texOffs(80, 30).addBox(-8.0F, -6.0F, -7.0F, 16, 6, 14),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        top.addOrReplaceChild("mantle_tip",
                CubeListBuilder.create()
                        .texOffs(80, 56).addBox(-5.0F, -5.0F, -4.5F, 10, 5, 9)
                        .texOffs(120, 56).addBox(-2.5F, -9.0F, -2.5F, 5, 4, 5),
                PartPose.offset(0.0F, -6.0F, 0.0F));
        top.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(140, 30).addBox(0.0F, -0.5F, -6.0F, 9, 1, 12),
                PartPose.offset(7.5F, -3.0F, 0.0F));
        top.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(140, 44).addBox(-9.0F, -0.5F, -6.0F, 9, 1, 12),
                PartPose.offset(-7.5F, -3.0F, 0.0F));

        // Eight arms on a ring under the head; every arm shares the same texture strip.
        for (int i = 0; i < ARMS; i++) {
            float heading = armHeading(i);
            PartDefinition part = head;
            for (int j = 0; j < ARM_WIDTH.length; j++) {
                float w = ARM_WIDTH[j];
                PartPose pose = j == 0
                        ? PartPose.offset(Mth.sin(heading) * 5.5F, 7.0F, Mth.cos(heading) * 5.5F)
                        : PartPose.offset(0.0F, ARM_LENGTH[j - 1] - 0.5F, 0.0F);
                part = part.addOrReplaceChild(j == 0 ? "arm" + i : "s" + j,
                        CubeListBuilder.create().texOffs(ARM_U[j], 84).addBox(-w / 2.0F, 0.0F, -w / 2.0F, w, ARM_LENGTH[j], w),
                        pose);
            }
        }

        // The two hunting tentacles, in front, ending in clubs.
        for (int c = 0; c < 2; c++) {
            float side = c == 0 ? 1.0F : -1.0F;
            PartDefinition part = head;
            for (int j = 0; j < CLUB_SEGMENTS; j++) {
                int w = CLUB_WIDTH[j];
                boolean club = j == CLUB_SEGMENTS - 1;
                int u = club ? 36 : (w == 4 ? 0 : (w == 3 ? 16 : 28));
                PartPose pose = j == 0 ? PartPose.offset(side * 4.0F, 6.5F, -5.5F) : PartPose.offset(0.0F, CLUB_LENGTH - 0.3F, 0.0F);
                CubeListBuilder cubes = CubeListBuilder.create().texOffs(u, 100);
                if (club) {
                    cubes.addBox(-3.0F, 0.0F, -3.0F, 6, 8, 6);
                } else {
                    cubes.addBox(-w / 2.0F, 0.0F, -w / 2.0F, w, CLUB_LENGTH, w);
                }
                part = part.addOrReplaceChild(j == 0 ? (c == 0 ? "club_left" : "club_right") : "s" + j, cubes, pose);
            }
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    private static float ease(float k) {
        k = Mth.clamp(k, 0.0F, 1.0F);
        return k * k * (3.0F - 2.0F * k);
    }

    @Override
    public void setupAnim(KrakenRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float swim = s.swim;
        float land = 1.0F - swim;
        float death = s.death;
        float alive = 1.0F - death;
        float walk = s.walkAmount * land * alive;
        float jet = s.jet * alive;
        float power = s.swimPower;
        // Mantle pump while swimming: contracts on the push, refills in between.
        float pulse = swim * (0.5F + 0.5F * Mth.sin(s.swimWave));
        float breathe = Mth.sin(t * 0.06F);
        // Rising from the deep: a roar with the arms thrown up.
        float roar = s.emerge < 1.0F ? Mth.sin(s.emerge * Mth.PI) : 0.0F;

        // ---- mantle
        float tilt = Mth.lerp(swim, -35.0F, -80.0F) * Mth.DEG_TO_RAD;
        mantle.xRot = tilt + land * 0.03F * breathe + walk * 0.05F * Mth.sin(s.walkPhase * 2.0F) + s.bob * 0.15F
                - jet * 0.08F * land;
        mantle.zRot = walk * 0.07F * Mth.sin(s.walkPhase) + land * 0.03F * Mth.sin(t * 0.03F);
        float puff = 1.0F + 0.035F * breathe * land + 0.08F * roar - 0.12F * pulse * (0.4F + 0.6F * power)
                - 0.1F * jet * (0.5F + 0.5F * Mth.sin(t * 0.9F));
        mantle.xScale = puff;
        mantle.zScale = puff;
        float ripple = swim * (0.3F + 0.7F * power);
        mantleMid.xRot = Mth.lerp(swim, -6.0F, -2.0F) * Mth.DEG_TO_RAD + land * 0.035F * Mth.sin(t * 0.05F + 1.0F)
                + ripple * 0.07F * Mth.sin(s.swimWave - 1.0F);
        mantleTop.xRot = Mth.lerp(swim, -9.0F, -2.0F) * Mth.DEG_TO_RAD + land * 0.04F * Mth.sin(t * 0.05F + 2.0F)
                + ripple * 0.09F * Mth.sin(s.swimWave - 1.7F);
        mantleTip.xRot = Mth.lerp(swim, -12.0F, 0.0F) * Mth.DEG_TO_RAD + land * 0.05F * Mth.sin(t * 0.05F + 3.0F)
                + ripple * 0.12F * Mth.sin(s.swimWave - 2.4F);
        mantleMid.zRot = walk * 0.05F * Mth.sin(s.walkPhase - 0.6F);
        mantleTop.zRot = walk * 0.05F * Mth.sin(s.walkPhase - 1.2F);
        float flap = land * 0.12F * Mth.sin(t * 0.08F) + swim * (0.2F + 0.35F * power) * Mth.sin(s.swimWave * 1.5F);
        leftFin.zRot = 0.15F + flap;
        rightFin.zRot = -0.15F - flap;
        if (death > 0) {
            mantle.xRot = Mth.lerp(ease(death), mantle.xRot, -100.0F * Mth.DEG_TO_RAD);
            mantleMid.xRot = Mth.lerp(ease(death), mantleMid.xRot, -20.0F * Mth.DEG_TO_RAD);
        }

        // ---- eyes and siphon
        boolean blink = (t % 97.0F) < 3.0F || death > 0.7F;
        leftEye.yScale = blink ? 0.15F : 1.0F;
        rightEye.yScale = leftEye.yScale;
        float siphonScale = 1.0F + 0.35F * jet + 0.25F * pulse * power;
        siphon.xScale = siphonScale;
        siphon.yScale = siphonScale;
        siphon.xRot = -0.25F * jet;

        // ---- the eight arms
        for (int i = 0; i < ARMS; i++) {
            float heading = armHeading(i);
            float sinH = Mth.sin(heading);
            float cosH = Mth.cos(heading);
            float phase = s.walkPhase + (i % 2) * Mth.PI;
            float lift = Math.max(0.0F, Mth.sin(phase)) * walk;
            float stride = -Mth.cos(phase) * walk;
            ModelPart[] seg = arms[i];

            float landYaw = heading + sinH * stride * 0.28F;
            float landRoot = ARM_LAND[0] + lift * 0.32F - cosH * stride * 0.25F + jet * 0.14F + roar * 0.45F;
            float swimYaw = heading * (0.16F + 0.12F * pulse);
            float swimRoot = 82.0F * Mth.DEG_TO_RAD + (pulse - 0.5F) * 0.25F - jet * 0.2F;
            seg[0].yRot = Mth.lerp(swim, landYaw, swimYaw);
            seg[0].xRot = Mth.lerp(swim, landRoot, swimRoot);
            seg[0].zRot = 0.0F;
            for (int j = 1; j < seg.length; j++) {
                float landJ = ARM_LAND[j];
                if (j == 1) landJ -= lift * 0.22F;
                if (j >= 4) landJ += lift * 0.26F;
                if (j == seg.length - 1) landJ -= roar * 0.5F;
                float idle = 0.07F * Mth.sin(t * 0.06F + i * 1.1F + j * 0.7F) * (1.0F - walk * 0.5F);
                float wave = (0.12F + 0.14F * power) * Mth.sin(s.swimWave - j * 0.9F + i * 0.8F);
                float swimJ = (j == 1 ? -0.07F : -0.035F) + wave;
                seg[j].xRot = Mth.lerp(swim, landJ, swimJ) + idle * land;
                seg[j].zRot = land * 0.05F * Mth.sin(t * 0.05F + i * 0.9F + j * 0.6F)
                        + swim * (0.06F + 0.06F * power) * Mth.sin(s.swimWave * 0.8F - j * 0.8F + i * 2.0F);
            }
            if (death > 0) {
                float d = ease(death);
                seg[0].xRot = Mth.lerp(d, seg[0].xRot, 95.0F * Mth.DEG_TO_RAD);
                for (int j = 1; j < seg.length; j++) seg[j].xRot = Mth.lerp(d, seg[j].xRot, (14.0F + 4.0F * j) * Mth.DEG_TO_RAD);
            }
        }

        // ---- the hunting tentacles
        for (int c = 0; c < 2; c++) {
            float side = c == 0 ? 1.0F : -1.0F;
            float front = Mth.PI - side * CLUB_SPREAD;
            float trail = side * 0.12F;
            float idleHeading = Mth.lerp(swim, front, trail);
            float[] pose = new float[CLUB_SEGMENTS];
            for (int j = 0; j < CLUB_SEGMENTS; j++) {
                float landIdle = CLUB_IDLE[j] + 0.08F * Mth.sin(t * 0.05F + j * 0.6F + c * 2.0F) * (j == 0 ? 0.5F : 1.0F);
                float swimIdle = j == 0 ? 85.0F * Mth.DEG_TO_RAD : -0.03F + (0.1F + 0.12F * power) * Mth.sin(s.swimWave - j * 0.8F + c * 1.5F);
                pose[j] = Mth.lerp(swim, landIdle, swimIdle);
                if (roar > 0) pose[j] = Mth.lerp(roar, pose[j], CLUB_RAISE[j]);
            }
            float heading = idleHeading;
            boolean striking = s.slamTime < KrakenEntity.SLAM_TICKS && (c == 0) == s.slamLeft;
            if (striking) {
                float st = s.slamTime;
                for (int j = 0; j < CLUB_SEGMENTS; j++) {
                    float a;
                    if (st < 6.0F) {
                        a = Mth.lerp(ease(st / 6.0F), pose[j], CLUB_RAISE[j]);
                    } else if (st < KrakenEntity.SLAM_HIT) {
                        float k = (st - 6.0F) / (KrakenEntity.SLAM_HIT - 6.0F);
                        a = Mth.lerp(k * k, CLUB_RAISE[j], CLUB_SLAM[j]);
                    } else if (st < 13.0F) {
                        a = CLUB_SLAM[j] + (j == 0 ? 0.06F : 0.03F) * Mth.sin((st - KrakenEntity.SLAM_HIT) * Mth.PI / 2.0F);
                    } else {
                        a = Mth.lerp(ease((st - 13.0F) / (KrakenEntity.SLAM_TICKS - 13.0F)), CLUB_SLAM[j], pose[j]);
                    }
                    pose[j] = a;
                }
                float w = st < 6.0F ? ease(st / 6.0F) : (st < 13.0F ? 1.0F : 1.0F - ease((st - 13.0F) / (KrakenEntity.SLAM_TICKS - 13.0F)));
                heading = Mth.lerp(w, idleHeading, front);
            } else if (s.slamTime < KrakenEntity.SLAM_TICKS) {
                // The other tentacle pulls back out of the way.
                pose[0] += 0.25F * Mth.sin(Mth.clamp(s.slamTime / KrakenEntity.SLAM_TICKS, 0.0F, 1.0F) * Mth.PI);
            }
            if (death > 0) {
                float d = ease(death);
                pose[0] = Mth.lerp(d, pose[0], 70.0F * Mth.DEG_TO_RAD);
                for (int j = 1; j < CLUB_SEGMENTS; j++) pose[j] = Mth.lerp(d, pose[j], 18.0F * Mth.DEG_TO_RAD);
            }
            ModelPart[] seg = clubs[c];
            seg[0].yRot = heading;
            for (int j = 0; j < CLUB_SEGMENTS; j++) {
                seg[j].xRot = pose[j];
                if (j > 0) seg[j].zRot = land * alive * 0.04F * Mth.sin(t * 0.07F + j * 0.9F + c);
            }
        }

        head.zRot = land * alive * 0.02F * Mth.sin(t * 0.04F);
    }
}
