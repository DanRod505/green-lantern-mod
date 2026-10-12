package com.danrod505.greenlantern.network;

import com.danrod505.greenlantern.SidedHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> client: the camera of a preset of the engine. {@link #SHAKE} shakes it (a strong punch, a
 * stomp, an explosion; {@code strength} 0 to 1), {@link #ZOOM} narrows or widens the view for a moment
 * ({@code strength} is the field of view, 0.6 is a close zoom, 1.3 wide).
 */
public record EngineFxPacket(int kind, float strength, int ticks) {
    public static final int SHAKE = 0;
    public static final int ZOOM = 1;

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineFxPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EngineFxPacket::kind,
            ByteBufCodecs.FLOAT, EngineFxPacket::strength,
            ByteBufCodecs.VAR_INT, EngineFxPacket::ticks,
            EngineFxPacket::new);

    public static void handle(EngineFxPacket packet, CustomPayloadEvent.Context context) {
        context.setPacketHandled(true);
        if (packet.kind == ZOOM) SidedHooks.cameraZoom.shake(packet.strength, packet.ticks);
        else SidedHooks.cameraShake.shake(packet.strength, packet.ticks);
    }
}
