package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: pick a speedster power (from the power wheel) or step to the next / previous one. */
public record SelectPowerPacket(int value, boolean relative) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPowerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, (SelectPowerPacket p) -> p.value + 1,
            ByteBufCodecs.BOOL, SelectPowerPacket::relative,
            (Integer value, Boolean relative) -> new SelectPowerPacket(value - 1, relative));

    public static void handle(SelectPowerPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null) return;
        ItemStack ring = FlashHelper.findRing(player);
        if (ring.isEmpty()) return;
        SpeedsterPower power;
        if (packet.relative) {
            power = SpeedsterPower.cycle(ring, Mth.clamp(packet.value, -1, 1));
        } else {
            power = SpeedsterPower.byIndex(packet.value);
            SpeedsterPower.select(ring, power);
        }
        player.displayClientMessage(Component.translatable("message.greenlantern.power_selected", power.name().copy().withStyle(ChatFormatting.YELLOW)), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POWER_SELECT.get(), SoundSource.PLAYERS,
                0.7F, 1.0F + 0.08F * power.ordinal());
    }
}
