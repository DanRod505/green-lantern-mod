package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.entity.BatmobileEntity;
import com.danrod505.greenlantern.network.BatmobileFirePacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;

/**
 * Driving the Batmobile: the attack key (left click) fires a pair of missiles instead of swinging
 * the hand; holding it keeps firing as fast as the launchers reload. Sprint is the boost (read by
 * the car itself).
 */
public final class BatmobileControls {
    private static int holdTicks;

    private BatmobileControls() {}

    private static boolean driving(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && player.getVehicle() instanceof BatmobileEntity car && car.isOwnedBy(player);
    }

    public static void tick(Minecraft mc) {
        if (driving(mc) && mc.screen == null && mc.options.keyAttack.isDown()) {
            if (++holdTicks % 10 == 0) ModNetwork.sendToServer(new BatmobileFirePacket());
        } else {
            holdTicks = 0;
        }
    }

    /** Returns true to cancel the vanilla interaction. */
    public static boolean onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isAttack() || !driving(mc)) return false;
        ModNetwork.sendToServer(new BatmobileFirePacket());
        holdTicks = 0;
        event.setSwingHand(false);
        return true;
    }
}
