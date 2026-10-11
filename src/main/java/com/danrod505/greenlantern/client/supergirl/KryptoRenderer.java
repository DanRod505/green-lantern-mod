package com.danrod505.greenlantern.client.supergirl;

import com.danrod505.greenlantern.supergirl.KryptoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Renders Krypto: a white dog with a red collar, the S in gold and his little red cape. */
public class KryptoRenderer extends MobRenderer<KryptoEntity, KryptoRenderState, KryptoModel> {
    public KryptoRenderer(EntityRendererProvider.Context context) {
        super(context, new KryptoModel(context.bakeLayer(KryptoModel.LAYER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(KryptoRenderState state) {
        return KryptoEntity.TEXTURE;
    }

    @Override
    public KryptoRenderState createRenderState() {
        return new KryptoRenderState();
    }

    @Override
    protected int getModelTint(KryptoRenderState state) {
        float shade = state.wetShade;
        return shade == 1.0F ? -1 : ARGB.colorFromFloat(1.0F, shade, shade, shade);
    }

    @Override
    public void extractRenderState(KryptoEntity krypto, KryptoRenderState state, float partialTick) {
        super.extractRenderState(krypto, state, partialTick);
        state.isAngry = krypto.isAngry();
        state.isSitting = krypto.isInSittingPose();
        state.tailAngle = krypto.getTailAngle();
        state.headRollAngle = krypto.getHeadRollAngle(partialTick);
        state.shakeAnim = krypto.getShakeAnim(partialTick);
        state.wetShade = krypto.getWetShade(partialTick);
        state.texture = KryptoEntity.TEXTURE;
        state.flying = krypto.isFlying();
    }
}
