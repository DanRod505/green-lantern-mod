package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.PhaseState;
import com.danrod505.greenlantern.flash.SpeedAction;
import com.danrod505.greenlantern.flash.SpeedFlags;
import com.danrod505.greenlantern.network.SpeedSyncPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side visual state of every speedster in view (the local player included): smoothed speed
 * and direction, running lean, the lightning trail and the particles around them.
 */
public final class SpeedVisuals {
    public static final int TRAIL_POINTS = 40;

    /** A point of the lightning trail (middle of the body). */
    public record TrailPoint(double x, double y, double z, long time) {}

    public static final class Visual {
        public final int entityId;
        /** Smoothed speed (blocks/tick) and normalized horizontal running direction. */
        public float speed;
        public Vec3 dir = new Vec3(0, 0, 1);
        public int flags;
        /** 0 = upright, 1 = full sprinting lean. */
        public float lean;
        public float leanO;
        /** 0-1 blend into the wall-running pose. */
        public float wall;
        public float wallO;
        public Vec3 wallNormal = new Vec3(0, 0, -1);
        public final ArrayDeque<TrailPoint> trail = new ArrayDeque<>();
        float syncedSpeed;
        int syncedFlags;
        long lastSync = -1000;
        Vec3 lastPos;
        boolean active;

        Visual(int entityId) {
            this.entityId = entityId;
        }

        public float lean(float partialTick) {
            return Mth.lerp(partialTick, leanO, lean);
        }

        public float wall(float partialTick) {
            return Mth.lerp(partialTick, wallO, wall);
        }

        public boolean has(int flag) {
            return SpeedFlags.has(flags, flag);
        }

        public boolean running() {
            return has(SpeedFlags.RUNNING);
        }

        /** Ticks a trail point stays, longer at supersonic speed. */
        public int trailLife() {
            return has(SpeedFlags.SUPERSONIC) ? 22 : 14;
        }
    }

    private static final Map<Integer, Visual> VISUALS = new HashMap<>();
    private static long gameTime;

    private SpeedVisuals() {}

    public static Visual get(int entityId) {
        return VISUALS.get(entityId);
    }

    public static Iterable<Visual> all() {
        return VISUALS.values();
    }

    /** Sync packet from the server about any player (the local one too, for phasing and the tornado). */
    public static void onSync(SpeedSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        PhaseState.setClient(packet.entityId(), SpeedFlags.has(packet.flags(), SpeedFlags.PHASING));
        Visual visual = VISUALS.computeIfAbsent(packet.entityId(), Visual::new);
        visual.syncedSpeed = packet.speed();
        visual.syncedFlags = packet.flags();
        visual.lastSync = gameTime;
        if (packet.action() < 0) return;
        Entity entity = mc.level.getEntity(packet.entityId());
        if (!(entity instanceof Player player) || player == mc.player) return;
        switch (SpeedAction.byId(packet.action())) {
            case BOOM -> SpeedController.spawnBoomRings(player, visual.dir);
            case LANDING -> {
                mc.level.addParticle(ModParticles.SPEED_RING.get(), player.getX(), player.getY() + 0.15, player.getZ(), 0, 1, 0);
                if (mc.player != null && mc.player.distanceTo(player) < 12) CameraShake.start(0.3F, 8);
            }
            case SUPER_JUMP, WALL_JUMP, START, SKID -> sparks(mc.level, player, 16);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            VISUALS.clear();
            PhaseState.clearClient();
            return;
        }
        gameTime = level.getGameTime();
        for (Visual visual : VISUALS.values()) visual.active = false;
        for (Player player : level.players()) {
            boolean isLocal = player == mc.player;
            if (!FlashHelper.isSuited(player) && !VISUALS.containsKey(player.getId())) continue;
            Visual visual = VISUALS.computeIfAbsent(player.getId(), Visual::new);
            visual.active = true;
            update(level, player, visual, isLocal);
        }
        // Forget players that left view once their trail faded.
        Iterator<Visual> it = VISUALS.values().iterator();
        while (it.hasNext()) {
            Visual visual = it.next();
            if (!visual.active) {
                prune(visual, 0);
                if (visual.trail.isEmpty()) it.remove();
            }
        }
    }

    private static void update(ClientLevel level, Player player, Visual visual, boolean isLocal) {
        Vec3 pos = player.position();
        Vec3 delta = visual.lastPos == null ? Vec3.ZERO : pos.subtract(visual.lastPos);
        visual.lastPos = pos;
        boolean suited = FlashHelper.isSuited(player);

        float targetSpeed;
        int flags;
        if (isLocal) {
            targetSpeed = (float) SpeedController.speed();
            flags = SpeedController.flags();
            if (PhaseState.isPhasing(player)) flags |= SpeedFlags.PHASING;
            if (SpeedController.isOrbiting()) flags |= SpeedFlags.TORNADO;
        } else {
            boolean fresh = gameTime - visual.lastSync < 20;
            targetSpeed = fresh ? visual.syncedSpeed : 0.0F;
            flags = fresh ? visual.syncedFlags : (visual.syncedFlags & SpeedFlags.PHASING);
        }
        if (!suited) {
            targetSpeed = 0;
            flags = 0;
        }
        visual.flags = flags;
        visual.speed += (targetSpeed - visual.speed) * 0.4F;

        Vec3 move = isLocal ? SpeedController.direction() : new Vec3(delta.x, 0, delta.z);
        if (move.lengthSqr() > 1.0E-3) {
            visual.dir = visual.dir.lerp(move.normalize(), 0.5);
            if (visual.dir.lengthSqr() < 1.0E-4) visual.dir = move.normalize();
            visual.dir = visual.dir.normalize();
        }

        visual.leanO = visual.lean;
        float leanTarget = visual.running() && !visual.has(SpeedFlags.WALL) ? Mth.clamp((visual.speed - 0.3F) / 1.6F, 0.0F, 1.0F) : 0.0F;
        visual.lean += (leanTarget - visual.lean) * 0.25F;
        visual.wallO = visual.wall;
        if (visual.has(SpeedFlags.WALL)) {
            visual.wallNormal = isLocal ? SpeedController.wallNormal() : visual.dir.scale(-1);
        }
        visual.wall += ((visual.has(SpeedFlags.WALL) ? 1.0F : 0.0F) - visual.wall) * 0.35F;

        // Lightning trail.
        if (visual.running() && visual.speed > 0.45F) {
            Vec3 c = pos.add(0, player.getBbHeight() * 0.5, 0);
            visual.trail.addFirst(new TrailPoint(c.x, c.y, c.z, gameTime));
        }
        prune(visual, visual.trailLife());

        RandomSource random = player.getRandom();
        if (!isLocal && visual.running() && visual.speed > 1.0F && random.nextFloat() < 0.4F) {
            level.addParticle(ModParticles.SPEED_SPARK.get(), player.getX() + random.nextGaussian() * 0.35, player.getY() + 0.3 + random.nextDouble() * 1.4,
                    player.getZ() + random.nextGaussian() * 0.35, 0, 0, 0);
        }
        if (visual.has(SpeedFlags.PHASING) && random.nextFloat() < 0.7F) {
            // Molecules buzzing: flickering sparks all over the body.
            level.addParticle(ModParticles.SPEED_SPARK.get(), player.getX() + random.nextGaussian() * 0.3, player.getY() + random.nextDouble() * player.getBbHeight(),
                    player.getZ() + random.nextGaussian() * 0.3, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
        }
    }

    private static void prune(Visual visual, int life) {
        while (visual.trail.size() > TRAIL_POINTS) visual.trail.removeLast();
        while (!visual.trail.isEmpty() && gameTime - visual.trail.peekLast().time() > life) visual.trail.removeLast();
    }

    private static void sparks(ClientLevel level, Player player, int count) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(ModParticles.SPEED_SPARK.get(), player.getX() + Math.cos(a) * 0.4, player.getY() + 0.2, player.getZ() + Math.sin(a) * 0.4,
                    Math.cos(a) * 0.3, 0.1 + random.nextDouble() * 0.2, Math.sin(a) * 0.3);
        }
    }
}
