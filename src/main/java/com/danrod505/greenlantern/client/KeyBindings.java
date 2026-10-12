package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Key bindings of the mod (configurable in Options > Controls > Key Binds > DC Universe). */
public final class KeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(GreenLantern.id("main"));

    public static final KeyMapping TOGGLE_UNIFORM = new KeyMapping("key.greenlantern.toggle_uniform",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY, 0);
    /** Hold to open the construct wheel, tap to switch to the next construct. */
    public static final KeyMapping CONSTRUCT_WHEEL = new KeyMapping("key.greenlantern.construct_wheel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY, 1);

    /** Uses the selected hero power (the Flash's tornado, phasing or lightning). */
    public static final KeyMapping HERO_POWER = new KeyMapping("key.greenlantern.hero_power",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY, 2);

    /** Engine characters: switches to the next unlocked skin. */
    public static final KeyMapping SKIN = new KeyMapping("key.greenlantern.skin",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY, 3);
    /** Optional direct keys for abilities 1 to 8 (unbound by default, so they don't fight the hotbar). */
    public static final KeyMapping[] ABILITIES = new KeyMapping[8];

    static {
        for (int i = 0; i < ABILITIES.length; i++) {
            ABILITIES[i] = new KeyMapping("key.greenlantern.ability_" + (i + 1), KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY, 4 + i);
        }
    }

    private KeyBindings() {}
}
