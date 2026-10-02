package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Network channel of the mod. Bump {@link #PROTOCOL} whenever a packet format changes. */
public final class ModNetwork {
    public static final int PROTOCOL = 2;

    public static final SimpleChannel CHANNEL = ChannelBuilder.named(GreenLantern.id("main"))
            .networkProtocolVersion(PROTOCOL)
            .simpleChannel();

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ToggleUniformPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .codec(ToggleUniformPacket.STREAM_CODEC)
                .consumerMainThread(ToggleUniformPacket::handle)
                .add();
        CHANNEL.messageBuilder(CycleConstructPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .codec(CycleConstructPacket.STREAM_CODEC)
                .consumerMainThread(CycleConstructPacket::handle)
                .add();
        CHANNEL.messageBuilder(FlightStatePacket.class, NetworkDirection.PLAY_TO_SERVER)
                .codec(FlightStatePacket.STREAM_CODEC)
                .consumerMainThread(FlightStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(FlightActionPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .codec(FlightActionPacket.STREAM_CODEC)
                .consumerMainThread(FlightActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(FlightSyncPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .codec(FlightSyncPacket.STREAM_CODEC)
                .consumerMainThread(FlightSyncPacket::handle)
                .add();
    }

    /** Sends a packet to every player tracking (seeing) the given entity, excluding the entity itself. */
    public static void sendToTracking(net.minecraft.world.entity.Entity entity, Object packet) {
        CHANNEL.send(packet, PacketDistributor.TRACKING_ENTITY.with(entity));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }
}
