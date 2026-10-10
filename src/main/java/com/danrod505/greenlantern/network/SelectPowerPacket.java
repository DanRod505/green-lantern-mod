package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: pick a hero power (from the power wheel) or step to the next / previous one. */
public record SelectPowerPacket(int value, boolean relative) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPowerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, (SelectPowerPacket p) -> p.value + 1,
            ByteBufCodecs.BOOL, SelectPowerPacket::relative,
            (Integer value, Boolean relative) -> new SelectPowerPacket(value - 1, relative));

    public static void handle(SelectPowerPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null) return;
        HeroDefinition hero = powerHero(player);
        ItemStack item = hero.findItem(player);
        if (item.isEmpty()) return;
        hero.powers().select(player, item, packet.value, packet.relative);
    }

    /**
     * The hero whose powers the power keys talk to: the one in context if it has a power wheel,
     * else the Flash (the first power hero, whose ring answers when the Lantern is in context).
     */
    static HeroDefinition powerHero(net.minecraft.world.entity.player.Player player) {
        return HeroRegistry.context(player).filter(HeroDefinition::hasPowers).orElse(FlashHero.INSTANCE);
    }
}
