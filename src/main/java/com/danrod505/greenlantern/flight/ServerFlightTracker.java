package com.danrod505.greenlantern.flight;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.network.FlightSyncPacket;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of power flight. Movement itself is simulated by the flying client (like vanilla
 * flight); the server keeps the reported speed for energy costs, validates special moves, applies
 * their gameplay effects (hero landing shockwave) and relays visuals to the players nearby.
 */
public final class ServerFlightTracker {
    /** Ticks a reported fast speed stays valid for validating moves such as the hero landing. */
    private static final int RECENT_TICKS = 20;

    public static final class State {
        public float speed;
        public int flags;
        long lastUpdate;
        float recentPeak;
        long recentPeakTime;
        long lastBoom = -1000;

        /** Game time of the last accepted sonic boom. */
        public long lastBoom() {
            return lastBoom;
        }
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private ServerFlightTracker() {}

    public static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void remove(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    /** Power-flight speed of the player as a fraction of the configured top speed (0 when not power flying). */
    public static float speedFraction(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || player.level().getGameTime() - state.lastUpdate > 40 || !FlightFlags.has(state.flags, FlightFlags.POWER)) {
            return 0.0F;
        }
        return Mth.clamp(state.speed / (float) FlightProfile.of(player).max(), 0.0F, 1.0F);
    }

    public static void onState(ServerPlayer player, float speed, int flags) {
        if (!Float.isFinite(speed)) return;
        long now = player.level().getGameTime();
        boolean allowed = FlightProfile.canPowerFly(player) && player.getAbilities().flying;
        State state = get(player);
        state.speed = allowed ? Mth.clamp(speed, 0.0F, (float) FlightProfile.of(player).max()) : 0.0F;
        state.flags = allowed ? flags & 0x7 : 0;
        state.lastUpdate = now;
        if (state.speed >= state.recentPeak || now - state.recentPeakTime > RECENT_TICKS) {
            state.recentPeak = state.speed;
            state.recentPeakTime = now;
        }
        ModNetwork.sendToTracking(player, new FlightSyncPacket(player.getId(), state.speed, (byte) state.flags, -1));
    }

    public static void onAction(ServerPlayer player, FlightAction action) {
        if (!FlightProfile.canPowerFly(player)) return;
        boolean superman = FlightProfile.isSuperman(player);
        ServerLevel level = player.level();
        State state = get(player);
        long now = level.getGameTime();
        float recent = now - state.recentPeakTime <= RECENT_TICKS ? state.recentPeak : state.speed;
        Vec3 pos = player.position();

        switch (action) {
            case TAKEOFF -> {
                playOthers(player, ModSounds.FLIGHT_TAKEOFF.get(), 1.0F, superman ? 0.8F : 1.0F);
                level.sendParticles(superman ? ModParticles.SUPER_SHOCKWAVE.get() : ModParticles.SHOCKWAVE.get(), pos.x, pos.y + 0.1, pos.z, 1, 0, 0, 0, 0);
                level.sendParticles(superman ? ModParticles.SOLAR_GLOW.get() : ModParticles.GLOW.get(), pos.x, pos.y + 0.2, pos.z, 30, 0.6, 0.1, 0.6, 0.08);
                dust(level, pos, superman ? 2.6 : 1.8, superman ? 26 : 16);
            }
            case SONIC_BOOM -> {
                // Requires actually being near the sound barrier; at most one boom every 2 seconds.
                if (recent < GLConfig.SOUND_BARRIER_SPEED.get() * 0.85 || now - state.lastBoom < 40) return;
                state.lastBoom = now;
                playOthers(player, superman ? ModSounds.SUPER_BOOM.get() : ModSounds.SONIC_BOOM.get(), 4.0F, 0.95F + level.random.nextFloat() * 0.1F);
            }
            case ROLL_LEFT, ROLL_RIGHT -> playOthers(player, ModSounds.FLIGHT_ROLL.get(), 0.9F, 1.0F);
            case AIR_BRAKE -> playOthers(player, ModSounds.FLIGHT_WHOOSH.get(), 1.0F, 0.7F);
            case HERO_LANDING -> {
                if (recent < 1.2F) {
                    GreenLantern.LOGGER.debug("Rejected hero landing of {} (recent speed {})", player.getName().getString(), recent);
                    return;
                }
                GreenLantern.LOGGER.debug("Hero landing of {} at speed {}", player.getName().getString(), recent);
                FlightProfile profile = FlightProfile.of(player);
                heroLanding(player, level, Mth.clamp(recent / (float) profile.max(), 0.3F, 1.0F), (float) profile.landingPower(), superman);
            }
        }
        ModNetwork.sendToTracking(player, new FlightSyncPacket(player.getId(), state.speed, (byte) state.flags, action.ordinal()));
    }

    private static void heroLanding(ServerPlayer player, ServerLevel level, float power, float multiplier, boolean superman) {
        Vec3 center = player.position();
        double radius = GLConfig.HERO_LANDING_RADIUS.get() * (0.6 + 0.4 * power) * Math.max(1.0F, (float) Math.sqrt(multiplier));
        float damage = GLConfig.HERO_LANDING_DAMAGE.get().floatValue() * (0.5F + 0.5F * power) * multiplier;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 2.5, radius))) {
            if (target == player || !target.isAlive()) continue;
            double dist = Math.sqrt(target.distanceToSqr(center.x, target.getY(), center.z));
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.6 * dist / radius);
            target.hurtServer(level, superman ? ModDamageTypes.superPunch(level, player) : ModDamageTypes.hardLight(level, player, player), damage * falloff);
            Vec3 away = new Vec3(target.getX() - center.x, 0, target.getZ() - center.z);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(1.1 * falloff);
            target.push(away.x, 0.45 * falloff + 0.15, away.z);
            target.hurtMarked = true;
        }
        playOthers(player, ModSounds.HERO_LANDING.get(), 1.6F, 1.0F);
        level.sendParticles(superman ? ModParticles.SUPER_SHOCKWAVE.get() : ModParticles.SHOCKWAVE.get(), center.x, center.y + 0.1, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(superman ? ModParticles.SOLAR_GLOW.get() : ModParticles.SPARK.get(), center.x, center.y + 0.3, center.z, 50, radius * 0.3, 0.2, radius * 0.3, 0.3);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z, 1, 0, 0, 0, 0);
        dust(level, center, radius, 36);
    }

    /** Debris of the ground blocks around a point. */
    private static void dust(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double r = level.random.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockPos pos = BlockPos.containing(x, center.y - 0.5, z);
            BlockState state = level.getBlockState(pos);
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 3, 0.2, 0.1, 0.2, 0.25);
            }
        }
    }

    /** The flying client plays its own sounds instantly; everyone else hears them from the server. */
    private static void playOthers(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
