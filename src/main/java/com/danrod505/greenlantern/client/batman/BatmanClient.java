package com.danrod505.greenlantern.client.batman;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.BatHud;
import com.danrod505.greenlantern.client.BatmobileControls;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.render.batman.BatDefenderRenderer;
import com.danrod505.greenlantern.client.render.batman.BatarangRenderer;
import com.danrod505.greenlantern.client.render.batman.BatmanLayer;
import com.danrod505.greenlantern.client.render.batman.BatmobileMissileRenderer;
import com.danrod505.greenlantern.client.render.batman.BatmobileRenderer;
import com.danrod505.greenlantern.client.render.batman.GrappleHookRenderer;
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
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of Batman: cape and cowl, glide and grapple, the belt HUD, gadgets and the Batmobile. */
public final class BatmanClient implements HeroClient {
    private static final GuideScreen.Recipe UTILITY_BELT = new GuideScreen.Recipe("utility_belt", () -> new ItemStack(ModItems.UTILITY_BELT.get()), () -> GuideScreen.grid(
            Items.LEATHER, Items.GOLD_INGOT, Items.LEATHER,
            Items.IRON_INGOT, Items.PHANTOM_MEMBRANE, Items.IRON_INGOT,
            Items.LEATHER, Items.GOLD_INGOT, Items.LEATHER));

    @Override
    public void registerEvents() {
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(BatmobileControls::onInteraction);
        BatmanRenderHandler.register();
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BATARANG.get(), BatarangRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAPPLE_HOOK.get(), GrappleHookRenderer::new);
        event.registerEntityRenderer(ModEntities.BAT_DEFENDER.get(), BatDefenderRenderer::new);
        event.registerEntityRenderer(ModEntities.BATMOBILE.get(), BatmobileRenderer::new);
        event.registerEntityRenderer(ModEntities.BATMOBILE_MISSILE.get(), BatmobileMissileRenderer::new);
    }

    @Override
    public void addPlayerLayers(AvatarRenderer<AbstractClientPlayer> renderer, EntityRenderersEvent.AddLayers event) {
        renderer.addLayer(new BatmanLayer(renderer));
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("bat_hud"), BatHud::render);
    }

    @Override
    public void tick() {
        BatmanVisuals.tick();
        BatmanAudio.tick();
    }

    @Override
    public void controlsTick(Minecraft mc) {
        BatmobileControls.tick(mc);
    }

    @Override
    public void preTick(LocalPlayer player) {
        GrappleController.preTick(player);
        GlideController.preTick(player);
    }

    @Override
    public void postTick(LocalPlayer player) {
        GrappleController.postTick(player);
        GlideController.postTick(player);
    }

    @Override
    public List<GuideScreen.Section> guideSections() {
        return List.of(
                new GuideScreen.Section("batman", () -> new ItemStack(ModItems.UTILITY_BELT.get()), 0xFFB8C0CC, List.of(
                        GuideScreen.text("batman"), GuideScreen.text("batman_powers"), GuideScreen.text("controls_batman"), GuideScreen.recipes("batman", UTILITY_BELT))));
    }
}
