package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.SidedHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> client: the powers a Superman is using ({@link com.danrod505.greenlantern.superman.SuperFlags})
 * and, now and then, a one-shot event ({@code event} = -1 when none), for him and everyone around.
 */
public record SupermanSyncPacket(int entityId, byte flags, int event) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SupermanSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SupermanSyncPacket::entityId,
            ByteBufCodecs.BYTE, SupermanSyncPacket::flags,
            ByteBufCodecs.VAR_INT, p -> p.event + 1,
            (id, flags, event) -> new SupermanSyncPacket(id, flags, event - 1));

    public static void handle(SupermanSyncPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        SidedHooks.supermanSync.accept(packet);
    }
}
