package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.entity.KrakenEntity;
import com.danrod505.greenlantern.network.KrakenAttackPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;

/**
 * Riding the Kraken: the attack key (left click) slams a tentacle down in front of it (holding it
 * keeps slamming), holding the use key (right click) fires the water jet. The normal item use /
 * attack is suppressed while riding.
 */
public final class KrakenControls {
    private static boolean jetOn;
    private static int holdTicks;

    private KrakenControls() {}

    private static boolean riding(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && player.getVehicle() instanceof KrakenEntity kraken && kraken.isOwnedBy(player);
    }

    public static void tick(Minecraft mc) {
        boolean riding = riding(mc) && mc.screen == null;
        boolean jet = riding && mc.options.keyUse.isDown();
        if (jet != jetOn) {
            jetOn = jet;
            ModNetwork.sendToServer(new KrakenAttackPacket(jet ? KrakenAttackPacket.JET_ON : KrakenAttackPacket.JET_OFF));
        }
        if (riding && mc.options.keyAttack.isDown()) {
            if (++holdTicks % 14 == 0) ModNetwork.sendToServer(new KrakenAttackPacket(KrakenAttackPacket.SLAM));
        } else {
            holdTicks = 0;
        }
    }

    /** Returns true to cancel the vanilla interaction. */
    public static boolean onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isPickBlock() || !riding(mc)) return false;
        if (event.isAttack()) {
            ModNetwork.sendToServer(new KrakenAttackPacket(KrakenAttackPacket.SLAM));
            holdTicks = 0;
        }
        event.setSwingHand(false);
        return true;
    }
}
