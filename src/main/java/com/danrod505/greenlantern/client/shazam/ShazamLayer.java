package com.danrod505.greenlantern.client.shazam;

import com.danrod505.greenlantern.client.flight.FlightVisuals;
import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.shazam.ShazamContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * What the flat suit texture can't do for Shazam: his short white cape with the golden border,
 * fastened on the shoulders by two golden buttons, that sways as he walks and streams out in flight.
 */
public class ShazamLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float P = 1.0F / 16.0F;
    private static final int ROWS = 6;
    private static final int COLS = 5;
    private static final int CAPE_OUT = 0xFFF4F2EC;
    private static final int CAPE_IN = 0xFFD6D2C8;
    private static final int GOLD = 0xFFF2B705;
    private static final int GOLD_DARK = 0xFFB88A04;
    private static final float ATTACH_Z = 2.3F;

    public ShazamLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.chestEquipment.is(ShazamContent.SUIT.get())) return;
        FlightVisuals.Visual flight = FlightVisuals.get(state.id);
        float partial = Mth.frac(state.ageInTicks);
        float hero = flight == null ? 0.0F : flight.heroPose(partial);
        float flying = flight == null ? 0.0F : flight.hover;
        float age = state.ageInTicks;

        float length = 11.0F;
        float topHalf = 4.5F;
        float bottomHalf = Mth.lerp(hero, 5.8F, 5.0F);
        float hang = 7.0F + state.capeLean / 2.0F + state.capeFlap * 1.2F;
        float billow = 38.0F + 9.0F * Mth.sin(age * 0.2F);
        float theta = Mth.lerp(hero, Mth.lerp(flying, Mth.clamp(hang, 0.0F, 115.0F), billow), 8.0F) * Mth.DEG_TO_RAD;
        float sway = state.capeLean2 / 2.0F * Mth.DEG_TO_RAD * (1.0F - hero);
        float cos = Mth.cos(theta);
        float sin = Mth.sin(theta);
        float flutter = Mth.lerp(hero, 0.25F + Mth.clamp(state.capeFlap / 25.0F, 0.0F, 0.8F) + flying * 0.55F, 1.3F);
        float waveSpeed = Mth.lerp(hero, 0.4F, 1.25F);

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
                // The last row is the golden border.
                int out = i == ROWS - 1 ? GOLD : CAPE_OUT;
                int in = i == ROWS - 1 ? GOLD_DARK : CAPE_IN;
                for (int j = 0; j < COLS; j++) {
                    float[] a = grid[i][j];
                    float[] b = grid[i][j + 1];
                    float[] c = grid[i + 1][j + 1];
                    float[] d = grid[i + 1][j];
                    Geo.doubleQuad(vc, pose, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2], in, out, light);
                }
            }
            // The collar of the cape and the two golden buttons on the shoulders.
            Geo.box(vc, pose, -4.2F * P, -0.5F * P, 1.9F * P, 4.2F * P, 0.5F * P, 2.6F * P, GOLD, light);
            Geo.box(vc, pose, -3.8F * P, -0.4F * P, -2.4F * P, -2.4F * P, 1.0F * P, -2.0F * P, GOLD, light);
            Geo.box(vc, pose, 2.4F * P, -0.4F * P, -2.4F * P, 3.8F * P, 1.0F * P, -2.0F * P, GOLD, light);
        });
        poseStack.popPose();
    }
}
