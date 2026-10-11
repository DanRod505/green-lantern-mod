package com.danrod505.greenlantern.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

/**
 * The client half of a hero: HUD, visuals and sounds, player layers, renderers of the hero's
 * entities and vehicle controls. {@link ClientSetup} calls these for every hero in
 * {@code HeroRegistry} order, after the shared ones (flight, construct wheel, Oa, Atlantis, the
 * Trench). Only ever loaded on the physical client (see {@code HeroDefinition#client}).
 */
public interface HeroClient {
    /** Forge bus listeners of the hero (cameras, render handlers, vehicle input). Called once at startup. */
    default void registerEvents() {
    }

    /** Client setup: hooks the common code calls on the client (sync packets). */
    default void onClientSetup() {
    }

    default void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }

    default void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    default void registerParticles(RegisterParticleProvidersEvent event) {
    }

    /** Layers drawn on every player model (suit details, glows). */
    default void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
    }

    /** HUD layers (drawn in hero order, after the flight HUD and before the respirator's). */
    default void addHud(AddGuiOverlayLayersEvent event) {
    }

    /** The hero's sections in the Heroes' Guide, after "Getting started" (see {@link GuideScreen}). */
    default List<GuideScreen.Section> guideSections() {
        return List.of();
    }

    /** Geometry the hero draws in the world every frame (beams, outlines through walls), or null for none. */
    default WorldLayer worldLayer() {
        return null;
    }

    /** Every client tick, after the shared flight visuals: the hero's visuals and sounds. */
    default void tick() {
    }

    /** Every client tick, from the key handling: controls of the hero's vehicles and mounts. */
    default void controlsTick(Minecraft mc) {
    }

    /** Before the local player's tick: movement controllers. */
    default void preTick(LocalPlayer player) {
    }

    /** After the local player's tick. */
    default void postTick(LocalPlayer player) {
    }
}
