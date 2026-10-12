package com.danrod505.greenlantern.client.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * The glowing layer of a skin ({@code <skin>_brilho.png}, catalog stage 13): eyes, emblems and
 * lights that shine in the dark, drawn over the skin at full brightness.
 */
public class SkinGlowLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public SkinGlowLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        SkinLook look = SkinLook.of(state.id);
        if (look == null) return;
        Identifier glow = look.skin().glow();
        if (!SkinLook.exists(glow)) return;
        collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(glow), light, OverlayTexture.NO_OVERLAY, -1, null,
                state.outlineColor, null);
    }
}
