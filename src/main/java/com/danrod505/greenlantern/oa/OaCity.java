package com.danrod505.greenlantern.oa;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.LanternCorpsmanEntity;
import com.danrod505.greenlantern.entity.OaGuardianEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds the capital of Oa around the dimension's origin the first time anyone arrives:
 * <ul>
 *     <li>a round plaza of dark stone with glowing green rings and eight walkways;</li>
 *     <li>the Central Power Battery: a 36 block tall lantern with a green beacon beam;</li>
 *     <li>a ring of eight pillars, each topped by one of the Guardians of the Universe;</li>
 *     <li>the arrival dais, a skyline of tapered spires with hard-light halos and green crystals;</li>
 *     <li>Lanterns of many species walking around the plaza and flying around the battery.</li>
 * </ul>
 * Everything is deterministic (fixed seed), so every world gets the same Oa.
 */
public final class OaCity {
    /** Placed last: if it is there, the city is complete. */
    private static final BlockPos MARKER = new BlockPos(0, 2, 0);
    private static final int G = Oa.GROUND_Y;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public static final int PLAZA_RADIUS = 44;
    public static final int PILLARS = 8;
    public static final double PILLAR_RING = 27.0;
    public static final int PILLAR_HEIGHT = 12;
    /** Top of the battery body (the cap, beacon and handle sit above it). */
    public static final int BATTERY_TOP = G + 22;

    private static final BlockState DARK = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState DARK_ALT = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState GLASS = Blocks.LIME_STAINED_GLASS.defaultBlockState();
    private static final BlockState DEEP_GLASS = Blocks.GREEN_STAINED_GLASS.defaultBlockState();
    private static final BlockState GLOW = Blocks.VERDANT_FROGLIGHT.defaultBlockState();
    private static final BlockState BRIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState METAL = Blocks.GREEN_CONCRETE.defaultBlockState();
    private static final BlockState TRIM = Blocks.EMERALD_BLOCK.defaultBlockState();
    private static final BlockState QUARTZ = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState QUARTZ_PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState QUARTZ_CHISELED = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState ROD = Blocks.END_ROD.defaultBlockState();

    private OaCity() {}

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.REINFORCED_DEEPSLATE);
    }

    /** Builds the city (once per world). Runs on the server thread and loads the chunks it needs. */
    public static void ensureBuilt(ServerLevel level) {
        if (isBuilt(level)) return;
        long start = System.nanoTime();
        RandomSource random = RandomSource.create(2814L);
        plaza(level);
        battery(level);
        for (int i = 0; i < PILLARS; i++) {
            pillar(level, i);
        }
        arrivalDais(level);
        List<int[]> spires = spires(level, random);
        crystals(level, random, spires);
        populate(level, random);
        level.setBlock(MARKER, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), FLAGS);
        GreenLantern.LOGGER.info("Built the city of Oa in {} ms", (System.nanoTime() - start) / 1_000_000);
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, FLAGS);
    }

    private static double dist(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    /** Filled disc of the given radius centred on block (cx, cz). */
    private static void disc(ServerLevel level, int cx, int y, int cz, double radius, BlockState state) {
        int r = Mth.ceil(radius);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (dist(x, z) <= radius) set(level, cx + x, y, cz + z, state);
            }
        }
    }

    /** Disc whose outermost block ring uses {@code rim}. */
    private static void rimmedDisc(ServerLevel level, int y, double radius, BlockState inner, BlockState rim) {
        int r = Mth.ceil(radius);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = dist(x, z);
                if (d <= radius) set(level, x, y, z, d > radius - 1.0 ? rim : inner);
            }
        }
    }

    /** A glowing floor strip: light under green glass. */
    private static void glowStrip(ServerLevel level, int x, int z) {
        set(level, x, G - 2, z, GLOW);
        set(level, x, G - 1, z, GLASS);
    }

    // ---- plaza ------------------------------------------------------------------------------------

    private static void plaza(ServerLevel level) {
        for (int x = -PLAZA_RADIUS - 1; x <= PLAZA_RADIUS + 1; x++) {
            for (int z = -PLAZA_RADIUS - 1; z <= PLAZA_RADIUS + 1; z++) {
                double d = dist(x, z);
                if (d > PLAZA_RADIUS + 0.5) continue;
                if (Math.abs(d - 13.5) < 0.5 || Math.abs(d - 34.0) < 0.5 || Math.abs(d - PLAZA_RADIUS) < 0.5) {
                    glowStrip(level, x, z);
                    continue;
                }
                // Eight walkways leave the battery like the spokes of a wheel.
                double angle = Math.atan2(z, x);
                double sector = Math.PI / 4.0;
                double off = angle - Math.round(angle / sector) * sector;
                double lateral = Math.abs(Math.sin(off) * d);
                if (d > 13.5 && lateral < 0.5 && Math.floorMod((int) d, 4) == 0) {
                    glowStrip(level, x, z);
                } else if (d > 13.5 && lateral < 1.6) {
                    set(level, x, G - 1, z, TILES);
                } else {
                    set(level, x, G - 1, z, ((x >> 2) + (z >> 2) & 1) == 0 ? DARK : DARK_ALT);
                }
            }
        }
    }

    // ---- the Central Power Battery ------------------------------------------------------------------

    private static void battery(ServerLevel level) {
        // Stepped pedestal and the lantern's foot.
        rimmedDisc(level, G, 12.0, TILES, METAL);
        rimmedDisc(level, G + 1, 10.6, DARK, METAL);
        rimmedDisc(level, G + 2, 9.6, METAL, TRIM);
        rimmedDisc(level, G + 3, 9.6, METAL, METAL);
        rimmedDisc(level, G + 4, 8.6, METAL, METAL);

        // The barrel-shaped body: green glass between eight vertical bars, glowing inside.
        RandomSource random = RandomSource.create(7L);
        int bottom = G + 5;
        for (int y = bottom; y <= BATTERY_TOP - 1; y++) {
            double t = (double) (y - bottom) / (BATTERY_TOP - 1 - bottom);
            double radius = 7.6 + 1.1 * Math.sin(Math.PI * t);
            boolean band = y == bottom || y == BATTERY_TOP - 1 || y == (bottom + BATTERY_TOP - 1) / 2;
            int r = Mth.ceil(radius);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double d = dist(x, z);
                    if (d > radius) continue;
                    if (d > radius - 1.2) {
                        double angle = Math.atan2(z, x);
                        double off = angle - Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
                        boolean bar = Math.abs(off * d) < 0.75;
                        set(level, x, y, z, band ? TRIM : bar ? METAL : GLASS);
                    } else if (d > radius - 3.2) {
                        set(level, x, y, z, random.nextInt(5) == 0 ? BRIGHT : GLOW);
                    }
                }
            }
        }

        // Domed cap.
        rimmedDisc(level, BATTERY_TOP, 9.6, METAL, TRIM);
        rimmedDisc(level, BATTERY_TOP + 1, 8.6, METAL, METAL);
        rimmedDisc(level, BATTERY_TOP + 2, 7.0, METAL, TRIM);
        rimmedDisc(level, BATTERY_TOP + 3, 5.5, METAL, METAL);
        rimmedDisc(level, BATTERY_TOP + 4, 4.0, METAL, TRIM);
        disc(level, 0, BATTERY_TOP + 5, 0, 2.5, TRIM);
        // A beacon on emerald shines a green beam into the sky through the glass at the top of the handle.
        set(level, 0, BATTERY_TOP + 6, 0, Blocks.BEACON.defaultBlockState());

        // The handle: a big ring arching over the cap.
        int handleBase = BATTERY_TOP + 6;
        double handleRadius = 7.0;
        for (int y = BATTERY_TOP + 1; y < handleBase; y++) {
            for (int z = -1; z <= 1; z++) {
                set(level, 7, y, z, METAL);
                set(level, -7, y, z, METAL);
            }
        }
        for (int step = 0; step <= 180; step++) {
            double a = Math.toRadians(step);
            for (double rr = handleRadius - 0.5; rr <= handleRadius + 0.5; rr += 0.5) {
                int x = (int) Math.round(Math.cos(a) * rr);
                int y = handleBase + (int) Math.round(Math.sin(a) * rr);
                for (int z = -1; z <= 1; z++) {
                    BlockState state = x == 0 && z == 0 ? GLASS : rr > handleRadius ? TRIM : METAL;
                    set(level, x, y, z, state);
                }
            }
        }
    }

    // ---- the Guardians' pillars ---------------------------------------------------------------------

    /** Block column of pillar {@code index} (they sit between the walkways). */
    public static BlockPos pillarTop(int index) {
        double a = Math.PI / PILLARS + index * Math.PI * 2 / PILLARS;
        int x = (int) Math.round(Math.cos(a) * PILLAR_RING);
        int z = (int) Math.round(Math.sin(a) * PILLAR_RING);
        return new BlockPos(x, G + PILLAR_HEIGHT + 1, z);
    }

    private static void pillar(ServerLevel level, int index) {
        BlockPos top = pillarTop(index);
        int px = top.getX();
        int pz = top.getZ();
        // Plinth.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                set(level, px + x, G, pz + z, corner ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : TILES);
            }
        }
        // Shaft with two glowing rings.
        for (int y = G + 1; y < G + PILLAR_HEIGHT; y++) {
            boolean ring = y == G + 4 || y == G + 8;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    boolean center = x == 0 && z == 0;
                    boolean corner = Math.abs(x) == 1 && Math.abs(z) == 1;
                    BlockState state = ring ? (center ? GLOW : GLASS) : corner ? QUARTZ_PILLAR : QUARTZ;
                    set(level, px + x, y, pz + z, state);
                }
            }
        }
        // Capital and the Guardian's platform.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                set(level, px + x, G + PILLAR_HEIGHT, pz + z, corner ? TRIM : x == 0 && z == 0 ? QUARTZ_CHISELED : QUARTZ);
                if (corner) set(level, px + x, G + PILLAR_HEIGHT + 1, pz + z, ROD);
            }
        }
    }

    // ---- arrival dais ---------------------------------------------------------------------------------

    private static void arrivalDais(ServerLevel level) {
        int cx = Mth.floor(Oa.ARRIVAL.x);
        int cz = Mth.floor(Oa.ARRIVAL.z);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double d = dist(x, z);
                if (d > 3.2) continue;
                set(level, cx + x, G, cz + z, d > 2.3 ? TRIM : d < 1.0 ? QUARTZ_CHISELED : QUARTZ);
            }
        }
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            set(level, cx + c[0], G + 1, cz + c[1], ROD);
        }
    }

    // ---- skyline ----------------------------------------------------------------------------------------

    /** Tapered spires around the plaza. Returns {x, z, radius} of each, so other features avoid them. */
    private static List<int[]> spires(ServerLevel level, RandomSource random) {
        List<int[]> placed = new ArrayList<>();
        int attempts = 0;
        while (placed.size() < 16 && attempts++ < 400) {
            double a = random.nextDouble() * Math.PI * 2;
            double ring = 54 + random.nextDouble() * 40;
            int cx = (int) Math.round(Math.cos(a) * ring);
            int cz = (int) Math.round(Math.sin(a) * ring);
            int radius = 3 + random.nextInt(4);
            boolean free = true;
            for (int[] other : placed) {
                if (dist(cx - other[0], cz - other[1]) < radius + other[2] + 9) free = false;
            }
            // Keep the view from the arrival dais to the south open.
            if (cz > 40 && Math.abs(cx) < 0.6 * cz) free = false;
            if (!free) continue;
            int height = 26 + random.nextInt(40);
            spire(level, random, cx, cz, radius, height, random.nextInt(10) < 7);
            placed.add(new int[] {cx, cz, radius});
        }
        return placed;
    }

    private static void spire(ServerLevel level, RandomSource random, int cx, int cz, int baseRadius, int height, boolean dark) {
        BlockState body = dark ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : QUARTZ;
        BlockState accent = dark ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
        disc(level, cx, G - 1, cz, baseRadius + 2.5, DARK);
        int haloAt = random.nextInt(3) == 0 ? -1 : (int) (height * (0.55 + random.nextDouble() * 0.25));
        for (int h = 0; h < height; h++) {
            double t = (double) h / height;
            double radius = baseRadius * (1.0 - 0.7 * Math.pow(t, 1.3)) + 0.6;
            int y = G + h;
            int r = Mth.ceil(radius);
            boolean band = h % 7 == 3;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double d = dist(x, z);
                    if (d > radius) continue;
                    boolean shell = d > radius - 1.0;
                    boolean window = shell && (band || x == 0 || z == 0);
                    if (window) {
                        set(level, cx + x, y, cz + z, GLASS);
                    } else {
                        set(level, cx + x, y, cz + z, h % 7 == 0 ? accent : body);
                    }
                }
            }
            // Light just behind the windows.
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double d = dist(x, z);
                    if (d <= radius - 1.0 && d > radius - 2.0 && (band || x == 0 || z == 0)) set(level, cx + x, y, cz + z, GLOW);
                }
            }
            if (h == haloAt) {
                double hr = radius + 3.0;
                int hri = Mth.ceil(hr + 0.5);
                for (int x = -hri; x <= hri; x++) {
                    for (int z = -hri; z <= hri; z++) {
                        if (Math.abs(dist(x, z) - hr) < 0.5) set(level, cx + x, y, cz + z, GLASS);
                    }
                }
            }
        }
        set(level, cx, G + height, cz, TRIM);
        for (int i = 1; i <= 3; i++) {
            set(level, cx, G + height + i, cz, ROD);
        }
    }

    /** Clusters of green crystal growing from the basalt plains. */
    private static void crystals(ServerLevel level, RandomSource random, List<int[]> spires) {
        int count = 0;
        int attempts = 0;
        while (count < 36 && attempts++ < 500) {
            double a = random.nextDouble() * Math.PI * 2;
            double ring = 50 + random.nextDouble() * 55;
            int cx = (int) Math.round(Math.cos(a) * ring);
            int cz = (int) Math.round(Math.sin(a) * ring);
            boolean free = true;
            for (int[] s : spires) {
                if (dist(cx - s[0], cz - s[1]) < s[2] + 5) free = false;
            }
            if (!free) continue;
            set(level, cx, G - 1, cz, GLOW);
            int shards = 3 + random.nextInt(4);
            for (int i = 0; i < shards; i++) {
                int ox = random.nextInt(3) - 1;
                int oz = random.nextInt(3) - 1;
                int tall = 1 + random.nextInt(i == 0 ? 6 : 3);
                for (int y = 0; y < tall; y++) {
                    set(level, cx + ox, G + y, cz + oz, y == 0 && i == 0 ? TRIM : random.nextBoolean() ? GLASS : DEEP_GLASS);
                }
            }
            count++;
        }
    }

    // ---- inhabitants --------------------------------------------------------------------------------------

    private static void populate(ServerLevel level, RandomSource random) {
        for (int i = 0; i < PILLARS; i++) {
            BlockPos top = pillarTop(i);
            OaGuardianEntity.spawn(level, top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 0.5);
        }
        // Lanterns of every species strolling around the plaza...
        for (int i = 0; i < 12; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double ring = 16 + random.nextDouble() * 24;
            double x = Math.cos(a) * ring + 0.5;
            double z = Math.sin(a) * ring + 0.5;
            LanternCorpsmanEntity.spawnWalker(level, x, G, z, i % LanternCorpsmanEntity.VARIANTS, random.nextFloat() * 360.0F);
        }
        // ...and flying patrols around the Central Power Battery.
        for (int i = 0; i < 7; i++) {
            float radius = 14.0F + random.nextFloat() * 12.0F;
            float height = G + 12 + random.nextFloat() * 22.0F;
            float speed = (0.012F + random.nextFloat() * 0.01F) * (random.nextBoolean() ? 1 : -1);
            float phase = random.nextFloat() * Mth.TWO_PI;
            LanternCorpsmanEntity.spawnFlyer(level, radius, height, speed, phase, (i * 3 + 1) % LanternCorpsmanEntity.VARIANTS);
        }
    }
}
