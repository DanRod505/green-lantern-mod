package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.SidedHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> client: flight state of another player ({@code action} = -1) or a flight move they
 * performed ({@code action} = {@link com.danrod505.greenlantern.flight.FlightAction} ordinal).
 */
public record FlightSyncPacket(int entityId, float speed, byte flags, int action) {
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlightSyncPacket::entityId,
            ByteBufCodecs.FLOAT, FlightSyncPacket::speed,
            ByteBufCodecs.BYTE, FlightSyncPacket::flags,
            ByteBufCodecs.VAR_INT, p -> p.action + 1,
            (id, speed, flags, action) -> new FlightSyncPacket(id, speed, flags, action - 1));

    public static void handle(FlightSyncPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        SidedHooks.flightSync.accept(packet);
    }
}
