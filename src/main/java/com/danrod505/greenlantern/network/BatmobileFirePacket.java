package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.entity.BatmobileEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: Batman, driving the Batmobile, pressed attack: a pair of missiles. */
public record BatmobileFirePacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, BatmobileFirePacket> STREAM_CODEC = StreamCodec.unit(new BatmobileFirePacket());

    public static void handle(BatmobileFirePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !(player.getVehicle() instanceof BatmobileEntity car) || !car.isOwnedBy(player)) return;
        car.fireMissiles();
    }
}
