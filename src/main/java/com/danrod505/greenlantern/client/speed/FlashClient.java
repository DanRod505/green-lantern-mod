package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.client.FlashHud;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.particle.SonicRingParticle;
import com.danrod505.greenlantern.client.particle.SpeedParticle;
import com.danrod505.greenlantern.client.render.speed.SpeedLightningRenderer;
import com.danrod505.greenlantern.client.render.speed.TornadoRenderer;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of the Flash: running camera and visuals, the Speed Force HUD, tornado and lightning. */
public final class FlashClient implements HeroClient {
    private static final GuideScreen.Recipe FLASH_RING = new GuideScreen.Recipe("flash_ring", () -> new ItemStack(ModItems.FLASH_RING.get()), () -> GuideScreen.grid(
            Items.REDSTONE, Items.GOLD_INGOT, Items.REDSTONE,
            Items.GOLD_INGOT, Items.LIGHTNING_ROD, Items.GOLD_INGOT,
            Items.REDSTONE, Items.GOLD_INGOT, Items.REDSTONE));

    @Override
    public void registerEvents() {
        ComputeFovModifierEvent.BUS.addListener(SpeedCamera::onFov);
        ViewportEvent.ComputeCameraAngles.BUS.addListener(SpeedCamera::onAngles);
        SpeedRenderHandler.register();
    }

    @Override
    public void onClientSetup() {
        SidedHooks.speedSync = SpeedVisuals::onSync;
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPEED_TORNADO.get(), TornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.SPEED_LIGHTNING.get(), SpeedLightningRenderer::new);
    }

    @Override
    public void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPEED_SPARK.get(), SpeedParticle.SparkProvider::new);
        event.registerSpriteSet(ModParticles.SPEED_STREAK.get(), SpeedParticle.StreakProvider::new);
        event.registerSpriteSet(ModParticles.SPEED_RING.get(), SonicRingParticle.SpeedProvider::new);
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("speed_hud"), SpeedHud::render);
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("flash_hud"), FlashHud::render);
    }

    @Override
    public void tick() {
        SpeedVisuals.tick();
        SpeedAudio.tick();
    }

    @Override
    public void preTick(LocalPlayer player) {
        SpeedController.preTick(player);
    }

    @Override
    public void postTick(LocalPlayer player) {
        SpeedController.postTick(player);
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(
                new GuideScreen.Section("flash", () -> new ItemStack(ModItems.FLASH_RING.get()), 0xFFFF5A4A, List.of(
                        GuideScreen.text("flash"), GuideScreen.text("flash_powers"), GuideScreen.text("controls_flash"), GuideScreen.recipes("flash", FLASH_RING))));
    }
}
