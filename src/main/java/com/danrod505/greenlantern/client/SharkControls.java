package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SharkBitePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;

/**
 * Riding the great white shark: the attack key (left click) makes the shark lunge and bite instead
 * of swinging the hand; holding it keeps lunging. Use (right click) is left alone, so the trident can
 * still be thrown from the shark's back.
 */
public final class SharkControls {
    private static int holdTicks;

    private SharkControls() {}

    private static boolean riding(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && player.getVehicle() instanceof GreatWhiteSharkEntity shark && shark.isOwnedBy(player);
    }

    public static void tick(Minecraft mc) {
        if (riding(mc) && mc.screen == null && mc.options.keyAttack.isDown()) {
            if (++holdTicks % (GreatWhiteSharkEntity.LUNGE_TICKS + 2) == 0) ModNetwork.sendToServer(new SharkBitePacket());
        } else {
            holdTicks = 0;
        }
    }

    /** Returns true to cancel the vanilla interaction. */
    public static boolean onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isAttack() || !riding(mc)) return false;
        ModNetwork.sendToServer(new SharkBitePacket());
        holdTicks = 0;
        event.setSwingHand(false);
        return true;
    }
}
