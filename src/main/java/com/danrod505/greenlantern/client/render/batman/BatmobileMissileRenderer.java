package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.entity.BatmobileMissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;

/** The Batmobile's missile: a dark rocket with a yellow band and a bright exhaust once it's lit. */
public class BatmobileMissileRenderer extends EntityRenderer<BatmobileMissileEntity, BatmobileMissileRenderer.State> {
    public BatmobileMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BatmobileMissileEntity missile, State state, float partialTick) {
        super.extractRenderState(missile, state, partialTick);
        state.yaw = missile.getYRot(partialTick);
        state.pitch = missile.getXRot(partialTick);
        state.ignited = missile.ignited();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 15.0F));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            Geo.box(vc, pose, -0.09F, -0.09F, -0.4F, 0.09F, 0.09F, 0.3F, 0xFF2A2B31, light);
            Geo.taper(vc, pose, -0.09F, -0.09F, 0.3F, 0.09F, 0.09F, 0.5F, -0.02F, 0.38F, 0.02F, 0.42F, 0xFF3B3D45, 0xFF3B3D45, light);
            Geo.box(vc, pose, -0.095F, -0.095F, 0.12F, 0.095F, 0.095F, 0.2F, 0xFFE0B020, light);
            Geo.box(vc, pose, -0.24F, -0.015F, -0.4F, 0.24F, 0.015F, -0.18F, 0xFF17181C, light);
            Geo.box(vc, pose, -0.015F, -0.24F, -0.4F, 0.015F, 0.24F, -0.18F, 0xFF17181C, light);
        });
        if (state.ignited) {
            float pulse = 0.8F + 0.2F * Mth.sin(state.ageInTicks * 2.7F);
            int full = LightTexture.FULL_BRIGHT;
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(Geo.PLAIN), (pose, vc) -> {
                float r = 0.12F * pulse;
                float len = 0.7F * pulse;
                int color = Geo.withAlpha(0xFFB040, 0.85F);
                Geo.doubleQuad(vc, pose, -r, 0, -0.41F, r, 0, -0.41F, 0, 0, -0.41F - len, 0, 0, -0.41F - len, color, color, full);
                Geo.doubleQuad(vc, pose, 0, -r, -0.41F, 0, r, -0.41F, 0, 0, -0.41F - len, 0, 0, -0.41F - len, color, color, full);
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        boolean ignited;
    }
}
