package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.Respirator;
import com.danrod505.greenlantern.client.render.HardLight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The Atlantean Respirator, clipped over whatever covers the face (a hero's mask or a helmet): a
 * golden mouthpiece with glowing gills on the cheeks and straps around the head. It shows while the
 * respirator is breathing for you.
 */
public class RespiratorLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/atlantis/respirator.png");
    private static final float P = 1.0F / 16.0F;

    public RespiratorLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    private static boolean breathing(int entityId) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof Player player)) return false;
        return player.isEyeInFluid(FluidTags.WATER) && !AquamanHelper.isSuited(player) && Respirator.canBreatheUnderwater(player);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !breathing(state.id)) return;
        float glow = 0.75F + 0.25F * Mth.sin(state.ageInTicks * 0.25F);
        int gold = 0xFFE8B83A;
        int teal = 0xFF1C8C8C;
        int gill = 0xFF000000 | (int) (120 * glow) << 16 | (int) (240 * glow) << 8 | 255;
        poseStack.pushPose();
        getParentModel().root().translateAndRotate(poseStack);
        getParentModel().translateToHead(poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), (pose, vc) -> {
            // Mouthpiece in front of the face (the head spans x -4..4, y -8..0, z -4..4 pixels; the face is at z = -4).
            HardLight.box(vc, pose, -2.0F * P, -3.2F * P, -5.6F * P, 2.0F * P, -0.8F * P, -4.3F * P, gold);
            HardLight.box(vc, pose, -1.0F * P, -2.6F * P, -6.2F * P, 1.0F * P, -1.4F * P, -5.6F * P, teal);
            // Gills on the cheeks.
            for (int side = -1; side <= 1; side += 2) {
                float x0 = side * 4.3F * P;
                float x1 = side * 5.0F * P;
                HardLight.box(vc, pose, Math.min(x0, x1), -3.6F * P, -3.6F * P, Math.max(x0, x1), -0.6F * P, -1.0F * P, teal);
                for (int i = 0; i < 3; i++) {
                    float y = (-3.2F + i) * P;
                    float xa = side * 5.0F * P;
                    float xb = side * 5.25F * P;
                    HardLight.box(vc, pose, Math.min(xa, xb), y, -3.3F * P, Math.max(xa, xb), y + 0.4F * P, -1.3F * P, gill);
                }
            }
            // Strap around the back of the head.
            HardLight.box(vc, pose, -4.4F * P, -2.4F * P, 3.9F * P, 4.4F * P, -1.6F * P, 4.4F * P, gold);
        });
        poseStack.popPose();
    }
}
