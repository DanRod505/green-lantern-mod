package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.hero.HeroRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> server: the keys of the engine for its characters (catalog, stage 14): crouch with the
 * power key uses the chosen ability in the world, the Supreme key and the skin key. The first 9
 * heroes never get this packet.
 */
public record EnginePacket(int action) {
    public static final int WORLD_USE = 0;
    public static final int SUPREME = 1;
    public static final int SKIN = 2;

    public static final StreamCodec<RegistryFriendlyByteBuf, EnginePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnginePacket::action, EnginePacket::new);

    public static void handle(EnginePacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        var player = context.getSender();
        if (player == null || !player.isAlive()) return;
        if (!(HeroRegistry.context(player).orElse(null) instanceof EngineHero hero)) return;
        switch (packet.action) {
            case WORLD_USE -> EngineServer.useInWorld(player, hero, hero.findItem(player));
            case SUPREME -> EngineServer.useSupreme(player, hero);
            case SKIN -> EngineServer.cycleSkin(player, hero, hero.findItem(player));
            default -> {
            }
        }
    }
}
