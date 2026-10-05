package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.aquaman.AquamanServer;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.flash.SpeedsterServer;
import com.danrod505.greenlantern.hero.Hero;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: use a hero power ({@code power} = -1 for the selected one), for the speedster, Aquaman or Batman. */
public record UsePowerPacket(int power) {
    public static final StreamCodec<RegistryFriendlyByteBuf, UsePowerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, (UsePowerPacket p) -> p.power + 1,
            (Integer p) -> new UsePowerPacket(p - 1));

    public static void handle(UsePowerPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !player.isAlive()) return;
        if (Hero.context(player) == Hero.AQUAMAN) {
            AquamanServer.usePowerKey(player, packet.power);
            return;
        }
        if (Hero.context(player) == Hero.SUPERMAN) {
            com.danrod505.greenlantern.superman.SupermanServer.usePowerKey(player, packet.power);
            return;
        }
        if (Hero.context(player) == Hero.BATMAN) {
            com.danrod505.greenlantern.batman.BatmanServer.usePowerKey(player, packet.power);
            return;
        }
        ItemStack ring = FlashHelper.findRing(player);
        if (ring.isEmpty()) return;
        SpeedsterPower power = packet.power < 0 ? SpeedsterPower.selected(ring) : SpeedsterPower.byIndex(packet.power);
        SpeedsterServer.usePower(player, ring, power);
    }
}
