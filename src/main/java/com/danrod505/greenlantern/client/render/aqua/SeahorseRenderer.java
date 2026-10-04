package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.GiantSeahorseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renders the giant seahorse of Atlantis: one texture per color and pattern (8 x 3). The upright
 * body follows only part of the swim pitch, so the rider on its back stays in place.
 */
public class SeahorseRenderer extends MountRenderer<GiantSeahorseEntity> {
    public static final float SCALE = 0.8F;
    private static final Identifier[] TEXTURES = new Identifier[GiantSeahorseEntity.COLORS * GiantSeahorseEntity.PATTERNS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = GreenLantern.id("textures/entity/atlantis/giant_seahorse_" + i + ".png");
    }

    public SeahorseRenderer(EntityRendererProvider.Context context) {
        super(context, new SeahorseModel(context.bakeLayer(SeahorseModel.LAYER)), 1.35F, 0.3F, 0.8F);
    }

    @Override
    protected Identifier texture(MountRenderState state) {
        return TEXTURES[Mth.clamp(state.variant, 0, TEXTURES.length - 1)];
    }

    @Override
    protected float scale(MountRenderState state) {
        return SCALE;
    }
}
