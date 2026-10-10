package com.danrod505.greenlantern.atlantis;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.AtlanteanEntity;
import com.danrod505.greenlantern.registry.ModEntities;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The people and the sea life of Atlantis. The Atlanteans move in the first time someone comes
 * close to the finished city; schools of fish, dolphins, turtles and squid, and the creatures found
 * only in Atlantis (giant manta rays, giant seahorses and Atlantean dolphins), are kept around the city
 * while players are near.
 */
public final class AtlantisLife {
    /** How many of each creature the city keeps around. */
    private record Kind(Supplier<? extends EntityType<? extends Mob>> type, int count) {}

    private static final List<Kind> SEA_LIFE = List.of(
            new Kind(() -> EntityType.TROPICAL_FISH, 36),
            new Kind(() -> EntityType.COD, 10),
            new Kind(() -> EntityType.SALMON, 6),
            new Kind(() -> EntityType.PUFFERFISH, 3),
            new Kind(() -> EntityType.GLOW_SQUID, 8),
            new Kind(() -> EntityType.SQUID, 4),
            new Kind(() -> EntityType.DOLPHIN, 3),
            new Kind(() -> EntityType.TURTLE, 4),
            // Found only in Atlantis.
            new Kind(ModEntities.MANTA_RAY, 3),
            new Kind(ModEntities.GIANT_SEAHORSE, 6),
            new Kind(ModEntities.ATLANTEAN_DOLPHIN, 6));

    public static final int CITIZENS = 16;

    private AtlantisLife() {}

    static void tick(ServerLevel level, Atlantis.Site site) {
        long time = level.getGameTime();
        if (time % 40 != 0) return;
        if (level.getNearestPlayer(site.x() + 0.5, site.floor() + 10, site.z() + 0.5, 112.0, false) == null) return;
        if (!level.areEntitiesLoaded(ChunkPos.asLong(site.x() >> 4, site.z() >> 4))) return;
        Atlantis.State state = Atlantis.state(level.getServer());
        if (!state.populated) {
            populate(level, site);
            state.populated = true;
            Atlantis.save(level.getServer());
        }
        if (time % 400 == 0) seaLife(level, site);
    }

    /** A free spot of water in the city to put a creature, or null. */
    private static BlockPos waterSpot(ServerLevel level, Atlantis.Site site, RandomSource random, double minRadius, double maxRadius, int minY, int maxY) {
        for (int tries = 0; tries < 12; tries++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double d = minRadius + random.nextDouble() * (maxRadius - minRadius);
            BlockPos pos = new BlockPos(site.x() + (int) Math.round(Math.cos(a) * d), site.floor() + minY + random.nextInt(maxY - minY + 1),
                    site.z() + (int) Math.round(Math.sin(a) * d));
            if (!level.areEntitiesLoaded(ChunkPos.asLong(pos))) continue;
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER)) return pos;
        }
        return null;
    }

    private static void populate(ServerLevel level, Atlantis.Site site) {
        RandomSource random = RandomSource.create(site.x() * 31L + site.z());
        BlockPos home = site.center();
        int citizens = 0;
        for (int i = 0; i < CITIZENS * 3 && citizens < CITIZENS; i++) {
            BlockPos pos = waterSpot(level, site, random, 20, 54, 0, 4);
            if (pos == null) continue;
            AtlanteanEntity.spawnCitizen(level, home, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, citizens);
            citizens++;
        }
        // The royal guard: four around the palace crown, four along the city wall.
        for (int i = 0; i < 4; i++) {
            AtlanteanEntity.spawnGuard(level, home, 20.0F, 25.0F + i * 1.5F, i % 2 == 0 ? 0.012F : -0.012F, i * Mth.HALF_PI, i);
            AtlanteanEntity.spawnGuard(level, home, Atlantis.RADIUS - 8, 9.0F + i, i % 2 == 0 ? -0.006F : 0.006F, i * Mth.HALF_PI + 0.7F, i + 2);
        }
        GreenLantern.LOGGER.info("The people of Atlantis came home ({} citizens, 8 guards)", citizens);
        seaLife(level, site);
    }

    /** Tops up the schools of fish and the other sea creatures around the city. */
    private static void seaLife(ServerLevel level, Atlantis.Site site) {
        RandomSource random = level.getRandom();
        AABB city = new AABB(site.x() - Atlantis.RADIUS - 8, site.floor() - 2, site.z() - Atlantis.RADIUS - 8,
                site.x() + Atlantis.RADIUS + 8, site.waterTop() + 1, site.z() + Atlantis.RADIUS + 8);
        for (Kind kind : SEA_LIFE) {
            EntityType<? extends Mob> type = kind.type().get();
            int present = level.getEntitiesOfClass(Mob.class, city, mob -> mob.getType() == type).size();
            for (int i = present; i < kind.count(); i++) {
                BlockPos pos = waterSpot(level, site, random, 6, Atlantis.RADIUS - 2, 1, 22);
                if (pos == null) continue;
                Mob mob = type.create(level, EntitySpawnReason.NATURAL);
                if (mob == null) continue;
                mob.snapTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        }
    }
}
