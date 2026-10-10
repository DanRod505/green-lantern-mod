package com.danrod505.greenlantern.atlantis;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Atlantis, the sunken kingdom. Unlike Oa it is not another dimension: it lies at the bottom of a
 * deep ocean of the overworld, in a crater dug below the sea floor, and is built by
 * {@link AtlantisBuilder} the first time the world runs with the mod.
 * <p>
 * Where it is depends on the world seed: a deep ocean some way from the world spawn, far from any
 * ocean monument. The spot is saved in {@code data/greenlantern_atlantis.dat} in the world folder,
 * together with the building progress.
 */
public final class Atlantis {
    /** Radius of the flat city floor. */
    public static final int RADIUS = 60;
    /** Width of the slope that joins the city floor to the natural sea floor. */
    public static final int BLEND = 20;
    public static final int OUTER = RADIUS + BLEND;
    /** How far below the sea surface the city floor lies. */
    public static final int DEPTH = 50;
    /** The arrival pavilion (a glass dome full of air) stands this far south of the palace. */
    public static final int PAVILION_Z = 24;
    public static final int PAVILION_RADIUS = 6;

    private static final String FILE = "greenlantern_atlantis.dat";

    /** Where the city is: its center column, the city floor (first water block) and the last water block. */
    public record Site(int x, int z, int floor, int waterTop) {
        public BlockPos center() {
            return new BlockPos(x, floor, z);
        }

        /** Arrival spot: inside the pavilion, facing the palace (north). */
        public Vec3 arrival() {
            return new Vec3(x + 0.5, floor, z + PAVILION_Z + 2.5);
        }

        public float arrivalYaw() {
            return 180.0F;
        }

        public boolean contains(Vec3 pos) {
            double dx = pos.x - (x + 0.5);
            double dz = pos.z - (z + 0.5);
            return dx * dx + dz * dz <= (RADIUS + 4) * (RADIUS + 4) && pos.y > floor - 8 && pos.y < waterTop + 4;
        }

        public boolean inPavilion(Vec3 pos) {
            double dx = pos.x - (x + 0.5);
            double dz = pos.z - (z + PAVILION_Z + 0.5);
            return dx * dx + dz * dz <= PAVILION_RADIUS * PAVILION_RADIUS && pos.y >= floor - 1 && pos.y < floor + PAVILION_RADIUS;
        }
    }

    /** Everything known about this world's Atlantis (one per server). */
    static final class State {
        boolean searched;
        @Nullable Site site;
        final java.util.Set<Long> builtChunks = new java.util.HashSet<>();
        boolean complete;
        boolean populated;
    }

    private static @Nullable MinecraftServer stateServer;
    private static @Nullable State state;

    private Atlantis() {}

    public static boolean enabled() {
        return GLConfig.ATLANTIS_ENABLED.get();
    }

    static State state(MinecraftServer server) {
        if (state == null || stateServer != server) {
            stateServer = server;
            state = load(server);
        }
        return state;
    }

    /** Tests only: pretend this world's Atlantis stands at the given site (null: no Atlantis). */
    public static void setSiteForTesting(MinecraftServer server, @Nullable Site site, boolean complete) {
        State s = state(server);
        s.searched = true;
        s.site = site;
        s.complete = complete;
        s.populated = true;
    }

    static void saveIfLoaded(MinecraftServer server) {
        if (state != null && stateServer == server) save(server);
    }

    static void forget() {
        state = null;
        stateServer = null;
    }

    /** This world's Atlantis, looking for a place for it the first time. Null if the world has no deep ocean. */
    public static @Nullable Site site(MinecraftServer server) {
        State s = state(server);
        if (!s.searched) {
            s.site = locate(server.overworld());
            s.searched = true;
            save(server);
            if (s.site != null) {
                GreenLantern.LOGGER.info("Atlantis lies at {} {} (floor y={})", s.site.x(), s.site.z(), s.site.floor());
            } else {
                GreenLantern.LOGGER.info("No deep ocean found for Atlantis in this world");
            }
        }
        return s.site;
    }

    /** The Site whose city contains the position, when the position is in the overworld. */
    public static @Nullable Site containing(Level level, Vec3 pos) {
        if (level.dimension() != Level.OVERWORLD || !(level instanceof ServerLevel server)) return null;
        State s = state(server.getServer());
        if (s.site == null || !s.site.contains(pos)) return null;
        return s.site;
    }

    public static boolean isComplete(MinecraftServer server) {
        return state(server).complete;
    }

    // ---- choosing the spot ---------------------------------------------------------------------------

    private static final int SEARCH_RADIUS = 3600;
    private static final int SEARCH_STEP = 48;

    /**
     * Searches outwards from a seed-chosen point some way from spawn for a deep ocean big enough
     * for the whole city (and its slopes), away from ocean monuments.
     */
    private static @Nullable Site locate(ServerLevel level) {
        RandomSource random = RandomSource.create(level.getSeed() ^ 0x41544C414E544953L);
        double angle = random.nextDouble() * Mth.TWO_PI;
        int distance = 600 + random.nextInt(600);
        BlockPos spawn = level.getServer().getRespawnData().pos();
        int ox = spawn.getX() + (int) (Math.cos(angle) * distance);
        int oz = spawn.getZ() + (int) (Math.sin(angle) * distance);

        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        int sampleY = level.getSeaLevel() - 20;
        int bestScore = -1;
        int bestX = 0;
        int bestZ = 0;
        int rejected = 0;
        for (int r = 0; r <= SEARCH_RADIUS; r += SEARCH_STEP) {
            int steps = Math.max(1, r / SEARCH_STEP);
            for (int i = 0; i < steps * 8 || (r == 0 && i == 0); i++) {
                int x;
                int z;
                if (r == 0) {
                    x = ox;
                    z = oz;
                } else {
                    // Walk the square ring of "radius" r.
                    int side = i / (steps * 2);
                    int along = i % (steps * 2) * SEARCH_STEP - r;
                    switch (side) {
                        case 0 -> { x = ox + along; z = oz - r; }
                        case 1 -> { x = ox + r; z = oz + along; }
                        case 2 -> { x = ox - along; z = oz + r; }
                        default -> { x = ox - r; z = oz - along; }
                    }
                }
                if (!isDeep(source, sampler, x, sampleY, z)) continue;
                int score = score(source, sampler, x, sampleY, z);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                }
                if (score >= 20) {
                    if (nearMonument(level, x, z)) {
                        if (++rejected > 6) break;
                        continue;
                    }
                    return site(level, x, z);
                }
            }
        }
        if (bestScore >= 8 && !nearMonument(level, bestX, bestZ)) return site(level, bestX, bestZ);
        return null;
    }

    private static Site site(ServerLevel level, int x, int z) {
        int waterTop = level.getSeaLevel() - 1;
        int floor = Math.max(level.getMinY() + 8, waterTop + 1 - DEPTH);
        return new Site(x, z, floor, waterTop);
    }

    private static boolean isDeep(BiomeSource source, Climate.Sampler sampler, int x, int y, int z) {
        Holder<Biome> biome = source.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler);
        return biome.is(BiomeTags.IS_DEEP_OCEAN);
    }

    private static boolean isOcean(BiomeSource source, Climate.Sampler sampler, int x, int y, int z) {
        Holder<Biome> biome = source.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler);
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
    }

    /**
     * How well the city fits here: 8 deep ocean samples around the city edge and 12 ocean samples
     * around the slopes (20 = perfect).
     */
    private static int score(BiomeSource source, Climate.Sampler sampler, int x, int y, int z) {
        int score = 0;
        for (int i = 0; i < 8; i++) {
            double a = i * Mth.TWO_PI / 8;
            if (isDeep(source, sampler, x + (int) (Math.cos(a) * RADIUS), y, z + (int) (Math.sin(a) * RADIUS))) score++;
        }
        for (int i = 0; i < 12; i++) {
            double a = (i + 0.5) * Mth.TWO_PI / 12;
            if (isOcean(source, sampler, x + (int) (Math.cos(a) * (OUTER + 16)), y, z + (int) (Math.sin(a) * (OUTER + 16)))) score++;
        }
        return score;
    }

    /** Elder guardians and their monument should not share the city. */
    private static boolean nearMonument(ServerLevel level, int x, int z) {
        try {
            BlockPos monument = level.findNearestMapStructure(StructureTags.ON_OCEAN_EXPLORER_MAPS, new BlockPos(x, level.getSeaLevel(), z), 8, false);
            if (monument == null) return false;
            double dx = monument.getX() - x;
            double dz = monument.getZ() - z;
            return dx * dx + dz * dz < (OUTER + 60) * (OUTER + 60);
        } catch (RuntimeException e) {
            GreenLantern.LOGGER.warn("Could not look for ocean monuments near Atlantis", e);
            return false;
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
            s.searched = tag.getBooleanOr("Searched", false);
            if (tag.getBooleanOr("Found", false)) {
                s.site = new Site(tag.getIntOr("X", 0), tag.getIntOr("Z", 0), tag.getIntOr("Floor", 13), tag.getIntOr("WaterTop", 62));
            }
            for (long key : tag.getLongArray("Built").orElse(new long[0])) {
                s.builtChunks.add(key);
            }
            s.complete = tag.getBooleanOr("Complete", false);
            s.populated = tag.getBooleanOr("Populated", false);
        } catch (IOException | RuntimeException e) {
            GreenLantern.LOGGER.error("Could not read {}", path, e);
        }
        return s;
    }

    static void save(MinecraftServer server) {
        State s = state(server);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Searched", s.searched);
        tag.putBoolean("Found", s.site != null);
        if (s.site != null) {
            tag.putInt("X", s.site.x());
            tag.putInt("Z", s.site.z());
            tag.putInt("Floor", s.site.floor());
            tag.putInt("WaterTop", s.site.waterTop());
        }
        tag.putLongArray("Built", s.builtChunks.stream().mapToLong(Long::longValue).toArray());
        tag.putBoolean("Complete", s.complete);
        tag.putBoolean("Populated", s.populated);
        Path path = file(server);
        try {
            Files.createDirectories(path.getParent());
            NbtIo.write(tag, path);
        } catch (IOException e) {
            GreenLantern.LOGGER.error("Could not save {}", path, e);
        }
    }
}
