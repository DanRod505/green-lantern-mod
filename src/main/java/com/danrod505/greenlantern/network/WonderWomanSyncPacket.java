package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.SidedHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> client: what a Wonder Woman is doing ({@link com.danrod505.greenlantern.wonderwoman.AmazonFlags})
 * and, now and then, a one-shot event ({@code event} = -1 when none), for her and everyone around.
 */
public record WonderWomanSyncPacket(int entityId, byte flags, int event) {
    public static final StreamCodec<RegistryFriendlyByteBuf, WonderWomanSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WonderWomanSyncPacket::entityId,
            ByteBufCodecs.BYTE, WonderWomanSyncPacket::flags,
            ByteBufCodecs.VAR_INT, p -> p.event + 1,
            (id, flags, event) -> new WonderWomanSyncPacket(id, flags, event - 1));

    public static void handle(WonderWomanSyncPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        SidedHooks.wonderWomanSync.accept(packet);
    }
}
