package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.entity.BatDefenderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ambient.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Batman's bats: the vanilla bat, flapping all the time, darker, a little bigger. */
public class BatDefenderRenderer extends EntityRenderer<BatDefenderEntity, BatRenderState> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/bat.png");
    private static final int TINT = 0xFF6A6470;

    private final BatModel model;

    public BatDefenderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BatModel(context.bakeLayer(ModelLayers.BAT));
    }

    @Override
    public BatRenderState createRenderState() {
        return new BatRenderState();
    }

    @Override
    public void extractRenderState(BatDefenderEntity bat, BatRenderState state, float partialTick) {
        super.extractRenderState(bat, state, partialTick);
        state.bodyRot = bat.getYRot(partialTick);
        state.yRot = 0.0F;
        state.xRot = 0.0F;
        state.isResting = false;
        state.flyAnimationState.startIfStopped(0);
    }

    @Override
    public void submit(BatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        poseStack.scale(-1.1F, -1.1F, 1.1F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(state);
        collector.submitModel(model, state, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY,
                TINT, null, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
