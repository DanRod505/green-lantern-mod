package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders Aquaman's great white shark, pitched along its swimming direction. */
public class SharkRenderer extends EntityRenderer<GreatWhiteSharkEntity, SharkRenderState> {
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/great_white_shark.png");
    private final SharkModel model;

    public SharkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SharkModel(context.bakeLayer(SharkModel.LAYER));
        this.shadowRadius = 1.0F;
    }

    @Override
    public SharkRenderState createRenderState() {
        return new SharkRenderState();
    }

    @Override
    protected boolean affectedByCulling(GreatWhiteSharkEntity entity) {
        return false;
    }

    @Override
    public void extractRenderState(GreatWhiteSharkEntity shark, SharkRenderState state, float partialTick) {
        super.extractRenderState(shark, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, shark.yRotO, shark.getYRot());
        state.pitch = Mth.lerp(partialTick, shark.xRotO, shark.getXRot());
        state.tailPhase = Mth.lerp(partialTick, shark.tailPhaseO, shark.tailPhase);
        state.swimAmount = Mth.lerp(partialTick, shark.swimAmountO, shark.swimAmount);
        state.jaw = shark.jaw(partialTick);
        state.lunge = shark.lungeProgress(partialTick);
        state.outOfWater = !shark.isInWater();
    }

    @Override
    public void submit(SharkRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, GreatWhiteSharkEntity.HEIGHT * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // Vanilla pitch is positive looking down: nose down means rotating the -Z front downwards.
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.setupAnim(state);
        collector.submitModel(model, state, poseStack, model.renderType(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY,
                state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
