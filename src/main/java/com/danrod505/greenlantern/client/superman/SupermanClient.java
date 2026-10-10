package com.danrod505.greenlantern.client.superman;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.SupermanHud;
import com.danrod505.greenlantern.client.particle.ShockwaveParticle;
import com.danrod505.greenlantern.client.particle.SonicRingParticle;
import com.danrod505.greenlantern.client.particle.SuperParticle;
import com.danrod505.greenlantern.item.KryptonianCrystalItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Superman: cape and glowing eyes, heat vision and X-ray, super breath and the solar HUD. */
public final class SupermanClient implements HeroClient {
    private static final GuideScreen.Recipe KRYPTONIAN_CRYSTAL = new GuideScreen.Recipe("kryptonian_crystal", () -> new ItemStack(ModItems.KRYPTONIAN_CRYSTAL.get()), () -> GuideScreen.grid(
            Items.AMETHYST_SHARD, Items.DIAMOND, Items.AMETHYST_SHARD,
            Items.GOLD_INGOT, Items.SUNFLOWER, Items.GOLD_INGOT,
            Items.AMETHYST_SHARD, Items.DIAMOND, Items.AMETHYST_SHARD));

    @Override
    public void onClientSetup() {
        SidedHooks.supermanSync = SupermanVisuals::onSync;
    }

    @Override
    public void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.HEAT_SPARK.get(), SuperParticle.HeatProvider::new);
        event.registerSpriteSet(ModParticles.SOLAR_GLOW.get(), SuperParticle.SolarProvider::new);
        event.registerSpriteSet(ModParticles.FROST_BREATH.get(), SuperParticle.FrostProvider::new);
        event.registerSpriteSet(ModParticles.SUPER_RING.get(), SonicRingParticle.SupermanProvider::new);
        event.registerSpriteSet(ModParticles.SUPER_SHOCKWAVE.get(), ShockwaveParticle.SupermanProvider::new);
    }

    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new SupermanLayer(renderer));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("superman_hud"), SupermanHud::render);
    }

    @Override
    public void tick() {
        SupermanVisuals.tick();
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(
                new GuideScreen.Section("superman", () -> KryptonianCrystalItem.charged(ModItems.KRYPTONIAN_CRYSTAL.get().getDefaultInstance()), 0xFF4A86FF, List.of(
                        GuideScreen.text("superman"), GuideScreen.text("superman_powers"), GuideScreen.text("controls_superman"), GuideScreen.recipes("superman", KRYPTONIAN_CRYSTAL))));
    }
}
