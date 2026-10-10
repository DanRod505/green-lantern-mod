package com.danrod505.greenlantern.client.render.oa;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.entity.OaPortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The portal: an upright oval ring of hard light around a swirling vortex. Portals on Oa (leading
 * home) swirl the other way and shine paler.
 */
public class OaPortalRenderer extends EntityRenderer<OaPortalEntity, OaPortalRenderer.State> {
    private static final Identifier SWIRL = GreenLantern.id("textures/entity/oa/portal_swirl.png");
    private static final int SEGMENTS = 48;

    public OaPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(OaPortalEntity portal, State state, float partialTick) {
        super.extractRenderState(portal, state, partialTick);
        state.open = portal.openness(partialTick);
        state.yaw = portal.getYRot();
        state.home = portal.leadsHome();
    }

    @Override
    protected int getBlockLightLevel(OaPortalEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float open = state.open;
        if (open <= 0.01F) return;
        float ease = open * (2.0F - open);
        float age = state.ageInTicks;
        float rx = OaPortalEntity.HALF_WIDTH * ease;
        float ry = OaPortalEntity.HALF_HEIGHT * (0.15F + 0.85F * ease);
        float spin = state.home ? -1.0F : 1.0F;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.2F);

        poseStack.pushPose();
        poseStack.translate(0.0F, OaPortalEntity.CENTER_Y, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));

        // Vortex: two counter-rotating layers.
        collector.submitCustomGeometry(poseStack, HardLight.type(SWIRL), (pose, vc) -> {
            int deep = state.home ? color(0.75F * pulse, 150, 255, 190) : color(0.85F * pulse, 40, 230, 90);
            disc(vc, pose, rx * 0.97F, ry * 0.97F, 0.0F, age * 0.05F * spin, deep, 1.0F);
            disc(vc, pose, rx * 0.97F, ry * 0.97F, 0.01F, -age * 0.08F * spin, color(0.45F, 200, 255, 215), 0.7F);
        });
        // Rim: a thick oval band with bright faces and a faint outer glow.
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL), (pose, vc) -> {
            ring(vc, pose, rx, ry, 0.22F, 0.12F, HardLight.color(0.9F, 0.35F * pulse));
            ring(vc, pose, rx + 0.1F, ry + 0.1F, 0.42F, 0.16F, HardLight.color(0.2F, 0.0F));
            // Sparks orbiting the rim.
            for (int i = 0; i < 8; i++) {
                float a = age * 0.12F * spin + i * Mth.TWO_PI / 8;
                float x = Mth.cos(a) * (rx + 0.05F);
                float y = Mth.sin(a) * (ry + 0.05F);
                HardLight.box(vc, pose, x - 0.07F, y - 0.07F, -0.07F, x + 0.07F, y + 0.07F, 0.07F, HardLight.color(1.0F, 1.0F));
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static int color(float alpha, int r, int g, int b) {
        return Mth.clamp((int) (alpha * 255), 0, 255) << 24 | r << 16 | g << 8 | b;
    }

    /** Filled oval facing both ways; the texture is rotated by {@code rot} radians and scaled by {@code uvScale}. */
    private static void disc(VertexConsumer vc, PoseStack.Pose pose, float rx, float ry, float z, float rot, int color, float uvScale) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            float x0 = Mth.cos(a0) * rx, y0 = Mth.sin(a0) * ry;
            float x1 = Mth.cos(a1) * rx, y1 = Mth.sin(a1) * ry;
            float u0 = 0.5F + 0.5F * uvScale * Mth.cos(a0 + rot), v0 = 0.5F + 0.5F * uvScale * Mth.sin(a0 + rot);
            float u1 = 0.5F + 0.5F * uvScale * Mth.cos(a1 + rot), v1 = 0.5F + 0.5F * uvScale * Mth.sin(a1 + rot);
            // Front (+Z) and back (-Z); the centre vertex is doubled to make a triangle.
            HardLight.vertex(vc, pose, 0, 0, z, 0.5F, 0.5F, color, 0, 0, 1);
            HardLight.vertex(vc, pose, x0, y0, z, u0, v0, color, 0, 0, 1);
            HardLight.vertex(vc, pose, x1, y1, z, u1, v1, color, 0, 0, 1);
            HardLight.vertex(vc, pose, 0, 0, z, 0.5F, 0.5F, color, 0, 0, 1);
            HardLight.vertex(vc, pose, 0, 0, -z, 0.5F, 0.5F, color, 0, 0, -1);
            HardLight.vertex(vc, pose, x1, y1, -z, u1, v1, color, 0, 0, -1);
            HardLight.vertex(vc, pose, x0, y0, -z, u0, v0, color, 0, 0, -1);
            HardLight.vertex(vc, pose, 0, 0, -z, 0.5F, 0.5F, color, 0, 0, -1);
        }
    }

    /** Oval band of the given width (in the plane) and depth (along Z). */
    private static void ring(VertexConsumer vc, PoseStack.Pose pose, float rx, float ry, float width, float depth, int color) {
        float d = depth / 2;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float ix0 = c0 * rx, iy0 = s0 * ry, ix1 = c1 * rx, iy1 = s1 * ry;
            float ox0 = c0 * (rx + width), oy0 = s0 * (ry + width), ox1 = c1 * (rx + width), oy1 = s1 * (ry + width);
            // Front and back faces.
            HardLight.quad(vc, pose, ix0, iy0, d, ox0, oy0, d, ox1, oy1, d, ix1, iy1, d, color, 0, 0, 1);
            HardLight.quad(vc, pose, ix1, iy1, -d, ox1, oy1, -d, ox0, oy0, -d, ix0, iy0, -d, color, 0, 0, -1);
            // Outer and inner edges.
            HardLight.quad(vc, pose, ox0, oy0, d, ox0, oy0, -d, ox1, oy1, -d, ox1, oy1, d, color, c0, s0, 0);
            HardLight.quad(vc, pose, ix1, iy1, d, ix1, iy1, -d, ix0, iy0, -d, ix0, iy0, d, color, -c0, -s0, 0);
        }
    }

    public static class State extends EntityRenderState {
        public float open;
        public float yaw;
        public boolean home;
    }
}
