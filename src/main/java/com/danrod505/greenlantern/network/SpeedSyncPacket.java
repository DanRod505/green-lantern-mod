package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.SidedHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> client: speedster state of a player ({@code action} = -1) or a move they performed
 * ({@code action} = {@link com.danrod505.greenlantern.flash.SpeedAction} ordinal). Also sent to the
 * speedster themselves when the server changes their state (phasing, tornado).
 */
public record SpeedSyncPacket(int entityId, float speed, int flags, int action) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeedSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpeedSyncPacket::entityId,
            ByteBufCodecs.FLOAT, SpeedSyncPacket::speed,
            ByteBufCodecs.VAR_INT, SpeedSyncPacket::flags,
            ByteBufCodecs.VAR_INT, p -> p.action + 1,
            (id, speed, flags, action) -> new SpeedSyncPacket(id, speed, flags, action - 1));

    public static void handle(SpeedSyncPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        SidedHooks.speedSync.accept(packet);
    }
}
