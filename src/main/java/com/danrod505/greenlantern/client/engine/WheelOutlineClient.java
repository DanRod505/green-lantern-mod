package com.danrod505.greenlantern.client.engine;

import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.WorldLayer;
import com.danrod505.greenlantern.hero.HeroDefinition;

/**
 * For a hero of before with the wheel of the new model (the first 9 heroes, stage 3): the outline of
 * the block the function V would use now works on, as the characters of the engine have it. Its own
 * client half keeps everything else.
 */
public final class WheelOutlineClient implements HeroClient {
    private final EngineOutline outline;

    public WheelOutlineClient(HeroDefinition hero) {
        this.outline = new EngineOutline(hero);
    }

    @Override
    public WorldLayer worldLayer() {
        return outline;
    }
}
