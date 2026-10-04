package com.danrod505.greenlantern.atlantis;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.registry.ModItems;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Raises Atlantis in the overworld, one chunk at a time on the server tick (so the world never
 * freezes), nearest chunks first. Each chunk is carved down to the city floor and flooded, then
 * gets its share of {@link AtlantisCity}. If someone travels there before it is finished, the rest
 * is built at once. The progress is saved, so a restart picks up where it stopped.
 */
public final class AtlantisBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Ticks after the server starts before building begins (let the spawn area load first). */
    private static final int START_DELAY = 200;

    private static @Nullable MinecraftServer planServer;
    private static @Nullable AtlantisCity plan;
    private static @Nullable LongArrayList order;

    private AtlantisBuilder() {}

    /** Forgets everything about the world that just closed (singleplayer can open another one). */
    public static void onServerStopped(MinecraftServer server) {
        Atlantis.saveIfLoaded(server);
        planServer = null;
        plan = null;
        order = null;
        Atlantis.forget();
    }

    private static AtlantisCity plan(MinecraftServer server, Atlantis.Site site) {
        if (plan == null || planServer != server) {
            long start = System.nanoTime();
            plan = AtlantisCity.plan(site);
            order = chunkOrder(site);
            planServer = server;
            GreenLantern.LOGGER.info("Planned Atlantis ({} chunks) in {} ms", order.size(), (System.nanoTime() - start) / 1_000_000);
        }
        return plan;
    }

    /** Every chunk the city (and its slopes) touches, nearest to the palace first. */
    private static LongArrayList chunkOrder(Atlantis.Site site) {
        List<long[]> chunks = new ArrayList<>();
        int r = Atlantis.OUTER;
        for (int cx = (site.x() - r) >> 4; cx <= (site.x() + r) >> 4; cx++) {
            for (int cz = (site.z() - r) >> 4; cz <= (site.z() + r) >> 4; cz++) {
                // Distance from the city center to the nearest point of the chunk.
                int nx = Mth.clamp(site.x(), cx << 4, (cx << 4) + 15);
                int nz = Mth.clamp(site.z(), cz << 4, (cz << 4) + 15);
                double d = Math.sqrt((double) (nx - site.x()) * (nx - site.x()) + (double) (nz - site.z()) * (nz - site.z()));
                if (d <= r) chunks.add(new long[] {ChunkPos.asLong(cx, cz), (long) (d * 16)});
            }
        }
        chunks.sort(Comparator.comparingLong(c -> c[1]));
        LongArrayList keys = new LongArrayList();
        for (long[] c : chunks) keys.add(c[0]);
        return keys;
    }

    /** Server tick: builds the next chunk every other tick until the city stands, then lets life go on. */
    public static void tick(MinecraftServer server) {
        if (!Atlantis.enabled() || server.getTickCount() < START_DELAY) return;
        Atlantis.Site site = Atlantis.site(server);
        if (site == null) return;
        if (Atlantis.isComplete(server)) {
            AtlantisLife.tick(server.overworld(), site);
            return;
        }
        if (server.getTickCount() % 2 == 0) buildNext(server, site, 1);
    }

    /** Finishes the city right now (someone is about to arrive). Returns false if the world has no Atlantis. */
    public static boolean ensureBuilt(MinecraftServer server) {
        Atlantis.Site site = Atlantis.site(server);
        if (site == null) return false;
        if (Atlantis.isComplete(server)) return true;
        long start = System.nanoTime();
        buildNext(server, site, Integer.MAX_VALUE);
        GreenLantern.LOGGER.info("Finished building Atlantis in {} ms", (System.nanoTime() - start) / 1_000_000);
        return true;
    }

    /** Tests only: builds a whole city right now at the given site, without touching the saved progress. */
    public static void buildAllAt(ServerLevel level, Atlantis.Site site) {
        AtlantisCity city = AtlantisCity.plan(site);
        LongArrayList chunks = chunkOrder(site);
        for (int i = 0; i < chunks.size(); i++) {
            buildChunk(level, site, city, ChunkPos.getX(chunks.getLong(i)), ChunkPos.getZ(chunks.getLong(i)));
        }
    }

    private static void buildNext(MinecraftServer server, Atlantis.Site site, int maxChunks) {
        Atlantis.State state = Atlantis.state(server);
        AtlantisCity city = plan(server, site);
        ServerLevel level = server.overworld();
        int built = 0;
        for (int i = 0; i < order.size() && built < maxChunks; i++) {
            long key = order.getLong(i);
            if (state.builtChunks.contains(key)) continue;
            buildChunk(level, site, city, ChunkPos.getX(key), ChunkPos.getZ(key));
            state.builtChunks.add(key);
            built++;
            if (state.builtChunks.size() % 8 == 0) Atlantis.save(server);
        }
        if (state.builtChunks.size() >= order.size() || builtAll(state)) {
            state.complete = true;
            Atlantis.save(server);
            GreenLantern.LOGGER.info("Atlantis is complete");
            // The plan is no longer needed: free the memory.
            plan = null;
            order = null;
            planServer = null;
        }
    }

    private static boolean builtAll(Atlantis.State state) {
        for (int i = 0; i < order.size(); i++) {
            if (!state.builtChunks.contains(order.getLong(i))) return false;
        }
        return true;
    }

    private static void buildChunk(ServerLevel level, Atlantis.Site site, AtlantisCity city, int chunkX, int chunkZ) {
        level.getChunk(chunkX, chunkZ); // generates the chunk if needed
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
            for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                carveColumn(level, site, x, z, pos);
            }
        }
        AtlantisCity.ChunkPlan chunk = city.chunk(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk != null) {
            for (int i = 0; i < chunk.positions.size(); i++) {
                pos.set(chunk.positions.getLong(i));
                level.setBlock(pos, chunk.states.get(i), FLAGS);
            }
        }
        for (AtlantisCity.ChestSpec chest : city.chests()) {
            if (chest.pos().getX() >> 4 == chunkX && chest.pos().getZ() >> 4 == chunkZ) fill(level, chest);
        }
    }

    // ---- the crater ----------------------------------------------------------------------------------

    /**
     * Digs one column down to the city floor (inside the city) or to a smooth slope joining the
     * natural sea floor (around it), fills it with sea water and grows kelp, seagrass and coral on
     * the slopes.
     */
    private static void carveColumn(ServerLevel level, Atlantis.Site site, int x, int z, BlockPos.MutableBlockPos pos) {
        double dx = x - site.x();
        double dz = z - site.z();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > Atlantis.OUTER) return;
        int floor = site.floor();
        int top = site.waterTop();
        boolean city = d <= Atlantis.RADIUS + 0.5;
        int ground;
        if (city) {
            ground = floor - 1;
        } else {
            int natural = Math.min(level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1, top - 1);
            double t = (d - Atlantis.RADIUS) / Atlantis.BLEND;
            t = t * t * (3.0 - 2.0 * t);
            ground = (int) Math.round(Mth.lerp(t, floor - 1, natural));
        }
        // A solid bed under the floor (caves and trenches would otherwise drain the eye).
        for (int y = ground - 4; y < ground; y++) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state.isAir() || !state.getFluidState().isEmpty()) level.setBlock(pos, Blocks.STONE.defaultBlockState(), FLAGS);
        }
        long hash = hash(x, z);
        if (!city) level.setBlock(pos.set(x, ground, z), slopeBlock(hash), FLAGS);
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int y = ground + 1; y <= top; y++) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state != water) level.setBlock(pos, water, FLAGS);
        }
        if (!city && d > Atlantis.RADIUS + 2) vegetation(level, x, ground + 1, z, top, hash, pos);
    }

    private static long hash(int x, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 29);
    }

    private static BlockState slopeBlock(long hash) {
        int roll = (int) Math.floorMod(hash, 100L);
        if (roll < 64) return Blocks.SAND.defaultBlockState();
        if (roll < 78) return Blocks.GRAVEL.defaultBlockState();
        if (roll < 86) return Blocks.CLAY.defaultBlockState();
        if (roll < 96) return Blocks.PRISMARINE.defaultBlockState();
        return Blocks.SEA_LANTERN.defaultBlockState();
    }

    /** Kelp forests, seagrass meadows and a little coral on the slopes around the city. */
    private static void vegetation(ServerLevel level, int x, int y, int z, int top, long hash, BlockPos.MutableBlockPos pos) {
        int roll = (int) Math.floorMod(hash >>> 8, 1000L);
        RandomSource random = RandomSource.create(hash);
        if (roll < 140) {
            int height = Math.min(top - y, 8 + random.nextInt(22));
            for (int i = 0; i < height - 1; i++) level.setBlock(pos.set(x, y + i, z), Blocks.KELP_PLANT.defaultBlockState(), FLAGS);
            if (height > 0) {
                level.setBlock(pos.set(x, y + height - 1, z), Blocks.KELP.defaultBlockState().setValue(BlockStateProperties.AGE_25, 25), FLAGS);
            }
        } else if (roll < 330) {
            level.setBlock(pos.set(x, y, z), Blocks.SEAGRASS.defaultBlockState(), FLAGS);
        } else if (roll < 380) {
            level.setBlock(pos.set(x, y, z), Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), FLAGS);
            level.setBlock(pos.set(x, y + 1, z), Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), FLAGS);
        } else if (roll < 410) {
            level.setBlock(pos.set(x, y, z), AtlantisCity.CORALS[random.nextInt(AtlantisCity.CORALS.length)], FLAGS);
        } else if (roll < 425) {
            level.setBlock(pos.set(x, y, z), AtlantisCity.CORAL_BLOCKS[random.nextInt(AtlantisCity.CORAL_BLOCKS.length)], FLAGS);
        } else if (roll < 440) {
            level.setBlock(pos.set(x, y, z), Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, 1 + random.nextInt(4)), FLAGS);
        }
    }

    // ---- treasure --------------------------------------------------------------------------------------

    private static void fill(ServerLevel level, AtlantisCity.ChestSpec spec) {
        if (!(level.getBlockEntity(spec.pos()) instanceof ChestBlockEntity chest)) return;
        RandomSource random = RandomSource.create(spec.pos().asLong());
        List<ItemStack> loot = new ArrayList<>();
        switch (spec.loot()) {
            case TREASURE, ROYAL -> {
                loot.add(new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(9)));
                loot.add(new ItemStack(Items.DIAMOND, 2 + random.nextInt(3)));
                loot.add(new ItemStack(Items.EMERALD, 4 + random.nextInt(6)));
                loot.add(new ItemStack(Items.HEART_OF_THE_SEA));
                loot.add(new ItemStack(Items.NAUTILUS_SHELL, 2 + random.nextInt(3)));
                loot.add(new ItemStack(Items.PRISMARINE_CRYSTALS, 6 + random.nextInt(8)));
                loot.add(new ItemStack(ModItems.ATLANTIS_GATE.get()));
                loot.add(new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
                // The emblem of the kings of Atlantis rests beside the throne.
                if (spec.loot() == AtlantisCity.Loot.ROYAL) loot.add(new ItemStack(ModItems.AQUAMAN_EMBLEM.get()));
            }
            case ARMORY -> {
                loot.add(new ItemStack(Items.TRIDENT));
                loot.add(new ItemStack(Items.TURTLE_HELMET));
                loot.add(new ItemStack(Items.PRISMARINE_SHARD, 8 + random.nextInt(8)));
                loot.add(new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(5)));
                loot.add(new ItemStack(Items.SPYGLASS));
            }
            case HOME -> {
                loot.add(new ItemStack(Items.COOKED_COD, 2 + random.nextInt(5)));
                loot.add(new ItemStack(Items.COOKED_SALMON, 1 + random.nextInt(4)));
                loot.add(new ItemStack(Items.DRIED_KELP, 4 + random.nextInt(10)));
                loot.add(new ItemStack(Items.PRISMARINE_SHARD, 2 + random.nextInt(6)));
                loot.add(new ItemStack(Items.GOLD_NUGGET, 3 + random.nextInt(10)));
                if (random.nextInt(3) == 0) loot.add(new ItemStack(Items.NAUTILUS_SHELL));
                if (random.nextInt(5) == 0) loot.add(new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
            }
            case PAVILION -> {
                loot.add(new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
                loot.add(new ItemStack(Items.COOKED_COD, 6));
            }
        }
        int size = chest.getContainerSize();
        for (ItemStack stack : loot) {
            // Scatter the loot over the chest like vanilla loot tables do.
            for (int tries = 0; tries < 20; tries++) {
                int slot = random.nextInt(size);
                if (chest.getItem(slot).isEmpty()) {
                    chest.setItem(slot, stack);
                    break;
                }
            }
        }
        chest.setChanged();
    }
}
