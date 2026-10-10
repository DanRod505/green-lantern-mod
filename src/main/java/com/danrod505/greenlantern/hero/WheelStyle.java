package com.danrod505.greenlantern.hero;

import net.minecraft.resources.Identifier;

/**
 * How a hero's power wheel looks: the generated wheel textures, the power icon sheet (16x16 icons
 * side by side, {@code iconsWidth} pixels wide), the lang keys for the cost line and the energy
 * line, and the text colours (RGB; {@code background} is ARGB).
 */
public record WheelStyle(WheelTheme theme, Identifier icons, int iconsWidth, String costKey, String energyKey,
                         int background, int nameColor, int costColor, int textColor, int dimIcon, int numberColor, int hintColor) {
    public static final int ICONS_HEIGHT = 16;
}
