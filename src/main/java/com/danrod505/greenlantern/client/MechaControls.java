package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.network.MechaWeaponPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.InputEvent;

/**
 * Mecha weapons: while piloting, holding the use key (right click) fires the lasers and the
 * attack key (left click) launches a missile salvo. The normal item use / attack is suppressed.
 */
public final class MechaControls {
    private static boolean laserOn;

    private MechaControls() {}

    private static boolean piloting(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && player.getVehicle() instanceof MechaEntity mecha && mecha.isOwnedBy(player);
    }

    public static void tick(Minecraft mc) {
        boolean laser = piloting(mc) && mc.screen == null && mc.options.keyUse.isDown();
        if (laser != laserOn) {
            laserOn = laser;
            ModNetwork.sendToServer(new MechaWeaponPacket(laser ? MechaWeaponPacket.LASER_ON : MechaWeaponPacket.LASER_OFF));
        }
    }

    /** Returns true to cancel the vanilla interaction. */
    public static boolean onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isPickBlock() || !piloting(mc)) return false;
        if (event.isAttack()) {
            ModNetwork.sendToServer(new MechaWeaponPacket(MechaWeaponPacket.MISSILES));
        }
        event.setSwingHand(false);
        return true;
    }
}
