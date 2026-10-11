package com.danrod505.greenlantern.client.cyborg;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.WorldLayer;
import com.danrod505.greenlantern.cyborg.CyborgConfig;
import com.danrod505.greenlantern.cyborg.CyborgHero;
import com.danrod505.greenlantern.cyborg.CyborgServer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Tech Scan on the client: while it is on (the Mother Box says so), the ores around show through
 * walls and the ground as red-tinted outlines, swept in like a radar. Enemies glow by the server's
 * doing (see {@code cyborg.CyborgServer}).
 */
public final class CyborgScanVisuals implements WorldLayer {
    /** Box face with bright edges and a faint center (shared with Superman's X-ray). */
    private static final Identifier BOX = GreenLantern.id("textures/misc/xray_box.png");
    private static final int MAX_BOXES = 600;
    /** The scan sweeps the sphere in this many ticks, one slice per tick. */
    private static final int SCAN_TICKS = 10;

    private record Found(BlockPos pos, int color) {}

    private List<Found> visible = List.of();
    private List<Found> scanning = new ArrayList<>();
    private int slice;
    private BlockPos center = BlockPos.ZERO;
    private float fade;
    private final List<float[]> boxes = new ArrayList<>();
    private float time;

    /** Whether the local player has Tech Scan on. */
    static boolean localScan(Player player) {
        if (player == null || !CyborgHero.INSTANCE.isSuited(player)) return false;
        ItemStack item = CyborgHero.INSTANCE.findItem(player);
        return !item.isEmpty() && CyborgServer.isScanning(item);
    }

    /** Every client tick: sweeps one slice of the sphere around the player. */
    void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            visible = List.of();
            fade = 0;
            return;
        }
        boolean on = localScan(mc.player);
        fade = Mth.approach(fade, on ? 1.0F : 0.0F, 0.1F);
        if (on) {
            step(level, mc.player);
        } else if (fade <= 0.0F && !visible.isEmpty()) {
            visible = List.of();
            scanning.clear();
            slice = 0;
        }
    }

    private void step(ClientLevel level, Player player) {
        int r = Math.min(32, (int) Math.round(CyborgConfig.SCAN_RADIUS.get()));
        if (slice == 0) {
            center = player.blockPosition();
            scanning = new ArrayList<>();
        }
        int size = 2 * r + 1;
        int from = -r + size * slice / SCAN_TICKS;
        int to = -r + size * (slice + 1) / SCAN_TICKS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r2 = r * r;
        for (int dx = from; dx < to; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2 || scanning.size() >= MAX_BOXES) continue;
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    int color = color(level.getBlockState(pos));
                    if (color != 0) scanning.add(new Found(pos.immutable(), color));
                }
            }
        }
        slice++;
        if (slice >= SCAN_TICKS) {
            visible = scanning;
            slice = 0;
        }
    }

    /** The colour an ore shows in the scan (Cyborg's red display tints them all a little), or 0. */
    static int color(BlockState state) {
        if (state.isAir()) return 0;
        if (state.is(BlockTags.DIAMOND_ORES)) return 0x70E8FF;
        if (state.is(BlockTags.EMERALD_ORES)) return 0x50FF80;
        if (state.is(BlockTags.GOLD_ORES)) return 0xFFC840;
        if (state.is(BlockTags.IRON_ORES)) return 0xF0A890;
        if (state.is(BlockTags.REDSTONE_ORES)) return 0xFF3020;
        if (state.is(BlockTags.LAPIS_ORES)) return 0x5070FF;
        if (state.is(BlockTags.COPPER_ORES)) return 0xFF8040;
        if (state.is(BlockTags.COAL_ORES)) return 0xA89898;
        if (state.is(Blocks.ANCIENT_DEBRIS)) return 0xC0602A;
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return 0xFFE8E0;
        return 0;
    }

    @Override
    public void extract(Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, float partialTick) {
        boxes.clear();
        time = level.getGameTime() + partialTick;
        if (fade <= 0.0F || visible.isEmpty()) return;
        // The radar sweep: ores light up as a ring grows from Cyborg every two seconds.
        double ring = (time % 40) / 40.0 * CyborgConfig.SCAN_RADIUS.get();
        for (Found found : visible) {
            double dist = Math.sqrt(found.pos().distToCenterSqr(cam));
            float ping = (float) Math.max(0.0, 1.0 - Math.abs(dist - ring) / 3.0);
            float x = (float) (found.pos().getX() - origin.x);
            float y = (float) (found.pos().getY() - origin.y);
            float z = (float) (found.pos().getZ() - origin.z);
            boxes.add(new float[] {x, y, z, Float.intBitsToFloat(found.color()), fade * (0.65F + 0.35F * ping)});
        }
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        if (boxes.isEmpty()) return;
        List<float[]> list = List.copyOf(boxes);
        collector.submitCustomGeometry(poseStack, RenderTypes.textSeeThrough(BOX), (pose, vc) -> {
            for (float[] b : list) {
                int rgb = Float.floatToRawIntBits(b[3]);
                int a = Mth.clamp((int) (230 * b[4]), 0, 255);
                box(vc, pose, b[0] + 0.04F, b[1] + 0.04F, b[2] + 0.04F, b[0] + 0.96F, b[1] + 0.96F, b[2] + 0.96F, a << 24 | rgb);
            }
        });
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        face(vc, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, color);
        face(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color);
        face(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color);
        face(vc, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, color);
        face(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color);
        face(vc, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, color);
    }

    /** A face in the text vertex format (position, colour, uv, light), seen from both sides. */
    private static void face(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        vertex(vc, pose, x0, y0, z0, 0, 1, color);
        vertex(vc, pose, x1, y1, z1, 1, 1, color);
        vertex(vc, pose, x2, y2, z2, 1, 0, color);
        vertex(vc, pose, x3, y3, z3, 0, 0, color);
        vertex(vc, pose, x3, y3, z3, 0, 0, color);
        vertex(vc, pose, x2, y2, z2, 1, 0, color);
        vertex(vc, pose, x1, y1, z1, 1, 1, color);
        vertex(vc, pose, x0, y0, z0, 0, 1, color);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(LightTexture.FULL_BRIGHT);
    }
}
