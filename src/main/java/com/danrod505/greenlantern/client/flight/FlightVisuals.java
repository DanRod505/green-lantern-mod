package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GLClientConfig;
import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.FlightFlags;
import com.danrod505.greenlantern.network.FlightSyncPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.flight.FlightProfile;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side visual state of every flying Lantern in view (the local player included): smoothed
 * speed and direction, superhero pose blend, barrel rolls and the energy trail.
 */
public final class FlightVisuals {
    public static final int TRAIL_POINTS = 48;

    /** A point of the energy trail. */
    public record TrailPoint(double x, double y, double z, float width, long time) {}

    public static final class Visual {
        public final int entityId;
        /** Smoothed speed (blocks/tick) and normalized flight direction. */
        public float speed;
        public Vec3 dir = new Vec3(0, 0, 1);
        public int flags;
        /** 0 = upright, 1 = fully horizontal superhero pose. */
        public float heroPose;
        public float heroPoseO;
        /** Aura strength 0-1. */
        public float aura;
        public int rollDir;
        public int rollTick;
        public int brakeTicks;
        public int heroLanding;
        /** Superman (white vapor trail, blue and red glow) rather than a Lantern (green hard light). */
        public boolean superman;
        /** Flying right now (power flight or hovering). */
        public boolean flying;
        /** Smoothed 0-1 version of {@link #flying} (the cape billows in and out). */
        public float hover;
        public final ArrayDeque<TrailPoint> trail = new ArrayDeque<>();
        float syncedSpeed;
        int syncedFlags;
        long lastSync = -1000;
        Vec3 lastPos;
        boolean active;

        Visual(int entityId) {
            this.entityId = entityId;
        }

        public float heroPose(float partialTick) {
            return Mth.lerp(partialTick, heroPoseO, heroPose);
        }

        public float rollAngle(float partialTick) {
            if (rollDir == 0) return 0.0F;
            float t = Mth.clamp((rollTick + partialTick) / FlightController.ROLL_TICKS, 0.0F, 1.0F);
            return rollDir * 360.0F * t * t * (3 - 2 * t);
        }

        public boolean supersonic() {
            return FlightFlags.has(flags, FlightFlags.SUPERSONIC);
        }
    }

    private static final Map<Integer, Visual> VISUALS = new HashMap<>();
    private static Visual local = new Visual(-1);
    private static long gameTime;

    private FlightVisuals() {}

    public static Visual get(int entityId) {
        return VISUALS.get(entityId);
    }

    /** Visual state of the local player. */
    public static Visual local() {
        return local;
    }

    public static Iterable<Visual> all() {
        return VISUALS.values();
    }

    /** Sync packet from the server about another player. */
    public static void onSync(FlightSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Visual visual = VISUALS.computeIfAbsent(packet.entityId(), Visual::new);
        visual.syncedSpeed = packet.speed();
        visual.syncedFlags = packet.flags();
        visual.lastSync = gameTime;
        if (packet.action() < 0) return;
        Entity entity = mc.level.getEntity(packet.entityId());
        if (!(entity instanceof Player player)) return;
        switch (FlightAction.byId(packet.action())) {
            case ROLL_LEFT -> startRoll(visual, -1);
            case ROLL_RIGHT -> startRoll(visual, 1);
            case AIR_BRAKE -> visual.brakeTicks = 10;
            case SONIC_BOOM -> FlightController.spawnSonicRings(player, visual.dir);
            case HERO_LANDING -> {
                visual.heroLanding = 16;
                FlightController.spawnLandingBurst(player);
                if (mc.player != null) {
                    double dist = mc.player.distanceTo(player);
                    if (dist < 20) com.danrod505.greenlantern.client.CameraShake.start((float) (0.8 * (1 - dist / 20)), 14);
                }
            }
            case TAKEOFF -> {
            }
        }
    }

    private static void startRoll(Visual visual, int dir) {
        visual.rollDir = dir;
        visual.rollTick = 0;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            VISUALS.clear();
            return;
        }
        gameTime = level.getGameTime();

        for (Visual visual : VISUALS.values()) visual.active = false;
        for (Player player : level.players()) {
            boolean isLocal = player == mc.player;
            Visual visual = VISUALS.computeIfAbsent(player.getId(), Visual::new);
            if (isLocal) local = visual;
            visual.active = true;
            update(level, player, visual, isLocal);
        }
        // Forget players that left view once their trail faded.
        Iterator<Visual> it = VISUALS.values().iterator();
        while (it.hasNext()) {
            Visual visual = it.next();
            if (!visual.active) {
                pruneTrail(visual, 0);
                if (visual.trail.isEmpty()) it.remove();
            }
        }
    }

    private static void update(ClientLevel level, Player player, Visual visual, boolean isLocal) {
        Vec3 pos = player.position();
        Vec3 delta = visual.lastPos == null ? Vec3.ZERO : pos.subtract(visual.lastPos);
        visual.lastPos = pos;
        boolean suited = FlightProfile.canPowerFly(player);
        visual.superman = FlightProfile.isSuperman(player);

        float targetSpeed;
        int flags;
        boolean flying;
        if (isLocal) {
            targetSpeed = (float) FlightController.speed();
            flags = FlightController.flags();
            flying = suited && player.getAbilities().flying;
        } else {
            boolean fresh = gameTime - visual.lastSync < 20;
            targetSpeed = fresh ? visual.syncedSpeed : 0.0F;
            flags = fresh ? visual.syncedFlags : 0;
            // Remote abilities are unknown: a suited player off the ground and moving counts as flying.
            flying = suited && (FlightFlags.has(flags, FlightFlags.POWER) || (!player.onGround() && delta.lengthSqr() > 0.01 && !player.isFallFlying()));
        }
        visual.flags = flags;
        visual.flying = flying;
        visual.hover += ((flying ? 1.0F : 0.0F) - visual.hover) * 0.12F;
        visual.speed += (targetSpeed - visual.speed) * 0.35F;

        Vec3 moveDir = isLocal ? player.getDeltaMovement() : delta;
        if (moveDir.lengthSqr() > 0.01) {
            visual.dir = visual.dir.lerp(moveDir.normalize(), 0.4).normalize();
        }

        visual.heroPoseO = visual.heroPose;
        if (visual.heroLanding > 0) {
            // Landed: snap upright into the kneeling hero landing pose.
            visual.heroPose = 0.0F;
            visual.heroPoseO = 0.0F;
        }
        boolean braking = FlightFlags.has(flags, FlightFlags.BRAKING) || visual.brakeTicks > 0;
        float poseTarget = braking ? 0.15F : Mth.clamp((visual.speed - 0.75F) / 0.9F, 0.0F, 1.0F);
        visual.heroPose += (poseTarget - visual.heroPose) * (poseTarget > visual.heroPose ? 0.12F : 0.2F);
        if (visual.brakeTicks > 0) visual.brakeTicks--;
        if (visual.heroLanding > 0) visual.heroLanding--;

        if (isLocal) {
            // The local player's roll comes straight from the controller.
            float angle = FlightController.rollAngle(0);
            visual.rollDir = angle == 0 ? 0 : (int) Math.signum(angle);
            visual.rollTick = angle == 0 ? 0 : Math.round(Math.abs(angle) / 360.0F * FlightController.ROLL_TICKS);
        } else if (visual.rollDir != 0 && ++visual.rollTick >= FlightController.ROLL_TICKS) {
            visual.rollDir = 0;
        }

        float auraTarget = flying && !visual.superman ? 0.45F + 0.55F * Mth.clamp(visual.speed / GLConfig.SOUND_BARRIER_SPEED.get().floatValue(), 0, 1) : 0.0F;
        visual.aura += (auraTarget - visual.aura) * 0.15F;

        // Trail.
        boolean trails = GLClientConfig.TRAILS.get();
        if (trails && flying && visual.speed > 0.35F) {
            float width = 0.18F + 0.55F * Mth.clamp(visual.speed / GLConfig.SOUND_BARRIER_SPEED.get().floatValue(), 0, 1.4F);
            Vec3 c = pos.add(0, player.getBbHeight() * 0.45, 0);
            visual.trail.addFirst(new TrailPoint(c.x, c.y, c.z, width, gameTime));
        }
        int life = visual.supersonic() ? 34 : 22;
        pruneTrail(visual, life);

        if (flying) spawnParticles(level, player, visual, delta);
    }

    /** Superman leaves no hard light behind: puffs of vapor at speed and the cone of the sound barrier. */
    private static void spawnSupermanParticles(ClientLevel level, Player player, Visual visual, Vec3 delta, Vec3 c, RandomSource random) {
        float speed = visual.speed;
        if (player.tickCount % 6 == 0 && speed < 0.6F) {
            level.addParticle(ModParticles.SOLAR_GLOW.get(), c.x + (random.nextDouble() - 0.5) * 0.8, c.y + (random.nextDouble() - 0.5) * 1.4,
                    c.z + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
        }
        if (speed < 1.0F) return;
        int steps = Mth.clamp((int) (delta.length() * 1.5), 1, 10);
        for (int i = 0; i < steps; i++) {
            Vec3 p = c.subtract(delta.scale((double) i / steps));
            if (random.nextFloat() < 0.5F) {
                Vec3 back = visual.dir.scale(-0.05 * speed);
                level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, p.x + random.nextGaussian() * 0.2, p.y + random.nextGaussian() * 0.2,
                        p.z + random.nextGaussian() * 0.2, back.x, back.y, back.z);
            }
        }
        if (visual.supersonic() && random.nextFloat() < 0.45F) {
            level.addParticle(ModParticles.SUPER_RING.get(), c.x - visual.dir.x * 1.4, c.y - visual.dir.y * 1.4, c.z - visual.dir.z * 1.4,
                    visual.dir.x * 0.4, visual.dir.y * 0.4, visual.dir.z * 0.4);
        }
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player && visual.supersonic() && mc.player != null && mc.player.distanceToSqr(player) < 144 && player.tickCount % 20 == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.FLIGHT_WHOOSH.get(), SoundSource.PLAYERS, 1.2F, 0.9F, false);
        }
    }

    private static void pruneTrail(Visual visual, int life) {
        while (visual.trail.size() > TRAIL_POINTS) visual.trail.removeLast();
        while (!visual.trail.isEmpty() && gameTime - visual.trail.peekLast().time() > life) visual.trail.removeLast();
    }

    private static void spawnParticles(ClientLevel level, Player player, Visual visual, Vec3 delta) {
        RandomSource random = player.getRandom();
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        float speed = visual.speed;
        if (visual.superman) {
            spawnSupermanParticles(level, player, visual, delta, c, random);
            return;
        }
        // Aura motes around the body.
        int motes = speed > 0.3F ? 2 : (player.tickCount % 3 == 0 ? 1 : 0);
        for (int i = 0; i < motes; i++) {
            level.addParticle(ModParticles.GLOW.get(), c.x + (random.nextDouble() - 0.5) * 0.9, c.y + (random.nextDouble() - 0.5) * 1.6,
                    c.z + (random.nextDouble() - 0.5) * 0.9, 0, 0.01, 0);
        }
        if (speed < 0.6F) return;
        // Streaks and sparks filling the path between the last two positions (no gaps at high speed).
        int steps = Mth.clamp((int) (delta.length() * 2), 1, 8);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            Vec3 p = c.subtract(delta.scale(t));
            Vec3 back = visual.dir.scale(-0.15 * speed);
            level.addParticle(ModParticles.STREAK.get(), p.x + random.nextGaussian() * 0.15, p.y + random.nextGaussian() * 0.15, p.z + random.nextGaussian() * 0.15,
                    back.x, back.y, back.z);
            if (random.nextFloat() < 0.4F) {
                level.addParticle(ModParticles.SPARK.get(), p.x + random.nextGaussian() * 0.3, p.y + random.nextGaussian() * 0.3, p.z + random.nextGaussian() * 0.3,
                        random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
            }
        }
        // Supersonic vapor cone flickering around the body.
        if (visual.supersonic() && random.nextFloat() < 0.35F) {
            level.addParticle(ModParticles.SONIC_RING.get(), c.x - visual.dir.x * 1.2, c.y - visual.dir.y * 1.2, c.z - visual.dir.z * 1.2,
                    visual.dir.x * 0.4, visual.dir.y * 0.4, visual.dir.z * 0.4);
        }
        // Remote supersonic Lanterns: a faint wind rush when they pass close by.
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player && visual.supersonic() && mc.player != null && mc.player.distanceToSqr(player) < 100 && player.tickCount % 20 == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.FLIGHT_WHOOSH.get(), SoundSource.PLAYERS, 1.0F, 1.2F, false);
        }
    }
}
