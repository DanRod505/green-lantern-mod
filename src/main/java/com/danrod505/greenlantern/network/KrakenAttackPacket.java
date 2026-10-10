package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.entity.KrakenEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: Aquaman, riding his Kraken, attacked (tentacle slam) or pressed / released the water jet. */
public record KrakenAttackPacket(int action) {
    public static final int JET_OFF = 0;
    public static final int JET_ON = 1;
    public static final int SLAM = 2;

    public static final StreamCodec<RegistryFriendlyByteBuf, KrakenAttackPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KrakenAttackPacket::action,
            KrakenAttackPacket::new);

    public static void handle(KrakenAttackPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !(player.getVehicle() instanceof KrakenEntity kraken) || !kraken.isOwnedBy(player)) return;
        switch (packet.action) {
            case JET_ON -> kraken.setJetFiring(true);
            case JET_OFF -> kraken.setJetFiring(false);
            case SLAM -> kraken.tentacleSlam();
            default -> {}
        }
    }
}
