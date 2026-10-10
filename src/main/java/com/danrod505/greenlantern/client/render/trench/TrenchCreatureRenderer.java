package com.danrod505.greenlantern.client.render.trench;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.TrenchCreatureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renders a creature of the Trench (or a brute: bigger, darker, red-eyed). Hovering, it stands
 * hunched; swimming, it leans forward until it lies flat along its course, pitched with its swim.
 * The eyes and the lateral lines glow in the dark.
 */
public class TrenchCreatureRenderer extends EntityRenderer<TrenchCreatureEntity, TrenchRenderState> {
    private static final Identifier CREATURE = GreenLantern.id("textures/entity/trench/creature.png");
    private static final Identifier CREATURE_GLOW = GreenLantern.id("textures/entity/trench/creature_glow.png");
    private static final Identifier BRUTE = GreenLantern.id("textures/entity/trench/brute.png");
    private static final Identifier BRUTE_GLOW = GreenLantern.id("textures/entity/trench/brute_glow.png");
    /** The model is a little smaller than its boxes (1.8 blocks standing). */
    private static final float MODEL_SCALE = 0.92F;

    private final TrenchCreatureModel model;

    public TrenchCreatureRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TrenchCreatureModel(context.bakeLayer(TrenchCreatureModel.LAYER));
        this.shadowRadius = 0.5F;
    }

    @Override
    public TrenchRenderState createRenderState() {
        return new TrenchRenderState();
    }

    @Override
    public void extractRenderState(TrenchCreatureEntity creature, TrenchRenderState state, float partialTick) {
        super.extractRenderState(creature, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, creature.yRotO, creature.getYRot());
        state.pitch = Mth.lerp(partialTick, creature.xRotO, creature.getXRot());
        state.swimPhase = creature.swimPhase(partialTick);
        state.swimAmount = creature.swimAmount(partialTick);
        state.attack = creature.attackProgress(partialTick);
        state.brute = creature.isBrute();
        state.carrying = creature.isVehicle();
        state.hurt = creature.hurtTime > 0 || creature.deathTime > 0;
        state.deathTime = creature.deathTime > 0 ? creature.deathTime + partialTick : 0.0F;
        state.scale = creature.getScale();
    }

    @Override
    public void submit(TrenchRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float s = MODEL_SCALE * state.scale;
        float hips = TrenchCreatureModel.LEGS / 16.0F * s;
        poseStack.translate(0.0F, hips, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // Lean into the swim (top forward), then follow the swim's pitch.
        float lean = state.swimAmount * 75.0F + state.pitch * 0.6F * state.swimAmount;
        poseStack.mulPose(Axis.XP.rotationDegrees(-lean));
        if (state.deathTime > 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(1.0F, state.deathTime / 15.0F) * 160.0F));
        }
        poseStack.scale(-s, -s, s);
        model.setupAnim(state);
        int overlay = OverlayTexture.pack(0.0F, state.hurt);
        collector.submitModel(model, state, poseStack, model.renderType(state.brute ? BRUTE : CREATURE), state.lightCoords, overlay, state.outlineColor, null);
        collector.submitModel(model, state, poseStack, RenderTypes.eyes(state.brute ? BRUTE_GLOW : CREATURE_GLOW), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
