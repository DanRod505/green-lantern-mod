package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Screenshot script "wheel": The construct wheel: hold R, point at the saw, check the selection. */
public final class WheelScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("wheel", WheelScript::build, null, false);

    private WheelScript() {}

    /** Construct wheel: hold R, point at the saw, screenshot, release and check the selection. */
    private static void build() {
        step(60, () -> {
            command("time set 6000");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                LanternHero.INSTANCE.summonSuit(sp);
            });
            look(0, 5);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, () -> key(mc().options.keyUp, false));
        step(1, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> shot("w01_wheel_open"));
        // Point at slice 4 (the saw): angle 3 * 72 degrees clockwise from the top.
        step(2, () -> {
            var screen = mc().screen;
            double angle = Math.toRadians(-90 + 3 * 72);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            double scale = mc().getWindow().getGuiScale();
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc().getWindow().handle(), gx * scale, gy * scale);
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("w02_wheel_saw"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> {
            var selected = ConstructRegistry.selected(mc().player.getMainHandItem());
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT wheel selected={} screenClosed={}", selected.id(), mc().screen == null);
            shot("w03_after_select");
        });
        // Quick tap selects the next construct (hammer).
        step(5, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(2, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false));
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT tap selected={} screen={}",
                ConstructRegistry.selected(mc().player.getMainHandItem()).id(), mc().screen));
        step(10, () -> mc().stop());
    }
}
