package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.network.CycleConstructPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.ToggleUniformPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;

/** Client tick / input handling: key bindings, scroll selection and the flight aura. */
public final class ClientEvents {
    private ClientEvents() {}

    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        CameraShake.tick();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        while (KeyBindings.TOGGLE_UNIFORM.consumeClick()) {
            if (!RingHelper.findRing(player).isEmpty()) ModNetwork.sendToServer(new ToggleUniformPacket());
        }
        tickWheelKey(mc, player);

        if (mc.isPaused()) return;
        // Green aura trail behind every flying Lantern in view.
        for (Player other : mc.level.players()) {
            if (!RingHelper.isSuited(other)) continue;
            boolean flying = other.getAbilities().flying || (!other.onGround() && other.getDeltaMovement().y < -0.1 && other != player);
            if (other == player && mc.options.getCameraType().isFirstPerson() && !flying) continue;
            int count = flying ? 2 : (other.tickCount % 6 == 0 ? 1 : 0);
            for (int i = 0; i < count; i++) {
                double x = other.getX() + (other.getRandom().nextDouble() - 0.5) * 0.5;
                double y = other.getY() + other.getRandom().nextDouble() * (flying ? 0.6 : 1.8);
                double z = other.getZ() + (other.getRandom().nextDouble() - 0.5) * 0.5;
                mc.level.addParticle(ModParticles.GLOW.get(), x, y, z, 0, flying ? -0.02 : 0.01, 0);
            }
        }
    }

    /** Ticks the wheel key has been held (0 when released). */
    private static int wheelKeyTicks;
    /** Hold this long (ticks) to open the wheel; a shorter tap switches to the next construct. */
    private static final int WHEEL_HOLD_TICKS = 4;

    private static void tickWheelKey(Minecraft mc, LocalPlayer player) {
        // Clicks are handled through isDown(); drop queued clicks so they don't pile up.
        while (KeyBindings.CONSTRUCT_WHEEL.consumeClick()) {
        }
        boolean hasRing = !RingHelper.findRing(player).isEmpty();
        if (mc.screen != null || !hasRing) {
            wheelKeyTicks = 0;
            return;
        }
        if (KeyBindings.CONSTRUCT_WHEEL.isDown()) {
            if (++wheelKeyTicks == WHEEL_HOLD_TICKS) {
                mc.setScreen(new ConstructWheelScreen());
            }
        } else {
            if (wheelKeyTicks > 0 && wheelKeyTicks < WHEEL_HOLD_TICKS) {
                ModNetwork.sendToServer(new CycleConstructPacket(1));
            }
            wheelKeyTicks = 0;
        }
    }

    /** Sneak + mouse wheel cycles constructs while holding the ring. */
    public static boolean onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !player.isShiftKeyDown() || RingHelper.heldRing(player).isEmpty()) {
            return false;
        }
        double delta = event.getDeltaY();
        if (delta == 0) return false;
        ModNetwork.sendToServer(new CycleConstructPacket(delta > 0 ? -1 : 1));
        return true;
    }
}
