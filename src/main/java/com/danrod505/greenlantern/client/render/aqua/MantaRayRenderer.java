package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.MantaRayEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders the giant manta ray of Atlantis (three looks). */
public class MantaRayRenderer extends MountRenderer<MantaRayEntity> {
    private static final Identifier[] TEXTURES = new Identifier[MantaRayEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = GreenLantern.id("textures/entity/atlantis/manta_ray_" + i + ".png");
    }

    public MantaRayRenderer(EntityRendererProvider.Context context) {
        super(context, new MantaRayModel(context.bakeLayer(MantaRayModel.LAYER)), MantaRayEntity.HEIGHT * 0.5F, 1.0F, 2.2F);
    }

    @Override
    protected Identifier texture(MountRenderState state) {
        return TEXTURES[Mth.clamp(state.variant, 0, TEXTURES.length - 1)];
    }
}
