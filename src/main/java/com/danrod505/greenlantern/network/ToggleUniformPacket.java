package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> server: the "suit up" key was pressed. Takes off the suit being worn; otherwise puts on
 * the suit of the ring in hand (or, failing that, of the first ring found: Lantern, then Flash).
 */
public record ToggleUniformPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleUniformPacket> STREAM_CODEC = StreamCodec.unit(new ToggleUniformPacket());

    public static void handle(ToggleUniformPacket packet, CustomPayloadEvent.Context context) {
        var player = context.getSender();
        if (player != null && player.isAlive()) {
            if (com.danrod505.greenlantern.flash.FlashHelper.isSuited(player)) {
                com.danrod505.greenlantern.flash.FlashSuit.dismiss(player, true);
            } else if (com.danrod505.greenlantern.ring.RingHelper.isSuited(player)) {
                Uniform.dismiss(player, true);
            } else if (!com.danrod505.greenlantern.flash.FlashHelper.heldRing(player).isEmpty()
                    || com.danrod505.greenlantern.ring.RingHelper.findRing(player).isEmpty()) {
                com.danrod505.greenlantern.flash.FlashSuit.summon(player);
            } else {
                Uniform.summon(player);
            }
        }
        context.setPacketHandled(true);
    }
}
