package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.hero.HeroDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> server: use a hero power. {@code power} = -1 uses the selected one (the power key); 0 or
 * more is a power of the hero's own code by its index (the grapple letting go, the tornado stopping),
 * which on the wheel of the new model is one of {@link HeroDefinition#ownPowers}.
 */
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
        if (packet.power >= 0 && hero.abilityWheel()) {
            if (packet.power < hero.ownPowers().size()) hero.useOwnPower(player, item, hero.ownPowers().get(packet.power).id());
            return;
        }
        hero.powers().use(player, item, packet.power);
    }
}
