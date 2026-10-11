package com.danrod505.greenlantern.cyborg;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Makes a creature glow for one player alone (its outline shows through walls), without touching
 * what everyone else sees: Tech Scan marks enemies this way. Reaches the entity flags accessor,
 * protected in {@link Entity}. Never instantiated.
 */
abstract class CyborgGlow extends Entity {
    /** Bit of the shared flags that makes an entity glow. */
    private static final int GLOWING_BIT = 1 << 6;

    private CyborgGlow() {
        super(null, null);
    }

    /** Tells only this player that the entity glows (or, with {@code glow} false, what its real flags are). */
    static void send(ServerPlayer player, Entity entity, boolean glow) {
        if (player.connection == null) return;
        EntityDataAccessor<Byte> flags = DATA_SHARED_FLAGS_ID;
        byte real = entity.getEntityData().get(flags);
        byte value = glow ? (byte) (real | GLOWING_BIT) : real;
        player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), List.of(SynchedEntityData.DataValue.create(flags, value))));
    }
}
