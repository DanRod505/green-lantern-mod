package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import com.danrod505.greenlantern.network.JetCloakPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;

/** Flying the Invisible Jet: the attack key (left click) switches the cloak on or off instead of swinging the hand. */
public final class JetControls {
    private JetControls() {}

    private static boolean flying(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && player.getVehicle() instanceof InvisibleJetEntity jet && jet.isOwnedBy(player);
    }

    /** Returns true to cancel the vanilla interaction. */
    public static boolean onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isAttack() || !flying(mc)) return false;
        ModNetwork.sendToServer(new JetCloakPacket());
        event.setSwingHand(false);
        return true;
    }
}
