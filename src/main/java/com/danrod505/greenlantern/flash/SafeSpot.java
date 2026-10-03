package com.danrod505.greenlantern.flash;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a speedster lands when molecular vibration ends: if the player stopped inside solid matter
 * (or below the world), this finds the closest spot where they can stand with ground under their
 * feet, preferring spots above. Nobody gets stuck in a wall or falls forever through the floor.
 */
public final class SafeSpot {
    /** How far (blocks) the search looks around the player before falling back to the surface. */
    public static final int SEARCH_RADIUS = 12;

    private SafeSpot() {}

    /** Whether a standing player at their current position would be inside a block or below the world. */
    public static boolean isStuck(Player player) {
        Level level = player.level();
        if (player.getY() < level.getMinY() + 1) return true;
        return !fits(level, player, player.position());
    }

    /**
     * The closest free standing position, or {@code null} when the player can stay where they are.
     * Never returns null for a player stuck in blocks or under the world.
     */
    public static @Nullable Vec3 find(Player player) {
        if (!isStuck(player)) return null;
        Level level = player.level();
        BlockPos origin = BlockPos.containing(player.getX(), Math.max(player.getY(), level.getMinY() + 1), player.getZ());
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            BlockPos best = null;
            double bestScore = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -r; dy <= r; dy++) {
                        // Only the shell of the cube of radius r (inner ones were searched already).
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != r) continue;
                        BlockPos feet = origin.offset(dx, dy, dz);
                        if (feet.getY() <= level.getMinY() || feet.getY() >= level.getMaxY() - 2) continue;
                        if (!canStand(level, player, feet)) continue;
                        // Going up is preferred over digging deeper.
                        double score = dx * dx + dz * dz + (dy < 0 ? 2.0 * dy * dy : 0.8 * dy * dy);
                        if (score < bestScore) {
                            bestScore = score;
                            best = feet;
                        }
                    }
                }
            }
            if (best != null) return Vec3.atBottomCenterOf(best);
        }
        // Nothing close: back to the surface of this column.
        int x = origin.getX();
        int z = origin.getZ();
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Vec3(x + 0.5, Math.max(top, level.getMinY() + 1), z + 0.5);
    }

    private static boolean canStand(Level level, Player player, BlockPos feet) {
        if (!fits(level, player, Vec3.atBottomCenterOf(feet))) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        return floor.isFaceSturdy(level, below, Direction.UP);
    }

    private static boolean fits(Level level, Player player, Vec3 feet) {
        EntityDimensions dims = player.getDimensions(Pose.STANDING);
        AABB box = dims.makeBoundingBox(feet).deflate(1.0E-3);
        return level.noCollision(player, box);
    }
}
