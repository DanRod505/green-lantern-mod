package com.danrod505.greenlantern.trench;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.atlantis.Atlantis;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Digs the nests of the Trench into the sea floor once Atlantis stands, one chunk at a time on the
 * server tick (like {@link com.danrod505.greenlantern.atlantis.AtlantisBuilder}). Each chunk is
 * carved (the pit, its black walls, the dead sea floor around it), gets its share of the
 * {@link TrenchNest} plan and is turned into the Trench's own biome: the dark, fishless sea that
 * tells a player they crossed into the territory.
 */
public final class TrenchBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int PRELOAD = 4;

    /** A chunk of one nest, in building order. */
    private record Job(int nest, long chunk, double distance) {}

    private static @Nullable MinecraftServer planServer;
    private static final Int2ObjectOpenHashMap<TrenchNest> PLANS = new Int2ObjectOpenHashMap<>();
    private static @Nullable List<Job> order;

    private TrenchBuilder() {}

    public static void onServerStopped(MinecraftServer server) {
        Trench.saveIfLoaded(server);
        TrenchLife.onServerStopped();
        planServer = null;
        PLANS.clear();
        order = null;
        Trench.forget();
    }

    /** Server tick: after Atlantis is complete, builds the nests (a chunk every other tick), then lets them live. */
    public static void tick(MinecraftServer server) {
        if (!Trench.enabled() || !Atlantis.isComplete(server)) return;
        List<Trench.Nest> nests = Trench.nests(server);
        if (nests.isEmpty()) return;
        if (Trench.isComplete(server)) {
            TrenchLife.tick(server.overworld(), nests);
            return;
        }
        if (server.getTickCount() % 2 == 0) buildNext(server, nests, 1, true);
    }

    /** Finishes every nest right now. Returns false if the world has no nests. */
    public static boolean ensureBuilt(MinecraftServer server) {
        if (!Atlantis.isComplete(server)) return false;
        List<Trench.Nest> nests = Trench.nests(server);
        if (nests.isEmpty()) return false;
        if (Trench.isComplete(server)) return true;
        long start = System.nanoTime();
        buildNext(server, nests, Integer.MAX_VALUE, false);
        GreenLantern.LOGGER.info("Finished digging the nests of the Trench in {} ms", (System.nanoTime() - start) / 1_000_000);
        return true;
    }

    /** Tests only: builds a whole nest right now, without touching the saved progress. */
    public static void buildNestAt(ServerLevel level, Trench.Nest nest) {
        TrenchNest plan = TrenchNest.plan(nest);
        for (long key : TrenchNest.chunkKeys(nest)) {
            buildChunk(level, List.of(nest), nest, plan, ChunkPos.getX(key), ChunkPos.getZ(key));
        }
    }

    private static TrenchNest plan(MinecraftServer server, Trench.Nest nest) {
        if (planServer != server) {
            PLANS.clear();
            order = null;
            planServer = server;
        }
        return PLANS.computeIfAbsent(nest.index(), i -> TrenchNest.plan(nest));
    }

    private static List<Job> order(List<Trench.Nest> nests) {
        if (order == null) {
            List<Job> jobs = new ArrayList<>();
            for (Trench.Nest nest : nests) {
                for (long key : TrenchNest.chunkKeys(nest)) {
                    double dx = (ChunkPos.getX(key) << 4) + 8 - nest.x();
                    double dz = (ChunkPos.getZ(key) << 4) + 8 - nest.z();
                    jobs.add(new Job(nest.index(), key, Math.sqrt(dx * dx + dz * dz)));
                }
            }
            jobs.sort(Comparator.comparingInt(Job::nest).thenComparingDouble(Job::distance));
            order = jobs;
        }
        return order;
    }

    private static void buildNext(MinecraftServer server, List<Trench.Nest> nests, int maxChunks, boolean background) {
        Trench.State state = Trench.state(server);
        ServerLevel level = server.overworld();
        List<Job> jobs = order(nests);
        int built = 0;
        int requested = 0;
        for (int i = 0; i < jobs.size() && built < maxChunks; i++) {
            Job job = jobs.get(i);
            if (state.builtChunks.contains(job.chunk())) continue;
            if (background && level.getChunkSource().getChunkNow(ChunkPos.getX(job.chunk()), ChunkPos.getZ(job.chunk())) == null) {
                if (requested++ >= PRELOAD) break;
                level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(job.chunk()), 1);
                continue;
            }
            Trench.Nest nest = null;
            for (Trench.Nest n : nests) if (n.index() == job.nest()) nest = n;
            if (nest == null) continue;
            buildChunk(level, nests, nest, plan(server, nest), ChunkPos.getX(job.chunk()), ChunkPos.getZ(job.chunk()));
            state.builtChunks.add(job.chunk());
            built++;
            if (state.builtChunks.size() % 8 == 0) Trench.save(server);
        }
        boolean all = true;
        for (Job job : jobs) {
            if (!state.builtChunks.contains(job.chunk())) {
                all = false;
                break;
            }
        }
        if (all) {
            state.complete = true;
            Trench.save(server);
            GreenLantern.LOGGER.info("The nests of the Trench are dug");
            PLANS.clear();
            order = null;
        }
    }

    private static void buildChunk(ServerLevel level, List<Trench.Nest> nests, Trench.Nest nest, TrenchNest plan, int chunkX, int chunkZ) {
        level.getChunk(chunkX, chunkZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
            for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                carveColumn(level, nest, x, z, pos);
            }
        }
        TrenchNest.ChunkPlan chunk = plan.chunk(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk != null) {
            for (int i = 0; i < chunk.positions.size(); i++) {
                pos.set(chunk.positions.getLong(i));
                level.setBlock(pos, chunk.states.get(i), FLAGS);
            }
        }
        paintBiome(level, nests, chunkX, chunkZ);
    }

    // ---- the pit and the dead sea floor ---------------------------------------------------------------

    private static boolean soft(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }

    private static void carveColumn(ServerLevel level, Trench.Nest nest, int x, int z, BlockPos.MutableBlockPos pos) {
        double dx = x + 0.5 - (nest.x() + 0.5);
        double dz = z + 0.5 - (nest.z() + 0.5);
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > Trench.TERRITORY) return;
        int floor = nest.floor();
        int top = nest.waterTop();
        double edge = TrenchNest.pitRadius(nest, Math.atan2(dz, dx));
        int natural = Math.min(level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1, top - 1);
        BlockState water = Blocks.WATER.defaultBlockState();
        if (d <= edge) {
            // The pit: steep walls, a little rubble slope at their foot.
            int ground = floor - 1 + (int) Math.round(Math.max(0.0, d - (edge - 4.0)) * 1.2);
            for (int y = ground - 4; y < ground; y++) {
                BlockState state = level.getBlockState(pos.set(x, y, z));
                if (soft(state)) level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), FLAGS);
            }
            level.setBlock(pos.set(x, ground, z), TrenchNest.pitFloorBlock(x, ground, z), FLAGS);
            for (int y = ground + 1; y <= top; y++) {
                if (level.getBlockState(pos.set(x, y, z)) != water) level.setBlock(pos, water, FLAGS);
            }
            return;
        }
        if (d <= edge + 4.0) {
            // The pit wall: black rock all the way down, sealed against caves.
            for (int y = floor - 5; y <= natural; y++) {
                level.setBlock(pos.set(x, y, z), TrenchNest.wallBlock(x, y, z), FLAGS);
            }
            clearAbove(level, x, natural + 1, z, top, pos);
            return;
        }
        // The territory: the corruption thins out towards its edge.
        double t = (d - edge - 4.0) / Math.max(1.0, Trench.TERRITORY - edge - 4.0);
        double r = TrenchNest.roll(x, 7, z);
        if (r > 1.0 - 0.85 * t) return;
        level.setBlock(pos.set(x, natural, z), TrenchNest.deadFloorBlock(x, natural, z), FLAGS);
        clearAbove(level, x, natural + 1, z, top, pos);
        double deco = TrenchNest.roll(x, 11, z);
        if (deco < 0.07) {
            level.setBlock(pos.set(x, natural + 1, z), TrenchNest.wet(TrenchNest.DEAD_CORALS[(int) (deco * 1000) % TrenchNest.DEAD_CORALS.length]), FLAGS);
        } else if (deco < 0.085) {
            // Bone spikes sticking out of the sea floor.
            int h = 1 + (int) (deco * 1000) % 3;
            for (int y = natural + 1; y <= natural + h && y < top; y++) level.setBlock(pos.set(x, y, z), Blocks.BONE_BLOCK.defaultBlockState(), FLAGS);
        }
    }

    /** Takes the kelp, seagrass and coral away (nothing grows here) and floods any pocket of air. */
    private static void clearAbove(ServerLevel level, int x, int from, int z, int top, BlockPos.MutableBlockPos pos) {
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int y = from; y <= Math.min(top, from + 40); y++) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state == water) continue;
            if (state.isAir() || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)
                    || state.is(Blocks.SEA_PICKLE) || state.is(net.minecraft.tags.BlockTags.CORALS) || state.is(net.minecraft.tags.BlockTags.CORAL_BLOCKS)) {
                level.setBlock(pos, water, FLAGS);
            } else if (!state.getFluidState().isEmpty()) {
                continue;
            } else {
                break;
            }
        }
    }

    // ---- the dark sea -------------------------------------------------------------------------------

    private static boolean biomeFailed;

    /** Turns the territory (below the surface) into the Trench's biome and sends the change to the players. */
    private static void paintBiome(ServerLevel level, List<Trench.Nest> nests, int chunkX, int chunkZ) {
        if (biomeFailed) return;
        Holder<Biome> trench;
        try {
            trench = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Trench.BIOME);
        } catch (RuntimeException e) {
            biomeFailed = true;
            GreenLantern.LOGGER.error("The biome of the Trench is missing", e);
            return;
        }
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        boolean[] changed = {false};
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
            int bx = (qx << 2) + 2;
            int by = (qy << 2) + 2;
            int bz = (qz << 2) + 2;
            for (Trench.Nest nest : nests) {
                double dx = bx - nest.x();
                double dz = bz - nest.z();
                if (by <= nest.waterTop() && dx * dx + dz * dz <= (Trench.TERRITORY + 2) * (Trench.TERRITORY + 2)) {
                    changed[0] = true;
                    return trench;
                }
            }
            return chunk.getNoiseBiome(qx, qy, qz);
        }, level.getChunkSource().randomState().sampler());
        if (changed[0]) {
            chunk.markUnsaved();
            level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        }
    }
}
