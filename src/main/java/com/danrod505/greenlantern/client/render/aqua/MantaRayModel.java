package com.danrod505.greenlantern.client.render.aqua;

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
 * The giant manta ray: a flat body with the two curled cephalic fins in front, the long whip of a
 * tail, an Atlantean saddle on its back and the great wings, each in three segments (inner, middle
 * and tip) so the beat of the wing travels outwards like a wave. About eight blocks from tip to tip.
 * Model space as in vanilla: facing -Z, Y pointing down. Texture (256x128) painted by
 * {@code tools/generate_atlantis_mounts.py} with the same box layout.
 */
public class MantaRayModel extends EntityModel<MountRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("manta_ray"), "main");

    private final ModelPart body;
    private final ModelPart leftInner;
    private final ModelPart leftMid;
    private final ModelPart leftTip;
    private final ModelPart rightInner;
    private final ModelPart rightMid;
    private final ModelPart rightTip;
    private final ModelPart tail;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;

    public MantaRayModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.body = root.getChild("body");
        this.leftInner = body.getChild("left_inner");
        this.leftMid = leftInner.getChild("left_mid");
        this.leftTip = leftMid.getChild("left_tip");
        this.rightInner = body.getChild("right_inner");
        this.rightMid = rightInner.getChild("right_mid");
        this.rightTip = rightMid.getChild("right_tip");
        this.tail = body.getChild("tail");
        this.leftHorn = body.getChild("left_horn");
        this.rightHorn = body.getChild("right_horn");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-12.0F, -3.0F, -16.0F, 24, 6, 34)
                        .texOffs(120, 0).addBox(-10.0F, -2.0F, -22.0F, 20, 4, 6)
                        .texOffs(176, 0).addBox(-6.0F, -4.5F, -6.0F, 12, 2, 12),
                PartPose.ZERO);
        body.addOrReplaceChild("left_horn",
                CubeListBuilder.create().texOffs(120, 12).addBox(-1.5F, -1.0F, -8.0F, 3, 3, 8),
                PartPose.offsetAndRotation(8.0F, 0.0F, -21.0F, 0.35F, -0.25F, 0.0F));
        body.addOrReplaceChild("right_horn",
                CubeListBuilder.create().texOffs(144, 12).addBox(-1.5F, -1.0F, -8.0F, 3, 3, 8),
                PartPose.offsetAndRotation(-8.0F, 0.0F, -21.0F, 0.35F, 0.25F, 0.0F));
        PartDefinition leftInner = body.addOrReplaceChild("left_inner",
                CubeListBuilder.create().texOffs(0, 42).addBox(0.0F, -1.5F, -12.0F, 20, 3, 28),
                PartPose.offset(12.0F, 0.0F, -2.0F));
        PartDefinition leftMid = leftInner.addOrReplaceChild("left_mid",
                CubeListBuilder.create().texOffs(0, 76).addBox(0.0F, -1.0F, -6.0F, 16, 2, 18),
                PartPose.offset(20.0F, 0.0F, 0.0F));
        leftMid.addOrReplaceChild("left_tip",
                CubeListBuilder.create().texOffs(136, 76).addBox(0.0F, -0.5F, -2.0F, 12, 1, 8),
                PartPose.offset(16.0F, 0.0F, 4.0F));
        PartDefinition rightInner = body.addOrReplaceChild("right_inner",
                CubeListBuilder.create().texOffs(96, 42).addBox(-20.0F, -1.5F, -12.0F, 20, 3, 28),
                PartPose.offset(-12.0F, 0.0F, -2.0F));
        PartDefinition rightMid = rightInner.addOrReplaceChild("right_mid",
                CubeListBuilder.create().texOffs(68, 76).addBox(-16.0F, -1.0F, -6.0F, 16, 2, 18),
                PartPose.offset(-20.0F, 0.0F, 0.0F));
        rightMid.addOrReplaceChild("right_tip",
                CubeListBuilder.create().texOffs(176, 76).addBox(-12.0F, -0.5F, -2.0F, 12, 1, 8),
                PartPose.offset(-16.0F, 0.0F, 4.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 98).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 28),
                PartPose.offset(0.0F, 0.0F, 18.0F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(MountRenderState state) {
        super.setupAnim(state);
        float phase = state.swimPhase * 0.6F;
        float amount = state.swimAmount;
        float a;
        if (state.outOfWater && !state.ridden) {
            // Stranded: wings lying limp, a slow heave now and then.
            a = 0.08F;
            phase = state.ageInTicks * 0.15F;
        } else if (state.outOfWater) {
            // Gliding: wings spread, curling up a little at the tips, barely moving.
            a = 0.05F;
            leftInner.zRot = -0.06F;
            rightInner.zRot = 0.06F;
            leftTip.zRot = -0.18F;
            rightTip.zRot = 0.18F;
        } else {
            a = 0.25F + 0.2F * amount;
        }
        // The beat runs from the body to the tips (each segment a little later and wider).
        leftInner.zRot += Mth.sin(phase) * a;
        leftMid.zRot += Mth.sin(phase - 0.7F) * a * 1.3F;
        leftTip.zRot += Mth.sin(phase - 1.4F) * a * 1.6F;
        rightInner.zRot -= Mth.sin(phase) * a;
        rightMid.zRot -= Mth.sin(phase - 0.7F) * a * 1.3F;
        rightTip.zRot -= Mth.sin(phase - 1.4F) * a * 1.6F;
        // The body rises and falls with the wings; the tail trails behind, swaying.
        body.y = -Mth.cos(phase) * 1.2F * a * 3.0F;
        body.xRot = Mth.sin(phase - 0.4F) * 0.03F;
        tail.yRot = Mth.sin(phase * 0.5F) * 0.18F;
        tail.xRot = -0.05F + Mth.sin(phase - 1.0F) * 0.08F;
        leftHorn.xRot = 0.35F + Mth.sin(phase * 0.8F) * 0.12F;
        rightHorn.xRot = 0.35F + Mth.sin(phase * 0.8F + 0.5F) * 0.12F;
    }
}
