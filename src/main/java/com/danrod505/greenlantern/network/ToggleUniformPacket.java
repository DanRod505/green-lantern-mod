package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.aquaman.AquamanSuit;
import com.danrod505.greenlantern.flash.FlashSuit;
import com.danrod505.greenlantern.hero.Hero;
import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> server: the "suit up" key was pressed. Takes off the suit being worn; otherwise puts on
 * the suit of the hero item in hand (or, failing that, of the first one carried; see {@link Hero#context}).
 */
public record ToggleUniformPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleUniformPacket> STREAM_CODEC = StreamCodec.unit(new ToggleUniformPacket());

    public static void handle(ToggleUniformPacket packet, CustomPayloadEvent.Context context) {
        var player = context.getSender();
        if (player != null && player.isAlive()) {
            switch (Hero.suited(player)) {
                case LANTERN -> Uniform.dismiss(player, true);
                case FLASH -> FlashSuit.dismiss(player, true);
                case AQUAMAN -> AquamanSuit.dismiss(player, true);
                case BATMAN -> com.danrod505.greenlantern.batman.BatmanSuit.dismiss(player, true);
                case NONE -> {
                    switch (Hero.context(player)) {
                        case FLASH -> FlashSuit.summon(player);
                        case AQUAMAN -> AquamanSuit.summon(player);
                        case BATMAN -> com.danrod505.greenlantern.batman.BatmanSuit.summon(player);
                        default -> Uniform.summon(player);
                    }
                }
            }
        }
        context.setPacketHandled(true);
    }
}
