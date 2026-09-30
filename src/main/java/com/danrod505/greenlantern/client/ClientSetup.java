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
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import net.minecraft.client.Minecraft;
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

        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientEvents::onClientTick);
        InputEvent.MouseScrollingEvent.BUS.addListener(ClientEvents::onMouseScroll);
        ViewportEvent.ComputeCameraAngles.BUS.addListener(CameraShake::onCameraAngles);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        SidedHooks.jumpKeyDown = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && mc.player.input.keyPresses.jump();
        };
        SidedHooks.cameraShake = CameraShake::start;
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.TOGGLE_UNIFORM);
        event.register(KeyBindings.NEXT_CONSTRUCT);
        event.register(KeyBindings.PREVIOUS_CONSTRUCT);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENERGY_BOLT.get(), EnergyBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.GUN_CONSTRUCT.get(), GunConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.BUBBLE_CONSTRUCT.get(), BubbleConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.SAW_CONSTRUCT.get(), SawConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.HAMMER_CONSTRUCT.get(), HammerConstructRenderer::new);
    }

    private static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPARK.get(), LanternParticle.SparkProvider::new);
        event.registerSpriteSet(ModParticles.GLOW.get(), LanternParticle.GlowProvider::new);
        event.registerSpriteSet(ModParticles.SHOCKWAVE.get(), ShockwaveParticle.Provider::new);
    }

    private static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("ring_hud"), RingHud::render);
    }
}
