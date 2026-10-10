package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.entity.InvisibleJetEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: Wonder Woman, flying the Invisible Jet, pressed attack: the cloak goes on (or off). */
public record JetCloakPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, JetCloakPacket> STREAM_CODEC = StreamCodec.unit(new JetCloakPacket());

    public static void handle(JetCloakPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !(player.getVehicle() instanceof InvisibleJetEntity jet) || !jet.isOwnedBy(player)) return;
        jet.toggleCloak();
    }
}
