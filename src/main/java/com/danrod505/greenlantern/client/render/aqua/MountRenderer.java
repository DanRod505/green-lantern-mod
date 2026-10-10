package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.entity.AtlanteanMountEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Shared renderer of the sea creatures of Atlantis: the model turned to the creature's yaw and
 * pitched along its swim (as the shark), the red flash when hurt, a roll onto the back when it dies
 * and an optional glowing layer.
 */
public abstract class MountRenderer<T extends AtlanteanMountEntity> extends EntityRenderer<T, MountRenderState> {
    private final EntityModel<MountRenderState> model;
    private final float centerHeight;
    private final float pitchFactor;

    protected MountRenderer(EntityRendererProvider.Context context, EntityModel<MountRenderState> model, float centerHeight, float pitchFactor,
            float shadow) {
        super(context);
        this.model = model;
        this.centerHeight = centerHeight;
        this.pitchFactor = pitchFactor;
        this.shadowRadius = shadow;
    }

    protected abstract Identifier texture(MountRenderState state);

    /** Glowing (full bright) layer drawn on top, or null. */
    protected @Nullable Identifier glow(MountRenderState state) {
        return null;
    }

    /** Uniform scale of the model. */
    protected float scale(MountRenderState state) {
        return 1.0F;
    }

    @Override
    public MountRenderState createRenderState() {
        return new MountRenderState();
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public void extractRenderState(T mount, MountRenderState state, float partialTick) {
        super.extractRenderState(mount, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, mount.yRotO, mount.getYRot());
        state.pitch = Mth.lerp(partialTick, mount.xRotO, mount.getXRot());
        state.swimPhase = mount.swimPhase(partialTick);
        state.swimAmount = mount.swimAmount(partialTick);
        state.outOfWater = !mount.isInWater();
        state.ridden = mount.isVehicle();
        state.hurt = mount.hurtTime > 0 || mount.deathTime > 0;
        state.deathTime = mount.deathTime > 0 ? mount.deathTime + partialTick : 0.0F;
        state.variant = mount.variant();
    }

    @Override
    public void submit(MountRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, centerHeight, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // Vanilla pitch is positive looking down: nose down means rotating the -Z front downwards.
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch * pitchFactor));
        if (state.deathTime > 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(1.0F, state.deathTime / 15.0F) * 180.0F));
        }
        float s = scale(state);
        poseStack.scale(-s, -s, s);
        model.setupAnim(state);
        int overlay = OverlayTexture.pack(0.0F, state.hurt);
        collector.submitModel(model, state, poseStack, model.renderType(texture(state)), state.lightCoords, overlay, state.outlineColor, null);
        Identifier glow = glow(state);
        if (glow != null) {
            collector.submitModel(model, state, poseStack, RenderTypes.eyes(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0, null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
