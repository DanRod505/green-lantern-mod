package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.AtlanteanDolphinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders the Atlantean dolphins: six colors, each with its own glowing markings. */
public class AtlanteanDolphinRenderer extends MountRenderer<AtlanteanDolphinEntity> {
    private static final Identifier[] TEXTURES = new Identifier[AtlanteanDolphinEntity.VARIANTS];
    private static final Identifier[] GLOW = new Identifier[AtlanteanDolphinEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = GreenLantern.id("textures/entity/atlantis/atlantean_dolphin_" + i + ".png");
            GLOW[i] = GreenLantern.id("textures/entity/atlantis/atlantean_dolphin_glow_" + i + ".png");
        }
    }

    public AtlanteanDolphinRenderer(EntityRendererProvider.Context context) {
        super(context, new AtlanteanDolphinModel(context.bakeLayer(AtlanteanDolphinModel.LAYER)), AtlanteanDolphinEntity.HEIGHT * 0.5F, 1.0F, 0.8F);
    }

    @Override
    protected Identifier texture(MountRenderState state) {
        return TEXTURES[Mth.clamp(state.variant, 0, TEXTURES.length - 1)];
    }

    @Override
    protected Identifier glow(MountRenderState state) {
        return GLOW[Mth.clamp(state.variant, 0, GLOW.length - 1)];
    }
}
