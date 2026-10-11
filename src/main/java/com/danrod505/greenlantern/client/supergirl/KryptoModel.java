package com.danrod505.greenlantern.client.supergirl;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.util.Mth;

/**
 * Krypto: the dog's body with a little red cape over his back. Flying, he stretches his front legs
 * forward and his hind legs back like a flying hero, and the cape streams out behind him.
 */
public class KryptoModel extends WolfModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("krypto"), "main");
    private final ModelPart cape;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart tail;

    public KryptoModel(ModelPart root) {
        super(root);
        cape = root.getChild("cape");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        tail = root.getChild("tail");
    }

    /** The dog's mesh (64x32 in the top half of the texture) and the cape (in the bottom half). */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = WolfModel.createMeshDefinition(CubeDeformation.NONE);
        mesh.getRoot().addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 11.0F),
                PartPose.offsetAndRotation(-0.0F, 10.6F, -4.5F, -0.08F, 0.0F, 0.0F));
        // A red collar behind his head with the golden tag of the House of El under his chin.
        mesh.getRoot().getChild("head").addOrReplaceChild("collar", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-2.5F, -3.5F, 1.5F, 7.0F, 7.0F, 1.0F)
                        .texOffs(20, 48).addBox(0.0F, 3.5F, 1.0F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WolfRenderState state) {
        super.setupAnim(state);
        float time = state.ageInTicks;
        boolean flying = state instanceof KryptoRenderState krypto && krypto.flying;
        if (flying) {
            rightFrontLeg.xRot = -1.35F;
            leftFrontLeg.xRot = -1.35F;
            rightHindLeg.xRot = 1.35F;
            leftHindLeg.xRot = 1.35F;
            tail.xRot = 1.35F;
            cape.xRot = -0.02F + Mth.sin(time * 0.9F) * 0.06F;
        } else if (state.isSitting) {
            cape.y += 2.5F;
            cape.xRot = 0.7F;
        } else {
            // The cape sways with his steps.
            cape.xRot = -0.08F + Mth.cos(state.walkAnimationPos * 0.6662F) * 0.12F * state.walkAnimationSpeed + 0.1F * state.walkAnimationSpeed;
        }
    }
}
