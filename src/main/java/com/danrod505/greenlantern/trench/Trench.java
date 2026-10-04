package com.danrod505.greenlantern.trench;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.atlantis.Atlantis;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Trench: Aquaman's old enemies, a hostile people of the deep. They live in colonies (nests)
 * dug into the sea floor around Atlantis: a pit full of caves, under a ribcage of bone, where they
 * keep the villagers they capture in cocoons. Around each nest lies their territory, a darker sea
 * (its own biome) where fish don't live and clicks echo out of the caves.
 * <p>
 * The nests are placed (and saved, in {@code data/greenlantern_trench.dat}) once Atlantis has a
 * site, and built by {@link TrenchBuilder} after the city. {@link TrenchLife} keeps them alive.
 */
public final class Trench {
    /** Average radius of the pit (the edge is jagged). */
    public static final int PIT_RADIUS = 20;
    /** Radius of the territory around the pit: the dark sea, the dead coral, the creatures' home. */
    public static final int TERRITORY = 46;
    /** The pit floor lies this far below the floor of Atlantis. */
    public static final int BELOW_ATLANTIS = 6;
    /** How far from the center of Atlantis the nests are (roughly). */
    public static final int DISTANCE = 200;
    public static final int NESTS = 3;

    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, GreenLantern.id("the_trench"));

    private static final String FILE = "greenlantern_trench.dat";

    /**
     * A colony of the Trench: its center column, the pit floor (first water block), the estimated
     * natural sea floor around it and the last water block.
     */
    public record Nest(int index, int x, int z, int floor, int rim, int waterTop) {
        public BlockPos center() {
            return new BlockPos(x, floor, z);
        }

        public Vec3 centerVec() {
            return new Vec3(x + 0.5, floor + 4, z + 0.5);
        }

        public double distance2D(Vec3 pos) {
            double dx = pos.x - (x + 0.5);
            double dz = pos.z - (z + 0.5);
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** Inside the territory (and in the sea, not on the surface above it). */
        public boolean inTerritory(Vec3 pos) {
            return distance2D(pos) <= TERRITORY && pos.y > floor - 10 && pos.y < waterTop + 2;
        }
    }

    static final class State {
        boolean placed;
        final List<Nest> nests = new ArrayList<>();
        final Set<Long> builtChunks = new HashSet<>();
        boolean complete;
        final Set<Integer> populated = new HashSet<>();
        long nextRaid = -1;
        int rescued;
    }

    private static @Nullable MinecraftServer stateServer;
    private static @Nullable State state;

    private Trench() {}

    public static boolean enabled() {
        return GLConfig.TRENCH_ENABLED.get() && Atlantis.enabled();
    }

    static State state(MinecraftServer server) {
        if (state == null || stateServer != server) {
            stateServer = server;
            state = load(server);
        }
        return state;
    }

    static void forget() {
        state = null;
        stateServer = null;
    }

    static void saveIfLoaded(MinecraftServer server) {
        if (state != null && stateServer == server) save(server);
    }

    /** This world's nests (placed the first time, around Atlantis). Empty when the world has no Atlantis. */
    public static List<Nest> nests(MinecraftServer server) {
        State s = state(server);
        if (!s.placed) {
            Atlantis.Site site = Atlantis.site(server);
            if (site == null) return List.of();
            s.nests.addAll(place(server.overworld(), site));
            s.placed = true;
            save(server);
            for (Nest nest : s.nests) {
                GreenLantern.LOGGER.info("A nest of the Trench lies at {} {} (floor y={}, rim y={})", nest.x(), nest.z(), nest.floor(), nest.rim());
            }
        }
        return s.nests;
    }

    public static @Nullable Nest nest(MinecraftServer server, int index) {
        for (Nest nest : state(server).nests) {
            if (nest.index() == index) return nest;
        }
        return null;
    }

    /** The nest whose territory holds the position (overworld only), or null. */
    public static @Nullable Nest territoryAt(Level level, Vec3 pos) {
        if (level.dimension() != Level.OVERWORLD || !(level instanceof ServerLevel server)) return null;
        for (Nest nest : state(server.getServer()).nests) {
            if (nest.inTerritory(pos)) return nest;
        }
        return null;
    }

    /** The nearest nest (by horizontal distance), or null. */
    public static @Nullable Nest nearest(MinecraftServer server, Vec3 pos) {
        Nest best = null;
        double bestD = Double.MAX_VALUE;
        for (Nest nest : state(server).nests) {
            double d = nest.distance2D(pos);
            if (d < bestD) {
                bestD = d;
                best = nest;
            }
        }
        return best;
    }

    public static boolean isComplete(MinecraftServer server) {
        return state(server).complete;
    }

    /** Villagers freed from the nests in this world. */
    public static int rescued(MinecraftServer server) {
        return state(server).rescued;
    }

    static void countRescue(MinecraftServer server) {
        state(server).rescued++;
        save(server);
    }

    /** Tests only: pretend this world's nests are the given ones (already built and populated). */
    public static void setNestsForTesting(MinecraftServer server, List<Nest> nests) {
        State s = state(server);
        s.placed = true;
        s.nests.clear();
        s.nests.addAll(nests);
        s.complete = true;
        s.populated.clear();
        for (Nest nest : nests) s.populated.add(nest.index());
    }

    // ---- placing the nests ---------------------------------------------------------------------------

    /**
     * Three nests around Atlantis, a third of a turn apart, each in the open sea some 200 blocks
     * from the city (out of sight of its lights, but close enough to raid it).
     */
    private static List<Nest> place(ServerLevel level, Atlantis.Site site) {
        RandomSource random = RandomSource.create(level.getSeed() ^ 0x5452454E4348L);
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        int sampleY = level.getSeaLevel() - 20;
        int floor = Math.max(level.getMinY() + 6, site.floor() - BELOW_ATLANTIS);
        double start = random.nextDouble() * Mth.TWO_PI;
        List<Nest> nests = new ArrayList<>();
        for (int i = 0; i < NESTS; i++) {
            double base = start + i * Mth.TWO_PI / NESTS;
            Nest found = null;
            search:
            for (int distance = DISTANCE - 10; distance <= DISTANCE + 70; distance += 16) {
                for (int k = 0; k < 9; k++) {
                    // 0, +12, -12, +24, -24 ... degrees around the ideal direction.
                    double a = base + ((k + 1) / 2) * (k % 2 == 0 ? -1 : 1) * Math.toRadians(12);
                    int x = site.x() + (int) Math.round(Math.cos(a) * distance);
                    int z = site.z() + (int) Math.round(Math.sin(a) * distance);
                    if (!isOcean(source, sampler, x, sampleY, z)) continue;
                    int rim = estimatedFloor(level, x, z);
                    if (rim > site.waterTop() - 10) continue;
                    found = new Nest(i, x, z, floor, Mth.clamp(rim, floor + 12, site.waterTop() - 6), site.waterTop());
                    break search;
                }
            }
            if (found != null) nests.add(found);
        }
        return nests;
    }

    private static boolean isOcean(BiomeSource source, Climate.Sampler sampler, int x, int y, int z) {
        Holder<Biome> biome = source.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler);
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
    }

    /** The natural sea floor as the world generator will make it (the chunk need not exist yet). */
    static int estimatedFloor(ServerLevel level, int x, int z) {
        try {
            return level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState()) - 1;
        } catch (RuntimeException e) {
            return level.getSeaLevel() - 24;
        }
    }

    // ---- saving ------------------------------------------------------------------------------------------

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE);
    }

    private static State load(MinecraftServer server) {
        State s = new State();
        Path path = file(server);
        if (!Files.exists(path)) return s;
        try {
            CompoundTag tag = NbtIo.read(path);
            if (tag == null) return s;
            s.placed = tag.getBooleanOr("Placed", false);
            ListTag list = tag.getListOrEmpty("Nests");
            for (int i = 0; i < list.size(); i++) {
                CompoundTag n = list.getCompoundOrEmpty(i);
                s.nests.add(new Nest(n.getIntOr("Index", i), n.getIntOr("X", 0), n.getIntOr("Z", 0), n.getIntOr("Floor", 7),
                        n.getIntOr("Rim", 30), n.getIntOr("WaterTop", 62)));
                if (n.getBooleanOr("Populated", false)) s.populated.add(n.getIntOr("Index", i));
            }
            for (long key : tag.getLongArray("Built").orElse(new long[0])) {
                s.builtChunks.add(key);
            }
            s.complete = tag.getBooleanOr("Complete", false);
            s.nextRaid = tag.getLongOr("NextRaid", -1L);
            s.rescued = tag.getIntOr("Rescued", 0);
        } catch (IOException | RuntimeException e) {
            GreenLantern.LOGGER.error("Could not read {}", path, e);
        }
        return s;
    }

    static void save(MinecraftServer server) {
        State s = state(server);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Placed", s.placed);
        ListTag list = new ListTag();
        for (Nest nest : s.nests) {
            CompoundTag n = new CompoundTag();
            n.putInt("Index", nest.index());
            n.putInt("X", nest.x());
            n.putInt("Z", nest.z());
            n.putInt("Floor", nest.floor());
            n.putInt("Rim", nest.rim());
            n.putInt("WaterTop", nest.waterTop());
            n.putBoolean("Populated", s.populated.contains(nest.index()));
            list.add(n);
        }
        tag.put("Nests", list);
        tag.putLongArray("Built", s.builtChunks.stream().mapToLong(Long::longValue).toArray());
        tag.putBoolean("Complete", s.complete);
        tag.putLong("NextRaid", s.nextRaid);
        tag.putInt("Rescued", s.rescued);
        Path path = file(server);
        try {
            Files.createDirectories(path.getParent());
            NbtIo.write(tag, path);
        } catch (IOException e) {
            GreenLantern.LOGGER.error("Could not save {}", path, e);
        }
    }
}
