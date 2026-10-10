package com.danrod505.greenlantern.client.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.network.GlideStatePacket;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Batman's cape glide (local player). Falling with the jump key held, the cape spreads into a
 * wing:
 * <ul>
 *     <li>Look down to dive and pick up speed; look up to trade that speed for height.</li>
 *     <li>Turn by looking around; S brakes. Let go of jump to fold the cape.</li>
 * </ul>
 * Like the other movement powers, it is simulated on the client (the velocity is set before vanilla
 * moves the player); the server only hears that the cape is open, to spare Batman the fall damage.
 */
public final class GlideController {
    /** Minimum height above the ground to open the cape. */
    private static final double MIN_HEIGHT = 2.2;

    private static boolean gliding;
    private static double speed;
    private static Vec3 heading = Vec3.ZERO;
    private static int glideTicks;
    /** Set by the screenshot script: glide as if the jump key were held. */
    public static boolean forceHold;

    private GlideController() {}

    public static boolean isGliding() {
        return gliding;
    }

    public static double speed() {
        return gliding ? speed : 0.0;
    }

    public static void preTick(LocalPlayer player) {
        Input input = player.input.keyPresses;
        boolean hold = input.jump() || forceHold;
        boolean can = BatmanHelper.isSuited(player) && !player.onGround() && !player.isInWater() && !player.isInLava()
                && !player.isPassenger() && !player.isSpectator() && !player.getAbilities().flying && !player.isFallFlying()
                && !GrappleController.isPulling();
        if (!can || !hold) {
            if (gliding) stop(player);
            return;
        }
        if (!gliding) {
            if (player.getDeltaMovement().y > -0.15 || nearGround(player)) return;
            start(player);
        }
        glideTicks++;

        float pitch = Mth.clamp(player.getXRot(), -30.0F, 70.0F);
        double base = GLConfig.GLIDE_SPEED.get();
        double dive = Math.max(base, GLConfig.GLIDE_DIVE_SPEED.get());
        double lift = 0.0;
        if (pitch >= 0.0F) {
            double t = pitch / 70.0;
            double target = base + (dive - base) * t;
            speed += (target - speed) * (0.03 + 0.05 * t);
        } else {
            // Pulling up: the speed carries Batman upwards, and runs out.
            double t = -pitch / 30.0;
            lift = t * Math.min(1.0, speed / base) * 0.11;
            speed -= 0.018 * t;
        }
        if (input.backward()) speed -= 0.02;
        speed = Mth.clamp(speed, 0.12, dive * 1.1);

        double vy = -GLConfig.GLIDE_SINK.get() - Math.sin(pitch * Mth.DEG_TO_RAD) * speed * 0.55 + lift;
        if (speed < base * 0.5) {
            // Stalling: the cape can't hold Batman up any more.
            double stall = 1.0 - speed / (base * 0.5);
            vy = Mth.lerp(stall, vy, -0.4);
        }
        vy = Math.max(vy, -1.2);

        Vec3 flat = Vec3.directionFromRotation(0, player.getYRot());
        heading = heading.lengthSqr() < 1.0E-4 ? flat : heading.lerp(flat, 0.12).normalize();
        player.setDeltaMovement(heading.x * speed, vy, heading.z * speed);
        player.resetFallDistance();
        spawnParticles(player);
    }

    /** After the player moved: crash into a wall and you lose the speed. */
    public static void postTick(LocalPlayer player) {
        if (!gliding) return;
        if (player.horizontalCollision) {
            if (speed > 0.8) CameraShake.start((float) Math.min(0.5, speed / 4.0), 6);
            speed *= 0.3;
        }
    }

    private static boolean nearGround(LocalPlayer player) {
        Vec3 from = player.position();
        HitResult hit = player.level().clip(new ClipContext(from, from.add(0, -MIN_HEIGHT, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, player));
        return hit.getType() != HitResult.Type.MISS;
    }

    private static void start(LocalPlayer player) {
        gliding = true;
        glideTicks = 0;
        Vec3 motion = player.getDeltaMovement();
        speed = Math.max(GLConfig.GLIDE_SPEED.get() * 0.8, motion.horizontalDistance());
        heading = Vec3.directionFromRotation(0, player.getYRot());
        ModNetwork.sendToServer(new GlideStatePacket(true));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CAPE_GLIDE.get(), 1.0F, 0.9F));
    }

    private static void stop(LocalPlayer player) {
        gliding = false;
        speed = 0;
        ModNetwork.sendToServer(new GlideStatePacket(false));
    }

    private static void spawnParticles(LocalPlayer player) {
        if (speed < 0.9 || glideTicks % 2 != 0) return;
        var random = player.getRandom();
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        for (int s = -1; s <= 1; s += 2) {
            // Air streaming off the cape's tips.
            Vec3 tip = player.position().add(0, 1.2, 0).add(side.scale(s * 1.1)).subtract(heading.scale(0.4));
            player.level().addParticle(ParticleTypes.CLOUD, tip.x, tip.y, tip.z,
                    -heading.x * 0.1 + random.nextGaussian() * 0.01, 0.0, -heading.z * 0.1 + random.nextGaussian() * 0.01);
        }
    }
}
