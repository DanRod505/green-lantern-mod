package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.block.PowerBatteryBlockEntity;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModBlocks;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.Uniform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.event.TickEvent;

/**
 * Development-only visual smoke test. Launch with {@code GL_CLIENT_SCRIPT=1 ./gradlew runClient -Pgametests}:
 * creates a flat world, exercises every feature and saves screenshots to {@code run/screenshots}.
 */
public final class ClientScript {
    private record Step(int delay, Runnable action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static int stepIndex;
    private static int wait;
    private static boolean worldRequested;
    private static BlockPos lanternPos = BlockPos.ZERO;

    private ClientScript() {}

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientScript::tick);
        if ("wheel".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildWheelSteps();
        } else if ("mecha".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildMechaSteps();
        } else if ("oa".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildOaSteps();
        } else if ("flight".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildFlightSteps();
        } else if ("flash".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildFlashSteps();
        } else {
            buildSteps();
        }
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static ServerPlayer serverPlayer() {
        return mc().getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    }

    private static void server(java.util.function.Consumer<ServerPlayer> action) {
        mc().getSingleplayerServer().execute(() -> action.accept(serverPlayer()));
    }

    private static void command(String command) {
        var server = mc().getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void look(float yaw, float pitch) {
        var player = mc().player;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        server(sp -> {
            sp.setYRot(yaw);
            sp.setXRot(pitch);
            sp.setYHeadRot(yaw);
        });
    }

    private static void shot(String name) {
        Screenshot.grab(mc().gameDirectory, "gl_" + name + ".png", mc().getMainRenderTarget(), 1, msg -> {});
    }

    private static void camera(CameraType type) {
        mc().options.setCameraType(type);
    }

    private static void select(Construct construct) {
        server(sp -> ConstructRegistry.select(sp.getMainHandItem(), construct));
    }

    private static void use() {
        server(sp -> sp.getMainHandItem().getItem().use(sp.level(), sp, InteractionHand.MAIN_HAND));
    }

    private static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static void key(net.minecraft.client.KeyMapping mapping, boolean down) {
        mapping.setDown(down);
    }

    /** Construct wheel: hold R, point at the saw, screenshot, release and check the selection. */
    private static void buildWheelSteps() {
        step(60, () -> {
            command("time set 6000");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                Uniform.summon(sp);
            });
            look(0, 5);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, () -> key(mc().options.keyUp, false));
        step(1, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> shot("w01_wheel_open"));
        // Point at slice 4 (the saw): angle 3 * 72 degrees clockwise from the top.
        step(2, () -> {
            var screen = mc().screen;
            double angle = Math.toRadians(-90 + 3 * 72);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            double scale = mc().getWindow().getGuiScale();
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc().getWindow().handle(), gx * scale, gy * scale);
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("w02_wheel_saw"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> {
            var selected = ConstructRegistry.selected(mc().player.getMainHandItem());
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT wheel selected={} screenClosed={}", selected.id(), mc().screen == null);
            shot("w03_after_select");
        });
        // Quick tap selects the next construct (hammer).
        step(5, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(2, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false));
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT tap selected={} screen={}",
                ConstructRegistry.selected(mc().player.getMainHandItem()).id(), mc().screen));
        step(10, () -> mc().stop());
    }

    /** Power flight showcase: take-off, acceleration, sonic boom, barrel roll and hero landing. */
    private static void buildFlightSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                // Pillars along the flight path give a sense of speed.
                for (int z = 8; z < 520; z += 14) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 7 + (z / 14) % 4; y++) {
                            level.setBlockAndUpdate(base.offset(side * 12, y, z), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState());
                        }
                    }
                }
                Uniform.summon(sp);
            });
            look(0, 5);
        });
        step(20, () -> camera(CameraType.THIRD_PERSON_BACK));
        // Particle check: shockwave + sonic ring a few blocks in front of the player.
        step(5, () -> {
            var p = mc().player;
            look(0, 35);
            mc().level.addParticle(com.danrod505.greenlantern.registry.ModParticles.SHOCKWAVE.get(), p.getX(), p.getY() + 0.1, p.getZ() + 5, 0, 0, 0);
            mc().level.addParticle(com.danrod505.greenlantern.registry.ModParticles.SONIC_RING.get(), p.getX() + 2, p.getY() + 1.5, p.getZ() + 6, 0, 0, 1);
            com.danrod505.greenlantern.client.flight.FlightController.spawnLandingBurst(p);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT fps={}", mc().getFps());
        });
        step(4, () -> shot("f00_particle_check"));
        step(1, () -> look(0, 5));
        // Take off: start flying next to the ground.
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(6, () -> shot("f01_takeoff"));
        step(10, () -> {
            look(0, -8);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(20, () -> shot("f02_accelerating"));
        step(20, () -> {
            look(0, 2);
            shot("f03_fast");
        });
        step(16, () -> shot("f04_near_mach1"));
        // Wait for the sonic boom (~3 s with sprint), then capture right after it.
        step(14, () -> shot("f05_sonic_boom"));
        step(6, () -> shot("f05b_sonic_boom"));
        step(20, () -> shot("f06_supersonic"));
        step(2, () -> camera(CameraType.FIRST_PERSON));
        step(6, () -> shot("f07_supersonic_first_person"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(6, () -> shot("f08_superhero_pose_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // Wide supersonic turn: the trail arcs behind the Lantern and the camera banks.
        for (int i = 0; i < 24; i++) {
            final float yaw = (i + 1) * 3.75F;
            step(1, () -> look(yaw, 2));
            if (i == 12) step(0, () -> shot("f09_turn"));
        }
        step(4, () -> shot("f09b_turn_trail"));
        step(2, () -> camera(CameraType.FIRST_PERSON));
        step(3, () -> look(110, 2));
        step(3, () -> shot("f09c_turn_first_person_bank"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // Dive into the ground at full speed: hero landing.
        step(4, () -> look(90, 60));
        step(4, () -> shot("f10_dive"));
        for (int i = 0; i < 40; i++) {
            step(1, () -> {
                if (landingShot && ++afterLanding == 3) shot("f11b_hero_landing");
                if (!landingShot && mc().player.onGround()) {
                    landingShot = true;
                    shot("f11_hero_landing");
                    com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT landing: heroLanding={} power={}",
                            com.danrod505.greenlantern.client.flight.FlightVisuals.local().heroLanding,
                            com.danrod505.greenlantern.client.flight.FlightController.isPowerFlying());
                }
            });
        }
        step(1, () -> {
            if (!landingShot) shot("f11_hero_landing_late");
        });
        // Take off again, accelerate and barrel roll (double tap right).
        step(10, () -> {
            key(mc().options.keyUp, false);
            look(90, -10);
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(3, () -> shot("f12_takeoff"));
        step(5, () -> key(mc().options.keyUp, true));
        step(30, () -> key(mc().options.keyRight, true));
        step(1, () -> key(mc().options.keyRight, false));
        step(1, () -> key(mc().options.keyRight, true));
        step(1, () -> key(mc().options.keyRight, false));
        step(5, () -> shot("f13_barrel_roll"));
        step(3, () -> shot("f13b_barrel_roll"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        step(140, () -> mc().stop());
    }

    private static final java.util.Set<String> TAKEN = new java.util.HashSet<>();
    private static BlockPos flashBase = BlockPos.ZERO;

    private record Watcher(java.util.function.BooleanSupplier condition, String name) {}

    private static final List<Watcher> WATCHERS = new ArrayList<>();

    /** From now on, takes the screenshot the first tick the condition holds (checked every tick, in parallel with the steps). */
    private static void watch(java.util.function.BooleanSupplier condition, String name) {
        step(0, () -> WATCHERS.add(new Watcher(condition, name)));
    }

    private static void tickWatchers() {
        WATCHERS.removeIf(w -> {
            if (TAKEN.contains(w.name())) return true;
            if (!w.condition().getAsBoolean()) return false;
            TAKEN.add(w.name());
            shot(w.name());
            return true;
        });
    }

    private static void flashPower(com.danrod505.greenlantern.flash.SpeedsterPower power) {
        server(sp -> {
            com.danrod505.greenlantern.flash.SpeedsterPower.select(sp.getMainHandItem(), power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(sp.getMainHandItem()));
            com.danrod505.greenlantern.flash.SpeedsterServer.usePower(sp, sp.getMainHandItem(), power);
        });
    }

    private static void flashZombies(int dx, int dz, int count) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                // Husks: they don't burn in the sun.
                var zombie = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                zombie.snapTo(sp.getX() + dx + Math.cos(a) * 1.5, sp.getY(), sp.getZ() + dz + Math.sin(a) * 1.5, 180, 0);
                zombie.setNoAi(true);
                zombie.setPersistenceRequired();
                level.addFreshEntity(zombie);
            }
        });
    }

    /** The Flash: suit, the run (water, sound barrier, up a wall), super jump, tornado, lightning, phasing and the power wheel. */
    private static void buildFlashSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, com.danrod505.greenlantern.item.FlashRingItem.charged(new ItemStack(ModItems.FLASH_RING.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                flashBase = base;
                var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
                var stone = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
                // A lake to run over.
                for (int z = 40; z < 120; z++) {
                    for (int x = -9; x <= 9; x++) {
                        level.setBlockAndUpdate(base.offset(x, -1, z), water);
                        level.setBlockAndUpdate(base.offset(x, -2, z), water);
                    }
                }
                // Pillars along the track for a sense of speed.
                for (int z = 8; z < 400; z += 12) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 6 + (z / 12) % 4; y++) {
                            level.setBlockAndUpdate(base.offset(side * 13, y, z), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState());
                        }
                    }
                }
                // A tall wall to run up.
                for (int x = -12; x <= 12; x++) {
                    for (int y = 0; y < 28; y++) {
                        for (int z = 0; z < 3; z++) level.setBlockAndUpdate(base.offset(x, y, 300 + z), stone);
                    }
                }
                // A thick wall to phase through, off to the side of the start.
                for (int x = -40; x <= -38; x++) {
                    for (int y = -1; y < 5; y++) {
                        for (int z = -3; z <= 3; z++) level.setBlockAndUpdate(base.offset(x, y, z), stone);
                    }
                }
                com.danrod505.greenlantern.flash.FlashSuit.summon(sp);
            });
            look(0, 5);
        });
        step(30, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(6, () -> shot("fl00_suit_front"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 5);
        });
        step(6, () -> {
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(8, () -> shot("fl01_run_start"));
        watch(() -> com.danrod505.greenlantern.flash.SpeedFlags.has(com.danrod505.greenlantern.client.speed.SpeedController.flags(),
                com.danrod505.greenlantern.flash.SpeedFlags.WATER) && mc().player.getZ() > flashBase.getZ() + 70, "fl03_on_water");
        watch(() -> com.danrod505.greenlantern.client.speed.SpeedController.boomFlash() > 2, "fl04_sound_barrier");
        watch(() -> com.danrod505.greenlantern.client.speed.SpeedController.isWallRunning() && mc().player.getY() > flashBase.getY() + 6, "fl06_wall_run");
        watch(() -> com.danrod505.greenlantern.client.speed.SpeedController.isWallRunning() && mc().player.getY() > flashBase.getY() + 16, "fl06b_wall_run_high");
        step(15, () -> shot("fl02_accelerating"));
        // Run until the wall (about 300 blocks), then over it.
        for (int i = 0; i < 160; i++) {
            step(1, () -> {
                if (TAKEN.contains("fl04_sound_barrier") && !TAKEN.contains("fl05_supersonic")
                        && com.danrod505.greenlantern.client.speed.SpeedController.boomFlash() == 0) {
                    TAKEN.add("fl05_supersonic");
                    shot("fl05_supersonic");
                }
            });
        }
        step(40, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        // Back to the start: super jump.
        step(30, () -> server(sp -> sp.teleportTo(flashBase.getX() + 0.5, flashBase.getY(), flashBase.getZ() - 60.5)));
        step(10, () -> {
            look(0, 0);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(40, () -> key(mc().options.keyJump, true));
        step(2, () -> key(mc().options.keyJump, false));
        step(6, () -> shot("fl07_super_jump"));
        step(8, () -> shot("fl07b_super_jump_high"));
        watch(60, () -> mc().player.onGround(), "fl08_landing");
        step(30, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        // Tornado.
        step(40, () -> {
            server(sp -> sp.teleportTo(flashBase.getX() + 0.5, flashBase.getY(), flashBase.getZ() - 20.5));
            look(0, 5);
        });
        step(10, () -> flashZombies(0, 4, 5));
        step(10, () -> flashPower(com.danrod505.greenlantern.flash.SpeedsterPower.TORNADO));
        step(30, () -> shot("fl09_tornado"));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
        });
        step(4, () -> shot("fl09b_tornado_front"));
        step(20, () -> shot("fl09c_tornado_lift"));
        step(2, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("fl09d_tornado_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(60, () -> {});
        // Lightning.
        step(10, () -> {
            server(sp -> sp.teleportTo(flashBase.getX() + 20.5, flashBase.getY(), flashBase.getZ() - 20.5));
            look(0, 3);
        });
        step(10, () -> flashZombies(0, 10, 3));
        step(10, () -> flashPower(com.danrod505.greenlantern.flash.SpeedsterPower.LIGHTNING));
        step(1, () -> shot("fl10_lightning"));
        step(2, () -> shot("fl10b_lightning_hit"));
        step(20, () -> flashPower(com.danrod505.greenlantern.flash.SpeedsterPower.LIGHTNING));
        step(2, () -> shot("fl10c_lightning_again"));
        // Molecular vibration through the thick wall.
        step(20, () -> {
            server(sp -> sp.teleportTo(flashBase.getX() - 34.5, flashBase.getY(), flashBase.getZ() + 0.5));
            look(90, 5);
        });
        step(10, () -> flashPower(com.danrod505.greenlantern.flash.SpeedsterPower.PHASE));
        step(5, () -> key(mc().options.keyUp, true));
        watch(60, () -> mc().player.getX() < flashBase.getX() - 37.6, "fl11_phasing_into_wall");
        step(3, () -> {
            key(mc().options.keyUp, false);
            camera(CameraType.FIRST_PERSON);
        });
        step(3, () -> shot("fl11b_phasing_inside_wall"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            flashPower(com.danrod505.greenlantern.flash.SpeedsterPower.PHASE);
        });
        step(5, () -> {
            shot("fl12_phase_exit");
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT phase exit pos={} health={}", mc().player.position(), mc().player.getHealth());
        });
        // Power wheel.
        step(10, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 120);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("fl13_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power selected={}",
                com.danrod505.greenlantern.flash.SpeedsterPower.selected(mc().player.getMainHandItem()).id()));
        step(20, () -> mc().stop());
    }

    /** Screenshot without the HUD and chat, so the model is easy to see. */
    private static void clean(String name) {
        mc().gui.getChat().clearMessages(false);
        mc().options.hideGui = true;
        shot(name);
    }

    private static void mecha(java.util.function.Consumer<com.danrod505.greenlantern.entity.MechaEntity> action) {
        server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.MechaEntity mecha) action.accept(mecha);
        });
    }

    /** Giant mecha: summon, poses from several angles, lasers, missiles, walking, flight and landing. */
    private static void buildMechaSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                Uniform.summon(sp);
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                for (int i = -1; i <= 1; i++) {
                    Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                    zombie.snapTo(base.getX() + 0.5 + i * 3.0, base.getY(), base.getZ() + 16.5, 180, 0);
                    zombie.setNoAi(true);
                    zombie.setPersistenceRequired();
                    zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                    level.addFreshEntity(zombie);
                }
                // A little stone wall and a lantern post for scale.
                for (int x = -6; x <= -3; x++) {
                    for (int y = 0; y < 2; y++) {
                        level.setBlockAndUpdate(base.offset(x, y, 6), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                ConstructRegistry.select(sp.getMainHandItem(), ConstructRegistry.MECHA);
            });
            look(0, 10);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, ClientScript::use);
        step(8, () -> clean("m00_assembling"));
        step(40, () -> clean("m01_back"));
        step(5, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> clean("m02_front"));
        step(2, () -> look(35, 25));
        step(5, () -> clean("m02b_front_low"));
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            mc().options.hideGui = false;
            look(0, 15);
        });
        step(5, () -> shot("m03_cockpit"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 16);
            mecha(m -> m.setLaserFiring(true));
        });
        step(12, () -> clean("m04_laser"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("m04b_laser_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mecha(m -> m.setLaserFiring(false));
        });
        step(25, () -> {
            look(-20, 10);
            mecha(m -> m.fireMissiles());
        });
        step(7, () -> clean("m05_missiles_launch"));
        step(8, () -> clean("m05b_missiles_fly"));
        step(10, () -> clean("m05c_missiles_hit"));
        // Walking.
        step(20, () -> {
            look(90, 12);
            key(mc().options.keyUp, true);
        });
        step(30, () -> clean("m06_walk"));
        step(7, () -> clean("m06b_walk"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> clean("m06c_walk_front"));
        step(6, () -> clean("m06d_walk_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, false);
        });
        // Take off and fly.
        step(20, () -> key(mc().options.keyJump, true));
        step(16, () -> clean("m07_takeoff"));
        step(30, () -> {
            clean("m07b_climb");
            key(mc().options.keyUp, true);
        });
        step(10, () -> key(mc().options.keyJump, false));
        step(25, () -> clean("m08_flying"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> clean("m08b_flying_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keySprint, true);
        });
        step(25, () -> clean("m08c_afterburner"));
        // Dive into the ground for the landing shockwave.
        step(2, () -> {
            key(mc().options.keySprint, false);
            look(mc().player.getYRot(), 70);
        });
        for (int i = 0; i < 6; i++) {
            String name = "m09_dive_" + i;
            step(8, () -> shot(name));
        }
        step(2, () -> key(mc().options.keyUp, false));
        step(6, () -> clean("m09z_after_landing"));
        step(20, () -> mc().stop());
    }

    private static void oaTp(double x, double y, double z, float yaw, float pitch) {
        command(String.format(java.util.Locale.ROOT, "execute in greenlantern:oa run tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    /** Portal construct and the planet Oa: the trip there, the city from several angles and the trip back. */
    private static void buildOaSteps() {
        int g = com.danrod505.greenlantern.oa.Oa.GROUND_Y;
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                Uniform.summon(sp);
                ConstructRegistry.select(sp.getMainHandItem(), ConstructRegistry.PORTAL);
            });
            look(0, 8);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, ClientScript::use);
        step(6, () -> clean("o00_portal_opening"));
        step(20, () -> clean("o01_portal"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("o01b_portal_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // Walk through the portal.
        step(2, () -> key(mc().options.keyUp, true));
        step(30, () -> key(mc().options.keyUp, false));
        step(100, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT oa dimension={} pos={}", mc().level.dimension(), mc().player.position());
            camera(CameraType.FIRST_PERSON);
            look(180, -12);
        });
        step(30, () -> clean("o02_arrival"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(10, () -> clean("o02b_arrival_lantern"));
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            server(sp -> {
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            oaTp(0.5, g + 38, 78.5, 180, 22);
        });
        step(120, () -> clean("o03_city_aerial"));
        step(2, () -> oaTp(0.5, g + 14, 26.5, 180, -8));
        step(80, () -> clean("o04_battery"));
        step(2, () -> oaTp(18.5, g + 4, 18.5, 135, -32));
        step(80, () -> clean("o05_battery_beam"));
        step(2, () -> {
            net.minecraft.core.BlockPos top = com.danrod505.greenlantern.oa.OaCity.pillarTop(0);
            double dx = -top.getX(), dz = -top.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            double x = top.getX() + 0.5 + dx / len * 4.5;
            double z = top.getZ() + 0.5 + dz / len * 4.5;
            float yaw = (float) Math.toDegrees(Math.atan2(-(top.getX() + 0.5 - x), top.getZ() + 0.5 - z));
            oaTp(x, top.getY() - 0.5, z, yaw, 8);
        });
        step(80, () -> clean("o06_guardian"));
        step(2, () -> {
            net.minecraft.core.BlockPos top = com.danrod505.greenlantern.oa.OaCity.pillarTop(2);
            oaTp(top.getX() * 0.55 + 0.5, top.getY() + 3, top.getZ() * 0.55 + 0.5, (float) Math.toDegrees(Math.atan2(-top.getX(), top.getZ())) + 20, 10);
        });
        step(80, () -> clean("o07_guardian_ring"));
        step(2, () -> {
            server(sp -> {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            });
            oaTp(30.5, g, 0.5, 90, -4);
        });
        step(80, () -> {
            var lanterns = mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.LanternCorpsmanEntity.class, mc().player.getBoundingBox().inflate(80));
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT lanterns near={}", lanterns.size());
            if (!lanterns.isEmpty()) {
                var walker = lanterns.stream().filter(l -> !l.isFlyingLantern()).min(java.util.Comparator.comparingDouble(l -> l.distanceToSqr(mc().player))).orElse(lanterns.getFirst());
                oaTp(walker.getX() + 2.5, g, walker.getZ() + 2.5, 135, 10);
            }
        });
        step(20, () -> clean("o08_lanterns"));
        step(2, () -> {
            server(sp -> {
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            var flyers = new java.util.ArrayList<>(mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.LanternCorpsmanEntity.class, mc().player.getBoundingBox().inflate(120)));
            flyers.removeIf(l -> !l.isFlyingLantern());
            if (!flyers.isEmpty()) {
                var f = flyers.getFirst();
                oaTp(f.getX() * 1.35, f.getY() + 1.5, f.getZ() * 1.35, (float) Math.toDegrees(Math.atan2(f.getX(), -f.getZ())), 15);
            }
        });
        step(30, () -> clean("o09_flying_lanterns"));
        step(2, () -> oaTp(70.5, g + 50, -40.5, 120, 20));
        step(100, () -> clean("o10_skyline"));
        step(2, () -> oaTp(0.5, g, 30.5, 180, 5));
        step(40, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            });
            use();
        });
        step(25, () -> clean("o11_portal_home"));
        step(2, () -> key(mc().options.keyUp, true));
        step(30, () -> key(mc().options.keyUp, false));
        step(80, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT back dimension={} pos={}", mc().level.dimension(), mc().player.position());
            clean("o12_back_home");
        });
        step(20, () -> mc().stop());
    }

    private static boolean landingShot;
    private static int afterLanding;

    private static void buildSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                lanternPos = base.offset(2, 0, 3);
                level.setBlockAndUpdate(lanternPos, ModBlocks.POWER_BATTERY.get().defaultBlockState());
                for (int i = -1; i <= 1; i++) {
                    Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                    zombie.snapTo(base.getX() + 0.5 + i * 2.5, base.getY(), base.getZ() + 9.5, 180, 0);
                    zombie.setNoAi(true);
                    zombie.setPersistenceRequired();
                    level.addFreshEntity(zombie);
                }
            });
            look(0, 10);
        });
        step(40, () -> shot("01_ring_and_lantern"));
        step(5, () -> {
            use(); // not suited -> summons the uniform
            camera(CameraType.THIRD_PERSON_FRONT);
        });
        step(12, () -> shot("02_uniform_front"));
        step(5, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(10, () -> shot("03_uniform_back"));
        // Energy blast.
        step(5, () -> {
            select(ConstructRegistry.ENERGY_BLAST);
            look(0, 0);
        });
        step(5, ClientScript::use);
        step(3, () -> shot("04_energy_blast"));
        // Minigun.
        step(20, () -> select(ConstructRegistry.MINIGUN));
        // Hold the real "use" key so the whole client -> server input path is exercised.
        step(5, () -> mc().options.keyUse.setDown(true));
        step(20, () -> shot("05_minigun"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("05b_minigun_front"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("05c_minigun_first_person"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mc().options.keyUse.setDown(false);
        });
        // Bubble.
        step(15, () -> select(ConstructRegistry.BUBBLE));
        step(5, ClientScript::use);
        step(10, () -> shot("06_bubble"));
        step(5, () -> camera(CameraType.FIRST_PERSON));
        step(5, () -> shot("06b_bubble_inside"));
        step(5, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            use();
        });
        // Saw.
        step(15, () -> select(ConstructRegistry.SAW));
        step(5, ClientScript::use);
        step(15, () -> shot("07_saw"));
        step(5, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> shot("07b_saw_front"));
        step(5, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            use();
        });
        // Hammer.
        step(25, () -> {
            select(ConstructRegistry.HAMMER);
            look(0, 30);
        });
        step(5, ClientScript::use);
        step(9, () -> shot("08_hammer_raised"));
        step(4, () -> shot("08b_hammer_swing"));
        step(3, () -> shot("08c_hammer_impact"));
        // Flight.
        step(30, () -> server(sp -> {
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(sp.getX(), sp.getY() + 3, sp.getZ());
        }));
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            look(200, 15);
        });
        step(20, () -> shot("09_flight"));
        // Recharge at the lantern.
        step(5, () -> server(sp -> {
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            Uniform.dismiss(sp, true);
            sp.teleportTo(lanternPos.getX() - 1.0, lanternPos.getY(), lanternPos.getZ() - 1.0);
            RingEnergy.set(sp.getMainHandItem(), 250);
        }));
        step(10, () -> {
            camera(CameraType.FIRST_PERSON);
            look(-45, 40);
            server(sp -> {
                if (sp.level().getBlockEntity(lanternPos) instanceof PowerBatteryBlockEntity battery) {
                    battery.toggleCharging(sp);
                }
            });
        });
        step(30, () -> shot("10_charging"));
        step(20, () -> shot("10b_charging"));
        step(20, () -> mc().stop());
    }

    private static void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = mc();
        if (mc.level == null || mc.player == null) {
            if (!worldRequested && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                mc.options.onboardAccessibility = false;
                worldRequested = true;
                LevelSettings settings = new LevelSettings("gltest", GameType.CREATIVE, false, Difficulty.EASY, true,
                        new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("gltest-" + System.currentTimeMillis(), settings,
                        new WorldOptions(2814L, false, false), WorldPresets::createFlatWorldDimensions, mc.screen);
            }
            return;
        }
        if (mc.player.tickCount % 10 == 0 && "flash".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} speed={} mach={} flags={} pos={} music: {}", mc.player.tickCount,
                    String.format("%.2f", com.danrod505.greenlantern.client.speed.SpeedController.speed()),
                    String.format("%.2f", com.danrod505.greenlantern.client.speed.SpeedController.mach()),
                    com.danrod505.greenlantern.client.speed.SpeedController.flags(), mc.player.blockPosition(),
                    com.danrod505.greenlantern.client.speed.SpeedAudio.describeMusic());
        }
        if (mc.player.tickCount % 10 == 0 && "flight".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} mach={} music: {}", mc.player.tickCount,
                    String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.mach()),
                    com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
        }
        tickWatchers();
        if (stepIndex >= STEPS.size()) return;
        if (++wait >= STEPS.get(stepIndex).delay()) {
            wait = 0;
            STEPS.get(stepIndex++).action().run();
        }
    }
}
