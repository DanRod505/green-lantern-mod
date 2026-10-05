package com.danrod505.greenlantern.superman;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

/**
 * Reaches the entity flags accessor (protected in {@link Entity}) so X-ray vision can show a
 * creature glowing to Superman alone, without touching what everyone else sees. Never instantiated.
 */
abstract class GlowAccess extends Entity {
    /** Bit of the shared flags that makes an entity glow (its outline shows through walls). */
    static final int GLOWING_BIT = 1 << 6;

    private GlowAccess() {
        super(null, null);
    }

    static EntityDataAccessor<Byte> sharedFlags() {
        return DATA_SHARED_FLAGS_ID;
    }
}
