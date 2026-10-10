package com.danrod505.greenlantern.client.wonderwoman;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.JetControls;
import com.danrod505.greenlantern.client.WonderWomanHud;
import com.danrod505.greenlantern.client.particle.ShockwaveParticle;
import com.danrod505.greenlantern.client.particle.SuperParticle;
import com.danrod505.greenlantern.client.render.wonderwoman.AmazonShieldRenderer;
import com.danrod505.greenlantern.client.render.wonderwoman.InvisibleJetRenderer;
import com.danrod505.greenlantern.client.render.wonderwoman.LassoRenderer;
import com.danrod505.greenlantern.client.render.wonderwoman.WonderWomanLayer;
import com.danrod505.greenlantern.item.AmazonTiaraItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Wonder Woman: armor details, the lasso, sword and shield, the divine power HUD and the Invisible Jet. */
public final class WonderWomanClient implements HeroClient {
    private static final GuideScreen.Recipe AMAZON_TIARA = new GuideScreen.Recipe("amazon_tiara", () -> new ItemStack(ModItems.AMAZON_TIARA.get()), () -> GuideScreen.grid(
            Items.GOLD_INGOT, Items.RED_DYE, Items.GOLD_INGOT,
            Items.LEAD, Items.DIAMOND, Items.LEAD,
            null, null, null));

    @Override
    public void registerEvents() {
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(JetControls::onInteraction);
        WonderWomanRenderHandler.register();
    }

    @Override
    public void onClientSetup() {
        SidedHooks.wonderWomanSync = WonderWomanVisuals::onSync;
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LASSO.get(), LassoRenderer::new);
        event.registerEntityRenderer(ModEntities.AMAZON_SHIELD.get(), AmazonShieldRenderer::new);
        event.registerEntityRenderer(ModEntities.INVISIBLE_JET.get(), InvisibleJetRenderer::new);
    }

    @Override
    public void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.AMAZON_SPARK.get(), SuperParticle.AmazonProvider::new);
        event.registerSpriteSet(ModParticles.AMAZON_SHOCKWAVE.get(), ShockwaveParticle.AmazonProvider::new);
    }

    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new WonderWomanLayer(renderer));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("wonder_woman_hud"), WonderWomanHud::render);
    }

    @Override
    public void tick() {
        WonderWomanVisuals.tick();
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(
                new GuideScreen.Section("wonder_woman", () -> AmazonTiaraItem.charged(ModItems.AMAZON_TIARA.get().getDefaultInstance()), 0xFFE0303A, List.of(
                        GuideScreen.text("wonder_woman"), GuideScreen.text("wonder_woman_powers"), GuideScreen.text("controls_wonder_woman"), GuideScreen.recipes("wonder_woman", AMAZON_TIARA))));
    }
}
