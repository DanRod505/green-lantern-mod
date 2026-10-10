package com.danrod505.greenlantern.client.render.oa;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Humanoid model of the people of Oa: Lanterns get a superhero flying pose, Guardians a large head
 * and hands clasped in front of their robes. Uses the classic 64x32 skin layout.
 */
public class OaNpcModel extends HumanoidModel<OaNpcRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("oa_npc"), "main");

    public OaNpcModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32);
    }

    @Override
    public void setupAnim(OaNpcRenderState state) {
        super.setupAnim(state);
        head.xScale = state.headScale;
        head.yScale = state.headScale;
        head.zScale = state.headScale;
        if (state.flying) {
            // The body is tilted forward by the renderer: one fist ahead, the other arm along the side.
            float sway = Mth.sin(state.ageInTicks * 0.15F) * 0.05F;
            rightArm.xRot = -Mth.PI * 0.95F;
            rightArm.yRot = 0.0F;
            rightArm.zRot = 0.05F;
            leftArm.xRot = 0.2F + sway;
            leftArm.yRot = 0.0F;
            leftArm.zRot = -0.15F;
            rightLeg.xRot = 0.05F + sway;
            leftLeg.xRot = -0.08F - sway;
            rightLeg.zRot = 0.05F;
            leftLeg.zRot = -0.05F;
            head.xRot = -1.0F;
        } else if (state.guardian) {
            rightArm.xRot = -0.75F;
            rightArm.yRot = -0.45F;
            rightArm.zRot = 0.0F;
            leftArm.xRot = -0.75F;
            leftArm.yRot = 0.45F;
            leftArm.zRot = 0.0F;
            rightLeg.xRot = 0.0F;
            leftLeg.xRot = 0.0F;
        }
    }
}
