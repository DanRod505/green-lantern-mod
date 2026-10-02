package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.GreenLantern;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Key bindings of the mod (configurable in Options > Controls > Key Binds > Green Lantern). */
public final class KeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(GreenLantern.id("main"));

    public static final KeyMapping TOGGLE_UNIFORM = new KeyMapping("key.greenlantern.toggle_uniform",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY, 0);
    /** Hold to open the construct wheel, tap to switch to the next construct. */
    public static final KeyMapping CONSTRUCT_WHEEL = new KeyMapping("key.greenlantern.construct_wheel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY, 1);

    private KeyBindings() {}
}
