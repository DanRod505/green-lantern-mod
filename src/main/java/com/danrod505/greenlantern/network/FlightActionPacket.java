package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the local player performed a flight move. */
public record FlightActionPacket(int action) {
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlightActionPacket::action,
            FlightActionPacket::new);

    public FlightActionPacket(FlightAction action) {
        this(action.ordinal());
    }

    public static void handle(FlightActionPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player != null) ServerFlightTracker.onAction(player, FlightAction.byId(packet.action));
    }
}
