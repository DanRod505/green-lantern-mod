package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GreenLantern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Swirling green energy aura around a flying Lantern (like a charged creeper, but hard light). */
public class AuraLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("aura"), "main");
    private static final Identifier TEXTURE = GreenLantern.id("textures/entity/aura.png");

    private final HumanoidModel<AvatarRenderState> model;

    public AuraLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, HumanoidModel<AvatarRenderState> model) {
        super(parent);
        this.model = model;
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.9F), 0.0F), 64, 32);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        FlightVisuals.Visual visual = FlightVisuals.get(state.id);
        if (visual == null || visual.aura < 0.02F || state.isInvisible) return;
        float age = state.ageInTicks;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.35F);
        float strength = Mth.clamp(visual.aura * pulse, 0.0F, 1.0F);
        int g = (int) (255 * strength);
        int rb = (int) (90 * strength * (visual.supersonic() ? 1.6F : 1.0F));
        int color = 0xFF000000 | Math.min(255, rb) << 16 | g << 8 | Math.min(255, rb + 20);
        float speed = 0.01F + 0.03F * visual.speed;
        collector.order(1).submitModel(model, state, poseStack,
                RenderTypes.energySwirl(TEXTURE, age * speed % 1.0F, age * speed * 0.7F % 1.0F),
                light, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
    }
}
