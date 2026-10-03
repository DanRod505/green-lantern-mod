package com.danrod505.greenlantern.client.render.oa;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.LanternCorpsmanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Lanterns of many species, in uniform, with a glowing emblem, ring and eyes. */
public class LanternCorpsmanRenderer extends HumanoidMobRenderer<LanternCorpsmanEntity, OaNpcRenderState, OaNpcModel> {
    private static final Identifier[] TEXTURES = new Identifier[LanternCorpsmanEntity.VARIANTS];
    private static final Identifier GLOW = GreenLantern.id("textures/entity/oa/lantern_glow.png");
    /** Species with a bigger build (index = variant). */
    private static final float[] SIZE = {1.0F, 1.0F, 0.95F, 1.0F, 1.3F, 0.9F, 1.05F, 1.15F};

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = GreenLantern.id("textures/entity/oa/lantern_" + i + ".png");
        }
    }

    public LanternCorpsmanRenderer(EntityRendererProvider.Context context) {
        super(context, new OaNpcModel(context.bakeLayer(OaNpcModel.LAYER)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RenderTypes.eyes(GLOW);
            }
        });
    }

    @Override
    public OaNpcRenderState createRenderState() {
        return new OaNpcRenderState();
    }

    @Override
    public void extractRenderState(LanternCorpsmanEntity lantern, OaNpcRenderState state, float partialTick) {
        super.extractRenderState(lantern, state, partialTick);
        state.variant = lantern.variant();
        state.flying = lantern.isFlyingLantern();
        state.sizeScale = SIZE[state.variant];
    }

    @Override
    public Identifier getTextureLocation(OaNpcRenderState state) {
        return TEXTURES[state.variant];
    }

    @Override
    protected void scale(OaNpcRenderState state, PoseStack poseStack) {
        poseStack.scale(state.sizeScale, state.sizeScale, state.sizeScale);
    }

    @Override
    protected void setupRotations(OaNpcRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (state.flying) {
            // Superhero flight: body level with the ground, around the middle of the hitbox.
            poseStack.translate(0.0F, 0.9F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.translate(0.0F, -0.9F, 0.0F);
        }
    }
}
