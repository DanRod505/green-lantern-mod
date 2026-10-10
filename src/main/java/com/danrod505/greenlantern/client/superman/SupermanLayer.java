package com.danrod505.greenlantern.client.superman;

import com.danrod505.greenlantern.client.flight.FlightVisuals;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.superman.SuperFlags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * What the flat suit texture can't do: Superman's long red cape (it hangs and sways like a vanilla
 * cape, billows while he hovers and streams out behind him at speed) and his eyes glowing red while
 * heat vision burns.
 */
public class SupermanLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float P = 1.0F / 16.0F;
    private static final int ROWS = 8;
    private static final int COLS = 5;
    private static final int CAPE_OUT = 0xFFC8141E;
    private static final int CAPE_IN = 0xFF9A0E16;
    /** Where the cape hangs from, behind the shoulders (pixels, body space: +Z is the back). */
    private static final float ATTACH_Z = 2.3F;

    public SupermanLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        if (state.chestEquipment.is(ModItems.SUPERMAN_SUIT.get())) submitCape(poseStack, collector, light, state);
        if (SuperFlags.has(SupermanVisuals.flags(state.id), SuperFlags.HEAT_VISION)) submitEyes(poseStack, collector, state);
    }

    private void submitEyes(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state) {
        float pulse = 0.75F + 0.25F * Mth.sin(state.ageInTicks * 1.7F);
        int glow = (int) (255 * pulse) << 24 | 0xFF2A10;
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().translateToHead(poseStack);
        collector.submitCustomGeometry(poseStack, HardLight.type(HardLight.PANEL_SOLID), (pose, vc) -> {
            // The face spans x -4..4, y -8..0 pixels and is at z = -4; the eyes are on the 4th row.
            for (int side = -1; side <= 1; side += 2) {
                float cx = side * 2.0F;
                HardLight.box(vc, pose, (cx - 1.0F) * P, -4.6F * P, -4.35F * P, (cx + 1.0F) * P, -3.4F * P, -4.05F * P, 0xFFFFE0C0);
                HardLight.box(vc, pose, (cx - 1.6F) * P, -5.1F * P, -4.6F * P, (cx + 1.6F) * P, -2.9F * P, -4.1F * P, glow & 0x80FFFFFF);
            }
        });
        poseStack.popPose();
    }

    private void submitCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
        FlightVisuals.Visual flight = FlightVisuals.get(state.id);
        float partial = Mth.frac(state.ageInTicks);
        // Speed: 0 standing, 1 lying horizontally in full flight.
        float hero = flight == null ? 0.0F : flight.heroPose(partial);
        float flying = flight == null ? 0.0F : flight.hover;
        float age = state.ageInTicks;

        float length = 20.0F;
        float topHalf = 4.8F;
        float bottomHalf = Mth.lerp(hero, 7.0F, 6.0F);
        // Standing it hangs like a vanilla cape; hovering it billows back; at speed it streams behind
        // (along the body, which lies along the flight).
        float hang = 6.0F + state.capeLean / 2.0F + state.capeFlap;
        float billow = 28.0F + 6.0F * Mth.sin(age * 0.12F);
        float theta = Mth.lerp(hero, Mth.lerp(flying, Mth.clamp(hang, 0.0F, 110.0F), billow), 6.0F) * Mth.DEG_TO_RAD;
        float sway = state.capeLean2 / 2.0F * Mth.DEG_TO_RAD * (1.0F - hero);
        float cos = Mth.cos(theta);
        float sin = Mth.sin(theta);
        float flutter = Mth.lerp(hero, 0.2F + Mth.clamp(state.capeFlap / 30.0F, 0.0F, 0.8F) + flying * 0.5F, 1.6F);
        float waveSpeed = Mth.lerp(hero, 0.3F, 1.1F);

        float[][][] grid = new float[ROWS + 1][COLS + 1][3];
        for (int i = 0; i <= ROWS; i++) {
            float t = (float) i / ROWS;
            float half = Mth.lerp(t, topHalf, bottomHalf);
            for (int j = 0; j <= COLS; j++) {
                float u = (float) j / COLS * 2.0F - 1.0F;
                float x = u * half;
                float y = t * length;
                // Ripples running down the cloth, stronger towards the hem.
                float dz = Mth.sin(age * waveSpeed + t * 6.0F + u * 1.5F) * flutter * t
                        + Mth.cos(u * Mth.PI) * 0.4F;
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
            // The clasps on the shoulders.
            Geo.box(vc, pose, -4.2F * P, -0.5F * P, 1.9F * P, 4.2F * P, 0.5F * P, 2.6F * P, CAPE_OUT, light);
            Geo.box(vc, pose, -3.6F * P, -0.2F * P, -2.3F * P, -2.4F * P, 1.0F * P, -2.0F * P, 0xFFF2C21A, light);
            Geo.box(vc, pose, 2.4F * P, -0.2F * P, -2.3F * P, 3.6F * P, 1.0F * P, -2.0F * P, 0xFFF2C21A, light);
        });
        poseStack.popPose();
    }
}
