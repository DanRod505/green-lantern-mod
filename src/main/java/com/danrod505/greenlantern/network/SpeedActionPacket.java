package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.flash.SpeedAction;
import com.danrod505.greenlantern.flash.SpeedsterServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the local speedster performed a move (super jump, skid, sonic boom...). */
public record SpeedActionPacket(int action) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeedActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpeedActionPacket::action,
            SpeedActionPacket::new);

    public SpeedActionPacket(SpeedAction action) {
        this(action.ordinal());
    }

    public static void handle(SpeedActionPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player != null) SpeedsterServer.onAction(player, SpeedAction.byId(packet.action));
    }
}
