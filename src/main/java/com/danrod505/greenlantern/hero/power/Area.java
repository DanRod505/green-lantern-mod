package com.danrod505.greenlantern.hero.power;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Blocks a power reaches: a sphere around a point or a cone in front of it. */
public final class Area {
    private Area() {
    }

    /** Block positions whose centre is within {@code radius} of {@code center}. */
    public static List<BlockPos> sphere(Vec3 center, double radius) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos c = BlockPos.containing(center);
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            if (pos.distToCenterSqr(center) <= radius * radius) found.add(pos.immutable());
        }
        return found;
    }

    /**
     * Block positions whose centre is within {@code range} of {@code origin}, at least half a block
     * away from it and at most {@code halfAngle} degrees off {@code direction}.
     */
    public static List<BlockPos> cone(Vec3 origin, Vec3 direction, double range, double halfAngle) {
        Vec3 dir = direction.normalize();
        double cos = Math.cos(Math.toRadians(halfAngle));
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : sphere(origin, range)) {
            Vec3 to = Vec3.atCenterOf(pos).subtract(origin);
            if (to.length() >= 0.5 && to.normalize().dot(dir) >= cos) found.add(pos);
        }
        return found;
    }
}
