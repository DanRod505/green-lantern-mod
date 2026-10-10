package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.entity.MechaEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the mecha pilot pulled a trigger (laser on/off, missile salvo). */
public record MechaWeaponPacket(int action) {
    public static final int LASER_OFF = 0;
    public static final int LASER_ON = 1;
    public static final int MISSILES = 2;

    public static final StreamCodec<RegistryFriendlyByteBuf, MechaWeaponPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MechaWeaponPacket::action,
            MechaWeaponPacket::new);

    public static void handle(MechaWeaponPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !(player.getVehicle() instanceof MechaEntity mecha) || !mecha.isOwnedBy(player)) return;
        switch (packet.action) {
            case LASER_ON -> mecha.setLaserFiring(true);
            case LASER_OFF -> mecha.setLaserFiring(false);
            case MISSILES -> mecha.fireMissiles();
            default -> {}
        }
    }
}
