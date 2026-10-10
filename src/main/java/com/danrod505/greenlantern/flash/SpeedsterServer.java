package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.entity.SpeedLightningEntity;
import com.danrod505.greenlantern.entity.SpeedTornadoEntity;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SpeedSyncPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.FlightHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the Flash. Like power flight, running is simulated by the running client; the
 * server keeps the reported speed (Speed Force charge, lightning power), validates moves, relays
 * the visuals to the players nearby and owns the powers: tornado, molecular vibration (phasing)
 * and lightning.
 */
public final class SpeedsterServer {
    /** Ticks a reported fast speed stays valid for validating moves. */
    private static final int RECENT_TICKS = 20;

    public static final class State {
        public float speed;
        public int flags;
        long lastUpdate = -1000;
        float recentPeak;
        long recentPeakTime;
        long lastBoom = -1000;
        /** Ticks spent phasing, or -1 when not phasing. */
        int phaseTicks = -1;
        float pendingCharge;
        float pendingHunger;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private SpeedsterServer() {}

    public static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static void remove(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static boolean isPhasing(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.phaseTicks >= 0;
    }

    /** Running speed as a fraction of the configured top speed (0 when not running). */
    public static float speedFraction(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || player.level().getGameTime() - state.lastUpdate > 40 || !SpeedFlags.has(state.flags, SpeedFlags.RUNNING)) {
            return 0.0F;
        }
        return Mth.clamp(state.speed / GLConfig.RUN_MAX_SPEED.get().floatValue(), 0.0F, 1.0F);
    }

    private static int syncedFlags(ServerPlayer player, State state) {
        int flags = state.flags;
        if (state.phaseTicks >= 0) flags |= SpeedFlags.PHASING;
        if (SpeedTornadoEntity.find(player) != null) flags |= SpeedFlags.TORNADO;
        return flags;
    }

    /** Sends the speedster state of a player to everyone around them and to the player themselves. */
    public static void sync(ServerPlayer player, int action) {
        State state = get(player);
        ModNetwork.sendToTrackingAndSelf(player, new SpeedSyncPacket(player.getId(), state.speed, syncedFlags(player, state), action));
    }

    // ---- reports from the running client ---------------------------------------------------------

    public static void onState(ServerPlayer player, float speed, int flags) {
        if (!Float.isFinite(speed)) return;
        long now = player.level().getGameTime();
        boolean allowed = FlashHelper.isSuited(player) && !player.isPassenger();
        State state = get(player);
        state.speed = allowed ? Mth.clamp(speed, 0.0F, GLConfig.RUN_MAX_SPEED.get().floatValue()) : 0.0F;
        state.flags = allowed ? flags & SpeedFlags.CLIENT_MASK : 0;
        state.lastUpdate = now;
        if (state.speed >= state.recentPeak || now - state.recentPeakTime > RECENT_TICKS) {
            state.recentPeak = state.speed;
            state.recentPeakTime = now;
        }
        ModNetwork.sendToTracking(player, new SpeedSyncPacket(player.getId(), state.speed, syncedFlags(player, state), -1));
    }

    public static void onAction(ServerPlayer player, SpeedAction action) {
        if (!FlashHelper.isSuited(player)) return;
        ServerLevel level = player.level();
        State state = get(player);
        long now = level.getGameTime();
        float recent = now - state.recentPeakTime <= RECENT_TICKS ? state.recentPeak : state.speed;
        Vec3 pos = player.position();
        switch (action) {
            case START -> {
                playOthers(player, ModSounds.SPEED_START.get(), 1.0F, 1.0F);
                dust(level, pos, 1.0, 10);
            }
            case BOOM -> {
                if (recent < GLConfig.RUN_SOUND_BARRIER_SPEED.get() * 0.85 || now - state.lastBoom < 40) return;
                state.lastBoom = now;
                playOthers(player, ModSounds.SPEED_BOOM.get(), 4.0F, 0.95F + level.random.nextFloat() * 0.1F);
            }
            case SUPER_JUMP -> {
                playOthers(player, ModSounds.SUPER_JUMP.get(), 1.0F, 1.0F);
                dust(level, pos, 1.4, 16);
            }
            case SKID -> {
                playOthers(player, ModSounds.SPEED_SKID.get(), 1.0F, 1.0F);
                dust(level, pos, 0.8, 12);
            }
            case WALL_JUMP -> playOthers(player, ModSounds.SUPER_JUMP.get(), 0.8F, 1.2F);
            case LANDING -> {
                playOthers(player, ModSounds.SPEED_LANDING.get(), 1.0F, 1.0F);
                level.sendParticles(ModParticles.SPEED_RING.get(), pos.x, pos.y + 0.15, pos.z, 0, 0, 1, 0, 1);
                dust(level, pos, 2.0, 20);
            }
        }
        ModNetwork.sendToTracking(player, new SpeedSyncPacket(player.getId(), state.speed, syncedFlags(player, state), action.ordinal()));
    }

    // ---- every server tick -------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        boolean suited = FlashHelper.isSuited(player);
        if (player.tickCount % 20 == 0) FlashSuit.updateModifiers(player, suited);
        State state = STATES.get(player.getUUID());
        if (!suited) {
            if (state != null && state.phaseTicks >= 0) endPhase(player, true);
            return;
        }
        ItemStack ring = FlashHelper.findRing(player);
        if (ring.isEmpty()) return;
        if (state == null) state = get(player);

        // The Speed Force refills on its own, and much faster while running.
        float fraction = speedFraction(player);
        state.pendingCharge += (GLConfig.SPEED_FORCE_REGEN.get() + GLConfig.SPEED_FORCE_RUN_CHARGE.get() * fraction) / 20.0F;
        if (state.pendingCharge >= 1.0F) {
            int whole = (int) state.pendingCharge;
            state.pendingCharge -= whole;
            FlashHero.SPEED_FORCE.add(ring, whole);
        }
        // A speedster's metabolism: running makes you hungry (a little).
        if (fraction > 0 && !player.isCreative()) {
            state.pendingHunger += GLConfig.RUN_HUNGER.get().floatValue() * fraction / 20.0F;
            if (state.pendingHunger >= 0.05F) {
                player.causeFoodExhaustion(state.pendingHunger);
                state.pendingHunger = 0;
            }
        }

        if (state.phaseTicks >= 0) tickPhase(player, ring, state);
    }

    // ---- powers --------------------------------------------------------------------------------------

    /** Uses a power (right click with the ring or the power key). Returns whether something happened. */
    public static boolean usePower(ServerPlayer player, ItemStack ring, SpeedsterPower power) {
        if (!FlashHelper.isSuited(player) || player.isSpectator()) return false;
        // Toggles can always be switched off.
        if (power == SpeedsterPower.PHASE && isPhasing(player)) {
            endPhase(player, true);
            return true;
        }
        if (power == SpeedsterPower.TORNADO) {
            SpeedTornadoEntity tornado = SpeedTornadoEntity.find(player);
            if (tornado != null) {
                tornado.collapse();
                return true;
            }
        }
        if (player.getCooldowns().isOnCooldown(ring)) return false;
        if (!player.isCreative() && !FlashHero.SPEED_FORCE.has(ring, power.cost())) {
            notifyNoSpeedForce(player);
            return false;
        }
        boolean used = switch (power) {
            case TORNADO -> startTornado(player);
            case PHASE -> startPhase(player);
            case LIGHTNING -> throwLightning(player);
        };
        if (used) {
            if (!player.isCreative()) FlashHero.SPEED_FORCE.tryConsume(ring, power.cost());
            int cooldown = power == SpeedsterPower.LIGHTNING ? 14 : 20;
            player.getCooldowns().addCooldown(ring, cooldown);
        }
        return used;
    }

    public static void notifyNoSpeedForce(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_speed_force").withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static boolean startTornado(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
        Vec3 center = player.position().add(flat.scale(SpeedTornadoEntity.ORBIT_RADIUS));
        SpeedTornadoEntity tornado = SpeedTornadoEntity.create(level, player, center);
        level.addFreshEntity(tornado);
        level.playSound(null, center.x, center.y, center.z, ModSounds.TORNADO_START.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        sync(player, -1);
        return true;
    }

    private static boolean throwLightning(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(look.scale(0.6)).add(0, -0.25, 0);
        float power = 1.0F + 0.75F * speedFraction(player);
        float damage = GLConfig.LIGHTNING_DAMAGE.get().floatValue() * power;
        SpeedLightningEntity bolt = SpeedLightningEntity.create(level, player, start, look.scale(3.2), damage);
        level.addFreshEntity(bolt);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LIGHTNING_THROW.get(), SoundSource.PLAYERS, 1.2F, 0.95F + level.random.nextFloat() * 0.1F);
        level.sendParticles(ModParticles.SPEED_SPARK.get(), start.x, start.y, start.z, 14, 0.15, 0.15, 0.15, 0.2);
        return true;
    }

    // ---- molecular vibration -------------------------------------------------------------------------

    public static boolean startPhase(ServerPlayer player) {
        State state = get(player);
        if (state.phaseTicks >= 0) return false;
        state.phaseTicks = 0;
        player.setForcedPose(Pose.STANDING);
        // Hovering inside walls must not look like cheating to the server's flight check.
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHASE_START.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.9, 0.4, 0.15);
        player.displayClientMessage(Component.translatable("message.greenlantern.phase_start").withStyle(ChatFormatting.YELLOW), true);
        sync(player, -1);
        return true;
    }

    private static void tickPhase(ServerPlayer player, ItemStack ring, State state) {
        state.phaseTicks++;
        player.resetFallDistance();
        player.setForcedPose(Pose.STANDING);
        boolean outOfTime = state.phaseTicks > GLConfig.PHASE_MAX_SECONDS.get() * 20;
        boolean outOfForce = false;
        if (state.phaseTicks % 20 == 0 && !player.isCreative()) {
            outOfForce = !FlashHero.SPEED_FORCE.tryConsume(ring, GLConfig.PHASE_COST_PER_SECOND.get());
        }
        if (outOfTime || outOfForce) {
            if (outOfForce) notifyNoSpeedForce(player);
            endPhase(player, true);
        }
    }

    /**
     * Ends molecular vibration. If the player is inside solid matter (or fell below the world) they
     * are moved to the closest free spot with ground under their feet.
     */
    public static void endPhase(ServerPlayer player, boolean effects) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.phaseTicks < 0) return;
        state.phaseTicks = -1;
        player.setForcedPose(null);
        player.noPhysics = player.isSpectator();
        Vec3 safe = SafeSpot.find(player);
        if (safe != null) {
            player.teleportTo(safe.x, safe.y, safe.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
        player.resetFallDistance();
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHASE_END.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.9, 0.4, 0.1);
            if (safe != null) {
                player.displayClientMessage(Component.translatable("message.greenlantern.phase_rescued").withStyle(ChatFormatting.YELLOW), true);
            }
        }
        sync(player, -1);
    }

    // ---- helpers -------------------------------------------------------------------------------------

    /** Debris of the ground blocks around a point. */
    static void dust(ServerLevel level, Vec3 center, double radius, int count) {
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

    /** The running client plays its own sounds instantly; everyone else hears them from the server. */
    private static void playOthers(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
