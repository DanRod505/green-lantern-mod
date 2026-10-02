package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: select a specific construct (from the construct wheel). */
public record SelectConstructPacket(Identifier construct) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectConstructPacket> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SelectConstructPacket::construct,
            SelectConstructPacket::new);

    public static void handle(SelectConstructPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null) return;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty() || player.isUsingItem()) return;
        Construct construct = ConstructRegistry.all().stream().filter(c -> c.id().equals(packet.construct)).findFirst().orElse(null);
        if (construct == null) return;
        ConstructRegistry.select(ring, construct);
        player.displayClientMessage(Component.translatable("message.greenlantern.selected", construct.name().copy().withStyle(ChatFormatting.GREEN)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CONSTRUCT_SELECT.get(), SoundSource.PLAYERS,
                0.8F, 1.0F + 0.05F * ConstructRegistry.all().indexOf(construct));
    }
}
