package com.danrod505.greenlantern.client.flight;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.FlightFlags;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.network.FlightActionPacket;
import com.danrod505.greenlantern.network.FlightStatePacket;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * Power flight of the local player. Like vanilla flight, movement is simulated on the client:
 * <ul>
 *     <li>Hold <b>forward</b> while flying and the speed keeps building up until the sound barrier
 *     is broken (sonic boom) and beyond. Sprint accelerates faster.</li>
 *     <li>The player flies where they look, with more inertia the faster they go.</li>
 *     <li>Double tap <b>left/right</b>: barrel roll dodge. Hold <b>back</b>: air brake flare.</li>
 *     <li>Start flying next to the ground: explosive take-off. Dive into the ground at high speed:
 *     hero landing shockwave.</li>
 * </ul>
 */
public final class FlightController {
    public static final int ROLL_TICKS = 14;
    private static final int DOUBLE_TAP_TICKS = 7;
    private static final double POWER_END_SPEED = 0.42;

    private static double speed;
    private static boolean power;
    private static boolean supersonic;
    private static boolean braking;
    private static int rollDir;
    private static int rollTick;
    private static boolean wasFlying;
    private static boolean prevLeft;
    private static boolean prevRight;
    private static int lastLeftTap = -100;
    private static int lastRightTap = -100;
    private static int tick;
    private static double descentSpeed;
    private static float savedFlyingSpeed = -1;
    private static int takeoffBoost;
    private static int sendTimer;
    private static int lastSentFlags = -1;
    private static float prevYaw;
    private static float bank;
    private static float bankO;
    private static float boomPunch;
    private static int boomFlash;
    /** The flight of the hero flying right now (Lantern or Superman), refreshed every tick. */
    private static FlightProfile profile;
    private static boolean superman;
    private static boolean amazon;

    private FlightController() {}

    // ---- queries used by visuals, HUD, camera and audio ------------------------------------------

    public static boolean isPowerFlying() {
        return power;
    }

    public static double speed() {
        return power ? speed : 0.0;
    }

    /** 0-1, relative to the configured top speed. */
    public static float speedFraction() {
        return power ? (float) Mth.clamp(speed / profile().max(), 0.0, 1.0) : 0.0F;
    }

    /** Speed in Mach (1.0 = sound barrier). */
    public static float mach() {
        return (float) (speed() / GLConfig.SOUND_BARRIER_SPEED.get());
    }

    /** Whether the local flyer is Superman (his own trail, colours and theme). */
    public static boolean isSuperman() {
        return superman;
    }

    /** Whether the local flyer is Wonder Woman (golden trail, red and gold colours, her own theme). */
    public static boolean isWonderWoman() {
        return amazon;
    }

    private static FlightProfile profile() {
        return profile != null ? profile : FlightProfile.lantern();
    }

    public static boolean isSupersonic() {
        return power && supersonic;
    }

    public static boolean isBraking() {
        return power && braking;
    }

    public static int flags() {
        if (!power) return 0;
        return FlightFlags.POWER | (supersonic ? FlightFlags.SUPERSONIC : 0) | (braking ? FlightFlags.BRAKING : 0);
    }

    /** Current barrel roll angle in degrees (signed), 0 when not rolling. */
    public static float rollAngle(float partialTick) {
        if (rollDir == 0) return 0.0F;
        float t = Mth.clamp((rollTick + partialTick) / ROLL_TICKS, 0.0F, 1.0F);
        float eased = t * t * (3 - 2 * t);
        return rollDir * 360.0F * eased;
    }

    public static float bank(float partialTick) {
        return Mth.lerp(partialTick, bankO, bank);
    }

    public static float boomPunch() {
        return boomPunch;
    }

    /** Ticks left of the white flash shown when breaking the sound barrier. */
    public static int boomFlash() {
        return boomFlash;
    }

    // ---- tick ------------------------------------------------------------------------------------

    /** Before the player moves (sets the velocity used by this tick's movement). */
    public static void preTick(LocalPlayer player) {
        tick++;
        if (boomPunch > 0) boomPunch = Math.max(0, boomPunch - 0.02F);
        if (boomFlash > 0) boomFlash--;

        Abilities abilities = player.getAbilities();
        boolean flying = abilities.flying && FlightProfile.canPowerFly(player) && !player.isPassenger() && !player.isSpectator();
        superman = FlightProfile.isSuperman(player);
        amazon = FlightProfile.isWonderWoman(player);
        profile = FlightProfile.of(player);
        if (!flying) {
            if (power) endPower(player);
            wasFlying = false;
            descentSpeed = 0;
            return;
        }

        // Explosive take-off when flight starts close to the ground.
        if (!wasFlying && distanceToGround(player, 2.5) < 2.5) {
            takeoff(player);
        }
        wasFlying = true;

        Input input = player.input.keyPresses;
        FlightProfile profile = profile();
        double cruise = profile.cruise();
        double barrier = profile.barrier();
        double max = profile.max();
        double accel = Math.max(0.001, (barrier - cruise) / (profile.seconds() * 20.0));
        Vec3 velocity = player.getDeltaMovement();

        if (input.forward() && !input.backward()) {
            if (!power) {
                power = true;
                speed = Math.max(velocity.length(), cruise * 0.8);
                if (savedFlyingSpeed < 0) savedFlyingSpeed = abilities.getFlyingSpeed();
            }
            double boost = input.sprint() || player.isSprinting() ? profile.sprintBoost() : 1.0;
            // Keeps accelerating past Mach 1, just more slowly.
            double soft = speed < barrier ? 1.0 : Mth.clamp(1.0 - 0.7 * (speed - barrier) / Math.max(0.01, max - barrier), 0.3, 1.0);
            speed = Math.min(max, speed + accel * boost * soft);
            braking = false;
        } else if (power) {
            if (input.backward()) {
                if (!braking && speed > 1.4) airBrake(player);
                braking = true;
                speed *= 0.84;
            } else {
                braking = false;
                speed *= 0.955;
            }
            if (speed < POWER_END_SPEED) endPower(player);
        }

        if (!power) {
            if (takeoffBoost > 0) {
                takeoffBoost--;
                player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.9), velocity.z);
            }
            prevLeft = input.left();
            prevRight = input.right();
            return;
        }

        // Vanilla flight acceleration is replaced by ours while power flying.
        abilities.setFlyingSpeed(0.0F);

        Vec3 look = player.getLookAngle();
        Vec3 dir = velocity.lengthSqr() > 0.0025 ? velocity.normalize() : look;
        double fraction = speed / max;
        // Superman turns on a dime even at top speed.
        double turn = Mth.lerp(fraction, 0.45, superman ? 0.2 : amazon ? 0.16 : 0.13);
        Vec3 newDir = dir.lerp(look, turn).normalize();
        Vec3 v = newDir.scale(speed);
        double lift = superman ? 0.35 : amazon ? 0.25 : 0.2;
        if (input.jump()) v = v.add(0, lift, 0);
        if (input.shift()) v = v.add(0, -lift, 0);

        // Barrel roll: double tap left/right.
        boolean left = input.left();
        boolean right = input.right();
        if (left && !prevLeft) {
            if (tick - lastLeftTap <= DOUBLE_TAP_TICKS) startRoll(player, -1);
            lastLeftTap = tick;
        }
        if (right && !prevRight) {
            if (tick - lastRightTap <= DOUBLE_TAP_TICKS) startRoll(player, 1);
            lastRightTap = tick;
        }
        prevLeft = left;
        prevRight = right;
        if (rollDir != 0) {
            rollTick++;
            Vec3 side = rightVector(newDir, player.getYRot());
            v = v.add(side.scale(rollDir * 0.55 * Math.sin(Math.PI * rollTick / ROLL_TICKS)));
            if (rollTick >= ROLL_TICKS) rollDir = 0;
        }
        if (takeoffBoost > 0) {
            takeoffBoost--;
            v = new Vec3(v.x, Math.max(v.y, 0.9), v.z);
        }

        player.setDeltaMovement(v);

        // Sound barrier (with hysteresis).
        if (!supersonic && speed >= barrier) {
            supersonic = true;
            sonicBoom(player, newDir);
        } else if (supersonic && speed < barrier * 0.92) {
            supersonic = false;
        }
        descentSpeed = v.y < -0.9 && speed > 1.3 ? -v.y : 0;
        sendState(false);
    }

    /** After the player moved: collisions, landing and camera banking. */
    public static void postTick(LocalPlayer player) {
        float yaw = player.getYRot();
        float yawDelta = Mth.wrapDegrees(yaw - prevYaw);
        prevYaw = yaw;
        bankO = bank;
        float target = power ? Mth.clamp(yawDelta * 1.6F * speedFraction() + yawDelta * 0.3F, -30.0F, 30.0F) : 0.0F;
        bank += (target - bank) * 0.12F;

        if (!power) return;
        if (player.onGround() && descentSpeed > 0) {
            heroLanding(player);
            return;
        }
        if (player.horizontalCollision && speed > (superman ? 2.6 : 1.6)) {
            // Crashed into a wall: the ring (or Kryptonian toughness) protects you, but you lose your momentum.
            CameraShake.start((float) Math.min(1.0, speed / 3.0), 12);
            play(ModSounds.BLAST_IMPACT.get(), 0.8F, 0.7F);
            speed *= 0.25;
        }
    }

    // ---- moves -----------------------------------------------------------------------------------

    private static void takeoff(LocalPlayer player) {
        takeoffBoost = 4;
        CameraShake.start(0.35F, 10);
        play(ModSounds.FLIGHT_TAKEOFF.get(), 1.0F, superman ? 0.8F : 1.0F);
        ModNetwork.sendToServer(new FlightActionPacket(FlightAction.TAKEOFF));
    }

    private static void sonicBoom(LocalPlayer player, Vec3 dir) {
        CameraShake.start(0.9F, 20);
        boomPunch = 0.35F;
        boomFlash = 6;
        play(superman ? ModSounds.SUPER_BOOM.get() : ModSounds.SONIC_BOOM.get(), 1.0F, 1.0F);
        spawnSonicRings(player, dir);
        ModNetwork.sendToServer(new FlightActionPacket(FlightAction.SONIC_BOOM));
    }

    /** Vapor cone rings left behind when a Lantern breaks the sound barrier (also used for other players). */
    public static void spawnSonicRings(net.minecraft.world.entity.player.Player player, Vec3 dir) {
        var level = player.level();
        boolean kryptonian = FlightProfile.isSuperman(player);
        boolean amazonian = FlightProfile.isWonderWoman(player);
        var ring = kryptonian || amazonian ? ModParticles.SUPER_RING.get() : ModParticles.SONIC_RING.get();
        var glow = amazonian ? ModParticles.AMAZON_SPARK.get() : kryptonian ? ModParticles.SOLAR_GLOW.get() : ModParticles.GLOW.get();
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 4; i++) {
            Vec3 p = c.subtract(dir.scale(0.8 + i * 1.6));
            level.addParticle(ring, p.x, p.y, p.z, dir.x, dir.y, dir.z);
        }
        for (int i = 0; i < 30; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            Vec3 side = rightVector(dir, player.getYRot());
            Vec3 up = side.cross(dir).normalize();
            Vec3 off = side.scale(Math.cos(a) * 1.4).add(up.scale(Math.sin(a) * 1.4));
            level.addParticle(glow, c.x + off.x, c.y + off.y, c.z + off.z, off.x * 0.15 - dir.x * 0.3, off.y * 0.15 - dir.y * 0.3, off.z * 0.15 - dir.z * 0.3);
        }
    }

    private static void startRoll(LocalPlayer player, int dir) {
        if (rollDir != 0 || speed < 0.8) return;
        rollDir = dir;
        rollTick = 0;
        play(ModSounds.FLIGHT_ROLL.get(), 0.9F, 1.0F);
        ModNetwork.sendToServer(new FlightActionPacket(dir < 0 ? FlightAction.ROLL_LEFT : FlightAction.ROLL_RIGHT));
    }

    private static void airBrake(LocalPlayer player) {
        CameraShake.start(0.3F, 8);
        play(ModSounds.FLIGHT_WHOOSH.get(), 1.0F, 0.7F);
        ModNetwork.sendToServer(new FlightActionPacket(FlightAction.AIR_BRAKE));
    }

    private static void heroLanding(LocalPlayer player) {
        CameraShake.start((float) Math.min(1.0, 0.5 + speed / 4.0), 18);
        play(ModSounds.HERO_LANDING.get(), 1.0F, 1.0F);
        ModNetwork.sendToServer(new FlightActionPacket(FlightAction.HERO_LANDING));
        FlightVisuals.local().heroLanding = 16;
        spawnLandingBurst(player);
        endPower(player);
        player.setDeltaMovement(Vec3.ZERO);
    }

    /** Immediate local burst for the hero landing (the server sends the shockwave to everyone else). */
    public static void spawnLandingBurst(net.minecraft.world.entity.player.Player player) {
        var level = player.level();
        var random = player.getRandom();
        boolean kryptonian = FlightProfile.isSuperman(player);
        boolean amazonian = FlightProfile.isWonderWoman(player);
        level.addParticle(amazonian ? ModParticles.AMAZON_SHOCKWAVE.get() : kryptonian ? ModParticles.SUPER_SHOCKWAVE.get() : ModParticles.SHOCKWAVE.get(), player.getX(), player.getY() + 0.1, player.getZ(), 0, 0, 0);
        level.addParticle(kryptonian ? ModParticles.SUPER_RING.get() : ModParticles.SONIC_RING.get(), player.getX(), player.getY() + 0.15, player.getZ(), 0, 1, 0);
        for (int i = 0; i < 40; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.3 + random.nextDouble() * 0.6;
            level.addParticle(amazonian ? ModParticles.AMAZON_SPARK.get() : kryptonian ? ModParticles.SOLAR_GLOW.get() : ModParticles.SPARK.get(), player.getX() + Math.cos(a) * r, player.getY() + 0.2, player.getZ() + Math.sin(a) * r,
                    Math.cos(a) * 0.6, 0.1 + random.nextDouble() * 0.3, Math.sin(a) * 0.6);
        }
        for (int i = 0; i < 16; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(kryptonian || amazonian ? ParticleTypes.CLOUD : ModParticles.GLOW.get(), player.getX() + Math.cos(a), player.getY() + 0.3, player.getZ() + Math.sin(a),
                    Math.cos(a) * 0.25, 0.05, Math.sin(a) * 0.25);
        }
    }

    private static void endPower(LocalPlayer player) {
        power = false;
        supersonic = false;
        braking = false;
        rollDir = 0;
        speed = 0;
        if (savedFlyingSpeed >= 0) {
            player.getAbilities().setFlyingSpeed(savedFlyingSpeed);
            savedFlyingSpeed = -1;
        }
        sendState(true);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static void sendState(boolean force) {
        int flags = flags();
        if (force || flags != lastSentFlags || ++sendTimer >= 2) {
            sendTimer = 0;
            lastSentFlags = flags;
            ModNetwork.sendToServer(new FlightStatePacket((float) speed(), (byte) flags));
        }
    }

    /** Player's right-hand direction for a movement direction (falls back to yaw when vertical). */
    public static Vec3 rightVector(Vec3 dir, float yaw) {
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            Vec3 flat = Vec3.directionFromRotation(0, yaw);
            right = new Vec3(-flat.z, 0, flat.x);
        }
        return right.normalize();
    }

    private static double distanceToGround(LocalPlayer player, double max) {
        for (double d = 0; d < max; d += 0.25) {
            if (!player.level().noCollision(player, player.getBoundingBox().move(0, -d - 0.05, 0))) return d;
        }
        return max;
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
