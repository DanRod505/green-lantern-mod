package com.danrod505.greenlantern.atlantis;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The plan of Atlantis: every block of the city, sorted by chunk so {@link AtlantisBuilder} can
 * raise it a chunk at a time. Coordinates below are relative to the city center, y = 0 being the
 * first water block above the floor.
 * <ul>
 *     <li>the Royal Palace: a four-tiered ziggurat of dark prismarine and gold with a throne hall
 *     inside and a beacon on top whose cyan beam rises out of the sea;</li>
 *     <li>two colossal statues of the King of Atlantis holding his trident at the palace gate;</li>
 *     <li>four glass obelisks of light and a mosaic plaza around the palace;</li>
 *     <li>four avenues of white stone lined with lit columns, ending at the arched city gates;</li>
 *     <li>eight round towers with copper domes, glass domed houses, coral gardens with bubble
 *     columns, a ring road and the city wall;</li>
 *     <li>the Pavilion of Portals: a glass dome full of air where travellers arrive.</li>
 * </ul>
 * The slopes around the city (kelp forests, sand and coral) depend on the natural sea floor, so
 * they are made by the builder itself.
 */
final class AtlantisCity {
    // ---- palette ------------------------------------------------------------------------------------
    static final BlockState BRICKS = Blocks.PRISMARINE_BRICKS.defaultBlockState();
    static final BlockState DARK = Blocks.DARK_PRISMARINE.defaultBlockState();
    static final BlockState ROUGH = Blocks.PRISMARINE.defaultBlockState();
    static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    static final BlockState RAW_GOLD = Blocks.RAW_GOLD_BLOCK.defaultBlockState();
    static final BlockState MARBLE = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    static final BlockState MARBLE_PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    static final BlockState MARBLE_CARVED = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
    static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    static final BlockState CYAN_GLASS = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
    static final BlockState COPPER = Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState();
    static final BlockState COPPER_CUT = Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState();
    static final BlockState PEARL = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
    static final BlockState OCHRE = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
    static final BlockState SAND = Blocks.SAND.defaultBlockState();
    static final BlockState SKIN = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    static final BlockState HAIR = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
    static final BlockState WATER = Blocks.WATER.defaultBlockState();
    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    static final BlockState[] CORAL_BLOCKS = {
            Blocks.TUBE_CORAL_BLOCK.defaultBlockState(), Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
            Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState(), Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
            Blocks.HORN_CORAL_BLOCK.defaultBlockState()};
    static final BlockState[] CORALS = {
            Blocks.TUBE_CORAL.defaultBlockState(), Blocks.BRAIN_CORAL.defaultBlockState(),
            Blocks.BUBBLE_CORAL.defaultBlockState(), Blocks.FIRE_CORAL.defaultBlockState(),
            Blocks.HORN_CORAL.defaultBlockState(), Blocks.TUBE_CORAL_FAN.defaultBlockState(),
            Blocks.BRAIN_CORAL_FAN.defaultBlockState(), Blocks.FIRE_CORAL_FAN.defaultBlockState()};

    // ---- layout -------------------------------------------------------------------------------------
    static final int TOWERS = 8;
    static final double TOWER_RING = 34.0;
    static final int TOWER_RADIUS = 4;
    static final int RING_ROAD = 45;
    static final int GARDEN_RING = 32;
    /** Palace tiers: half width, first y, roof y. */
    static final int[][] TIERS = {{12, 0, 6}, {9, 7, 12}, {6, 13, 18}, {3, 19, 23}};
    static final int BEACON_Y = 24;

    /** A chest of the city, filled when its chunk is built. */
    record ChestSpec(BlockPos pos, Loot loot) {}

    enum Loot { ROYAL, TREASURE, ARMORY, HOME, PAVILION }

    /** Blocks of one chunk, in placement order (later entries win). */
    static final class ChunkPlan {
        final LongArrayList positions = new LongArrayList();
        final List<BlockState> states = new ArrayList<>();
    }

    private final Atlantis.Site site;
    private final int cx;
    private final int cz;
    private final int floor;
    private final Long2ObjectOpenHashMap<ChunkPlan> chunks = new Long2ObjectOpenHashMap<>();
    private final List<ChestSpec> chests = new ArrayList<>();
    /** Houses: {x, z, radius}. */
    private final List<int[]> houses = new ArrayList<>();

    private AtlantisCity(Atlantis.Site site) {
        this.site = site;
        this.cx = site.x();
        this.cz = site.z();
        this.floor = site.floor();
    }

    /** Draws the whole city. Deterministic: the same site always gives the same city. */
    static AtlantisCity plan(Atlantis.Site site) {
        AtlantisCity city = new AtlantisCity(site);
        RandomSource random = RandomSource.create(1941L);
        city.floorPlan(random);
        city.gardens(random);
        city.palace();
        city.statue(-10, -1);
        city.statue(10, 1);
        for (int i = 0; i < 4; i++) {
            city.obelisk(i);
        }
        city.avenueColumns();
        for (int i = 0; i < TOWERS; i++) {
            city.tower(i);
        }
        city.houses(random);
        city.wall();
        city.pavilion();
        return city;
    }

    ChunkPlan chunk(long key) {
        return chunks.get(key);
    }

    List<ChestSpec> chests() {
        return chests;
    }

    // ---- placing ------------------------------------------------------------------------------------

    /** Places a block (waterlogged when it can be: the city is under the sea). */
    private void set(int lx, int ly, int lz, BlockState state) {
        put(lx, ly, lz, wet(state, true));
    }

    /** Places a block that stays dry (inside the air-filled pavilion). */
    private void dry(int lx, int ly, int lz, BlockState state) {
        put(lx, ly, lz, wet(state, false));
    }

    private void put(int lx, int ly, int lz, BlockState state) {
        int x = cx + lx;
        int y = floor + ly;
        int z = cz + lz;
        long key = ChunkPos.asLong(x >> 4, z >> 4);
        ChunkPlan plan = chunks.get(key);
        if (plan == null) {
            plan = new ChunkPlan();
            chunks.put(key, plan);
        }
        plan.positions.add(BlockPos.asLong(x, y, z));
        plan.states.add(state);
    }

    static BlockState wet(BlockState state, boolean wet) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, wet) : state;
    }

    private void chest(int lx, int ly, int lz, Direction facing, Loot loot, boolean wet) {
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        if (wet) set(lx, ly, lz, chest); else dry(lx, ly, lz, chest);
        chests.add(new ChestSpec(new BlockPos(cx + lx, floor + ly, cz + lz), loot));
    }

    private static double dist(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    /** Whether a spot lies on one of the four avenues (or the plaza). */
    static boolean onAvenue(int lx, int lz) {
        return Math.abs(lx) <= 3 || Math.abs(lz) <= 3;
    }

    static int[] towerCenter(int index) {
        double a = Math.toRadians(22.5 + 45.0 * index);
        return new int[] {(int) Math.round(Math.cos(a) * TOWER_RING), (int) Math.round(Math.sin(a) * TOWER_RING)};
    }

    static int towerHeight(int index) {
        return 20 + (index % 2) * 8 + (index % 3) * 2;
    }

    // ---- the floor ------------------------------------------------------------------------------------

    private void floorPlan(RandomSource random) {
        int r = Atlantis.RADIUS;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = dist(x, z);
                if (d > r + 0.5) continue;
                set(x, -2, z, DARK);
                set(x, -1, z, floorBlock(x, z, d));
            }
        }
    }

    private BlockState floorBlock(int x, int z, double d) {
        int ax = Math.abs(x);
        int az = Math.abs(z);
        if (Math.max(ax, az) <= 12) return DARK; // under the palace
        if (d < 22.0) {
            // The plaza: white marble with rings of gold and light.
            if (Math.abs(d - 20.5) < 0.5) return GOLD;
            if (Math.abs(d - 17.0) < 0.5) return ((x + z) & 1) == 0 ? LIGHT : MARBLE_CARVED;
            if (Math.max(ax, az) == 13) return GOLD;
            return ((x >> 1) + (z >> 1) & 1) == 0 ? MARBLE : BRICKS;
        }
        if (onAvenue(x, z)) {
            int lateral = ax <= 3 && az <= 3 ? 0 : (ax <= 3 ? ax : az);
            int along = ax <= 3 ? az : ax;
            if (lateral == 0 && along % 4 == 0) return LIGHT;
            if (lateral == 3) return DARK;
            return MARBLE;
        }
        if (Math.abs(d - RING_ROAD) < 1.5) {
            return (int) (Math.atan2(z, x) * 40) % 5 == 0 && Math.abs(d - RING_ROAD) < 0.5 ? LIGHT : MARBLE;
        }
        if (d > Atlantis.RADIUS - 3) return DARK;
        if (Math.floorMod(x, 9) == 0 && Math.floorMod(z, 9) == 0) return LIGHT;
        return ((x >> 1) + (z >> 1) & 1) == 0 ? BRICKS : ROUGH;
    }

    // ---- gardens ----------------------------------------------------------------------------------------

    /** Coral gardens in the four quarters between the avenues, each with a bubble column in the middle. */
    private void gardens(RandomSource random) {
        for (int q = 0; q < 4; q++) {
            double a = Math.toRadians(45 + 90 * q);
            int gx = (int) Math.round(Math.cos(a) * GARDEN_RING);
            int gz = (int) Math.round(Math.sin(a) * GARDEN_RING);
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    double d = dist(x, z) + random.nextDouble() * 1.5;
                    if (d > 7.0) continue;
                    int px = gx + x;
                    int pz = gz + z;
                    set(px, -1, pz, SAND);
                    if (d < 1.5) continue;
                    float roll = random.nextFloat();
                    if (roll < 0.10F) {
                        // A small stack of living coral topped with a fan.
                        int h = 1 + random.nextInt(3);
                        BlockState block = CORAL_BLOCKS[random.nextInt(CORAL_BLOCKS.length)];
                        for (int y = 0; y < h; y++) set(px, y, pz, block);
                        set(px, h, pz, CORALS[random.nextInt(CORALS.length)]);
                    } else if (roll < 0.28F) {
                        set(px, 0, pz, CORALS[random.nextInt(CORALS.length)]);
                    } else if (roll < 0.36F) {
                        set(px, 0, pz, Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, 1 + random.nextInt(4)));
                    } else if (roll < 0.50F) {
                        set(px, 0, pz, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
                        set(px, 1, pz, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
                    } else if (roll < 0.62F) {
                        set(px, 0, pz, Blocks.SEAGRASS.defaultBlockState());
                    } else if (roll < 0.66F) {
                        kelp(px, 0, pz, 6 + random.nextInt(10));
                    }
                }
            }
            // The bubble column: a ring of light around soul sand, bubbles rising all the way up.
            set(gx, -1, gz, Blocks.SOUL_SAND.defaultBlockState());
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                set(gx + dir.getStepX(), -1, gz + dir.getStepZ(), LIGHT);
            }
            int top = site.waterTop() - floor;
            for (int y = 0; y <= top; y++) {
                put(gx, y, gz, Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BlockStateProperties.DRAG, false));
            }
        }
    }

    void kelp(int lx, int ly, int lz, int height) {
        for (int y = 0; y < height - 1; y++) set(lx, ly + y, lz, Blocks.KELP_PLANT.defaultBlockState());
        set(lx, ly + height - 1, lz, Blocks.KELP.defaultBlockState().setValue(BlockStateProperties.AGE_25, 25));
    }

    // ---- the Royal Palace -------------------------------------------------------------------------------

    private void palace() {
        for (int t = 0; t < TIERS.length; t++) {
            int half = TIERS[t][0];
            int y0 = TIERS[t][1];
            int y1 = TIERS[t][2];
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    boolean wall = Math.abs(x) == half || Math.abs(z) == half;
                    for (int y = y0; y <= y1; y++) {
                        if (y == y1) {
                            // Roof / terrace: bricks with a golden rim.
                            set(x, y, z, wall ? GOLD : (t == TIERS.length - 1 ? DARK : BRICKS));
                        } else if (wall) {
                            set(x, y, z, palaceWall(x, y - y0, z, half, y1 - y0));
                        } else if (t > 0 || y > y0) {
                            set(x, y, z, WATER);
                        }
                    }
                    // Golden crenels along the terrace edge.
                    if (wall && ((x + z) & 1) == 0 && t < TIERS.length - 1) set(x, y1 + 1, z, GOLD);
                }
            }
            // Corner pillars of marble with a light and a golden tip.
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    int px = sx * half;
                    int pz = sz * half;
                    for (int y = y0; y <= y1 + 3; y++) set(px, y, pz, MARBLE_PILLAR);
                    set(px, y1 + 4, pz, LIGHT);
                    set(px, y1 + 5, pz, GOLD);
                }
            }
        }
        throneHall();
        // Entrances on the four sides of the first tier, with golden arches.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int lateral = -2; lateral <= 2; lateral++) {
                for (int y = 0; y <= 4; y++) {
                    for (int depth = 11; depth <= 12; depth++) {
                        set(dir.getStepX() * depth + (dir.getStepZ() != 0 ? lateral : 0), y, dir.getStepZ() * depth + (dir.getStepX() != 0 ? lateral : 0), WATER);
                    }
                }
                set(dir.getStepX() * 12 + (dir.getStepZ() != 0 ? lateral : 0), 5, dir.getStepZ() * 12 + (dir.getStepX() != 0 ? lateral : 0), GOLD);
            }
            // Steps of marble in front of each gate.
            for (int lateral = -3; lateral <= 3; lateral++) {
                set(dir.getStepX() * 13 + (dir.getStepZ() != 0 ? lateral : 0), 0,
                        dir.getStepZ() * 13 + (dir.getStepX() != 0 ? lateral : 0),
                        Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
            }
        }
        // The crown: a beacon on a bed of gold, its beam tinted cyan, between four golden spires.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(x, TIERS[3][2], z, GOLD);
            }
        }
        set(0, BEACON_Y, 0, Blocks.BEACON.defaultBlockState());
        set(0, BEACON_Y + 1, 0, CYAN_GLASS);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = BEACON_Y; y < BEACON_Y + 6; y++) set(sx * 2, y, sz * 2, y % 2 == 0 ? GOLD : RAW_GOLD);
                set(sx * 2, BEACON_Y + 6, sz * 2, LIGHT);
                set(sx * 2, BEACON_Y + 7, sz * 2, Blocks.END_ROD.defaultBlockState());
            }
        }
    }

    /** Walls of a palace tier: dark base, bricks, a band of windows and lights. */
    private BlockState palaceWall(int x, int y, int z, int half, int height) {
        if (y == 0) return DARK;
        int along = Math.abs(x) == half ? z : x;
        boolean corner = Math.abs(x) == half && Math.abs(z) == half;
        if (!corner && y == height / 2 + 1 && height >= 4) {
            return Math.floorMod(along, 3) == 0 ? LIGHT : GLASS;
        }
        if (!corner && y == height / 2 && height >= 4 && Math.floorMod(along, 3) != 0) return GLASS;
        return Math.floorMod(along + y, 4) == 0 ? DARK : BRICKS;
    }

    private void throneHall() {
        // A carpet of gold from the south gate to the throne, between rows of marble columns.
        for (int z = -9; z <= 11; z++) {
            set(0, -1, z, z % 3 == 0 ? LIGHT : GOLD);
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int z = -6; z <= 6; z += 6) {
                for (int y = 0; y <= 4; y++) set(sx * 6, y, z, MARBLE_PILLAR);
                set(sx * 6, 5, z, LIGHT);
            }
        }
        // The throne of Atlantis.
        for (int x = -2; x <= 2; x++) {
            set(x, 0, -10, MARBLE_CARVED);
            set(x, 0, -11, MARBLE_CARVED);
        }
        set(0, 1, -10, Blocks.PRISMARINE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        set(-1, 1, -10, GOLD);
        set(1, 1, -10, GOLD);
        for (int y = 1; y <= 4; y++) set(0, y, -11, y == 4 ? LIGHT : GOLD);
        set(-1, 2, -11, GOLD);
        set(1, 2, -11, GOLD);
        set(-1, 3, -11, RAW_GOLD);
        set(1, 3, -11, RAW_GOLD);
        // Treasures of the kingdom on either side of the throne, and the royal armory.
        chest(-4, 0, -10, Direction.SOUTH, Loot.ROYAL, true);
        chest(4, 0, -10, Direction.SOUTH, Loot.TREASURE, true);
        chest(-10, 0, 0, Direction.EAST, Loot.ARMORY, true);
        chest(10, 0, 0, Direction.WEST, Loot.ARMORY, true);
        for (int sx = -1; sx <= 1; sx += 2) {
            set(sx * 3, 0, -10, Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, 4));
            set(sx * 10, 0, -10, CORAL_BLOCKS[2]);
            set(sx * 10, 1, -10, CORALS[5]);
            set(sx * 10, 0, 10, CORAL_BLOCKS[4]);
            set(sx * 10, 1, 10, CORALS[7]);
        }
    }

    // ---- statues ------------------------------------------------------------------------------------------

    /**
     * A colossal statue of the King of Atlantis, facing south (towards the pavilion), holding his
     * trident on the outer {@code side} (+1 east, -1 west).
     */
    private void statue(int sx, int side) {
        int sz = 16;
        // Pedestal.
        for (int x = -4; x <= 4; x++) {
            for (int z = -2; z <= 2; z++) {
                set(sx + x, 0, sz + z, Math.abs(x) == 4 || Math.abs(z) == 2 ? MARBLE_CARVED : MARBLE);
                set(sx + x, 1, sz + z, Math.abs(x) == 4 || Math.abs(z) == 2 ? GOLD : MARBLE);
            }
        }
        int b = 2;
        // Legs: the green scales of the king's leggings.
        for (int y = 0; y < 6; y++) {
            for (int z = -1; z <= 0; z++) {
                set(sx - 2, b + y, sz + z, COPPER);
                set(sx - 1, b + y, sz + z, y == 5 ? COPPER_CUT : COPPER);
                set(sx + 1, b + y, sz + z, COPPER);
                set(sx + 2, b + y, sz + z, y == 5 ? COPPER_CUT : COPPER);
            }
        }
        // Torso of golden scales, the belt and the arms.
        for (int y = 6; y < 12; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 0; z++) {
                    set(sx + x, b + y, sz + z, y == 6 ? COPPER_CUT : ((x + y) & 1) == 0 ? GOLD : RAW_GOLD);
                }
            }
        }
        for (int y = 7; y < 12; y++) {
            for (int z = -1; z <= 0; z++) {
                set(sx - 3 * side, b + y, sz + z, y < 9 ? SKIN : ((y & 1) == 0 ? GOLD : RAW_GOLD));
                set(sx + 3 * side, b + y, sz + z, y < 9 ? SKIN : ((y & 1) == 0 ? GOLD : RAW_GOLD));
            }
        }
        // Head, long hair and beard.
        for (int y = 12; y < 15; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 0; z++) {
                    set(sx + x, b + y, sz + z, z == 0 && y > 12 ? SKIN : HAIR);
                }
            }
            set(sx - 2, b + y, sz - 1, HAIR);
            set(sx + 2, b + y, sz - 1, HAIR);
        }
        set(sx, b + 12, sz + 1, HAIR); // beard
        for (int x = -1; x <= 1; x++) set(sx + x, b + 15, sz - 1, HAIR);
        // The trident, taller than its bearer.
        int tx = sx + 4 * side;
        int tz = sz;
        for (int y = 0; y < 18; y++) set(tx, b + y, tz, y == 7 ? SKIN : GOLD);
        for (int x = -1; x <= 1; x++) set(tx + x, b + 17, tz, GOLD);
        set(tx - 1, b + 18, tz, GOLD);
        set(tx + 1, b + 18, tz, GOLD);
        set(tx, b + 18, tz, GOLD);
        set(tx, b + 19, tz, GOLD);
        set(tx - 1, b + 19, tz, LIGHT);
        set(tx + 1, b + 19, tz, LIGHT);
    }

    // ---- obelisks, avenues ---------------------------------------------------------------------------------

    /** Glass obelisks with a core of light at the four corners of the plaza. */
    private void obelisk(int index) {
        int ox = (index & 1) == 0 ? -16 : 16;
        int oz = (index & 2) == 0 ? -16 : 16;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(ox + x, 0, oz + z, DARK);
                boolean core = x == 0 && z == 0;
                for (int y = 1; y <= 8; y++) set(ox + x, y, oz + z, core ? (y % 3 == 0 ? PEARL : LIGHT) : GLASS);
                set(ox + x, 9, oz + z, x == 0 || z == 0 ? GOLD : DARK);
            }
        }
        set(ox, 10, oz, GOLD);
        set(ox, 11, oz, LIGHT);
    }

    /** Marble columns crowned with light along the avenues. */
    private void avenueColumns() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int along = 28; along <= 52; along += 6) {
                // The pavilion sits on the southern avenue.
                if (dir == Direction.SOUTH && along < 34) continue;
                for (int side = -1; side <= 1; side += 2) {
                    int x = dir.getStepX() * along + (dir.getStepZ() != 0 ? side * 5 : 0);
                    int z = dir.getStepZ() * along + (dir.getStepX() != 0 ? side * 5 : 0);
                    set(x, 0, z, MARBLE_CARVED);
                    for (int y = 1; y <= 4; y++) set(x, y, z, MARBLE_PILLAR);
                    set(x, 5, z, LIGHT);
                    set(x, 6, z, GOLD);
                }
            }
        }
    }

    // ---- towers ------------------------------------------------------------------------------------------------

    private void tower(int index) {
        int[] c = towerCenter(index);
        int height = towerHeight(index);
        double r = TOWER_RADIUS + 0.5;
        // Facing the palace: the door.
        double toCenter = Math.atan2(-c[1], -c[0]);
        int doorX = (int) Math.round(Math.cos(toCenter) * TOWER_RADIUS);
        int doorZ = (int) Math.round(Math.sin(toCenter) * TOWER_RADIUS);
        for (int x = -TOWER_RADIUS - 1; x <= TOWER_RADIUS + 1; x++) {
            for (int z = -TOWER_RADIUS - 1; z <= TOWER_RADIUS + 1; z++) {
                double d = dist(x, z);
                if (d > r) continue;
                boolean shell = d > r - 1.0;
                set(c[0] + x, -1, c[1] + z, shell ? DARK : MARBLE);
                for (int y = 0; y <= height; y++) {
                    if (!shell) {
                        set(c[0] + x, y, c[1] + z, y == height ? BRICKS : WATER);
                        continue;
                    }
                    boolean door = Math.abs(x - doorX) <= 1 && Math.abs(z - doorZ) <= 1 && y <= 3 && d > r - 1.0;
                    if (door) {
                        set(c[0] + x, y, c[1] + z, WATER);
                        continue;
                    }
                    BlockState block;
                    if (y % 6 == 5) {
                        block = LIGHT;
                    } else if (y % 6 >= 2 && y % 6 <= 3 && y > 4 && (Math.abs(x) <= 1 || Math.abs(z) <= 1)) {
                        block = GLASS;
                    } else {
                        block = y == 0 ? DARK : ((y / 3 + x + z) % 5 == 0 ? ROUGH : BRICKS);
                    }
                    set(c[0] + x, y, c[1] + z, block);
                }
            }
        }
        // A balcony ring of gold near the top.
        for (int x = -TOWER_RADIUS - 2; x <= TOWER_RADIUS + 2; x++) {
            for (int z = -TOWER_RADIUS - 2; z <= TOWER_RADIUS + 2; z++) {
                double d = dist(x, z);
                if (d > r && d <= r + 1.0) set(c[0] + x, height - 3, c[1] + z, GOLD);
            }
        }
        // Copper dome with a golden spire.
        for (int x = -TOWER_RADIUS - 1; x <= TOWER_RADIUS + 1; x++) {
            for (int z = -TOWER_RADIUS - 1; z <= TOWER_RADIUS + 1; z++) {
                for (int y = 1; y <= TOWER_RADIUS + 1; y++) {
                    double d = Math.sqrt(x * x + z * z + y * y * 1.4);
                    if (d <= r && d > r - 1.2) set(c[0] + x, height + y, c[1] + z, y == 1 ? COPPER_CUT : COPPER);
                }
            }
        }
        int top = height + (int) Math.ceil(r / Math.sqrt(1.4));
        set(c[0], top, c[1], GOLD);
        set(c[0], top + 1, c[1], GOLD);
        set(c[0], top + 2, c[1], LIGHT);
        set(c[0], top + 3, c[1], GOLD);
        set(c[0], height - 1, c[1], PEARL);
    }

    // ---- houses -------------------------------------------------------------------------------------------------

    private boolean houseFits(int x, int z, int radius) {
        double d = dist(x, z);
        if (d < 24 + radius || d > Atlantis.RADIUS - 4 - radius) return false;
        if (Math.abs(d - RING_ROAD) < radius + 2.5) return false;
        if (Math.abs(x) < radius + 7 || Math.abs(z) < radius + 7) return false;
        for (int i = 0; i < TOWERS; i++) {
            int[] t = towerCenter(i);
            if (dist(x - t[0], z - t[1]) < radius + TOWER_RADIUS + 3) return false;
        }
        for (int q = 0; q < 4; q++) {
            double a = Math.toRadians(45 + 90 * q);
            if (dist(x - Math.cos(a) * GARDEN_RING, z - Math.sin(a) * GARDEN_RING) < radius + 8) return false;
        }
        for (int[] h : houses) {
            if (dist(x - h[0], z - h[1]) < radius + h[2] + 2) return false;
        }
        return true;
    }

    /** Glass domed houses scattered around the outer districts. */
    private void houses(RandomSource random) {
        int attempts = 0;
        while (houses.size() < 26 && attempts++ < 600) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double d = 24 + random.nextDouble() * (Atlantis.RADIUS - 28);
            int x = (int) Math.round(Math.cos(a) * d);
            int z = (int) Math.round(Math.sin(a) * d);
            int radius = 3 + random.nextInt(3);
            if (!houseFits(x, z, radius)) continue;
            houses.add(new int[] {x, z, radius});
            house(x, z, radius, random);
        }
    }

    private void house(int hx, int hz, int radius, RandomSource random) {
        double r = radius + 0.5;
        double toCenter = Math.atan2(-hz, -hx);
        int doorX = (int) Math.round(Math.cos(toCenter) * radius);
        int doorZ = (int) Math.round(Math.sin(toCenter) * radius);
        BlockState rib = random.nextBoolean() ? BRICKS : COPPER_CUT;
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
                double flat = dist(x, z);
                if (flat <= r) set(hx + x, -1, hz + z, flat > r - 1 ? DARK : MARBLE);
                for (int y = 0; y <= radius + 1; y++) {
                    double d = Math.sqrt(x * x + z * z + y * y);
                    if (d > r) continue;
                    if (d <= r - 1.0) {
                        set(hx + x, y, hz + z, WATER);
                        continue;
                    }
                    boolean door = Math.abs(x - doorX) + Math.abs(z - doorZ) <= 1 && y <= 1;
                    if (door) {
                        set(hx + x, y, hz + z, WATER);
                    } else if (y == 0) {
                        set(hx + x, y, hz + z, BRICKS);
                    } else if (x == 0 || z == 0) {
                        set(hx + x, y, hz + z, rib);
                    } else {
                        set(hx + x, y, hz + z, GLASS);
                    }
                }
            }
        }
        set(hx, radius, hz, LIGHT);
        set(hx, 0, hz, CORAL_BLOCKS[random.nextInt(CORAL_BLOCKS.length)]);
        set(hx, 1, hz, CORALS[random.nextInt(CORALS.length)]);
        if (random.nextInt(3) == 0) {
            int sx = -doorX / Math.max(1, Math.abs(doorX)) * (radius - 2);
            int sz = -doorZ / Math.max(1, Math.abs(doorZ)) * (radius - 2);
            if (sx == 0 && sz == 0) sx = radius - 2;
            chest(hx + sx, 0, hz + sz, Direction.NORTH, Loot.HOME, true);
        }
        if (random.nextBoolean()) {
            set(hx + 1, 0, hz + 1, Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, 3));
        }
    }

    // ---- the wall and its gates ---------------------------------------------------------------------------------

    private void wall() {
        int r = Atlantis.RADIUS;
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double d = dist(x, z);
                if (d > r + 0.5 || d <= r - 1.5) continue;
                boolean gate = Math.abs(x) <= 4 || Math.abs(z) <= 4;
                if (gate) continue;
                int angle = (int) Math.round(Math.toDegrees(Math.atan2(z, x)) * 2);
                for (int y = 0; y <= 5; y++) {
                    BlockState block = y == 0 ? DARK : (y == 3 && Math.floorMod(angle, 16) == 0 ? LIGHT : (y == 5 ? DARK : BRICKS));
                    set(x, y, z, block);
                }
                if (Math.floorMod(angle, 4) < 2) set(x, 6, z, BRICKS);
            }
        }
        // Gates: twin towers and a golden arch over each avenue.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int side = -1; side <= 1; side += 2) {
                for (int dl = 5; dl <= 6; dl++) {
                    for (int da = r - 2; da <= r; da++) {
                        int x = dir.getStepX() * da + (dir.getStepZ() != 0 ? side * dl : 0);
                        int z = dir.getStepZ() * da + (dir.getStepX() != 0 ? side * dl : 0);
                        for (int y = 0; y <= 10; y++) set(x, y, z, y == 10 ? GOLD : (y % 4 == 3 ? LIGHT : BRICKS));
                    }
                }
            }
            for (int lateral = -4; lateral <= 4; lateral++) {
                for (int da = r - 2; da <= r; da++) {
                    int x = dir.getStepX() * da + (dir.getStepZ() != 0 ? lateral : 0);
                    int z = dir.getStepZ() * da + (dir.getStepX() != 0 ? lateral : 0);
                    set(x, 7, z, Math.abs(lateral) == 0 ? GOLD : DARK);
                    set(x, 8, z, Math.abs(lateral) <= 1 ? LIGHT : BRICKS);
                    set(x, 9, z, GOLD);
                }
            }
        }
    }

    // ---- the Pavilion of Portals --------------------------------------------------------------------------------

    /**
     * A glass dome full of air at the end of the southern avenue, where portals arrive: anyone can
     * catch their breath here. Its copper door holds the sea back (water can't flow through doors).
     */
    private void pavilion() {
        int pz = Atlantis.PAVILION_Z;
        int radius = Atlantis.PAVILION_RADIUS;
        double inner = radius - 0.5;
        double outer = radius + 0.5;
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
                double flat = dist(x, z);
                if (flat < outer + 1.0) {
                    set(x, -2, pz + z, DARK);
                    set(x, -1, pz + z, flat >= inner ? DARK : (Math.abs(flat - 3.5) < 0.5 ? GOLD : (flat < 1 ? LIGHT : MARBLE)));
                }
                for (int y = 0; y <= radius + 1; y++) {
                    double d = Math.sqrt(x * x + z * z + y * y);
                    if (d < inner) {
                        dry(x, y, pz + z, AIR);
                    } else if (d < outer) {
                        boolean rib = x == 0 || z == 0;
                        BlockState block = y == 0 ? DARK : (rib ? BRICKS : GLASS);
                        if (x == 0 && z == 0) block = LIGHT;
                        set(x, y, pz + z, block);
                    }
                }
            }
        }
        // The door, on the side of the palace.
        BlockState door = Blocks.WAXED_OXIDIZED_COPPER_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT)
                .setValue(BlockStateProperties.OPEN, false);
        dry(0, 0, pz - radius, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        dry(0, 1, pz - radius, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        set(0, 2, pz - radius, GOLD);
        // Lanterns and a welcome gift for visitors without gills.
        dry(-4, 0, pz + 2, Blocks.LANTERN.defaultBlockState());
        dry(4, 0, pz + 2, Blocks.LANTERN.defaultBlockState());
        dry(-3, 0, pz - 3, Blocks.POTTED_FERN.defaultBlockState());
        chest(3, 0, pz - 3, Direction.WEST, Loot.PAVILION, false);
    }
}
