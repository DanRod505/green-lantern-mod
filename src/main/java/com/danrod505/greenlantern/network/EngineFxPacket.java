package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.SidedHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server -> client: the camera shake of a preset of the engine (a strong punch, a stomp, an explosion). */
public record EngineFxPacket(float strength, int ticks) {
    public static final StreamCodec<RegistryFriendlyByteBuf, EngineFxPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, EngineFxPacket::strength,
            ByteBufCodecs.VAR_INT, EngineFxPacket::ticks,
            EngineFxPacket::new);

    public static void handle(EngineFxPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        SidedHooks.cameraShake.shake(packet.strength, packet.ticks);
    }
}
