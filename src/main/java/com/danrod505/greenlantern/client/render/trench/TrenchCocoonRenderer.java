package com.danrod505.greenlantern.client.render.trench;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.TrenchCocoonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Renders a cocoon of the Trench around its captive (the villager inside is drawn as a passenger). */
public class TrenchCocoonRenderer extends EntityRenderer<TrenchCocoonEntity, TrenchCocoonRenderer.State> {
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/trench/cocoon.png");

    public static class State extends EntityRenderState {
        public float yaw;
        public boolean hurt;
    }

    private final TrenchCocoonModel model;

    public TrenchCocoonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TrenchCocoonModel(context.bakeLayer(TrenchCocoonModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TrenchCocoonEntity cocoon, State state, float partialTick) {
        super.extractRenderState(cocoon, state, partialTick);
        state.yaw = cocoon.getYRot();
        state.hurt = cocoon.hurtTime > 0;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.setupAnim(state);
        collector.submitModel(model, state, poseStack, model.renderType(TEXTURE), state.lightCoords, OverlayTexture.pack(0.0F, state.hurt),
                state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
