package com.danrod505.greenlantern.client.speed;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.client.CameraShake;
import com.danrod505.greenlantern.entity.SpeedTornadoEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.PhaseState;
import com.danrod505.greenlantern.flash.SpeedAction;
import com.danrod505.greenlantern.flash.SpeedFlags;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.network.ModNetwork;
import com.danrod505.greenlantern.network.SpeedActionPacket;
import com.danrod505.greenlantern.network.SpeedStatePacket;
import com.danrod505.greenlantern.network.UsePowerPacket;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Super speed of the local player (the Flash). Like vanilla movement, running is simulated on the
 * client:
 * <ul>
 *     <li>Sprint while wearing the suit and the run starts; keep holding <b>forward</b> and the
 *     speed keeps building up, breaks the sound barrier (boom) and goes beyond.</li>
 *     <li>Fast enough, the Flash runs on water and, running into a wall, straight up it (jump to
 *     kick off the wall, keep going to vault over the top).</li>
 *     <li>Jump while running: super jump, higher the faster you go.</li>
 *     <li>Hold <b>back</b> to skid to a stop. Sneak slows down.</li>
 *     <li>With the tornado power active, the Flash runs in circles around the vortex.</li>
 *     <li>While phasing (molecular vibration) block collisions are ignored: hold jump / sneak to
 *     rise / sink, otherwise the Flash hovers instead of falling through the floor.</li>
 * </ul>
 */
public final class SpeedController {
    private static final double END_SPEED = 0.32;
    private static final double WATER_MIN_SPEED = 0.55;
    private static final double WALL_MIN_SPEED = 0.8;
    private static final double ORBIT_SPEED = 1.6;
    private static final double PHASE_WALK_SPEED = 0.32;

    private enum Mode { GROUND, AIR, WALL }

    private static boolean running;
    private static double speed;
    private static Vec3 dir = new Vec3(0, 0, 1);
    private static Mode mode = Mode.GROUND;
    private static boolean onWater;
    private static boolean supersonic;
    private static boolean orbiting;
    private static boolean skidding;
    private static Vec3 wallNormal = Vec3.ZERO;
    private static boolean prevJump;
    private static boolean prevShift;
    private static double airDescent;
    private static double prevY;
    private static double orbitAngle = Double.NaN;
    private static int sendTimer;
    private static int lastSentFlags = -1;
    private static float boomPunch;
    private static int boomFlash;
    private static int crackleTicks;

    private SpeedController() {}

    // ---- queries used by visuals, HUD, camera and audio ------------------------------------------

    public static boolean isRunning() {
        return running;
    }

    public static double speed() {
        return running ? speed : 0.0;
    }

    /** 0-1, relative to the configured top speed. */
    public static float speedFraction() {
        return running ? (float) Mth.clamp(speed / GLConfig.RUN_MAX_SPEED.get(), 0.0, 1.0) : 0.0F;
    }

    /** Speed in Mach (1.0 = the sound barrier). */
    public static float mach() {
        return (float) (speed() / GLConfig.RUN_SOUND_BARRIER_SPEED.get());
    }

    public static boolean isSupersonic() {
        return running && supersonic;
    }

    public static boolean isWallRunning() {
        return running && mode == Mode.WALL;
    }

    public static boolean isOrbiting() {
        return orbiting;
    }

    public static Vec3 direction() {
        return dir;
    }

    public static Vec3 wallNormal() {
        return wallNormal;
    }

    public static int flags() {
        if (!running) return 0;
        int flags = SpeedFlags.RUNNING;
        if (supersonic) flags |= SpeedFlags.SUPERSONIC;
        if (mode == Mode.WALL) flags |= SpeedFlags.WALL;
        if (mode == Mode.AIR) flags |= SpeedFlags.AIR;
        if (onWater) flags |= SpeedFlags.WATER;
        return flags;
    }

    public static float boomPunch() {
        return boomPunch;
    }

    public static int boomFlash() {
        return boomFlash;
    }

    // ---- tick ------------------------------------------------------------------------------------

    /** Before the player moves (sets the velocity used by this tick's movement). */
    public static void preTick(LocalPlayer player) {
        if (boomPunch > 0) boomPunch = Math.max(0, boomPunch - 0.02F);
        if (boomFlash > 0) boomFlash--;
        prevY = player.getY();

        boolean suited = FlashHelper.isSuited(player) && !player.isPassenger() && !player.isSpectator() && !player.getAbilities().flying;
        boolean phasing = suited && PhaseState.isPhasing(player);
        if (phasing && player.getAbilities().flying) player.getAbilities().flying = false;
        Input input = player.input.keyPresses;
        boolean jumpEdge = input.jump() && !prevJump;
        boolean shiftEdge = input.shift() && !prevShift;
        prevJump = input.jump();
        prevShift = input.shift();
        if (!suited) {
            if (running) end();
            orbiting = false;
            orbitAngle = Double.NaN;
            return;
        }

        // The tornado has priority: run in circles around it.
        SpeedTornadoEntity tornado = ownTornado(player);
        if (tornado != null) {
            orbit(player, tornado, shiftEdge);
            return;
        }
        if (orbiting) {
            orbiting = false;
            orbitAngle = Double.NaN;
            speed = Math.max(speed, GLConfig.RUN_START_SPEED.get());
        }

        boolean forward = input.forward() && !input.backward();
        double surface = waterSurface(player);
        boolean nearSurface = !Double.isNaN(surface) && player.getY() > surface - 1.6;
        boolean grounded = player.onGround() || onWater || phasing || (player.isInWater() && nearSurface);
        if (!running) {
            if (forward && (input.sprint() || player.isSprinting()) && grounded) {
                start(player);
            } else {
                if (phasing) phaseWalk(player, input);
                return;
            }
        }

        double start = GLConfig.RUN_START_SPEED.get();
        double barrier = GLConfig.RUN_SOUND_BARRIER_SPEED.get();
        double max = Math.max(barrier, GLConfig.RUN_MAX_SPEED.get());
        double accel = Math.max(0.001, (barrier - start) / (GLConfig.RUN_SECONDS_TO_SOUND_BARRIER.get() * 20.0));
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;

        if (mode == Mode.WALL && !phasing) {
            wallRun(player, input, jumpEdge);
            afterSpeedChange(player, barrier);
            return;
        }

        // ---- speed ----
        if (forward) {
            if (mode == Mode.GROUND || phasing) {
                // Keeps accelerating past the sound barrier, just more slowly.
                double soft = speed < barrier ? 1.0 : Mth.clamp(1.0 - 0.75 * (speed - barrier) / Math.max(0.01, max - barrier), 0.25, 1.0);
                speed = Math.min(max, speed + accel * soft);
            } else {
                speed *= 0.998;
            }
            skidding = false;
        } else if (input.backward() && mode == Mode.GROUND) {
            if (!skidding && speed > 1.2) skid(player);
            skidding = true;
            speed *= 0.8;
        } else {
            speed *= mode == Mode.GROUND ? 0.86 : 0.995;
        }
        if (input.shift() && !phasing) speed *= 0.75;
        if (speed < END_SPEED && mode != Mode.AIR) {
            end();
            if (phasing) phaseWalk(player, input);
            return;
        }

        // ---- direction: follow the look direction, with more inertia the faster you go ----
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        double fraction = speed / max;
        double turn = mode == Mode.AIR ? 0.07 : Mth.lerp(fraction, 0.55, 0.2);
        Vec3 turned = dir.lerp(look, turn);
        dir = turned.lengthSqr() < 1.0E-4 ? look : turned.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        double strafe = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
        Vec3 horizontal = dir.scale(speed).add(side.scale(strafe * Math.min(0.3, speed * 0.15)));

        // ---- vertical: water, super jump, ground hugging, phasing ----
        onWater = false;
        if (phasing) {
            vy = input.jump() ? 0.3 : (input.shift() ? -0.3 : 0.0);
            mode = Mode.GROUND;
        } else {
            boolean runOnWater = nearSurface && speed >= WATER_MIN_SPEED
                    && ((mode != Mode.AIR && vy <= 0.1 && player.getY() < surface + 0.25) || (mode == Mode.AIR && vy < 0 && player.getY() < surface + 0.4));
            if (runOnWater) {
                // Fast enough to run on water: stay on the surface.
                if (player.getY() < surface + 0.01) player.setPos(player.getX(), surface + 0.01, player.getZ());
                vy = 0;
                onWater = true;
                if (mode == Mode.AIR) land(player);
                mode = Mode.GROUND;
            }
            if (jumpEdge && mode == Mode.GROUND && (player.onGround() || onWater)) {
                vy = superJump(player, fraction);
                onWater = false;
            } else if (mode == Mode.GROUND && !onWater && !player.onGround()) {
                // Hug the ground over small drops instead of flying off every slope.
                double drop = distanceToGround(player, 1.6);
                if (drop < 1.6 && vy <= 0.05) {
                    vy = -(drop + 0.05);
                } else {
                    mode = Mode.AIR;
                    airDescent = 0;
                }
            }
        }

        player.setDeltaMovement(horizontal.x, vy, horizontal.z);
        afterSpeedChange(player, barrier);
        spawnRunParticles(player, horizontal);
    }

    /** After the player moved: walls, crashes, landings and the sprint flag. */
    public static void postTick(LocalPlayer player) {
        if (!running) return;
        // Vanilla sprinting would burn food by the distance run: speedsters use their own metabolism.
        player.setSprinting(false);
        if (orbiting) return;
        boolean phasing = PhaseState.isPhasing(player);
        Input input = player.input.keyPresses;
        boolean forward = input.forward() && !input.backward();

        if (mode != Mode.WALL && player.horizontalCollision && !phasing) {
            Vec3 normal = wallNormal(player);
            if (normal != null && forward && speed >= WALL_MIN_SPEED && dir.dot(normal.scale(-1)) > 0.5) {
                startWall(player, normal);
            } else if (speed > 1.5) {
                // Crashed into something sideways: you keep your bones, not your momentum.
                CameraShake.start((float) Math.min(1.0, speed / 4.0), 10);
                play(ModSounds.SPEED_SKID.get(), 0.8F, 0.6F);
                speed *= 0.35;
            }
        }
        if (mode == Mode.WALL && player.verticalCollision && player.getDeltaMovement().y >= 0) {
            // Hit a ceiling (or an overhang) while running up a wall.
            mode = Mode.AIR;
            airDescent = 0;
        }
        if (mode == Mode.AIR) {
            airDescent = Math.max(airDescent, prevY - player.getY());
            if (player.onGround()) {
                land(player);
                mode = Mode.GROUND;
            }
        }
    }

    // ---- moves -----------------------------------------------------------------------------------

    private static void start(LocalPlayer player) {
        running = true;
        mode = player.onGround() || player.isInWater() ? Mode.GROUND : Mode.AIR;
        Vec3 velocity = player.getDeltaMovement();
        speed = Math.max(velocity.horizontalDistance(), GLConfig.RUN_START_SPEED.get());
        dir = Vec3.directionFromRotation(0, player.getYRot());
        supersonic = false;
        skidding = false;
        CameraShake.start(0.25F, 6);
        play(ModSounds.SPEED_START.get(), 1.0F, 1.0F);
        burst(player, 18);
        ModNetwork.sendToServer(new SpeedActionPacket(SpeedAction.START));
    }

    private static void end() {
        running = false;
        speed = 0;
        mode = Mode.GROUND;
        supersonic = false;
        onWater = false;
        skidding = false;
        sendState(true);
    }

    private static void afterSpeedChange(LocalPlayer player, double barrier) {
        // Sound barrier, with hysteresis.
        if (!supersonic && speed >= barrier) {
            supersonic = true;
            boom(player);
        } else if (supersonic && speed < barrier * 0.92) {
            supersonic = false;
        }
        sendState(false);
    }

    private static void boom(LocalPlayer player) {
        CameraShake.start(0.9F, 18);
        boomPunch = 0.3F;
        boomFlash = 6;
        play(ModSounds.SPEED_BOOM.get(), 1.0F, 1.0F);
        spawnBoomRings(player, dir);
        ModNetwork.sendToServer(new SpeedActionPacket(SpeedAction.BOOM));
    }

    /** Golden rings and a burst of lightning left behind at the sound barrier (also used for other players). */
    public static void spawnBoomRings(net.minecraft.world.entity.player.Player player, Vec3 dir) {
        Level level = player.level();
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 4; i++) {
            Vec3 p = c.subtract(dir.scale(0.8 + i * 1.6));
            level.addParticle(ModParticles.SPEED_RING.get(), p.x, p.y, p.z, dir.x, dir.y, dir.z);
        }
        for (int i = 0; i < 40; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            Vec3 off = side.scale(Math.cos(a) * 1.3).add(0, Math.sin(a) * 1.3, 0);
            level.addParticle(ModParticles.SPEED_SPARK.get(), c.x + off.x, c.y + off.y, c.z + off.z,
                    off.x * 0.2 - dir.x * 0.4, off.y * 0.2, off.z * 0.2 - dir.z * 0.4);
        }
    }

    private static double superJump(LocalPlayer player, double fraction) {
        mode = Mode.AIR;
        airDescent = 0;
        CameraShake.start(0.3F, 6);
        play(ModSounds.SUPER_JUMP.get(), 1.0F, 0.9F + (float) fraction * 0.3F);
        burst(player, 24);
        ModNetwork.sendToServer(new SpeedActionPacket(SpeedAction.SUPER_JUMP));
        return Math.min(1.6, (0.75 + 0.7 * fraction) * GLConfig.SUPER_JUMP_POWER.get());
    }

    private static void skid(LocalPlayer player) {
        play(ModSounds.SPEED_SKID.get(), 1.0F, 1.0F);
        CameraShake.start(0.25F, 8);
        ModNetwork.sendToServer(new SpeedActionPacket(SpeedAction.SKID));
    }

    private static void land(LocalPlayer player) {
        if (airDescent > 0.9) {
            CameraShake.start((float) Math.min(0.8, 0.3 + airDescent * 0.2), 12);
            play(ModSounds.SPEED_LANDING.get(), 1.0F, 1.0F);
            player.level().addParticle(ModParticles.SPEED_RING.get(), player.getX(), player.getY() + 0.15, player.getZ(), 0, 1, 0);
            burst(player, 30);
            ModNetwork.sendToServer(new SpeedActionPacket(SpeedAction.LANDING));
        }
        airDescent = 0;
    }

    // ---- wall run --------------------------------------------------------------------------------

    private static void startWall(LocalPlayer player, Vec3 normal) {
        mode = Mode.WALL;
        wallNormal = normal;
        CameraShake.start(0.2F, 6);
        play(ModSounds.SUPER_JUMP.get(), 0.7F, 1.3F);
    }

    private static void wallRun(LocalPlayer player, Input input, boolean jumpEdge) {
        boolean forward = input.forward() && !input.backward();
        Vec3 into = wallNormal.scale(-1);
        boolean touching = !player.level().noCollision(player, player.getBoundingBox().move(into.x * 0.2, 0.01, into.z * 0.2));
        Vec3 horizontal;
        double vy;
        if (jumpEdge) {
            // Kick off the wall: backwards and up.
            dir = wallNormal;
            speed = Math.max(0.8, speed * 0.6);
            horizontal = dir.scale(speed);
            vy = 0.85;
            mode = Mode.AIR;
            airDescent = 0;
            play(ModSounds.SUPER_JUMP.get(), 0.9F, 1.2F);
            ModNetwork.sendToServer(new SpeedActionPacket(SpeedAction.WALL_JUMP));
        } else if (!forward || input.shift() || speed < 0.55) {
            // Let go of the wall.
            mode = Mode.AIR;
            airDescent = 0;
            speed = Math.max(END_SPEED, speed * 0.3);
            dir = wallNormal;
            horizontal = wallNormal.scale(0.15);
            vy = Math.min(player.getDeltaMovement().y, 0.2);
        } else if (touching) {
            // Up the wall, pressed against it.
            speed *= 0.996;
            horizontal = into.scale(0.25);
            vy = Math.min(1.5, 0.45 + speed * 0.45);
            if (player.tickCount % 2 == 0) wallDust(player, into);
        } else {
            // Top reached: vault over the edge.
            dir = into;
            speed = Math.max(0.6, speed * 0.5);
            horizontal = into.scale(0.6);
            vy = 0.6;
            mode = Mode.AIR;
            airDescent = 0;
        }
        player.setDeltaMovement(horizontal.x, vy, horizontal.z);
    }

    private static void wallDust(LocalPlayer player, Vec3 into) {
        Level level = player.level();
        BlockPos pos = BlockPos.containing(player.getX() + into.x * 0.6, player.getY() + 0.2, player.getZ() + into.z * 0.6);
        var state = level.getBlockState(pos);
        if (!state.isAir()) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                        player.getX() + into.x * 0.3 + (player.getRandom().nextDouble() - 0.5) * 0.5, player.getY(),
                        player.getZ() + into.z * 0.3 + (player.getRandom().nextDouble() - 0.5) * 0.5, 0, -0.2, 0);
            }
        }
    }

    /** Direction pointing out of the wall the player is touching (the one most in front of them), or null. */
    private static @Nullable Vec3 wallNormal(LocalPlayer player) {
        AABB box = player.getBoundingBox().move(0, 0.05, 0);
        Vec3[] sides = {new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 0, -1)};
        Vec3 best = null;
        double bestDot = 0;
        for (Vec3 d : sides) {
            if (!player.level().noCollision(player, box.move(d.x * 0.1, 0, d.z * 0.1))) {
                double dot = dir.dot(d);
                if (dot > bestDot) {
                    bestDot = dot;
                    best = d.scale(-1);
                }
            }
        }
        return best;
    }

    // ---- tornado ---------------------------------------------------------------------------------

    private static @Nullable SpeedTornadoEntity ownTornado(LocalPlayer player) {
        var list = player.level().getEntitiesOfClass(SpeedTornadoEntity.class, player.getBoundingBox().inflate(12),
                t -> t.ownerId() == player.getId() && !t.isCollapsing());
        return list.isEmpty() ? null : list.getFirst();
    }

    private static void orbit(LocalPlayer player, SpeedTornadoEntity tornado, boolean shiftEdge) {
        if (shiftEdge) {
            // Sneak stops the tornado.
            ModNetwork.sendToServer(new UsePowerPacket(SpeedsterPower.TORNADO.ordinal()));
        }
        Vec3 c = tornado.position();
        if (Double.isNaN(orbitAngle)) orbitAngle = Math.atan2(player.getZ() - c.z, player.getX() - c.x);
        double radius = SpeedTornadoEntity.ORBIT_RADIUS;
        orbitAngle += ORBIT_SPEED / radius;
        Vec3 target = c.add(Math.cos(orbitAngle) * radius, 0, Math.sin(orbitAngle) * radius);
        Vec3 horizontal = new Vec3(target.x - player.getX(), 0, target.z - player.getZ());
        if (horizontal.length() > 3.0) horizontal = horizontal.normalize().scale(3.0);
        double vy = player.getDeltaMovement().y;
        if (!player.onGround()) {
            double drop = distanceToGround(player, 1.6);
            if (drop < 1.6 && vy <= 0.05) vy = -(drop + 0.05);
        }
        player.setDeltaMovement(horizontal.x, vy, horizontal.z);
        running = true;
        orbiting = true;
        mode = Mode.GROUND;
        speed = ORBIT_SPEED;
        dir = new Vec3(-Math.sin(orbitAngle), 0, Math.cos(orbitAngle));
        sendState(false);
        spawnRunParticles(player, dir.scale(speed));
    }

    // ---- phasing ---------------------------------------------------------------------------------

    /** Walking (not running) while phasing: no gravity, jump / sneak to rise / sink. */
    private static void phaseWalk(LocalPlayer player, Input input) {
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 side = new Vec3(-look.z, 0, look.x);
        double f = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        double s = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
        Vec3 move = look.scale(f).add(side.scale(s));
        if (move.lengthSqr() > 1) move = move.normalize();
        move = move.scale(PHASE_WALK_SPEED);
        double vy = input.jump() ? 0.3 : (input.shift() ? -0.3 : 0.0);
        player.setDeltaMovement(move.x, vy, move.z);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static void sendState(boolean force) {
        int flags = flags();
        if (force || flags != lastSentFlags || ++sendTimer >= 2) {
            sendTimer = 0;
            lastSentFlags = flags;
            ModNetwork.sendToServer(new SpeedStatePacket((float) speed(), flags));
        }
    }

    /** Height of the water surface the player is running over, or NaN when there is none. */
    private static double waterSurface(LocalPlayer player) {
        Level level = player.level();
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.3, player.getZ());
        for (int i = 0; i < 3; i++) {
            BlockPos pos = feet.below(i);
            FluidState fluid = level.getFluidState(pos);
            if (fluid.is(FluidTags.WATER)) {
                if (level.getFluidState(pos.above()).is(FluidTags.WATER)) continue;
                return pos.getY() + fluid.getHeight(level, pos);
            }
            if (!level.getBlockState(pos).isAir()) return Double.NaN;
        }
        return Double.NaN;
    }

    private static double distanceToGround(LocalPlayer player, double max) {
        for (double d = 0; d < max; d += 0.25) {
            if (!player.level().noCollision(player, player.getBoundingBox().move(0, -d - 0.05, 0))) return d;
        }
        return max;
    }

    private static void burst(LocalPlayer player, int count) {
        Level level = player.level();
        var random = player.getRandom();
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            level.addParticle(ModParticles.SPEED_SPARK.get(), player.getX() + Math.cos(a) * 0.4, player.getY() + 0.1 + random.nextDouble() * 0.4,
                    player.getZ() + Math.sin(a) * 0.4, Math.cos(a) * 0.35, 0.05 + random.nextDouble() * 0.2, Math.sin(a) * 0.35);
        }
    }

    /** Spray of water / dust at the feet and crackling lightning around the body while running. */
    private static void spawnRunParticles(LocalPlayer player, Vec3 horizontal) {
        Level level = player.level();
        var random = player.getRandom();
        if (onWater) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH, player.getX() + random.nextGaussian() * 0.3, player.getY(),
                        player.getZ() + random.nextGaussian() * 0.3, -horizontal.x * 0.2, 0.25 + random.nextDouble() * 0.2, -horizontal.z * 0.2);
            }
            level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, player.getX(), player.getY(), player.getZ(), 0, 0.05, 0);
        } else if (player.onGround() && speed > 0.6) {
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
            var state = level.getBlockState(below);
            if (!state.isAir() && random.nextFloat() < 0.6F) {
                level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                        player.getX() + random.nextGaussian() * 0.2, player.getY() + 0.05, player.getZ() + random.nextGaussian() * 0.2,
                        -horizontal.x * 0.15, 0.2, -horizontal.z * 0.15);
            }
        }
        if (++crackleTicks % 3 == 0 && speed > 1.0) {
            level.addParticle(ModParticles.SPEED_SPARK.get(), player.getX() + random.nextGaussian() * 0.35, player.getY() + 0.3 + random.nextDouble() * 1.4,
                    player.getZ() + random.nextGaussian() * 0.35, 0, 0, 0);
        }
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
