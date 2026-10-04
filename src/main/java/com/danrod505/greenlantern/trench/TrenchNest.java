package com.danrod505.greenlantern.trench;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * The shape of a nest of the Trench, everything Atlantis is not: black rock instead of white
 * marble, bone and flesh instead of gold and glass, a few dim, warm glows instead of sea lanterns.
 * <ul>
 *     <li>the pit: a jagged hole in the sea floor, its walls of deepslate, blackstone, basalt and
 *     sculk (carved column by column by {@link TrenchBuilder});</li>
 *     <li>a crown of black spires leaning over the edge;</li>
 *     <li>a ribcage of bone arching over the pit, roots hanging from it like tendrils;</li>
 *     <li>the brood mound in the middle of the pit floor, studded with glowing egg sacs;</li>
 *     <li>five caves running out of the pit walls into round chambers of flesh and bone: the
 *     nests proper, where the captured villagers hang in cocoons.</li>
 * </ul>
 * Everything is deterministic: the same nest always has the same shape.
 */
public final class TrenchNest {
    public static final int CHAMBERS = 5;
    /** Where the chambers are: this far beyond the pit edge. */
    private static final double CHAMBER_OUT = 11.0;
    private static final double CHAMBER_RX = 5.5;
    private static final double CHAMBER_RY = 4.0;
    private static final double TUNNEL_R = 2.3;
    private static final double SHELL = 1.4;

    /** Blocks of one chunk, in placement order (later entries win). */
    static final class ChunkPlan {
        final LongArrayList positions = new LongArrayList();
        final List<BlockState> states = new ArrayList<>();
    }

    private final Trench.Nest nest;
    private final Long2ObjectOpenHashMap<ChunkPlan> chunks = new Long2ObjectOpenHashMap<>();

    private TrenchNest(Trench.Nest nest) {
        this.nest = nest;
    }

    static TrenchNest plan(Trench.Nest nest) {
        TrenchNest plan = new TrenchNest(nest);
        RandomSource random = RandomSource.create(seed(nest));
        for (int i = 0; i < CHAMBERS; i++) plan.chamberShell(i);
        plan.spires(random);
        plan.ribs(random);
        plan.mound(random);
        for (int i = 0; i < CHAMBERS; i++) plan.chamberHollow(i, random);
        return plan;
    }

    ChunkPlan chunk(long key) {
        return chunks.get(key);
    }

    // ---- shared geometry ------------------------------------------------------------------------------

    static long seed(Trench.Nest nest) {
        return nest.x() * 341873128712L + nest.z() * 132897987541L + 0x7E4C;
    }

    /** Radius of the pit edge in the given direction (radians): jagged, like something tore it open. */
    static double pitRadius(Trench.Nest nest, double angle) {
        long s = seed(nest);
        double p1 = (s & 0xFF) / 40.0;
        double p2 = ((s >> 8) & 0xFF) / 40.0;
        double p3 = ((s >> 16) & 0xFF) / 40.0;
        return Trench.PIT_RADIUS + 3.5 * Math.sin(3 * angle + p1) + 1.8 * Math.sin(7 * angle + p2) + 1.0 * Math.sin(13 * angle + p3);
    }

    private static double chamberAngle(Trench.Nest nest, int i) {
        return i * Mth.TWO_PI / CHAMBERS + ((seed(nest) >> 24) & 0xFF) / 60.0;
    }

    /** Center of a chamber. */
    static Vec3 chamberCenter(Trench.Nest nest, int i) {
        double a = chamberAngle(nest, i);
        double r = pitRadius(nest, a) + CHAMBER_OUT;
        return new Vec3(nest.x() + 0.5 + Math.cos(a) * r, nest.floor() + 4.5, nest.z() + 0.5 + Math.sin(a) * r);
    }

    /** Where cocoons hang: two in each chamber, side by side across it. */
    public static List<Vec3> cocoonSpots(Trench.Nest nest) {
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < CHAMBERS; i++) {
            Vec3 c = chamberCenter(nest, i);
            double a = chamberAngle(nest, i);
            double sx = -Math.sin(a) * 2.2;
            double sz = Math.cos(a) * 2.2;
            spots.add(new Vec3(c.x + sx, nest.floor() + 2.0, c.z + sz));
            spots.add(new Vec3(c.x - sx, nest.floor() + 2.0, c.z - sz));
        }
        return spots;
    }

    /** The mouth of a chamber's cave, in the pit wall. */
    public static Vec3 mouth(Trench.Nest nest, int i) {
        double a = chamberAngle(nest, Math.floorMod(i, CHAMBERS));
        double r = pitRadius(nest, a) - 3.0;
        return new Vec3(nest.x() + 0.5 + Math.cos(a) * r, nest.floor() + 3.0, nest.z() + 0.5 + Math.sin(a) * r);
    }

    /** Where creatures come and go from above the pit, between two ribs (high above them). */
    public static Vec3 entry(Trench.Nest nest, double y) {
        double spine = ((seed(nest) >> 32) & 0xFF) / 40.0;
        double ax = Math.cos(spine);
        double az = Math.sin(spine);
        return new Vec3(nest.x() + 0.5 + ax * 3.0 - az * 8.0, y, nest.z() + 0.5 + az * 3.0 + ax * 8.0);
    }

    /** Where creatures come out of the deep: the back of each chamber. */
    public static Vec3 den(Trench.Nest nest, int i) {
        Vec3 c = chamberCenter(nest, Math.floorMod(i, CHAMBERS));
        return new Vec3(c.x, nest.floor() + 4.5, c.z);
    }

    // ---- placing ------------------------------------------------------------------------------------

    private void put(int x, int y, int z, BlockState state) {
        long key = ChunkPos.asLong(x >> 4, z >> 4);
        ChunkPlan plan = chunks.computeIfAbsent(key, k -> new ChunkPlan());
        plan.positions.add(BlockPos.asLong(x, y, z));
        plan.states.add(wet(state));
    }

    static BlockState wet(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, true) : state;
    }

    private static long hash(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xD1B54A32D192ED03L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 29);
    }

    static double roll(int x, int y, int z) {
        return Math.floorMod(hash(x, y, z), 10_000L) / 10_000.0;
    }

    // ---- palettes -----------------------------------------------------------------------------------

    static BlockState wallBlock(int x, int y, int z) {
        double r = roll(x, y, z);
        if (r < 0.30) return Blocks.DEEPSLATE.defaultBlockState();
        if (r < 0.52) return Blocks.BLACKSTONE.defaultBlockState();
        if (r < 0.68) return Blocks.BASALT.defaultBlockState();
        if (r < 0.80) return Blocks.SCULK.defaultBlockState();
        if (r < 0.90) return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        if (r < 0.97) return Blocks.TUFF.defaultBlockState();
        return Blocks.CRYING_OBSIDIAN.defaultBlockState();
    }

    static BlockState pitFloorBlock(int x, int y, int z) {
        double r = roll(x, y, z);
        if (r < 0.36) return Blocks.SCULK.defaultBlockState();
        if (r < 0.60) return Blocks.BLACKSTONE.defaultBlockState();
        if (r < 0.78) return Blocks.DEEPSLATE.defaultBlockState();
        if (r < 0.88) return Blocks.SOUL_SOIL.defaultBlockState();
        if (r < 0.96) return Blocks.BONE_BLOCK.defaultBlockState();
        return Blocks.NETHER_WART_BLOCK.defaultBlockState();
    }

    /** The dead sea floor of the territory: rock, sculk and bleached coral. */
    static BlockState deadFloorBlock(int x, int y, int z) {
        double r = roll(x, y, z);
        if (r < 0.26) return Blocks.TUFF.defaultBlockState();
        if (r < 0.46) return Blocks.DEEPSLATE.defaultBlockState();
        if (r < 0.60) return Blocks.SCULK.defaultBlockState();
        if (r < 0.72) return Blocks.GRAVEL.defaultBlockState();
        if (r < 0.80) return Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState();
        if (r < 0.88) return Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState();
        if (r < 0.95) return Blocks.BLACKSTONE.defaultBlockState();
        return Blocks.BONE_BLOCK.defaultBlockState();
    }

    static final BlockState[] DEAD_CORALS = {
            Blocks.DEAD_BRAIN_CORAL.defaultBlockState(), Blocks.DEAD_TUBE_CORAL.defaultBlockState(),
            Blocks.DEAD_HORN_CORAL.defaultBlockState(), Blocks.DEAD_FIRE_CORAL_FAN.defaultBlockState(),
            Blocks.DEAD_BUBBLE_CORAL_FAN.defaultBlockState(), Blocks.DEAD_BRAIN_CORAL_FAN.defaultBlockState()};

    private static BlockState fleshBlock(int x, int y, int z) {
        double r = roll(x, y, z);
        if (r < 0.40) return Blocks.NETHER_WART_BLOCK.defaultBlockState();
        if (r < 0.62) return Blocks.SCULK.defaultBlockState();
        if (r < 0.74) return Blocks.BONE_BLOCK.defaultBlockState();
        if (r < 0.85) return Blocks.MANGROVE_ROOTS.defaultBlockState();
        if (r < 0.90) return Blocks.SHROOMLIGHT.defaultBlockState();
        return Blocks.BLACKSTONE.defaultBlockState();
    }

    private static BlockState tunnelBlock(int x, int y, int z) {
        double r = roll(x, y, z);
        if (r < 0.40) return Blocks.SCULK.defaultBlockState();
        if (r < 0.70) return Blocks.BLACKSTONE.defaultBlockState();
        if (r < 0.88) return Blocks.DEEPSLATE.defaultBlockState();
        return Blocks.NETHER_WART_BLOCK.defaultBlockState();
    }

    // ---- the caves -----------------------------------------------------------------------------------

    /** Distance (in chamber radii) from a point to the chamber center: below 1 is inside. */
    private static double chamberDist(Vec3 c, double x, double y, double z, double grow) {
        double dx = (x - c.x) / (CHAMBER_RX + grow);
        double dy = (y - c.y) / (CHAMBER_RY + grow);
        double dz = (z - c.z) / (CHAMBER_RX + grow);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Distance from a point to the tunnel's axis (a segment from the pit wall to the chamber). */
    private double tunnelDist(int i, double x, double y, double z) {
        double a = chamberAngle(nest, i);
        double r0 = pitRadius(nest, a) - 3.0;
        Vec3 from = new Vec3(nest.x() + 0.5 + Math.cos(a) * r0, nest.floor() + 3.0, nest.z() + 0.5 + Math.sin(a) * r0);
        Vec3 to = chamberCenter(nest, i);
        Vec3 d = to.subtract(from);
        Vec3 p = new Vec3(x, y, z).subtract(from);
        double t = Mth.clamp(p.dot(d) / d.lengthSqr(), 0.0, 1.0);
        return p.subtract(d.scale(t)).length();
    }

    /** Inside the open pit (the lining of the caves stops at the pit wall). */
    private boolean inPit(int x, int z) {
        double dx = x + 0.5 - (nest.x() + 0.5);
        double dz = z + 0.5 - (nest.z() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) < pitRadius(nest, Math.atan2(dz, dx)) + 0.5;
    }

    private void chamberShell(int i) {
        Vec3 c = chamberCenter(nest, i);
        int rx = (int) Math.ceil(CHAMBER_RX + SHELL) + 1;
        int ry = (int) Math.ceil(CHAMBER_RY + SHELL) + 1;
        for (int x = (int) Math.floor(c.x) - rx; x <= (int) Math.floor(c.x) + rx; x++) {
            for (int y = (int) Math.floor(c.y) - ry; y <= (int) Math.floor(c.y) + ry; y++) {
                for (int z = (int) Math.floor(c.z) - rx; z <= (int) Math.floor(c.z) + rx; z++) {
                    if (chamberDist(c, x + 0.5, y + 0.5, z + 0.5, SHELL) <= 1.0) put(x, y, z, fleshBlock(x, y, z));
                }
            }
        }
        // The tunnel's lining.
        double a = chamberAngle(nest, i);
        double r0 = pitRadius(nest, a) - 3.0;
        double r1 = pitRadius(nest, a) + CHAMBER_OUT;
        int minX = (int) Math.floor(nest.x() + Math.min(Math.cos(a) * r0, Math.cos(a) * r1)) - 5;
        int maxX = (int) Math.floor(nest.x() + Math.max(Math.cos(a) * r0, Math.cos(a) * r1)) + 5;
        int minZ = (int) Math.floor(nest.z() + Math.min(Math.sin(a) * r0, Math.sin(a) * r1)) - 5;
        int maxZ = (int) Math.floor(nest.z() + Math.max(Math.sin(a) * r0, Math.sin(a) * r1)) + 5;
        for (int x = minX; x <= maxX; x++) {
            for (int y = nest.floor() - 2; y <= nest.floor() + 8; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double d = tunnelDist(i, x + 0.5, y + 0.5, z + 0.5);
                    if (d <= TUNNEL_R + 1.2 && chamberDist(c, x + 0.5, y + 0.5, z + 0.5, 0) > 1.0 && !inPit(x, z)) put(x, y, z, tunnelBlock(x, y, z));
                }
            }
        }
    }

    private void chamberHollow(int i, RandomSource random) {
        Vec3 c = chamberCenter(nest, i);
        BlockState water = Blocks.WATER.defaultBlockState();
        int rx = (int) Math.ceil(CHAMBER_RX) + 1;
        int ry = (int) Math.ceil(CHAMBER_RY) + 1;
        for (int x = (int) Math.floor(c.x) - rx; x <= (int) Math.floor(c.x) + rx; x++) {
            for (int y = (int) Math.floor(c.y) - ry; y <= (int) Math.floor(c.y) + ry; y++) {
                for (int z = (int) Math.floor(c.z) - rx; z <= (int) Math.floor(c.z) + rx; z++) {
                    if (chamberDist(c, x + 0.5, y + 0.5, z + 0.5, 0) <= 1.0) put(x, y, z, water);
                }
            }
        }
        double a = chamberAngle(nest, i);
        double r0 = pitRadius(nest, a) - 3.0;
        double r1 = pitRadius(nest, a) + CHAMBER_OUT;
        int minX = (int) Math.floor(nest.x() + Math.min(Math.cos(a) * r0, Math.cos(a) * r1)) - 4;
        int maxX = (int) Math.floor(nest.x() + Math.max(Math.cos(a) * r0, Math.cos(a) * r1)) + 4;
        int minZ = (int) Math.floor(nest.z() + Math.min(Math.sin(a) * r0, Math.sin(a) * r1)) - 4;
        int maxZ = (int) Math.floor(nest.z() + Math.max(Math.sin(a) * r0, Math.sin(a) * r1)) + 4;
        for (int x = minX; x <= maxX; x++) {
            for (int y = nest.floor() - 1; y <= nest.floor() + 7; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (tunnelDist(i, x + 0.5, y + 0.5, z + 0.5) <= TUNNEL_R) put(x, y, z, water);
                }
            }
        }
        // Bones and egg sacs on the chamber floor.
        for (int k = 0; k < 7; k++) {
            int x = (int) Math.floor(c.x + (random.nextDouble() - 0.5) * CHAMBER_RX * 1.4);
            int z = (int) Math.floor(c.z + (random.nextDouble() - 0.5) * CHAMBER_RX * 1.4);
            int y = (int) Math.floor(c.y - CHAMBER_RY + 1.0);
            while (chamberDist(c, x + 0.5, y - 0.5, z + 0.5, 0) <= 1.0 && y > c.y - CHAMBER_RY - 2) y--;
            put(x, y, z, k < 4 ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.SHROOMLIGHT.defaultBlockState());
        }
    }

    // ---- above the pit ------------------------------------------------------------------------------

    /** A crown of black spires around the pit, leaning outwards like claws. */
    private void spires(RandomSource random) {
        int count = 10;
        for (int i = 0; i < count; i++) {
            double a = i * Mth.TWO_PI / count + random.nextDouble() * 0.3;
            double r = pitRadius(nest, a) + 2.5 + random.nextDouble() * 4.0;
            double bx = nest.x() + 0.5 + Math.cos(a) * r;
            double bz = nest.z() + 0.5 + Math.sin(a) * r;
            int base = nest.rim() - 10;
            int above = 8 + random.nextInt(15);
            int top = Math.min(nest.waterTop() - 3, nest.rim() + above);
            double r0 = 1.6 + random.nextDouble() * 1.2;
            double leanX = Math.cos(a) * (1.5 + random.nextDouble() * 2.5);
            double leanZ = Math.sin(a) * (1.5 + random.nextDouble() * 2.5);
            for (int y = base; y <= top; y++) {
                double t = Mth.clamp((y - nest.rim()) / (double) Math.max(1, top - nest.rim()), 0.0, 1.0);
                double rad = Mth.lerp(t, r0, 0.45);
                double cx = bx + leanX * t * t;
                double cz = bz + leanZ * t * t;
                int ir = (int) Math.ceil(rad);
                for (int x = (int) Math.floor(cx) - ir; x <= (int) Math.floor(cx) + ir; x++) {
                    for (int z = (int) Math.floor(cz) - ir; z <= (int) Math.floor(cz) + ir; z++) {
                        double dx = x + 0.5 - cx;
                        double dz = z + 0.5 - cz;
                        if (dx * dx + dz * dz > rad * rad) continue;
                        BlockState state;
                        if (y >= top - 1) {
                            state = Blocks.BONE_BLOCK.defaultBlockState();
                        } else {
                            double rr = roll(x, y, z);
                            state = rr < 0.5 ? Blocks.BLACKSTONE.defaultBlockState()
                                    : rr < 0.8 ? Blocks.BASALT.defaultBlockState()
                                    : rr < 0.96 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                                    : Blocks.CRYING_OBSIDIAN.defaultBlockState();
                        }
                        put(x, y, z, state);
                    }
                }
            }
        }
    }

    /** A ball of bone (the ribs are drawn with them). */
    private void boneBall(double cx, double cy, double cz, double rad) {
        int ir = (int) Math.ceil(rad);
        for (int x = (int) Math.floor(cx) - ir; x <= (int) Math.floor(cx) + ir; x++) {
            for (int y = (int) Math.floor(cy) - ir; y <= (int) Math.floor(cy) + ir; y++) {
                for (int z = (int) Math.floor(cz) - ir; z <= (int) Math.floor(cz) + ir; z++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    double dz = z + 0.5 - cz;
                    if (dx * dx + dy * dy + dz * dz <= rad * rad) put(x, y, z, Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
        }
    }

    /** The ribcage: five arches of bone across the pit, joined by a spine, with roots hanging down. */
    private void ribs(RandomSource random) {
        double spine = ((seed(nest) >> 32) & 0xFF) / 40.0;
        double ax = Math.cos(spine);
        double az = Math.sin(spine);
        // Across the spine.
        double px = -az;
        double pz = ax;
        double foot = nest.rim() - 2;
        int top = nest.waterTop() - 4;
        for (int k = -2; k <= 2; k++) {
            double o = k * 6.0;
            double peak = Math.min(top, nest.rim() + 13 - Math.abs(k) * 2);
            double half = Math.sqrt(Math.max(1, Trench.PIT_RADIUS * Trench.PIT_RADIUS - o * o)) + 4.0;
            double ox = nest.x() + 0.5 + ax * o;
            double oz = nest.z() + 0.5 + az * o;
            for (double t = -1.0; t <= 1.0; t += 0.02) {
                double y = foot + (peak - foot) * Math.sqrt(Math.max(0, 1 - t * t));
                double x = ox + px * t * half;
                double z = oz + pz * t * half;
                boneBall(x, y, z, 1.1);
                // Roots hang from the ribs like tendrils.
                if (random.nextInt(40) == 0 && Math.abs(t) < 0.8) {
                    int len = 2 + random.nextInt(6);
                    for (int d = 1; d <= len; d++) {
                        put((int) Math.floor(x), (int) Math.floor(y) - 1 - d, (int) Math.floor(z), Blocks.MANGROVE_ROOTS.defaultBlockState());
                    }
                }
            }
        }
        // The spine, along the top of the arches.
        for (double s = -13.0; s <= 13.0; s += 0.4) {
            double k = s / 6.0;
            double peak = Math.min(top, nest.rim() + 13 - Math.abs(k) * 2);
            boneBall(nest.x() + 0.5 + ax * s, peak + 0.6, nest.z() + 0.5 + az * s, 1.3);
        }
    }

    /** The brood mound in the middle of the pit: flesh, sculk and bone, studded with glowing egg sacs. */
    private void mound(RandomSource random) {
        double cx = nest.x() + 0.5;
        double cz = nest.z() + 0.5;
        int base = nest.floor() - 1;
        double rad = 6.5;
        for (int x = nest.x() - 8; x <= nest.x() + 8; x++) {
            for (int z = nest.z() - 8; z <= nest.z() + 8; z++) {
                for (int y = base; y <= base + 6; y++) {
                    double dx = x + 0.5 - cx;
                    double dz = z + 0.5 - cz;
                    double dy = (y - base) * 1.25;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > rad) continue;
                    double rr = roll(x, y, z);
                    BlockState state;
                    if (d > rad - 1.2 && rr < 0.16) {
                        state = Blocks.SHROOMLIGHT.defaultBlockState();
                    } else if (rr < 0.48) {
                        state = Blocks.NETHER_WART_BLOCK.defaultBlockState();
                    } else if (rr < 0.76) {
                        state = Blocks.SCULK.defaultBlockState();
                    } else if (rr < 0.9) {
                        state = Blocks.BONE_BLOCK.defaultBlockState();
                    } else {
                        state = Blocks.MANGROVE_ROOTS.defaultBlockState();
                    }
                    put(x, y, z, state);
                }
            }
        }
        put(nest.x(), base + 6, nest.z(), Blocks.SCULK_CATALYST.defaultBlockState());
        // Bone spikes around the mound.
        for (int i = 0; i < 8; i++) {
            double a = i * Mth.TWO_PI / 8 + random.nextDouble() * 0.4;
            double r = 9 + random.nextDouble() * 5;
            int x = (int) Math.floor(cx + Math.cos(a) * r);
            int z = (int) Math.floor(cz + Math.sin(a) * r);
            int h = 2 + random.nextInt(4);
            for (int y = base; y < base + h; y++) put(x, y, z, Blocks.BONE_BLOCK.defaultBlockState());
        }
    }

    /** Chunk keys the nest (territory included) touches. */
    static List<Long> chunkKeys(Trench.Nest nest) {
        List<Long> keys = new ArrayList<>();
        int r = Trench.TERRITORY + 4;
        for (int cx = (nest.x() - r) >> 4; cx <= (nest.x() + r) >> 4; cx++) {
            for (int cz = (nest.z() - r) >> 4; cz <= (nest.z() + r) >> 4; cz++) {
                int nx = Mth.clamp(nest.x(), cx << 4, (cx << 4) + 15);
                int nz = Mth.clamp(nest.z(), cz << 4, (cz << 4) + 15);
                double d = Math.sqrt((double) (nx - nest.x()) * (nx - nest.x()) + (double) (nz - nest.z()) * (nz - nest.z()));
                if (d <= r) keys.add(ChunkPos.asLong(cx, cz));
            }
        }
        return keys;
    }
}
