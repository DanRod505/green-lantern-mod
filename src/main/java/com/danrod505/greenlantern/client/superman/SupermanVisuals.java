package com.danrod505.greenlantern.client.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.client.render.HardLight;
import com.danrod505.greenlantern.network.SupermanSyncPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.superman.SuperFlags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client side of Superman's powers: which Superman around is using what (synced by the server),
 * the red beams of heat vision from both eyes, and X-ray vision: ores, chests and spawners show
 * through walls and the ground as glowing outlines (creatures glow too, see the server side).
 * <p>
 * The geometry is drawn by the trail renderer's client-only entity, which follows the camera.
 */
public final class SupermanVisuals {
    /** Neutral (white) ribbon texture, tinted per vertex. */
    private static final Identifier BEAM = GreenLantern.id("textures/entity/speed_trail.png");
    /** Box face with bright edges and a faint center: the outline of a block seen through walls. */
    private static final Identifier XRAY = GreenLantern.id("textures/misc/xray_box.png");
    private static final int MAX_BOXES = 700;
    /** The scan runs over this many ticks (one slice of the cube per tick). */
    private static final int SCAN_TICKS = 8;

    private record Entry(int flags, long time) {}

    /** A beam from an eye to the point it burns (relative to the frame origin). */
    record Beam(float x0, float y0, float z0, float x1, float y1, float z1, float width) {}

    /** A block shown through the walls. */
    record XBox(BlockPos pos, int color) {}

    /** What to draw this frame (filled while extracting the render state). */
    public static final class Frame {
        final List<Beam> beams = new ArrayList<>();
        final List<float[]> boxes = new ArrayList<>();
        /** Camera position relative to the frame origin. */
        Vec3 cam = Vec3.ZERO;
        float time;
    }

    private static final Map<Integer, Entry> STATES = new HashMap<>();
    private static List<XBox> visible = List.of();
    private static List<XBox> scanning = new ArrayList<>();
    private static int scanSlice;
    private static BlockPos scanCenter = BlockPos.ZERO;
    private static float xrayFade;

    private SupermanVisuals() {}

    public static int flags(int entityId) {
        Entry entry = STATES.get(entityId);
        return entry == null ? 0 : entry.flags();
    }

    /** Whether the local player has X-ray vision on. */
    public static boolean localXray() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && SuperFlags.has(flags(mc.player.getId()), SuperFlags.XRAY);
    }

    /** 0-1: how strongly the X-ray tint shows (fades in and out). */
    public static float xrayFade() {
        return xrayFade;
    }

    public static boolean localHeatVision() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && SuperFlags.has(flags(mc.player.getId()), SuperFlags.HEAT_VISION);
    }

    public static void onSync(SupermanSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (packet.flags() == 0) {
            STATES.remove(packet.entityId());
        } else {
            STATES.put(packet.entityId(), new Entry(packet.flags(), mc.level.getGameTime()));
        }
        if (packet.event() == SuperFlags.EVENT_PUNCH) {
            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity != null && mc.player != null) {
                double dist = mc.player.distanceTo(entity);
                if (dist < 32) CameraShake.start((float) Math.min(1.0, 1.1 * (1 - dist / 32)), 16);
            }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            STATES.clear();
            visible = List.of();
            xrayFade = 0;
            return;
        }
        long now = level.getGameTime();
        // A Superman out of view stops sending: forget him after a while.
        Iterator<Map.Entry<Integer, Entry>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().time() > 40) it.remove();
        }
        boolean xray = localXray();
        xrayFade = Mth.approach(xrayFade, xray ? 1.0F : 0.0F, 0.1F);
        if (xray) {
            scanStep(level, mc.player);
        } else if (!visible.isEmpty() && xrayFade <= 0.0F) {
            visible = List.of();
            scanning.clear();
            scanSlice = 0;
        }
        // Heat vision: the eyes glow and smoke a little.
        for (Map.Entry<Integer, Entry> e : STATES.entrySet()) {
            if (!SuperFlags.has(e.getValue().flags(), SuperFlags.HEAT_VISION)) continue;
            Entity entity = level.getEntity(e.getKey());
            if (!(entity instanceof Player player) || (player == mc.player && mc.options.getCameraType().isFirstPerson())) continue;
            if (player.getRandom().nextFloat() < 0.3F) {
                Vec3 eye = player.getEyePosition().add(player.getLookAngle().scale(0.3));
                level.addParticle(ModParticles.HEAT_SPARK.get(), eye.x, eye.y, eye.z, 0, 0.01, 0);
            }
        }
    }

    // ---- X-ray scan ------------------------------------------------------------------------------------

    private static void scanStep(ClientLevel level, Player player) {
        int r = GLConfig.XRAY_RADIUS.get();
        if (scanSlice == 0) {
            scanCenter = player.blockPosition();
            scanning = new ArrayList<>();
        }
        int size = 2 * r + 1;
        int from = -r + size * scanSlice / SCAN_TICKS;
        int to = -r + size * (scanSlice + 1) / SCAN_TICKS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r2 = r * r;
        for (int dx = from; dx < to; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2 || scanning.size() >= MAX_BOXES) continue;
                    pos.set(scanCenter.getX() + dx, scanCenter.getY() + dy, scanCenter.getZ() + dz);
                    int color = xrayColor(level.getBlockState(pos));
                    if (color != 0) scanning.add(new XBox(pos.immutable(), color));
                }
            }
        }
        scanSlice++;
        if (scanSlice >= SCAN_TICKS) {
            visible = scanning;
            scanSlice = 0;
        }
    }

    /** The colour a block shows in X-ray vision, or 0 when it is not worth seeing. */
    static int xrayColor(BlockState state) {
        if (state.isAir()) return 0;
        if (state.is(BlockTags.DIAMOND_ORES)) return 0x4CF0FF;
        if (state.is(BlockTags.EMERALD_ORES)) return 0x30FF6A;
        if (state.is(BlockTags.GOLD_ORES)) return 0xFFD030;
        if (state.is(BlockTags.IRON_ORES)) return 0xE8B898;
        if (state.is(BlockTags.REDSTONE_ORES)) return 0xFF3030;
        if (state.is(BlockTags.LAPIS_ORES)) return 0x3050FF;
        if (state.is(BlockTags.COPPER_ORES)) return 0xFF8A40;
        if (state.is(BlockTags.COAL_ORES)) return 0x9A9AA8;
        if (state.is(Blocks.ANCIENT_DEBRIS)) return 0xB0602A;
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return 0xF4ECE0;
        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL) || state.is(BlockTags.SHULKER_BOXES)) return 0xFFB020;
        if (state.is(Blocks.SPAWNER) || state.is(Blocks.TRIAL_SPAWNER)) return 0xD040FF;
        return 0;
    }

    // ---- render state ------------------------------------------------------------------------------------

    public static void extract(Frame frame, Minecraft mc, ClientLevel level, Vec3 cam, Vec3 origin, float partialTick) {
        frame.beams.clear();
        frame.boxes.clear();
        frame.cam = cam.subtract(origin);
        frame.time = level.getGameTime() + partialTick;
        for (Map.Entry<Integer, Entry> e : STATES.entrySet()) {
            if (!SuperFlags.has(e.getValue().flags(), SuperFlags.HEAT_VISION)) continue;
            if (level.getEntity(e.getKey()) instanceof Player player) addBeams(frame, mc, level, player, origin, partialTick);
        }
        if (xrayFade > 0.0F && !visible.isEmpty()) {
            float alpha = xrayFade;
            for (XBox box : visible) {
                float x = (float) (box.pos().getX() - origin.x);
                float y = (float) (box.pos().getY() - origin.y);
                float z = (float) (box.pos().getZ() - origin.z);
                frame.boxes.add(new float[] {x, y, z, Float.intBitsToFloat(box.color()), alpha});
            }
        }
    }

    private static void addBeams(Frame frame, Minecraft mc, ClientLevel level, Player player, Vec3 origin, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        double range = GLConfig.HEAT_VISION_RANGE.get();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e.isAlive() && e != player && !e.isSpectator() && e.isPickable(), 0.3F);
        if (hit != null) end = hit.getLocation();

        Vec3 right = new Vec3(-look.z, 0, look.x);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
        // In first person the beams come from just below the view, so they show converging on the target.
        double side = firstPerson ? 0.14 : 0.085;
        double drop = firstPerson ? -0.12 : 0.02;
        double ahead = firstPerson ? 0.25 : 0.27;
        float flicker = 0.85F + 0.15F * Mth.sin(frame.time * 2.1F + player.getId());
        for (int s = -1; s <= 1; s += 2) {
            Vec3 from = eye.add(right.scale(s * side)).add(up.scale(drop)).add(look.scale(ahead));
            frame.beams.add(new Beam((float) (from.x - origin.x), (float) (from.y - origin.y), (float) (from.z - origin.z),
                    (float) (end.x - origin.x), (float) (end.y - origin.y), (float) (end.z - origin.z), 0.06F * flicker));
        }
        // A spray of sparks where the beams meet (locally, every frame-tick, for responsiveness).
        if (player.getRandom().nextFloat() < 0.25F) {
            level.addParticle(ParticleTypes.SMALL_FLAME, end.x, end.y, end.z, 0, 0.02, 0);
        }
    }

    public static void submit(Frame frame, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!frame.beams.isEmpty()) {
            List<Beam> beams = List.copyOf(frame.beams);
            Vec3 cam = frame.cam;
            collector.submitCustomGeometry(poseStack, HardLight.type(BEAM), (pose, vc) -> {
                for (Beam beam : beams) {
                    // Glowing red halo, then the white-hot core.
                    ribbon(vc, pose, beam, cam, beam.width() * 3.2F, 0x90FF1A10);
                    ribbon(vc, pose, beam, cam, beam.width() * 1.6F, 0xD0FF5030);
                    ribbon(vc, pose, beam, cam, beam.width() * 0.6F, 0xFFFFF0D0);
                }
            });
        }
        if (!frame.boxes.isEmpty()) {
            List<float[]> boxes = List.copyOf(frame.boxes);
            float pulse = 0.8F + 0.2F * Mth.sin(frame.time * 0.25F);
            collector.submitCustomGeometry(poseStack, RenderTypes.textSeeThrough(XRAY), (pose, vc) -> {
                for (float[] b : boxes) {
                    int rgb = Float.floatToRawIntBits(b[3]);
                    int a = Mth.clamp((int) (230 * b[4] * pulse), 0, 255);
                    box(vc, pose, b[0] + 0.02F, b[1] + 0.02F, b[2] + 0.02F, b[0] + 0.98F, b[1] + 0.98F, b[2] + 0.98F, a << 24 | rgb);
                }
            });
        }
    }

    /** Camera-facing strip along a beam. */
    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, Beam beam, Vec3 cam, float width, int color) {
        Vec3 a = new Vec3(beam.x0(), beam.y0(), beam.z0());
        Vec3 b = new Vec3(beam.x1(), beam.y1(), beam.z1());
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = b.subtract(a).cross(cam.subtract(mid));
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(0, 1, 0);
        side = side.normalize().scale(width);
        float len = (float) a.distanceTo(b);
        HardLight.vertex(vc, pose, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z), 0, 0, color, 0, 1, 0);
        HardLight.vertex(vc, pose, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z), 0, 1, color, 0, 1, 0);
        HardLight.vertex(vc, pose, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z), len * 0.25F, 1, color, 0, 1, 0);
        HardLight.vertex(vc, pose, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z), len * 0.25F, 0, color, 0, 1, 0);
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        face(vc, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, color);
        face(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color);
        face(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color);
        face(vc, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, color);
        face(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color);
        face(vc, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, color);
    }

    /** A face in the text vertex format (position, colour, uv, light): seen from both sides. */
    private static void face(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        textVertex(vc, pose, x0, y0, z0, 0, 1, color);
        textVertex(vc, pose, x1, y1, z1, 1, 1, color);
        textVertex(vc, pose, x2, y2, z2, 1, 0, color);
        textVertex(vc, pose, x3, y3, z3, 0, 0, color);
        textVertex(vc, pose, x3, y3, z3, 0, 0, color);
        textVertex(vc, pose, x2, y2, z2, 1, 0, color);
        textVertex(vc, pose, x1, y1, z1, 1, 1, color);
        textVertex(vc, pose, x0, y0, z0, 0, 1, color);
    }

    private static void textVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(LightTexture.FULL_BRIGHT);
    }
}
