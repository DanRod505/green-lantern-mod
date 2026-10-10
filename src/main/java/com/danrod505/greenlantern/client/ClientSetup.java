package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.client.particle.LanternParticle;
import com.danrod505.greenlantern.client.particle.ShockwaveParticle;
import com.danrod505.greenlantern.client.render.BubbleConstructRenderer;
import com.danrod505.greenlantern.client.render.EnergyBoltRenderer;
import com.danrod505.greenlantern.client.render.GunConstructRenderer;
import com.danrod505.greenlantern.client.render.HammerConstructRenderer;
import com.danrod505.greenlantern.client.render.SawConstructRenderer;
import com.danrod505.greenlantern.client.render.DrillConstructRenderer;
import com.danrod505.greenlantern.client.render.MechaMissileRenderer;
import com.danrod505.greenlantern.client.render.MechaRenderer;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.client.flight.AuraLayer;
import com.danrod505.greenlantern.client.flight.FlightAudio;
import com.danrod505.greenlantern.client.flight.FlightCamera;
import com.danrod505.greenlantern.client.flight.FlightController;
import com.danrod505.greenlantern.client.flight.FlightHud;
import com.danrod505.greenlantern.client.flight.FlightRenderHandler;
import com.danrod505.greenlantern.client.flight.FlightVisuals;
import com.danrod505.greenlantern.client.flight.TrailRenderer;
import com.danrod505.greenlantern.client.particle.SonicRingParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only initialization. Only ever loaded on the physical client. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(BusGroup modBus) {
        FMLClientSetupEvent.getBus(modBus).addListener(ClientSetup::onClientSetup);
        RegisterKeyMappingsEvent.BUS.addListener(ClientSetup::onRegisterKeys);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ClientSetup::onRegisterRenderers);
        RegisterParticleProvidersEvent.BUS.addListener(ClientSetup::onRegisterParticles);
        AddGuiOverlayLayersEvent.BUS.addListener(ClientSetup::onAddGuiLayers);

        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(ClientSetup::onRegisterLayerDefinitions);
        EntityRenderersEvent.AddLayers.BUS.addListener(ClientSetup::onAddLayers);

        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientEvents::onClientTick);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientSetup::onFlightClientTick);
        TickEvent.PlayerTickEvent.Pre.BUS.addListener(ClientSetup::onPlayerTickPre);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(ClientSetup::onPlayerTickPost);
        ComputeFovModifierEvent.BUS.addListener(FlightCamera::onFov);
        ViewportEvent.ComputeCameraAngles.BUS.addListener(FlightCamera::onAngles);
        FlightRenderHandler.register();
        InputEvent.MouseScrollingEvent.BUS.addListener(ClientEvents::onMouseScroll);
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(MechaControls::onInteraction);
        ViewportEvent.ComputeCameraAngles.BUS.addListener(CameraShake::onCameraAngles);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        SidedHooks.jumpKeyDown = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && mc.player.input.keyPresses.jump();
        };
        SidedHooks.sprintKeyDown = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && mc.player.input.keyPresses.sprint();
        };
        SidedHooks.cameraShake = CameraShake::start;
        SidedHooks.openGuide = () -> Minecraft.getInstance().setScreen(new GuideScreen());
        SidedHooks.flightSync = FlightVisuals::onSync;
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.TOGGLE_UNIFORM);
        event.register(KeyBindings.CONSTRUCT_WHEEL);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENERGY_BOLT.get(), EnergyBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.GUN_CONSTRUCT.get(), GunConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.BUBBLE_CONSTRUCT.get(), BubbleConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.SAW_CONSTRUCT.get(), SawConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.HAMMER_CONSTRUCT.get(), HammerConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.DRILL_CONSTRUCT.get(), DrillConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHA.get(), MechaRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHA_MISSILE.get(), MechaMissileRenderer::new);
        event.registerEntityRenderer(ModEntities.FLIGHT_TRAIL.get(), TrailRenderer::new);
        event.registerEntityRenderer(ModEntities.OA_PORTAL.get(), com.danrod505.greenlantern.client.render.oa.OaPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.OA_GUARDIAN.get(), com.danrod505.greenlantern.client.render.oa.OaGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.LANTERN_CORPSMAN.get(), com.danrod505.greenlantern.client.render.oa.LanternCorpsmanRenderer::new);
    }

    private static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPARK.get(), LanternParticle.SparkProvider::new);
        event.registerSpriteSet(ModParticles.GLOW.get(), LanternParticle.GlowProvider::new);
        event.registerSpriteSet(ModParticles.SHOCKWAVE.get(), ShockwaveParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SONIC_RING.get(), SonicRingParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STREAK.get(), LanternParticle.StreakProvider::new);
    }

    private static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AuraLayer.LAYER, AuraLayer::createLayer);
        event.registerLayerDefinition(com.danrod505.greenlantern.client.render.oa.OaNpcModel.LAYER, com.danrod505.greenlantern.client.render.oa.OaNpcModel::createLayer);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType type : event.getModelTypes()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(type);
            if (renderer != null) {
                renderer.addLayer(new AuraLayer(renderer, new HumanoidModel<>(event.getEntityModels().bakeLayer(AuraLayer.LAYER))));
            }
        }
    }

    private static void onFlightClientTick(TickEvent.ClientTickEvent.Post event) {
        FlightVisuals.tick();
        TrailRenderer.tickHolder();
        FlightAudio.tick();
    }

    private static void onPlayerTickPre(TickEvent.PlayerTickEvent.Pre event) {
        if (event.player() instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
            FlightController.preTick(player);
        }
    }

    private static void onPlayerTickPost(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
            FlightController.postTick(player);
        }
    }

    private static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("flight_hud"), FlightHud::render);
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("ring_hud"), RingHud::render);
    }
}
