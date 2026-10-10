package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.flight.AuraLayer;
import com.danrod505.greenlantern.client.render.BubbleConstructRenderer;
import com.danrod505.greenlantern.client.render.DrillConstructRenderer;
import com.danrod505.greenlantern.client.render.EnergyBoltRenderer;
import com.danrod505.greenlantern.client.render.GunConstructRenderer;
import com.danrod505.greenlantern.client.render.HammerConstructRenderer;
import com.danrod505.greenlantern.client.render.MechaMissileRenderer;
import com.danrod505.greenlantern.client.render.MechaRenderer;
import com.danrod505.greenlantern.client.render.SawConstructRenderer;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of the Green Lantern: ring HUD, aura, constructs and the mecha. */
public final class LanternClient implements HeroClient {
    private static final GuideScreen.Recipe POWER_RING = new GuideScreen.Recipe("power_ring", () -> new ItemStack(ModItems.POWER_RING.get()), () -> GuideScreen.grid(
            null, Items.DIAMOND, null,
            Items.EMERALD, null, Items.EMERALD,
            null, Items.EMERALD, null));
    private static final GuideScreen.Recipe POWER_BATTERY = new GuideScreen.Recipe("power_battery", () -> new ItemStack(ModItems.POWER_BATTERY.get()), () -> GuideScreen.grid(
            Items.IRON_INGOT, Items.LIME_STAINED_GLASS, Items.IRON_INGOT,
            Items.LIME_STAINED_GLASS, Items.LANTERN, Items.LIME_STAINED_GLASS,
            Items.IRON_INGOT, Items.EMERALD_BLOCK, Items.IRON_INGOT));

    @Override
    public void registerEvents() {
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(MechaControls::onInteraction);
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENERGY_BOLT.get(), EnergyBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.GUN_CONSTRUCT.get(), GunConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.BUBBLE_CONSTRUCT.get(), BubbleConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.SAW_CONSTRUCT.get(), SawConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.HAMMER_CONSTRUCT.get(), HammerConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.DRILL_CONSTRUCT.get(), DrillConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHA.get(), MechaRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHA_MISSILE.get(), MechaMissileRenderer::new);
    }

    @Override
    public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AuraLayer.LAYER, AuraLayer::createLayer);
    }

    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new AuraLayer(renderer, new HumanoidModel<>(event.getEntityModels().bakeLayer(AuraLayer.LAYER))));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("ring_hud"), RingHud::render);
    }

    @Override
    public void controlsTick(Minecraft mc) {
        MechaControls.tick(mc);
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(
                new GuideScreen.Section("lantern", () -> PowerRingItem.charged(ModItems.POWER_RING.get().getDefaultInstance()), 0xFF4CE070, List.of(
                        GuideScreen.text("ring"), GuideScreen.text("battery"), GuideScreen.text("uniform"), GuideScreen.text("flight"), GuideScreen.text("constructs"), GuideScreen.text("drill"), GuideScreen.text("mecha"),
                        GuideScreen.text("oa"), GuideScreen.text("controls_lantern"), GuideScreen.recipes("lantern", POWER_RING, POWER_BATTERY))));
    }
}
