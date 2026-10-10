package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.render.oa.OaNpcModel;
import com.danrod505.greenlantern.client.render.oa.OaNpcRenderState;
import com.danrod505.greenlantern.entity.AtlanteanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The people of Atlantis: citizens in sea-green and gold robes, and the royal guard in scale armor
 * with a finned helmet and a trident. Fast swimmers glide with the body level; the others float
 * upright, swaying with the current.
 */
public class AtlanteanRenderer extends HumanoidMobRenderer<AtlanteanEntity, OaNpcRenderState, OaNpcModel> {
    public static final int GUARD_SKINS = 3;
    private static final Identifier[] CITIZENS = new Identifier[AtlanteanEntity.VARIANTS];
    private static final Identifier[] GUARDS = new Identifier[GUARD_SKINS];

    static {
        for (int i = 0; i < CITIZENS.length; i++) CITIZENS[i] = GreenLantern.id("textures/entity/atlantis/citizen_" + i + ".png");
        for (int i = 0; i < GUARDS.length; i++) GUARDS[i] = GreenLantern.id("textures/entity/atlantis/guard_" + i + ".png");
    }

    public AtlanteanRenderer(EntityRendererProvider.Context context) {
        super(context, new OaNpcModel(context.bakeLayer(OaNpcModel.LAYER)), 0.0F);
    }

    @Override
    public OaNpcRenderState createRenderState() {
        return new OaNpcRenderState();
    }

    @Override
    public void extractRenderState(AtlanteanEntity atlantean, OaNpcRenderState state, float partialTick) {
        super.extractRenderState(atlantean, state, partialTick);
        state.variant = atlantean.isGuard() ? 100 + atlantean.variant() % GUARD_SKINS : atlantean.variant();
        state.flying = atlantean.isSwimmingFast();
        state.sizeScale = atlantean.isGuard() ? 1.08F : 0.96F + (atlantean.variant() % 3) * 0.03F;
    }

    @Override
    public Identifier getTextureLocation(OaNpcRenderState state) {
        return state.variant >= 100 ? GUARDS[state.variant - 100] : CITIZENS[Mth.clamp(state.variant, 0, CITIZENS.length - 1)];
    }

    @Override
    protected void scale(OaNpcRenderState state, PoseStack poseStack) {
        poseStack.scale(state.sizeScale, state.sizeScale, state.sizeScale);
    }

    @Override
    protected void setupRotations(OaNpcRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        poseStack.translate(0.0F, 0.9F, 0.0F);
        if (state.flying) {
            // Swimming like a torpedo: body level, gently undulating.
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F + Mth.sin(state.ageInTicks * 0.3F) * 4.0F));
        } else {
            // Floating upright, swaying with the current.
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(state.ageInTicks * 0.05F + state.variant) * 3.0F));
        }
        poseStack.translate(0.0F, -0.9F, 0.0F);
    }
}
