package com.danrod505.greenlantern.client.render.batman;

import com.danrod505.greenlantern.client.batman.BatmanVisuals;
import com.danrod505.greenlantern.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The parts of Batman's suit a flat armor texture can't do: the cowl's pointed ears and the long
 * scalloped cape. The cape hangs and sways like a vanilla cape and, when gliding, spreads into a
 * wide rigid wing.
 */
public class BatmanLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float P = 1.0F / 16.0F;
    private static final int ROWS = 8;
    private static final int COLS = 6;
    private static final int CAPE_OUT = 0xFF1C1D22;
    private static final int CAPE_IN = 0xFF2A2B33;
    private static final int COWL = 0xFF17181C;
    /** Where the cape hangs from, behind the shoulders (pixels, body space: +Z is the back). */
    private static final float ATTACH_Z = 2.3F;

    public BatmanLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        if (state.headEquipment.is(ModItems.BATMAN_COWL.get())) submitEars(poseStack, collector, light);
        if (state.chestEquipment.is(ModItems.BATMAN_SUIT.get())) submitCape(poseStack, collector, light, state);
    }

    private void submitEars(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().translateToHead(poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            // The head spans x -4..4, y -8..0, z -4..4 pixels (y grows downwards); the ears stand on top of it.
            for (int side = -1; side <= 1; side += 2) {
                float c = side * 2.8F;
                box(vc, pose, c - 1.0F, -9.4F, -1.0F, c + 1.0F, -8.0F, 0.6F, light);
                box(vc, pose, c - 0.7F, -10.8F, -0.8F, c + 0.7F, -9.4F, 0.4F, light);
                box(vc, pose, c - 0.4F, -12.0F, -0.6F, c + 0.4F, -10.8F, 0.2F, light);
                float tip = c + side * 0.1F;
                box(vc, pose, tip - 0.2F, -12.8F, -0.45F, tip + 0.2F, -12.0F, 0.05F, light);
            }
        });
        poseStack.popPose();
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        Geo.box(vc, pose, x0 * P, y0 * P, z0 * P, x1 * P, y1 * P, z1 * P, COWL, light);
    }

    private void submitCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
        float s = BatmanVisuals.spread(state.id, Mth.frac(state.ageInTicks));
        float length = Mth.lerp(s, 21.0F, 20.0F);
        float topHalf = Mth.lerp(s, 5.5F, 16.0F);
        float bottomHalf = Mth.lerp(s, 8.0F, 18.0F);
        float hang = 6.0F + state.capeLean / 2.0F + state.capeFlap;
        float theta = Mth.lerp(s, Mth.clamp(hang, 0.0F, 110.0F), 12.0F) * Mth.DEG_TO_RAD;
        float sway = state.capeLean2 / 2.0F * Mth.DEG_TO_RAD * (1.0F - s);
        float age = state.ageInTicks;
        float cos = Mth.cos(theta);
        float sin = Mth.sin(theta);
        float flutter = (1.0F - s) * Mth.clamp(state.capeFlap / 30.0F, 0.15F, 1.0F) + s * 0.25F;

        // Grid of points (x, y, z) in pixels, body space.
        float[][][] grid = new float[ROWS + 1][COLS + 1][3];
        for (int i = 0; i <= ROWS; i++) {
            float t = (float) i / ROWS;
            float half = Mth.lerp(t, topHalf, bottomHalf);
            for (int j = 0; j <= COLS; j++) {
                float u = (float) j / COLS * 2.0F - 1.0F;
                float x = u * half;
                float y = t * length;
                // The scalloped hem: the points between the ribs ride up.
                if (i == ROWS && j % 2 == 1) y -= 2.5F;
                // Folds and ripples, stronger down the cape.
                float dz = Mth.sin(age * 0.35F + t * 5.0F + u * 2.0F) * 0.6F * t * flutter
                        + Mth.cos(u * Mth.PI * 1.5F) * 0.5F * (1.0F - s);
                // Spread as a wing: the outer parts bow back a little, like a bat's membrane.
                dz += u * u * 2.0F * s - Mth.sin(t * Mth.PI) * 1.2F * s;
                float yy = y * cos - dz * sin;
                float zz = ATTACH_Z + y * sin + dz * cos;
                x += zz * Mth.sin(sway) * 0.5F;
                grid[i][j][0] = x * P;
                grid[i][j][1] = yy * P;
                grid[i][j][2] = zz * P;
            }
        }

        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().body.translateAndRotate(poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(Geo.PLAIN), (pose, vc) -> {
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    float[] a = grid[i][j];
                    float[] b = grid[i][j + 1];
                    float[] c = grid[i + 1][j + 1];
                    float[] d = grid[i + 1][j];
                    Geo.doubleQuad(vc, pose, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2], CAPE_IN, CAPE_OUT, light);
                }
            }
            // A stiff collar where the cape meets the shoulders.
            Geo.box(vc, pose, -4.2F * P, -0.6F * P, 1.9F * P, 4.2F * P, 0.6F * P, 2.6F * P, CAPE_OUT, light);
        });
        poseStack.popPose();
    }
}
