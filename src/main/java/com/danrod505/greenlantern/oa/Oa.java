package com.danrod505.greenlantern.oa;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The planet Oa, home of the Guardians and the Corps' Central Power Battery. A flat dimension
 * (data/greenlantern/dimension/oa.json) whose city is built by {@link OaCity} the first time someone
 * arrives.
 */
public final class Oa {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, GreenLantern.id("oa"));

    /** First air block above the flat ground (the generator stacks 45 blocks). */
    public static final int GROUND_Y = 45;
    /** The Central Power Battery stands on this column. */
    public static final BlockPos CENTER = new BlockPos(0, GROUND_Y, 0);
    /** Where travellers arrive, on a dais at the edge of the plaza, facing the battery. */
    public static final Vec3 ARRIVAL = new Vec3(0.5, GROUND_Y + 1, 38.5);
    public static final float ARRIVAL_YAW = 180.0F;
    /** Rings recharge on their own this close (horizontally) to the Central Power Battery. */
    public static final double BATTERY_AURA_RADIUS = 18.0;

    private Oa() {}

    public static @Nullable ServerLevel level(MinecraftServer server) {
        return server.getLevel(LEVEL);
    }

    public static boolean is(Level level) {
        return level.dimension() == LEVEL;
    }

    public static boolean nearBattery(Vec3 pos) {
        double dx = pos.x - (CENTER.getX() + 0.5);
        double dz = pos.z - (CENTER.getZ() + 0.5);
        return dx * dx + dz * dz <= BATTERY_AURA_RADIUS * BATTERY_AURA_RADIUS && pos.y < GROUND_Y + 40;
    }
}
