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
    public static final KeyMapping NEXT_CONSTRUCT = new KeyMapping("key.greenlantern.next_construct",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY, 1);
    public static final KeyMapping PREVIOUS_CONSTRUCT = new KeyMapping("key.greenlantern.previous_construct",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY, 2);

    private KeyBindings() {}
}
