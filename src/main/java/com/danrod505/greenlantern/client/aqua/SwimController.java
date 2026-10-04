package com.danrod505.greenlantern.client.aqua;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Aquaman's swimming (local player). In the water the suit turns swimming into flight: you go where
 * you look, in all three dimensions.
 * <ul>
 *     <li>W / S / A / D move along / against / across the view; jump rises, sneak dives.</li>
 *     <li>Hold sprint and the speed keeps building up to the top speed (a dash bursts at the start).</li>
 *     <li>Burst out of the surface at speed and you leap out of the water like a dolphin.</li>
 * </ul>
 * Like the Flash's running, the movement is simulated on the client (the velocity is set before
 * vanilla moves the player).
 */
public final class SwimController {
    private static boolean swimming;
    private static boolean sprinting;
    private static Vec3 velocity = Vec3.ZERO;
    private static double sprintSpeed;
    private static Vec3 prevPos = Vec3.ZERO;
    private static int leapCooldown;

    private SwimController() {}

    public static boolean isSwimming() {
        return swimming;
    }

    public static boolean isSprinting() {
        return swimming && sprinting;
    }

    public static double speed() {
        return swimming ? velocity.length() : 0.0;
    }

    /** 0-1, relative to the configured top (sprint) speed. */
    public static float speedFraction() {
        return (float) Mth.clamp(speed() / Math.max(0.1, GLConfig.SWIM_SPRINT_SPEED.get()), 0.0, 1.0);
    }

    public static Vec3 velocity() {
        return velocity;
    }

    /** Before the player moves: sets this tick's velocity. */
    public static void preTick(LocalPlayer player) {
        if (leapCooldown > 0) leapCooldown--;
        boolean active = AquamanHelper.isSuited(player) && player.isInWater() && !player.isPassenger() && !player.isSpectator()
                && !player.getAbilities().flying;
        if (!active) {
            if (swimming) stop(player);
            return;
        }
        Input input = player.input.keyPresses;
        if (!swimming) {
            swimming = true;
            velocity = player.getDeltaMovement();
        }
        prevPos = player.position();

        Vec3 look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 flat = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        double f = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        double s = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
        double u = (input.jump() ? 1 : 0) - (input.shift() ? 1 : 0);

        boolean wantSprint = f > 0 && (input.sprint() || player.isSprinting() || sprinting);
        double base = GLConfig.SWIM_SPEED.get();
        double top = Math.max(base, GLConfig.SWIM_SPRINT_SPEED.get());
        if (wantSprint) {
            if (!sprinting) dash(player);
            sprinting = true;
            double accel = (top - base) / (GLConfig.SWIM_SECONDS_TO_TOP_SPEED.get() * 20.0);
            sprintSpeed = Math.min(top, Math.max(sprintSpeed, base) + accel);
        } else {
            sprinting = false;
            sprintSpeed = Math.max(0, sprintSpeed - 0.15);
        }
        double speed = sprinting ? sprintSpeed : base;

        Vec3 wish = look.scale(f).add(side.scale(s * 0.75)).add(0, u * 0.8, 0);
        if (wish.lengthSqr() > 1.0) wish = wish.normalize();
        wish = wish.scale(speed);
        // Responsive, with a little glide: quick to answer, slower to stop at speed.
        double response = wish.lengthSqr() > 1.0E-4 ? (sprinting ? 0.22 : 0.3) : 0.12;
        velocity = velocity.lerp(wish, response);
        if (velocity.lengthSqr() < 1.0E-5) velocity = Vec3.ZERO;

        // Near the surface, racing upwards: leap out like a dolphin.
        if (velocity.y > 0.35 && !player.isUnderWater() && leapCooldown == 0 && velocity.length() > 0.9) {
            leap(player);
        }
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        spawnParticles(player);
    }

    /** After the player moved: lose the speed you crashed into a wall / the floor with; keep the swim pose. */
    public static void postTick(LocalPlayer player) {
        if (!swimming) return;
        if (player.horizontalCollision || player.verticalCollision) {
            Vec3 moved = player.position().subtract(prevPos);
            if (player.horizontalCollision) velocity = new Vec3(moved.x, velocity.y, moved.z);
            if (player.verticalCollision) velocity = new Vec3(velocity.x, moved.y, velocity.z);
            if (player.horizontalCollision && sprintSpeed > 1.2) {
                CameraShake.start((float) Math.min(0.6, sprintSpeed / 5.0), 8);
                sprintSpeed *= 0.5;
            }
        }
        // Moving fast underwater uses the vanilla swimming pose (lying flat, arms forward).
        boolean pose = velocity.length() > 0.25 && player.isUnderWater();
        if (player.isSprinting() != pose) player.setSprinting(pose);
    }

    private static void stop(LocalPlayer player) {
        swimming = false;
        sprinting = false;
        sprintSpeed = 0;
        // Leaving the water keeps the momentum (that's the leap), once.
        velocity = Vec3.ZERO;
    }

    private static void dash(LocalPlayer player) {
        play(ModSounds.SWIM_DASH.get(), 0.9F, 1.0F);
        CameraShake.start(0.15F, 5);
        Level level = player.level();
        var random = player.getRandom();
        for (int i = 0; i < 20; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.BUBBLE, player.getX() + Math.cos(a) * 0.5, player.getY() + 0.4 + random.nextDouble(),
                    player.getZ() + Math.sin(a) * 0.5, Math.cos(a) * 0.2, random.nextDouble() * 0.1, Math.sin(a) * 0.2);
        }
    }

    private static void leap(LocalPlayer player) {
        leapCooldown = 20;
        velocity = new Vec3(velocity.x, Math.min(1.6, velocity.y * 1.35 + 0.25), velocity.z);
        play(ModSounds.SWIM_DASH.get(), 0.8F, 1.3F);
        Level level = player.level();
        var random = player.getRandom();
        for (int i = 0; i < 30; i++) {
            level.addParticle(ParticleTypes.SPLASH, player.getX() + random.nextGaussian() * 0.5, player.getY() + 0.5,
                    player.getZ() + random.nextGaussian() * 0.5, random.nextGaussian() * 0.2, 0.3 + random.nextDouble() * 0.3, random.nextGaussian() * 0.2);
        }
    }

    private static void spawnParticles(LocalPlayer player) {
        double speed = velocity.length();
        if (speed < 0.5) return;
        Level level = player.level();
        var random = player.getRandom();
        // A wake of bubbles from the feet; spray when skimming the surface.
        Vec3 back = player.position().add(0, 0.3, 0).subtract(velocity.normalize().scale(0.6));
        int count = (int) Math.min(4, 1 + speed * 1.2);
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.BUBBLE, back.x + random.nextGaussian() * 0.25, back.y + random.nextGaussian() * 0.25,
                    back.z + random.nextGaussian() * 0.25, -velocity.x * 0.1, 0.02, -velocity.z * 0.1);
        }
        if (!player.isUnderWater()) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SPLASH, player.getX() + random.nextGaussian() * 0.4, player.getY() + 0.8,
                        player.getZ() + random.nextGaussian() * 0.4, -velocity.x * 0.2, 0.2 + random.nextDouble() * 0.2, -velocity.z * 0.2);
            }
        }
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
