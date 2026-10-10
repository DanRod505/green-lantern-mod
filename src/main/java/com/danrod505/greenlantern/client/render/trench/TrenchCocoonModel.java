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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The cocoon of the Trench: a swollen pod of translucent membrane (the captive shows through)
 * hanging from a sinewy stalk, two tendrils clinging to the rock above. It breathes slowly.
 * Origin at the bottom, Y pointing down; texture (128x64) by {@code tools/generate_trench_textures.py}.
 */
public class TrenchCocoonModel extends EntityModel<EntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("trench_cocoon"), "main");

    private final ModelPart pod;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;

    public TrenchCocoonModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.pod = root.getChild("pod");
        this.leftTendril = pod.getChild("left_tendril");
        this.rightTendril = pod.getChild("right_tendril");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition pod = root.addOrReplaceChild("pod",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -6.0F, -4.0F, 8, 4, 8)
                        .texOffs(0, 12).addBox(-6.0F, -12.0F, -6.0F, 12, 6, 12)
                        .texOffs(0, 30).addBox(-7.0F, -24.0F, -7.0F, 14, 12, 14)
                        .texOffs(64, 0).addBox(-6.0F, -32.0F, -6.0F, 12, 8, 12)
                        .texOffs(64, 20).addBox(-4.0F, -37.0F, -4.0F, 8, 5, 8)
                        .texOffs(112, 0).addBox(-1.5F, -61.0F, -1.5F, 3, 24, 3),
                PartPose.ZERO);
        pod.addOrReplaceChild("left_tendril",
                CubeListBuilder.create().texOffs(96, 20).addBox(-1.0F, -16.0F, -1.0F, 2, 16, 2),
                PartPose.offsetAndRotation(3.0F, -34.0F, 0.0F, 0.0F, 0.0F, 0.45F));
        pod.addOrReplaceChild("right_tendril",
                CubeListBuilder.create().texOffs(96, 20).addBox(-1.0F, -16.0F, -1.0F, 2, 16, 2),
                PartPose.offsetAndRotation(-3.0F, -34.0F, 0.0F, 0.0F, 0.0F, -0.45F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float breath = 1.0F + Mth.sin(t * 0.11F) * 0.035F;
        pod.xScale = breath;
        pod.zScale = breath;
        pod.zRot = Mth.sin(t * 0.04F) * 0.03F;
        pod.xRot = Mth.sin(t * 0.033F + 1.0F) * 0.025F;
        leftTendril.zRot = 0.45F + Mth.sin(t * 0.07F) * 0.05F;
        rightTendril.zRot = -0.45F - Mth.sin(t * 0.07F + 2.0F) * 0.05F;
    }
}
