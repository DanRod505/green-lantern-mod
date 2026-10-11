package com.danrod505.greenlantern.client.supergirl;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.HeroHud;
import com.danrod505.greenlantern.client.WorldLayer;
import com.danrod505.greenlantern.supergirl.SupergirlContent;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.supergirl.SupergirlPower;
import com.danrod505.greenlantern.supergirl.SupergirlServer;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Supergirl: the HUD, Krypto's renderer, Super Hearing's sound waves and arrows, and her section in the Heroes' Guide. */
public final class SupergirlClient implements HeroClient {
    /** Same grid as {@code data/greenlantern/recipe/argo_pendant.json}. */
    private static final GuideScreen.Recipe RECIPE = new GuideScreen.Recipe("argo_pendant", () -> new ItemStack(SupergirlContent.ITEM.get()), () -> GuideScreen.grid(
            Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT,
            Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT,
            null, null, null));

    private final SupergirlHearingVisuals hearing = new SupergirlHearingVisuals();
    private final HeroHud hud = new HeroHud(SupergirlHero.INSTANCE, 0x2A6FE0, 0xFFD447, 0xC0D8202E, 0xFFFFD447,
            (player, power) -> power == SupergirlPower.SUPER_HEARING && SupergirlServer.isHearing(SupergirlHero.INSTANCE.findItem(player)));

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SupergirlContent.KRYPTO.get(), KryptoRenderer::new);
    }

    @Override
    public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KryptoModel.LAYER, KryptoModel::createLayer);
    }

    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new SupergirlLayer(renderer));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("supergirl_hud"), hud::render);
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("supergirl_hearing"), hearing::renderArrows);
    }

    @Override
    public WorldLayer worldLayer() {
        return hearing;
    }

    @Override
    public void tick() {
        hearing.tick();
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(new GuideScreen.Section("supergirl", () -> SupergirlContent.ITEM.get().charged(SupergirlContent.ITEM.get().getDefaultInstance()), 0xFFD8202E,
                List.of(GuideScreen.text("supergirl"), GuideScreen.text("supergirl_powers"), GuideScreen.text("krypto"),
                        GuideScreen.text("controls_supergirl"), GuideScreen.recipes("supergirl", RECIPE))));
    }
}
