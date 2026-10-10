package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.flash.SpeedsterServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: current running speed (blocks/tick) and {@link com.danrod505.greenlantern.flash.SpeedFlags}. */
public record SpeedStatePacket(float speed, int flags) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeedStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SpeedStatePacket::speed,
            ByteBufCodecs.VAR_INT, SpeedStatePacket::flags,
            SpeedStatePacket::new);

    public static void handle(SpeedStatePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player != null) SpeedsterServer.onState(player, packet.speed, packet.flags);
    }
}
