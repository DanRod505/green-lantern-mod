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
 * The great white shark: a torso with a big head and opening jaws, a two-part tail that sweeps
 * from side to side with the tall crescent tail fin, the dorsal fin and the pectoral fins.
 * Model space as in vanilla: facing -Z, Y pointing down (the renderer flips it). The texture
 * (256x128) is painted by {@code tools/generate_aquaman_textures.py} using the same box layout.
 */
public class SharkModel extends EntityModel<SharkRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("great_white_shark"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailEnd;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public SharkModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tail = body.getChild("tail");
        this.tailEnd = tail.getChild("tail_end");
        this.leftFin = body.getChild("left_fin");
        this.rightFin = body.getChild("right_fin");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -9.0F, -16.0F, 18, 18, 32),
                PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(100, 0).addBox(-8.0F, -8.0F, -14.0F, 16, 14, 14)
                        .texOffs(160, 0).addBox(-6.0F, -6.0F, -22.0F, 12, 9, 8)
                        .texOffs(160, 17).addBox(-5.5F, 3.0F, -21.5F, 11, 2, 8),
                PartPose.offset(0.0F, 0.0F, -16.0F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(100, 28).addBox(-7.0F, 0.0F, -16.0F, 14, 4, 16)
                        .texOffs(164, 50).addBox(-6.0F, -2.0F, -15.5F, 12, 2, 12),
                PartPose.offset(0.0F, 5.0F, -2.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 50).addBox(-7.0F, -7.0F, 0.0F, 14, 14, 16),
                PartPose.offset(0.0F, -1.0F, 16.0F));
        PartDefinition tailEnd = tail.addOrReplaceChild("tail_end",
                CubeListBuilder.create().texOffs(60, 50).addBox(-4.0F, -4.0F, 0.0F, 8, 8, 14),
                PartPose.offset(0.0F, -1.0F, 16.0F));
        tailEnd.addOrReplaceChild("tail_fin_top",
                CubeListBuilder.create().texOffs(104, 50).addBox(-1.0F, -18.0F, -2.0F, 2, 18, 8),
                PartPose.offsetAndRotation(0.0F, 0.0F, 12.0F, -0.6F, 0.0F, 0.0F));
        tailEnd.addOrReplaceChild("tail_fin_bottom",
                CubeListBuilder.create().texOffs(124, 50).addBox(-1.0F, 0.0F, -2.0F, 2, 10, 6),
                PartPose.offsetAndRotation(0.0F, 0.0F, 12.0F, 0.7F, 0.0F, 0.0F));
        body.addOrReplaceChild("dorsal_fin",
                CubeListBuilder.create().texOffs(140, 50).addBox(-1.0F, -14.0F, 0.0F, 2, 14, 10),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, -0.55F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(0, 80).addBox(0.0F, -1.0F, 0.0F, 16, 2, 9),
                PartPose.offsetAndRotation(8.0F, 6.0F, -8.0F, 0.0F, -0.35F, 0.5F));
        body.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(0, 91).addBox(-16.0F, -1.0F, 0.0F, 16, 2, 9),
                PartPose.offsetAndRotation(-8.0F, 6.0F, -8.0F, 0.0F, 0.35F, -0.5F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(SharkRenderState state) {
        super.setupAnim(state);
        float swim = state.swimAmount;
        float phase = state.tailPhase;
        // The tail drives the swimming: a wave running from the tail root to the fin.
        tail.yRot = Mth.sin(phase) * 0.22F * swim;
        tailEnd.yRot = Mth.sin(phase - 0.9F) * 0.38F * swim;
        body.yRot = -Mth.sin(phase) * 0.05F * swim;
        head.yRot = -Mth.sin(phase + 0.4F) * 0.06F * swim;
        jaw.xRot = state.jaw * 0.75F + 0.05F;
        float flap = Mth.sin(phase * 0.5F) * 0.08F;
        leftFin.zRot = 0.5F + flap;
        rightFin.zRot = -0.5F - flap;
        if (state.outOfWater) {
            // Thrashing about on dry land.
            body.zRot = Mth.sin(state.ageInTicks * 0.9F) * 0.25F;
        }
    }
}
