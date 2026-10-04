package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.batman.BatmanServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: Batman spread (or folded) his cape to glide. */
public record GlideStatePacket(boolean gliding) {
    public static final StreamCodec<RegistryFriendlyByteBuf, GlideStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GlideStatePacket::gliding,
            GlideStatePacket::new);

    public static void handle(GlideStatePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !player.isAlive()) return;
        BatmanServer.setGliding(player, packet.gliding);
    }
}
