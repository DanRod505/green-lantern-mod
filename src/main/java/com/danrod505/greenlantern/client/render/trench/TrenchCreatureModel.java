package com.danrod505.greenlantern.client.render.trench;

import com.danrod505.greenlantern.GreenLantern;
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
 * A creature of the Trench: a hunched, gaunt body over long legs ending in webbed flippers, long
 * sinewy arms with hooked talons, a torn dorsal fin, and a big head pushed forward, crowned by a
 * ragged crest, with a lower jaw that drops wide open over rows of needle teeth. Model space as in
 * vanilla (facing -Z, Y pointing down), the origin at the hips. Texture (64x64) painted by
 * {@code tools/generate_trench_textures.py} with the same box layout.
 */
public class TrenchCreatureModel extends EntityModel<TrenchRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("trench_creature"), "main");
    /** Leg length (model pixels): how far below the origin the feet are. */
    public static final float LEGS = 14.0F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart dorsal;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftShin;
    private final ModelPart rightShin;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;

    public TrenchCreatureModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        this.dorsal = body.getChild("dorsal");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftForearm = leftArm.getChild("forearm");
        this.rightForearm = rightArm.getChild("forearm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftShin = leftLeg.getChild("shin");
        this.rightShin = rightLeg.getChild("shin");
        this.leftFlipper = leftShin.getChild("flipper");
        this.rightFlipper = rightShin.getChild("flipper");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -11.0F, -2.5F, 8, 11, 5),
                PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-3.5F, -6.0F, -6.0F, 7, 6, 8)
                        .texOffs(30, 16).addBox(-3.0F, 0.0F, -6.0F, 6, 2, 6)
                        .texOffs(0, 30).addBox(-0.5F, -10.0F, -3.0F, 1, 4, 9),
                PartPose.offset(0.0F, -11.0F, -0.5F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-3.0F, 0.0F, -7.0F, 6, 2, 7)
                        .texOffs(30, 24).addBox(-2.5F, -2.0F, -6.5F, 5, 2, 5),
                PartPose.offset(0.0F, 1.5F, 1.0F));
        body.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -10.0F, 0.0F, 1, 10, 3),
                PartPose.offset(0.0F, 0.0F, 2.5F));
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 1.0F : -1.0F;
            PartDefinition arm = body.addOrReplaceChild(side == 0 ? "left_arm" : "right_arm",
                    CubeListBuilder.create().texOffs(20, 30).addBox(-1.0F, -1.0F, -1.0F, 2, 9, 2),
                    PartPose.offset(4.8F * sx, -10.0F, 0.0F));
            PartDefinition forearm = arm.addOrReplaceChild("forearm",
                    CubeListBuilder.create().texOffs(28, 31).addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2),
                    PartPose.offset(0.0F, 8.0F, 0.0F));
            forearm.addOrReplaceChild("hand",
                    CubeListBuilder.create()
                            .texOffs(36, 31).addBox(-1.5F, 0.0F, -1.0F, 3, 2, 2)
                            .texOffs(46, 31).addBox(-1.5F, 2.0F, 0.0F, 3, 4, 0),
                    PartPose.offset(0.0F, 8.0F, 0.0F));
            PartDefinition leg = root.addOrReplaceChild(side == 0 ? "left_leg" : "right_leg",
                    CubeListBuilder.create().texOffs(52, 31).addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3),
                    PartPose.offset(2.0F * sx, 0.0F, 0.0F));
            PartDefinition shin = leg.addOrReplaceChild("shin",
                    CubeListBuilder.create().texOffs(36, 36).addBox(-1.0F, 0.0F, -1.0F, 2, 7, 2),
                    PartPose.offset(0.0F, 7.0F, 0.0F));
            shin.addOrReplaceChild("flipper",
                    CubeListBuilder.create().texOffs(44, 36).addBox(-2.0F, 0.0F, 0.0F, 4, 5, 0),
                    PartPose.offsetAndRotation(0.0F, 6.5F, 0.0F, 1.3F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TrenchRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float swim = state.swimAmount;
        float phase = state.swimPhase;
        float a = state.attack;
        float strike = Mth.sin(a * Mth.PI);
        // Hunched and twitching when it hovers; the body straightens out when it swims fast.
        body.xRot = 0.4F * (1.0F - swim) + Mth.sin(t * 0.09F) * 0.04F - strike * 0.15F;
        head.xRot = -0.4F * (1.0F - swim) - 0.85F * swim + Mth.sin(t * 0.13F) * 0.05F - strike * 0.2F;
        head.yRot = Mth.sin(t * 0.05F) * 0.15F * (1.0F - swim);
        // The jaw chatters, and drops wide open to bite.
        jaw.xRot = 0.12F + Mth.sin(t * 0.35F) * 0.06F + strike * 0.95F + (state.carrying ? 0.3F : 0.0F);
        dorsal.zRot = Mth.sin(t * 0.2F + phase * 0.3F) * 0.12F;

        float stroke = Mth.sin(phase * 0.6F);
        // Arms: hanging forward, claws ready; streamlined and stroking in a fast swim; a slash to strike.
        float armX = Mth.lerp(swim, -0.45F + Mth.sin(t * 0.08F) * 0.08F, 0.6F + stroke * 0.5F);
        leftArm.xRot = armX - strike * 2.0F;
        rightArm.xRot = Mth.lerp(swim, -0.45F + Mth.sin(t * 0.08F + 1.0F) * 0.08F, 0.6F - stroke * 0.5F) - strike * 1.7F;
        leftArm.zRot = -0.18F - swim * 0.1F + strike * 0.3F;
        rightArm.zRot = 0.18F + swim * 0.1F - strike * 0.3F;
        if (state.carrying) {
            // Holding the captive against its chest.
            leftArm.xRot = -1.1F;
            rightArm.xRot = -1.1F;
            leftArm.zRot = 0.35F;
            rightArm.zRot = -0.35F;
        }
        leftForearm.xRot = -0.6F * (1.0F - swim) - strike * 0.4F - (state.carrying ? 0.6F : 0.0F);
        rightForearm.xRot = -0.6F * (1.0F - swim) - strike * 0.4F - (state.carrying ? 0.6F : 0.0F);

        // Legs: bent and crouched when it hovers, kicking (alternately) when it swims.
        float kick = Mth.sin(phase);
        leftLeg.xRot = Mth.lerp(swim, -0.35F, kick * 0.55F);
        rightLeg.xRot = Mth.lerp(swim, -0.15F, -kick * 0.55F);
        leftShin.xRot = Mth.lerp(swim, 0.6F, 0.25F + Math.max(0.0F, Mth.sin(phase + 1.0F)) * 0.6F);
        rightShin.xRot = Mth.lerp(swim, 0.4F, 0.25F + Math.max(0.0F, Mth.sin(phase + 1.0F + Mth.PI)) * 0.6F);
        leftFlipper.xRot = 1.1F + kick * 0.25F * swim;
        rightFlipper.xRot = 1.1F - kick * 0.25F * swim;
    }
}
