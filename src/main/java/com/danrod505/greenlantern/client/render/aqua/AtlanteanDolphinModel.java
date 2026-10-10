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
 * The Atlantean dolphin: a long body with the head and beak, a swept-back dorsal fin, the flippers, a
 * small saddle and a two-part tail ending in the flukes, which beats up and down. About three blocks
 * long. Model space as in vanilla: facing -Z, Y pointing down. Textures (128x64, plus a glow layer)
 * painted by {@code tools/generate_atlantis_mounts.py} with the same box layout.
 */
public class AtlanteanDolphinModel extends EntityModel<MountRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("atlantean_dolphin"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart tailEnd;
    private final ModelPart fluke;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;

    public AtlanteanDolphinModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.tail = body.getChild("tail");
        this.tailEnd = tail.getChild("tail_end");
        this.fluke = tailEnd.getChild("fluke");
        this.leftFlipper = body.getChild("left_flipper");
        this.rightFlipper = body.getChild("right_flipper");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -5.0F, -10.0F, 10, 10, 20)
                        .texOffs(96, 32).addBox(-4.0F, -6.5F, -4.0F, 8, 2, 7),
                PartPose.ZERO);
        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(62, 0).addBox(-4.0F, -4.0F, -7.0F, 8, 7, 7)
                        .texOffs(94, 0).addBox(-1.5F, 0.5F, -11.0F, 3, 2, 4),
                PartPose.offset(0.0F, 0.5F, -10.0F));
        body.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(62, 16).addBox(-0.5F, -5.0F, -1.0F, 1, 5, 5),
                PartPose.offsetAndRotation(0.0F, -4.5F, 1.0F, -0.6F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_flipper",
                CubeListBuilder.create().texOffs(76, 16).addBox(0.0F, -0.5F, 0.0F, 6, 1, 3),
                PartPose.offsetAndRotation(4.5F, 3.0F, -5.0F, 0.0F, -0.6F, 0.6F));
        body.addOrReplaceChild("right_flipper",
                CubeListBuilder.create().texOffs(96, 16).addBox(-6.0F, -0.5F, 0.0F, 6, 1, 3),
                PartPose.offsetAndRotation(-4.5F, 3.0F, -5.0F, 0.0F, 0.6F, -0.6F));
        PartDefinition tail = body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -3.0F, 0.0F, 6, 6, 10),
                PartPose.offset(0.0F, -0.5F, 10.0F));
        PartDefinition tailEnd = tail.addOrReplaceChild("tail_end",
                CubeListBuilder.create().texOffs(34, 32).addBox(-2.0F, -2.0F, 0.0F, 4, 4, 6),
                PartPose.offset(0.0F, 0.0F, 10.0F));
        tailEnd.addOrReplaceChild("fluke",
                CubeListBuilder.create().texOffs(56, 32).addBox(-7.0F, -0.5F, 0.0F, 14, 1, 5),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(MountRenderState state) {
        super.setupAnim(state);
        float phase = state.swimPhase * 0.9F;
        float swim = state.swimAmount;
        if (state.outOfWater && !state.ridden) {
            // Stranded: flapping the tail.
            phase = state.ageInTicks * 0.5F;
            swim = 1.0F;
        }
        // Dolphins swim with an up-and-down beat of the tail running back to the flukes.
        tail.xRot = Mth.sin(phase) * 0.22F * swim;
        tailEnd.xRot = Mth.sin(phase - 0.8F) * 0.32F * swim;
        fluke.xRot = Mth.sin(phase - 1.5F) * 0.35F * swim;
        body.xRot = -Mth.sin(phase) * 0.05F * swim;
        head.xRot = Mth.sin(phase + 0.5F) * 0.05F * swim;
        leftFlipper.zRot = 0.6F + Mth.sin(phase * 0.5F) * 0.15F;
        rightFlipper.zRot = -0.6F - Mth.sin(phase * 0.5F) * 0.15F;
    }
}
