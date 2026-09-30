package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: select the next / previous construct on the ring. */
public record CycleConstructPacket(int offset) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleConstructPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CycleConstructPacket::offset,
            CycleConstructPacket::new);

    public static void handle(CycleConstructPacket packet, CustomPayloadEvent.Context context) {
        var player = context.getSender();
        context.setPacketHandled(true);
        if (player == null) return;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty() || player.isUsingItem()) return;
        Construct construct = ConstructRegistry.cycle(ring, Mth.clamp(packet.offset, -1, 1));
        player.displayClientMessage(Component.translatable("message.greenlantern.selected", construct.name().copy().withStyle(ChatFormatting.GREEN)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CONSTRUCT_SELECT.get(), SoundSource.PLAYERS, 0.7F, 1.0F + 0.05F * ConstructRegistry.all().indexOf(construct));
    }
}
