package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.resources.Identifier;

/**
 * Colours of a hero's wheel textures (generated at runtime by {@code client.WheelTextures}): green
 * hard light for the Lantern, red and gold Speed Force for the Flash, teal and gold for Aquaman,
 * black, gray and yellow for Batman, and so on. {@code name} names the generated textures.
 */
public record WheelTheme(String name, int centerInner, int centerOuter, int centerRing, int border, int sliceInner, int sliceOuter,
                         int highlightInner, int highlightOuter, int highlightEdge, int highlightRim, int current) {
    public Identifier base() {
        return GreenLantern.id("dynamic/" + name + "_base");
    }

    public Identifier highlight() {
        return GreenLantern.id("dynamic/" + name + "_highlight");
    }

    public Identifier currentLayer() {
        return GreenLantern.id("dynamic/" + name + "_current");
    }
}
