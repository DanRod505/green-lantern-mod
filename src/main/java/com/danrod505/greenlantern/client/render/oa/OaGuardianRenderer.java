package com.danrod505.greenlantern.client.render.oa;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.OaGuardianEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;

/** Small, big-headed, blue-skinned Guardians in red robes, gently hovering. */
public class OaGuardianRenderer extends HumanoidMobRenderer<OaGuardianEntity, OaNpcRenderState, OaNpcModel> {
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/oa/guardian.png");

    public OaGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new OaNpcModel(context.bakeLayer(OaNpcModel.LAYER)), 0.35F);
    }

    @Override
    public OaNpcRenderState createRenderState() {
        return new OaNpcRenderState();
    }

    @Override
    public void extractRenderState(OaGuardianEntity guardian, OaNpcRenderState state, float partialTick) {
        super.extractRenderState(guardian, state, partialTick);
        state.guardian = true;
        state.headScale = 1.35F;
        state.sizeScale = 0.72F;
    }

    @Override
    public Identifier getTextureLocation(OaNpcRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(OaNpcRenderState state, PoseStack poseStack) {
        poseStack.scale(state.sizeScale, state.sizeScale, state.sizeScale);
    }

    @Override
    protected void setupRotations(OaNpcRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        poseStack.translate(0.0F, 0.12F + Mth.sin(state.ageInTicks * 0.06F) * 0.08F, 0.0F);
    }
}
