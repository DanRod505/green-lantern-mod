package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: Aquaman, riding his shark, pressed attack: the shark bites. */
public record SharkBitePacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, SharkBitePacket> STREAM_CODEC = StreamCodec.unit(new SharkBitePacket());

    public static void handle(SharkBitePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !(player.getVehicle() instanceof GreatWhiteSharkEntity shark) || !shark.isOwnedBy(player)) return;
        shark.bite();
    }
}
