package com.danrod505.greenlantern.client.supergirl;

import com.danrod505.greenlantern.client.flight.FlightVisuals;
import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.supergirl.SupergirlContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * What the flat suit texture can't do for Supergirl: her short red cape (down to the waist, it
 * sways more than Superman's long one and streams out in flight), the flared red skirt with the
 * golden belt, and her long blonde hair falling over the shoulders.
 */
public class SupergirlLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float P = 1.0F / 16.0F;
    private static final int ROWS = 6;
    private static final int COLS = 5;
    private static final int CAPE_OUT = 0xFFD8202E;
    private static final int CAPE_IN = 0xFFA0141E;
    private static final int GOLD = 0xFFFFD447;
    private static final int GOLD_DARK = 0xFFC89A1E;
    private static final int SKIRT = 0xFFD8202E;
    private static final int SKIRT_DARK = 0xFFB01822;
    private static final int HAIR = 0xFFF2D27A;
    private static final int HAIR_LIGHT = 0xFFFFE9A8;
    private static final float ATTACH_Z = 2.3F;

    public SupergirlLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.chestEquipment.is(SupergirlContent.SUIT.get())) return;
        submitCape(poseStack, collector, light, state);
        submitSkirt(poseStack, collector, light);
        if (state.headEquipment.is(SupergirlContent.MASK.get())) submitHair(poseStack, collector, light, state);
    }

    private void submitCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
        FlightVisuals.Visual flight = FlightVisuals.get(state.id);
        float partial = Mth.frac(state.ageInTicks);
        float hero = flight == null ? 0.0F : flight.heroPose(partial);
        float flying = flight == null ? 0.0F : flight.hover;
        float age = state.ageInTicks;

        float length = 12.0F;
        float topHalf = 4.6F;
        float bottomHalf = Mth.lerp(hero, 6.0F, 5.2F);
        float hang = 8.0F + state.capeLean / 2.0F + state.capeFlap * 1.3F;
        float billow = 40.0F + 10.0F * Mth.sin(age * 0.18F);
        float theta = Mth.lerp(hero, Mth.lerp(flying, Mth.clamp(hang, 0.0F, 120.0F), billow), 8.0F) * Mth.DEG_TO_RAD;
        float sway = state.capeLean2 / 2.0F * Mth.DEG_TO_RAD * (1.0F - hero);
        float cos = Mth.cos(theta);
        float sin = Mth.sin(theta);
        float flutter = Mth.lerp(hero, 0.3F + Mth.clamp(state.capeFlap / 25.0F, 0.0F, 0.9F) + flying * 0.6F, 1.4F);
        float waveSpeed = Mth.lerp(hero, 0.4F, 1.3F);

        float[][][] grid = new float[ROWS + 1][COLS + 1][3];
        for (int i = 0; i <= ROWS; i++) {
            float t = (float) i / ROWS;
            float half = Mth.lerp(t, topHalf, bottomHalf);
            for (int j = 0; j <= COLS; j++) {
                float u = (float) j / COLS * 2.0F - 1.0F;
                float x = u * half;
                float y = t * length;
                float dz = Mth.sin(age * waveSpeed + t * 7.0F + u * 1.8F) * flutter * t + Mth.cos(u * Mth.PI) * 0.4F;
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
            // The collar of the cape and the golden clasps with the S.
            Geo.box(vc, pose, -4.2F * P, -0.5F * P, 1.9F * P, 4.2F * P, 0.5F * P, 2.6F * P, CAPE_OUT, light);
            Geo.box(vc, pose, -3.7F * P, -0.3F * P, -2.35F * P, -2.3F * P, 1.1F * P, -2.0F * P, GOLD, light);
            Geo.box(vc, pose, 2.3F * P, -0.3F * P, -2.35F * P, 3.7F * P, 1.1F * P, -2.0F * P, GOLD, light);
        });
        poseStack.popPose();
    }

    /** The skirt flares from the golden belt at the waist down over the hips. */
    private void submitSkirt(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().body.translateAndRotate(poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            // Pleats: panels around the hips, alternating red and a darker red.
            float top = 10.4F;
            float bottom = 14.6F;
            float[][] corners = {{-4.25F, -2.25F}, {4.25F, -2.25F}, {4.25F, 2.25F}, {-4.25F, 2.25F}};
            float flare = 1.25F;
            int panels = 4;
            for (int side = 0; side < 4; side++) {
                float[] a = corners[side];
                float[] b = corners[(side + 1) % 4];
                for (int k = 0; k < panels; k++) {
                    float t0 = (float) k / panels;
                    float t1 = (float) (k + 1) / panels;
                    float x0 = Mth.lerp(t0, a[0], b[0]);
                    float z0 = Mth.lerp(t0, a[1], b[1]);
                    float x1 = Mth.lerp(t1, a[0], b[0]);
                    float z1 = Mth.lerp(t1, a[1], b[1]);
                    int color = k % 2 == 0 ? SKIRT : SKIRT_DARK;
                    Geo.doubleQuad(vc, pose, x0 * P, top * P, z0 * P, x1 * P, top * P, z1 * P,
                            x1 * flare * P, bottom * P, z1 * flare * P, x0 * flare * P, bottom * P, z0 * flare * P, color, SKIRT_DARK, light);
                }
            }
            // The belt.
            Geo.box(vc, pose, -4.3F * P, 9.4F * P, -2.3F * P, 4.3F * P, 10.6F * P, 2.3F * P, GOLD, GOLD, light);
            Geo.box(vc, pose, -1.0F * P, 9.2F * P, -2.5F * P, 1.0F * P, 10.8F * P, -2.25F * P, GOLD_DARK, light);
        });
        poseStack.popPose();
    }

    /** Long blonde hair down the back and over the shoulders. */
    private void submitHair(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
        FlightVisuals.Visual flight = FlightVisuals.get(state.id);
        float hero = flight == null ? 0.0F : flight.heroPose(Mth.frac(state.ageInTicks));
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().head.translateAndRotate(poseStack);
        float lift = hero * 4.5F;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            // The hair painted on the helmet falls on below it: down the back, blown back in flight...
            Geo.box(vc, pose, -4.7F * P, -2.0F * P, 4.6F * P, 4.7F * P, 0.0F, 5.4F * P, HAIR, HAIR_LIGHT, light);
            Geo.doubleQuad(vc, pose, -4.7F * P, 0.0F, 5.4F * P, 4.7F * P, 0.0F, 5.4F * P,
                    4.3F * P, (6.0F - lift) * P, (5.6F + lift * 1.2F) * P, -4.3F * P, (6.0F - lift) * P, (5.6F + lift * 1.2F) * P, HAIR, HAIR, light);
            // ...and two locks over the shoulders in front.
            for (int side = -1; side <= 1; side += 2) {
                float x0 = side < 0 ? -4.9F : 3.3F;
                float x1 = side < 0 ? -3.3F : 4.9F;
                Geo.box(vc, pose, x0 * P, -2.5F * P, -2.0F * P, x1 * P, (3.5F - lift) * P, 4.6F * P, HAIR, HAIR_LIGHT, light);
            }
        });
        poseStack.popPose();
    }
}
