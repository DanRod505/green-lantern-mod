package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.render.batman.Geo;
import com.danrod505.greenlantern.engine.CharacterData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The two wing bones of the Winged skeleton (catalog, stage 7): folded on the back on the ground,
 * open in the air, beating while climbing. Drawn in the character's colours, feather by feather.
 */
public class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float P = 1.0F / 16.0F;
    private static final int FEATHERS = 6;

    public WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        SkinLook look = SkinLook.of(state.id);
        if (look == null || !look.skeleton().wings()) return;
        float open = openness(state);
        float age = state.ageInTicks;
        float beat = open * Mth.sin(age * 0.45F) * 0.35F;
        CharacterData.Colors colors = look.hero().data().colors();
        int main = 0xFF000000 | colors.main();
        int edge = 0xFF000000 | colors.light();
        int inner = Geo.darken(main, 0.7F);

        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().body.translateAndRotate(poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Geo.PLAIN), (pose, vc) -> {
            for (int side = -1; side <= 1; side += 2) {
                // Folded: along the back, pointing down. Open: out to the side and a little back.
                float spread = Mth.lerp(open, 0.15F, 1.35F) + beat;
                float sx = Mth.cos(spread) * side;
                float sy = Mth.sin(spread);
                float rootX = side * 1.5F;
                float rootY = 1.5F;
                float rootZ = 2.2F;
                for (int i = 0; i < FEATHERS; i++) {
                    float t0 = (float) i / FEATHERS;
                    float t1 = (float) (i + 1) / FEATHERS;
                    float len0 = 14.0F * t0;
                    float len1 = 14.0F * t1;
                    float drop0 = Mth.lerp(t0, 7.0F, 13.0F);
                    float drop1 = Mth.lerp(t1, 7.0F, 13.0F);
                    float back0 = rootZ + t0 * Mth.lerp(open, 1.5F, 3.0F);
                    float back1 = rootZ + t1 * Mth.lerp(open, 1.5F, 3.0F);
                    // Along the wing: x out to the side, y down the back as it folds.
                    float ax = rootX + sx * len0, ay = rootY - sy * len0 * 0.4F + (1 - open) * len0 * 0.9F;
                    float bx = rootX + sx * len1, by = rootY - sy * len1 * 0.4F + (1 - open) * len1 * 0.9F;
                    int color = i == FEATHERS - 1 ? edge : (i % 2 == 0 ? main : inner);
                    Geo.doubleQuad(vc, pose, ax * P, ay * P, back0 * P, bx * P, by * P, back1 * P,
                            bx * P, (by + drop1) * P, (back1 + 0.3F) * P, ax * P, (ay + drop0) * P, (back0 + 0.3F) * P, inner, color, light);
                }
            }
        });
        poseStack.popPose();
    }

    /** 0 folded (on the ground), 1 wide open (in the air). */
    private static float openness(AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(state.id);
        if (entity == null) return 0.0F;
        if (entity.onGround() || entity.isInWater()) return 0.0F;
        return Mth.clamp((float) (Math.abs(entity.getDeltaMovement().y) * 4.0 + 0.5), 0.0F, 1.0F);
    }
}
