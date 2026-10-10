package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.hero.HeroDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: use a hero power ({@code power} = -1 for the selected one). */
public record UsePowerPacket(int power) {
    public static final StreamCodec<RegistryFriendlyByteBuf, UsePowerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, (UsePowerPacket p) -> p.power + 1,
            (Integer p) -> new UsePowerPacket(p - 1));

    public static void handle(UsePowerPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !player.isAlive()) return;
        HeroDefinition hero = SelectPowerPacket.powerHero(player);
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        hero.powers().use(player, item, packet.power);
    }
}
