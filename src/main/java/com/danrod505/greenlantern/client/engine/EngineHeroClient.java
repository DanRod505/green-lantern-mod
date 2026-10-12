package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.WorldLayer;
import com.danrod505.greenlantern.engine.EngineHero;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/**
 * The client half of a character of the engine: its HUD and the outline of the block it aims at. The skin, the skeleton, the glow layer and
 * the wings are drawn for every character by {@link SkinRenderHandler}, {@link SkinGlowLayer} and
 * {@link WingsLayer}.
 */
public final class EngineHeroClient implements HeroClient {
    private final EngineHero hero;
    private final EngineHud hud;
    private final EngineOutline outline;

    public EngineHeroClient(EngineHero hero) {
        this.hero = hero;
        this.hud = new EngineHud(hero);
        this.outline = new EngineOutline(hero);
    }

    @Override
    public WorldLayer worldLayer() {
        return outline;
    }

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("engine_" + hero.id() + "_hud"), hud::render);
    }
}
