package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.flight.ServerFlightTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: current power-flight speed (blocks/tick) and {@link com.danrod505.greenlantern.flight.FlightFlags}. */
public record FlightStatePacket(float speed, byte flags) {
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FlightStatePacket::speed,
            ByteBufCodecs.BYTE, FlightStatePacket::flags,
            FlightStatePacket::new);

    public static void handle(FlightStatePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player != null) ServerFlightTracker.onState(player, packet.speed, packet.flags);
    }
}
