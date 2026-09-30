package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the "suit up" key was pressed. */
public record ToggleUniformPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleUniformPacket> STREAM_CODEC = StreamCodec.unit(new ToggleUniformPacket());

    public static void handle(ToggleUniformPacket packet, CustomPayloadEvent.Context context) {
        var player = context.getSender();
        if (player != null && player.isAlive()) {
            Uniform.toggle(player);
        }
        context.setPacketHandled(true);
    }
}
