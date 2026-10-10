package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.ring.LanternHero;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> server: the "suit up" key was pressed. Takes off the suit being worn; otherwise puts on
 * the suit of the hero item in hand (or, failing that, of the first one carried; see
 * {@link HeroRegistry#context}). With no hero item at all the Lantern's uniform answers (and says the ring is missing).
 */
public record ToggleUniformPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleUniformPacket> STREAM_CODEC = StreamCodec.unit(new ToggleUniformPacket());

    public static void handle(ToggleUniformPacket packet, CustomPayloadEvent.Context context) {
        var player = context.getSender();
        if (player != null && player.isAlive()) {
            var suited = HeroRegistry.suited(player);
            if (suited.isPresent()) {
                suited.get().dismissSuit(player, true);
            } else {
                HeroRegistry.context(player).orElse(LanternHero.INSTANCE).summonSuit(player);
            }
        }
        context.setPacketHandled(true);
    }
}
