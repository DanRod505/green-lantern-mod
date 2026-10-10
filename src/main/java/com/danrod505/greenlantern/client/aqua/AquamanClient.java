package com.danrod505.greenlantern.client.aqua;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.AquaHud;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.KrakenControls;
import com.danrod505.greenlantern.client.SharkControls;
import com.danrod505.greenlantern.client.render.aqua.AquaTridentRenderer;
import com.danrod505.greenlantern.client.render.aqua.KrakenModel;
import com.danrod505.greenlantern.client.render.aqua.KrakenRenderer;
import com.danrod505.greenlantern.client.render.aqua.RespiratorLayer;
import com.danrod505.greenlantern.client.render.aqua.SharkModel;
import com.danrod505.greenlantern.client.render.aqua.SharkRenderer;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Aquaman: swimming, the Power of the Seas HUD, the trident, the shark and the Kraken. */
public final class AquamanClient implements HeroClient {
    private static final GuideScreen.Recipe AQUAMAN_EMBLEM = new GuideScreen.Recipe("aquaman_emblem", () -> new ItemStack(ModItems.AQUAMAN_EMBLEM.get()), () -> GuideScreen.grid(
            Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD,
            Items.GOLD_INGOT, Items.NAUTILUS_SHELL, Items.GOLD_INGOT,
            Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD));
    private static final GuideScreen.Recipe ATLANTIS_GATE = new GuideScreen.Recipe("atlantis_gate", () -> new ItemStack(ModItems.ATLANTIS_GATE.get()), () -> GuideScreen.grid(
            Items.GOLD_INGOT, Items.PRISMARINE_CRYSTALS, Items.GOLD_INGOT,
            Items.PRISMARINE_CRYSTALS, Items.HEART_OF_THE_SEA, Items.PRISMARINE_CRYSTALS,
            Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT));
    private static final GuideScreen.Recipe ATLANTEAN_RESPIRATOR = new GuideScreen.Recipe("atlantean_respirator", () -> new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()), () -> GuideScreen.grid(
            Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD,
            Items.KELP, Items.GLASS_BOTTLE, Items.KELP,
            null, Items.PRISMARINE_SHARD, null));

    @Override
    public void registerEvents() {
        ComputeFovModifierEvent.BUS.addListener(SwimCamera::onFov);
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(SharkControls::onInteraction);
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(KrakenControls::onInteraction);
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AQUA_TRIDENT.get(), AquaTridentRenderer::new);
        event.registerEntityRenderer(ModEntities.GREAT_WHITE_SHARK.get(), SharkRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAKEN.get(), KrakenRenderer::new);
    }

    @Override
    public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SharkModel.LAYER, SharkModel::createLayer);
        event.registerLayerDefinition(KrakenModel.LAYER, KrakenModel::createLayer);
    }

    /** The Atlantean respirator (worn by anyone who carries one under water). */
    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new RespiratorLayer(renderer));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("aqua_hud"), AquaHud::render);
    }

    @Override
    public void tick() {
        SwimVisuals.tick();
        SwimAudio.tick();
    }

    @Override
    public void controlsTick(Minecraft mc) {
        SharkControls.tick(mc);
        KrakenControls.tick(mc);
    }

    @Override
    public void preTick(LocalPlayer player) {
        SwimController.preTick(player);
    }

    @Override
    public void postTick(LocalPlayer player) {
        SwimController.postTick(player);
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(
                new GuideScreen.Section("aquaman", () -> new ItemStack(ModItems.AQUAMAN_EMBLEM.get()), 0xFFFFA040, List.of(
                        GuideScreen.text("aquaman"), GuideScreen.text("aquaman_swim"), GuideScreen.text("aquaman_trident"), GuideScreen.text("aquaman_shark"), GuideScreen.text("aquaman_sea_call"),
                        GuideScreen.text("kraken"), GuideScreen.text("controls_aquaman"), GuideScreen.recipes("aquaman", AQUAMAN_EMBLEM))),
                new GuideScreen.Section("atlantis", () -> new ItemStack(ModItems.ATLANTIS_GATE.get()), 0xFF4FE0E8, List.of(
                        GuideScreen.text("atlantis"), GuideScreen.text("atlantis_people"), GuideScreen.text("atlantis_travel"), GuideScreen.text("atlantis_respirator"),
                        GuideScreen.text("atlantis_creatures"), GuideScreen.text("atlantis_manta"), GuideScreen.text("atlantis_seahorse"), GuideScreen.text("atlantis_dolphin"),
                        GuideScreen.recipes("atlantis", ATLANTIS_GATE, ATLANTEAN_RESPIRATOR))),
                new GuideScreen.Section("trench", () -> new ItemStack(ModItems.TRENCH_CREATURE_EGG.get()), 0xFFD0453A, List.of(
                        GuideScreen.text("trench"), GuideScreen.text("trench_territory"), GuideScreen.text("trench_nest"), GuideScreen.text("trench_creatures"), GuideScreen.text("trench_captives"),
                        GuideScreen.text("trench_raids"))));
    }
}
