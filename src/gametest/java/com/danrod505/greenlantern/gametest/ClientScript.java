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
import net.minecraft.world.phys.Vec3;
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
        } else if ("aquaman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildAquamanSteps();
        } else if ("atlantis".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildAtlantisSteps();
        } else if ("kraken".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildKrakenSteps();
        } else if ("batman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildBatmanSteps();
        } else if ("mounts".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildMountsSteps();
        } else if ("superman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildSupermanSteps();
        } else if ("trench".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildTrenchSteps();
        } else if ("wonderwoman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildWonderWomanSteps();
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

    private static java.util.function.BooleanSupplier hold;
    private static int holdLeft;

    /** Holds the next steps until the condition holds (or for at most {@code maxTicks} ticks). */
    private static void waitFor(int maxTicks, java.util.function.BooleanSupplier condition) {
        step(0, () -> {
            hold = condition;
            holdLeft = maxTicks;
        });
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

    /** Watches the condition for the next {@code ticks} ticks, blocking the steps; shoots anyway at the end. */
    private static void watch(int ticks, java.util.function.BooleanSupplier condition, String name) {
        for (int i = 0; i < ticks; i++) {
            step(1, () -> {
                if (!TAKEN.contains(name) && condition.getAsBoolean()) {
                    TAKEN.add(name);
                    shot(name);
                }
            });
        }
        step(0, () -> {
            if (TAKEN.add(name)) shot(name + "_late");
        });
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

    private static BlockPos aquaBase = BlockPos.ZERO;

    private static void aquaPower(com.danrod505.greenlantern.aquaman.AquaPower power) {
        server(sp -> {
            ItemStack emblem = com.danrod505.greenlantern.aquaman.AquamanHelper.findEmblem(sp);
            com.danrod505.greenlantern.aquaman.AquaPower.select(emblem, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(emblem));
            com.danrod505.greenlantern.aquaman.AquamanServer.usePower(sp, emblem, power);
        });
    }

    private static void aquaTeleport(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> sp.teleportTo(aquaBase.getX() + dx, aquaBase.getY() + dy, aquaBase.getZ() + dz));
        look(yaw, pitch);
    }

    private static void aquaMobs(EntityType<?> type, double dx, double dy, double dz, int count, boolean noAi) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                var mob = type.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                mob.snapTo(aquaBase.getX() + dx + Math.cos(a) * 1.8, aquaBase.getY() + dy, aquaBase.getZ() + dz + Math.sin(a) * 1.8, 180, 0);
                if (mob instanceof net.minecraft.world.entity.Mob m) {
                    m.setNoAi(noAi);
                    m.setPersistenceRequired();
                }
                level.addFreshEntity(mob);
            }
        });
    }

    /** Aquaman: suit, fast 3D swimming with the trail, the leap, the trident, riding the shark, the call of the sea and the wheel. */
    private static void buildAquamanSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, com.danrod505.greenlantern.item.AquamanEmblemItem.charged(new ItemStack(ModItems.AQUAMAN_EMBLEM.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                aquaBase = base;
                var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
                var wall = net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState();
                var sand = net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
                var light = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
                // A big glass-walled sea: 41 wide, 100 long, 12 deep.
                for (int x = -21; x <= 21; x++) {
                    for (int z = 3; z <= 105; z++) {
                        boolean edge = x == -21 || x == 21 || z == 3 || z == 105;
                        level.setBlock(base.offset(x, -1, z), (x * 7 + z * 3) % 23 == 0 ? light : sand, 2);
                        for (int y = 0; y < 12; y++) {
                            level.setBlock(base.offset(x, y, z), edge ? wall : water, 2);
                        }
                    }
                }
                // Coral and pillars on the sea floor, for a sense of speed.
                var corals = List.of(net.minecraft.world.level.block.Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
                        net.minecraft.world.level.block.Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                        net.minecraft.world.level.block.Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
                        net.minecraft.world.level.block.Blocks.HORN_CORAL_BLOCK.defaultBlockState());
                for (int z = 10; z < 100; z += 9) {
                    for (int side = -1; side <= 1; side += 2) {
                        int h = 2 + (z / 9) % 4;
                        for (int y = 0; y < h; y++) level.setBlock(base.offset(side * 12, y, z), corals.get((z / 9 + y) % 4), 2);
                    }
                }
                com.danrod505.greenlantern.aquaman.AquamanSuit.summon(sp);
            });
            look(0, 5);
        });
        step(30, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("aq00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("aq00b_suit_back"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
        });
        step(4, () -> shot("aq01_hud"));
        // Swimming: into the sea, then sprint along it.
        step(10, () -> aquaTeleport(0.5, 5, 6.5, 0, 5));
        step(20, () -> key(mc().options.keyUp, true));
        step(10, () -> shot("aq02_swim"));
        step(1, () -> key(mc().options.keySprint, true));
        watch(80, () -> com.danrod505.greenlantern.client.aqua.SwimController.speed() > 1.6, "aq03_swim_fast");
        step(10, () -> shot("aq03b_swim_trail"));
        step(2, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("aq03c_swim_first_person"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // Turning and diving: the trail curves through the water (back at the start of the pool first).
        step(2, () -> aquaTeleport(0.5, 7, 8.5, 0, 0));
        step(6, () -> look(-40, 25));
        step(8, () -> shot("aq03d_swim_dive"));
        step(4, () -> look(-80, -10));
        step(8, () -> shot("aq03e_swim_turn"));
        // Leap: race up through the surface.
        step(4, () -> {
            aquaTeleport(0.5, 4, 40.5, 0, -55);
        });
        watch(60, () -> !mc().player.isInWater() && mc().player.getY() > aquaBase.getY() + 12.5, "aq04_leap");
        step(10, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        // The trident, on the shore.
        step(30, () -> aquaTeleport(30.5, 0, 20.5, 0, 0));
        step(10, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.TRIDENT));
        step(8, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(4, () -> clean("aq05_trident_hand"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
            look(0, 0);
        });
        step(6, () -> shot("aq05b_trident_first_person"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> aquaMobs(EntityType.HUSK, 30.5, 0, 32.5, 3, true));
        step(4, () -> camera(CameraType.FIRST_PERSON));
        step(6, () -> server(sp -> com.danrod505.greenlantern.item.AquaTridentItem.throwTrident(sp, sp.getMainHandItem())));
        step(2, () -> shot("aq06_trident_thrown"));
        step(2, () -> shot("aq06b_trident_flight"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        watch(60, () -> {
            var list = mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.AquaTridentEntity.class, mc().player.getBoundingBox().inflate(40));
            return !list.isEmpty() && list.getFirst().isReturning() && list.getFirst().distanceTo(mc().player) < 6;
        }, "aq06c_trident_returning");
        step(30, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trident back in hand={}",
                com.danrod505.greenlantern.aquaman.AquamanHelper.isTrident(mc().player.getMainHandItem())));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("aq06d_trident_caught"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // The great white shark.
        step(10, () -> aquaTeleport(0.5, 5, 12.5, 0, 10));
        step(10, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.SHARK));
        step(20, () -> shot("aq07_shark_ride"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("aq07b_shark_front"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, true);
        });
        step(30, () -> shot("aq08_shark_swim"));
        step(1, () -> key(mc().options.keySprint, true));
        step(25, () -> shot("aq08b_shark_sprint"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        step(20, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark) {
                Vec3 mouth = shark.mouth();
                var drowned = EntityType.DROWNED.create(sp.level(), EntitySpawnReason.COMMAND);
                drowned.snapTo(mouth.x, mouth.y - 0.8, mouth.z, 0, 0);
                drowned.setNoAi(true);
                sp.level().addFreshEntity(drowned);
            }
        }));
        step(4, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.SharkBitePacket()));
        step(3, () -> shot("aq09_shark_bite"));
        step(20, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark) {
                sp.stopRiding();
                sp.teleportTo(shark.getX() + 5.5, shark.getY() + 0.5, shark.getZ() + 0.5);
            }
        }));
        step(3, () -> look(90, 5));
        step(4, () -> clean("aq10_shark_side"));
        step(30, () -> clean("aq10b_shark_circling"));
        step(2, () -> mc().options.hideGui = false);
        // The call of the sea.
        step(4, () -> {
            server(sp -> {
                var shark = com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.find(sp);
                if (shark != null) shark.swimAway();
            });
            aquaTeleport(0.5, 4, 80.5, 0, 5);
        });
        step(4, () -> {
            aquaMobs(EntityType.COD, -6, 5, 74, 6, false);
            aquaMobs(EntityType.SQUID, 6, 5, 74, 3, false);
            aquaMobs(EntityType.TROPICAL_FISH, 0, 7, 72, 5, false);
            aquaMobs(EntityType.TURTLE, 4, 1, 78, 2, false);
            aquaMobs(EntityType.DROWNED, 0.5, 2, 92.5, 3, false);
        });
        step(20, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.SEA_CALL));
        step(6, () -> shot("aq11_sea_call"));
        step(40, () -> shot("aq11b_sea_call_following"));
        step(60, () -> shot("aq11c_sea_call_attack"));
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
        step(6, () -> shot("aq12_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power selected={}",
                com.danrod505.greenlantern.aquaman.AquaPower.selected(com.danrod505.greenlantern.aquaman.AquamanHelper.findEmblem(mc().player)).id()));
        // The manual.
        step(5, () -> mc().setScreen(new com.danrod505.greenlantern.client.GuideScreen()));
        step(10, () -> shot("aq13_guide"));
        step(20, () -> mc().stop());
    }


    private static BlockPos batBase = BlockPos.ZERO;

    private static void batPower(com.danrod505.greenlantern.batman.BatPower power) {
        server(sp -> {
            ItemStack belt = com.danrod505.greenlantern.batman.BatmanHelper.findBelt(sp);
            com.danrod505.greenlantern.batman.BatPower.select(belt, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(belt));
            com.danrod505.greenlantern.batman.BatmanServer.usePower(sp, belt, power);
        });
    }

    private static void batTp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            sp.teleportTo(batBase.getX() + dx, batBase.getY() + dy, batBase.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    private static void batMobs(double dx, double dz, int count, boolean noAi) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                var mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                mob.snapTo(batBase.getX() + dx + Math.cos(a) * 1.8, batBase.getY(), batBase.getZ() + dz + Math.sin(a) * 1.8, 180, 0);
                mob.setNoAi(noAi);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        });
    }

    private static void clearMobs() {
        server(sp -> {
            for (var mob : sp.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, sp.getBoundingBox().inflate(80))) mob.discard();
        });
    }

    /** Batman: suit and cape, the batarang, the grappling hook, gliding, the swarm of bats, the Batmobile, the wheel and the manual. */
    private static void buildBatmanSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, com.danrod505.greenlantern.item.UtilityBeltItem.charged(new ItemStack(ModItems.UTILITY_BELT.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                batBase = base;
                var bricks = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
                var dark = net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES.defaultBlockState();
                var light = net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState();
                // A Gotham rooftop: a 12-high tower to grapple onto, and a taller one to glide from.
                for (int x = -4; x <= 4; x++) {
                    for (int z = 25; z <= 31; z++) {
                        for (int y = 0; y < 12; y++) level.setBlock(base.offset(x, y, z), (y % 4 == 3) ? dark : bricks, 2);
                    }
                }
                level.setBlock(base.offset(0, 12, 26), light, 2);
                for (int x = 14; x <= 20; x++) {
                    for (int z = -6; z <= 0; z++) {
                        for (int y = 0; y < 30; y++) level.setBlock(base.offset(x, y, z), (y % 5 == 4) ? dark : bricks, 2);
                    }
                }
                // Some street-side props, for a sense of speed.
                for (int z = 40; z < 140; z += 8) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 3; y++) level.setBlock(base.offset(side * 9, y, z), dark, 2);
                        level.setBlock(base.offset(side * 9, 3, z), light, 2);
                    }
                }
                com.danrod505.greenlantern.batman.BatmanSuit.summon(sp);
            });
            look(0, 5);
        });
        step(30, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(20, () -> {
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("bat00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("bat00b_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("bat00c_suit_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("bat01_hud"));
        // Walking: the cape sways.
        step(2, () -> key(mc().options.keyUp, true));
        step(20, () -> clean("bat02_cape_walk"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            mc().options.hideGui = false;
        });
        // The batarang.
        step(10, () -> {
            batTp(0.5, 0, 2.5, 0, 6);
            batMobs(0.5, 12.5, 3, true);
        });
        step(10, () -> batPower(com.danrod505.greenlantern.batman.BatPower.BATARANG));
        step(3, () -> shot("bat03_batarang"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(2, () -> shot("bat03b_batarang_first_person"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        watch(60, () -> {
            var list = mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.BatarangEntity.class, mc().player.getBoundingBox().inflate(40));
            return !list.isEmpty() && list.getFirst().isReturning();
        }, "bat03c_batarang_returning");
        step(10, () -> clearMobs());
        // The grappling hook, up the tower.
        step(10, () -> batTp(0.5, 0, 15.5, 0, -45));
        step(10, () -> batPower(com.danrod505.greenlantern.batman.BatPower.GRAPPLE));
        step(2, () -> shot("bat04_grapple_fire"));
        watch(40, () -> com.danrod505.greenlantern.client.batman.GrappleController.isPulling(), "bat04b_grapple_pull");
        step(3, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(2, () -> shot("bat04c_grapple_pull_front"));
        step(1, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(40, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT grapple end y={} (top at {})", mc().player.getY(), batBase.getY() + 12);
            shot("bat04d_grapple_top");
        });
        // Gliding, from the top of the tall tower.
        step(10, () -> batTp(17.5, 30, -3.5, 0, 10));
        step(2, () -> {
            com.danrod505.greenlantern.client.batman.GlideController.forceHold = true;
            batTp(17.5, 30, 1.5, 0, 10);
        });
        watch(40, () -> com.danrod505.greenlantern.client.batman.GlideController.isGliding(), "bat05_glide_start");
        step(14, () -> shot("bat05b_glide"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(3, () -> clean("bat05c_glide_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
        });
        step(4, () -> shot("bat05d_glide_first_person"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 45);
        });
        step(10, () -> shot("bat05e_glide_dive"));
        step(1, () -> look(30, -15));
        step(10, () -> {
            shot("bat05f_glide_pull_up");
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT glide speed={} y={}",
                    String.format("%.2f", com.danrod505.greenlantern.client.batman.GlideController.speed()), mc().player.getY() - batBase.getY());
        });
        step(40, () -> com.danrod505.greenlantern.client.batman.GlideController.forceHold = false);
        // The swarm of bats.
        step(20, () -> {
            batTp(0.5, 0, 50.5, 0, 10);
            batMobs(0.5, 57.5, 3, false);
        });
        step(10, () -> batPower(com.danrod505.greenlantern.batman.BatPower.BAT_SWARM));
        step(8, () -> shot("bat06_swarm"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("bat06b_swarm_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(40, () -> shot("bat06c_swarm_attack"));
        step(2, () -> batPower(com.danrod505.greenlantern.batman.BatPower.BAT_SWARM));
        step(6, () -> shot("bat06d_swarm_scatter"));
        step(20, () -> clearMobs());
        // The Batmobile.
        step(10, () -> batTp(0.5, 0, 40.5, 0, 10));
        step(10, () -> batPower(com.danrod505.greenlantern.batman.BatPower.BATMOBILE));
        step(20, () -> shot("bat07_batmobile"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("bat07b_batmobile_front"));
        step(1, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.BatmobileEntity car) {
                sp.stopRiding();
                sp.teleportTo(car.getX() + 5.5, car.getY(), car.getZ() + 1.0);
            }
        }));
        step(3, () -> {
            camera(CameraType.FIRST_PERSON);
            look(90, 15);
        });
        step(4, () -> clean("bat07c_batmobile_side"));
        step(2, () -> server(sp -> sp.teleportTo(sp.getX() - 3.5, sp.getY(), sp.getZ() + 4.5)));
        step(3, () -> look(150, 20));
        step(4, () -> clean("bat07d_batmobile_rear"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            batPower(com.danrod505.greenlantern.batman.BatPower.BATMOBILE);
        });
        step(6, () -> {
            look(0, 10);
            key(mc().options.keyUp, true);
        });
        step(30, () -> shot("bat08_batmobile_drive"));
        step(1, () -> key(mc().options.keySprint, true));
        step(20, () -> shot("bat08b_batmobile_boost"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("bat08c_batmobile_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        step(30, () -> {
            var player = mc().player;
            Vec3 ahead = player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(16));
            server(sp -> {
                ServerLevel level = sp.level();
                for (int i = 0; i < 3; i++) {
                    var mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                    mob.snapTo(ahead.x + (i - 1) * 2.0, sp.getY(), ahead.z, 180, 0);
                    mob.setNoAi(true);
                    level.addFreshEntity(mob);
                }
            });
        });
        step(10, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.BatmobileFirePacket()));
        step(5, () -> shot("bat09_missiles"));
        watch(30, () -> !mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.BatmobileMissileEntity.class,
                mc().player.getBoundingBox().inflate(40)).isEmpty() && mc().level.getEntitiesOfClass(
                com.danrod505.greenlantern.entity.BatmobileMissileEntity.class, mc().player.getBoundingBox().inflate(40)).getFirst().distanceTo(mc().player) > 9,
                "bat09b_missiles_flight");
        step(10, () -> shot("bat09c_missiles_hit"));
        // Power wheel.
        step(20, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 90);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("bat10_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT gadget selected={}",
                com.danrod505.greenlantern.batman.BatPower.selected(com.danrod505.greenlantern.batman.BatmanHelper.findBelt(mc().player)).id()));
        // The manual.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("batman")));
        step(10, () -> shot("bat11_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("batman_powers")));
        step(10, () -> shot("bat11b_guide_gadgets"));
        step(20, () -> mc().stop());
    }

    private static BlockPos supBase = BlockPos.ZERO;

    private static void supPower(com.danrod505.greenlantern.superman.SuperPower power) {
        server(sp -> {
            ItemStack crystal = com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(sp);
            com.danrod505.greenlantern.superman.SuperPower.select(crystal, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(crystal));
            com.danrod505.greenlantern.superman.SupermanServer.usePower(sp, crystal, power);
        });
    }

    private static void supTp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            sp.teleportTo(supBase.getX() + dx, supBase.getY() + dy, supBase.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    private static void supMobs(double dx, double dz, int count, double spread) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                var mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                mob.snapTo(supBase.getX() + dx + Math.cos(a) * spread, supBase.getY(), supBase.getZ() + dz + Math.sin(a) * spread, 180, 0);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        });
    }

    /** Superman: suit and cape, heat vision, the super punch, super breath, X-ray vision, flight, the wheel and the guide. */
    private static void buildSupermanSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND,
                        com.danrod505.greenlantern.item.KryptonianCrystalItem.charged(new ItemStack(ModItems.KRYPTONIAN_CRYSTAL.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                supBase = base;
                var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
                // Ice for the heat vision to melt, next to where the husks will stand.
                for (int x = 3; x <= 5; x++) {
                    for (int y = 0; y < 2; y++) level.setBlock(base.offset(x, y, 12), net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 2);
                }
                // A thick wall behind the start, with treasure hidden behind it for the X-ray vision.
                for (int x = -6; x <= 6; x++) {
                    for (int y = 0; y <= 5; y++) {
                        for (int z = -14; z <= -8; z++) level.setBlock(base.offset(x, y, z), stone, 2);
                    }
                }
                var ores = new net.minecraft.world.level.block.Block[] {net.minecraft.world.level.block.Blocks.DIAMOND_ORE,
                        net.minecraft.world.level.block.Blocks.GOLD_ORE, net.minecraft.world.level.block.Blocks.IRON_ORE,
                        net.minecraft.world.level.block.Blocks.EMERALD_ORE, net.minecraft.world.level.block.Blocks.REDSTONE_ORE,
                        net.minecraft.world.level.block.Blocks.LAPIS_ORE, net.minecraft.world.level.block.Blocks.COAL_ORE,
                        net.minecraft.world.level.block.Blocks.COPPER_ORE};
                for (int i = 0; i < 16; i++) {
                    level.setBlock(base.offset(-5 + (i * 7) % 11, (i * 3) % 5, -10 - (i % 4)), ores[i % ores.length].defaultBlockState(), 2);
                }
                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 2; y++) level.setBlock(base.offset(x, y, -11), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(base.offset(2, 0, -10), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 2);
                level.setBlock(base.offset(-3, 1, -12), net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState(), 2);
                // A pool for the super breath to freeze.
                for (int x = 10; x <= 18; x++) {
                    for (int z = -3; z <= 5; z++) level.setBlock(base.offset(x, -1, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2);
                }
                // Pillars along the flight path give a sense of speed.
                for (int z = 40; z < 700; z += 16) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 8 + (z / 16) % 5; y++) {
                            level.setBlock(base.offset(side * 14, y, z), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                        }
                    }
                }
                com.danrod505.greenlantern.superman.SupermanSuit.summon(sp);
            });
            look(0, 5);
        });
        step(40, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(20, () -> {
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("sup00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("sup00b_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("sup00c_suit_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("sup01_hud"));
        // Walking: the cape sways.
        step(2, () -> key(mc().options.keyUp, true));
        step(20, () -> clean("sup02_cape_walk"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            mc().options.hideGui = false;
        });
        // Heat vision on a group of husks (and the ice next to them).
        step(10, () -> {
            supTp(0.5, 0, 2.5, 0, 8);
            supMobs(0.5, 11.5, 3, 1.6);
        });
        step(10, () -> supPower(com.danrod505.greenlantern.superman.SuperPower.HEAT_VISION));
        step(8, () -> shot("sup03_heat_vision"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("sup03b_heat_vision_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("sup03c_heat_vision_eyes"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(25, 12);
        });
        step(30, () -> shot("sup03d_heat_vision_ice"));
        step(40, () -> clearMobs());
        // The super punch into a crowd.
        step(10, () -> {
            supTp(0.5, 0, 22.5, 0, 10);
            supMobs(0.5, 26.5, 6, 2.2);
        });
        step(10, () -> shot("sup04_before_punch"));
        step(1, () -> supPower(com.danrod505.greenlantern.superman.SuperPower.SUPER_PUNCH));
        step(2, () -> shot("sup04b_super_punch"));
        step(5, () -> shot("sup04c_super_punch_blast"));
        step(12, () -> shot("sup04d_super_punch_after"));
        step(30, () -> clearMobs());
        // Super breath over the pool, at a few husks standing in the water.
        step(10, () -> {
            supTp(6.5, 0, 1.5, -90, 12);
            supMobs(13.5, 1.5, 3, 1.4);
        });
        step(10, () -> supPower(com.danrod505.greenlantern.superman.SuperPower.SUPER_BREATH));
        step(8, () -> shot("sup05_super_breath"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("sup05b_super_breath_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("sup05c_super_breath_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(30, () -> shot("sup05d_frozen_pool"));
        step(20, () -> clearMobs());
        // X-ray vision through the wall behind the start.
        step(10, () -> {
            supTp(0.5, 0, -5.5, 180, 8);
            supMobs(0.5, -10.5, 1, 0);
        });
        step(10, () -> shot("sup06_wall"));
        step(1, () -> {
            camera(CameraType.FIRST_PERSON);
            supPower(com.danrod505.greenlantern.superman.SuperPower.XRAY_VISION);
        });
        step(30, () -> shot("sup06b_xray"));
        step(2, () -> look(160, 20));
        step(10, () -> shot("sup06c_xray_angle"));
        step(2, () -> supPower(com.danrod505.greenlantern.superman.SuperPower.XRAY_VISION));
        step(20, () -> clearMobs());
        // Flight: take off, accelerate and break the sound barrier.
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            supTp(0.5, 0, 30.5, 0, 5);
        });
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(4, () -> key(mc().options.keyJump, true));
        step(10, () -> {
            key(mc().options.keyJump, false);
            look(0, 0);
        });
        step(10, () -> clean("sup07_hover"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("sup07b_hover_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, -6);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(12, () -> shot("sup08_accelerating"));
        watch(60, () -> com.danrod505.greenlantern.client.flight.FlightController.isSupersonic(), "sup08b_sound_barrier");
        step(4, () -> shot("sup08c_sound_barrier"));
        step(20, () -> shot("sup09_supersonic"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("sup09b_supersonic_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
        });
        step(4, () -> shot("sup09c_supersonic_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_BACK));
        for (int i = 0; i < 24; i++) {
            final float yaw = (i + 1) * 3.75F;
            step(1, () -> look(yaw, 0));
            if (i == 14) step(0, () -> shot("sup10_turn"));
        }
        step(4, () -> shot("sup10b_turn_trail"));
        // Dive into the ground at full speed: a landing that shakes everything.
        step(4, () -> look(90, 60));
        step(4, () -> shot("sup11_dive"));
        watch(60, () -> mc().player.onGround(), "sup11b_landing");
        step(3, () -> shot("sup11c_landing_after"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        // Power wheel.
        step(20, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 90);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("sup12_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power selected={}",
                com.danrod505.greenlantern.superman.SuperPower.selected(com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(mc().player)).id()));
        // The guide.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("superman")));
        step(10, () -> shot("sup13_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("superman_powers")));
        step(10, () -> shot("sup13b_guide_powers"));
        step(20, () -> mc().stop());
    }

    private static BlockPos wwBase = BlockPos.ZERO;

    private static void wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower power) {
        server(sp -> {
            ItemStack tiara = com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.findTiara(sp);
            com.danrod505.greenlantern.wonderwoman.AmazonPower.select(tiara, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(tiara));
            boolean used = com.danrod505.greenlantern.wonderwoman.WonderWomanServer.usePower(sp, tiara, power);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power {} used={}", power.id(), used);
        });
    }

    private static void wwTp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            if (sp.isPassenger()) sp.stopRiding();
            sp.teleportTo(wwBase.getX() + dx, wwBase.getY() + dy, wwBase.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    private static void wwMobs(double dx, double dz, int count, double spread) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                var mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                mob.snapTo(wwBase.getX() + dx + Math.cos(a) * spread, wwBase.getY(), wwBase.getZ() + dz + Math.sin(a) * spread, 180, 0);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        });
    }

    /** Arrows fired at her from a husk standing in front of her. */
    private static void wwArrows(int count) {
        server(sp -> {
            ServerLevel level = sp.level();
            var shooter = level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, sp.getBoundingBox().inflate(16)).stream().findFirst().orElse(null);
            Vec3 eye = sp.getEyePosition().add(0, -0.4, 0);
            Vec3 from = shooter != null ? shooter.getEyePosition() : eye.add(sp.getLookAngle().scale(10));
            for (int i = 0; i < count; i++) {
                var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(level, from.x + (i - count / 2.0) * 0.4, from.y, from.z, new ItemStack(Items.ARROW), null);
                if (shooter != null) arrow.setOwner(shooter);
                Vec3 to = eye.subtract(arrow.position());
                arrow.shoot(to.x, to.y, to.z, 1.6F, 0.0F);
                level.addFreshEntity(arrow);
            }
        });
    }

    /** Wonder Woman: armor, the Lasso of Truth, the bracelets, sword and shield, flight, the Invisible Jet, the wheel and the guide. */
    private static void buildWonderWomanSteps() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND,
                        com.danrod505.greenlantern.item.AmazonTiaraItem.charged(new ItemStack(ModItems.AMAZON_TIARA.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                wwBase = base;
                // Glass panes and ice around the spot of the shockwave, to shatter.
                for (int i = -3; i <= 3; i++) {
                    for (int y = 0; y < 2; y++) {
                        level.setBlock(base.offset(i, y, 44), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 2);
                        level.setBlock(base.offset(-4, y, 40 + i), net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 2);
                    }
                }
                // Pillars along the flight path give a sense of speed.
                for (int z = 120; z < 600; z += 16) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 8 + (z / 16) % 5; y++) {
                            level.setBlock(base.offset(side * 14, y, z), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                        }
                    }
                }
                com.danrod505.greenlantern.wonderwoman.WonderWomanSuit.summon(sp);
            });
            look(0, 5);
        });
        step(40, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(20, () -> {
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("ww00_armor_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("ww00b_armor_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("ww00c_armor_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("ww01_hud"));
        // Lasso of Truth: capture a husk, then swing it around and hurl it.
        step(10, () -> {
            wwTp(0.5, 0, 2.5, 0, 10);
            wwMobs(0.5, 9.5, 1, 0);
        });
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_CAPTURE));
        step(2, () -> shot("ww02_lasso_throw"));
        step(16, () -> shot("ww02b_lasso_caught"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("ww02c_lasso_caught_front"));
        step(1, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(2, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_PULL));
        step(4, () -> shot("ww03_lasso_pull"));
        step(16, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_SPIN));
        step(8, () -> shot("ww04_lasso_swing"));
        step(8, () -> shot("ww04b_lasso_swing"));
        step(20, () -> shot("ww04c_lasso_hurl"));
        step(20, () -> clearMobs());
        // The lasso whirling around her with nothing caught.
        step(10, () -> wwMobs(0.5, 2.5, 6, 3.2));
        step(10, () -> {
            look(0, 25);
            wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_SPIN);
        });
        step(6, () -> shot("ww05_lasso_whirl"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww05b_lasso_whirl_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, () -> clearMobs());
        // Bracelets of Submission: arrows bounce back where they came from.
        step(10, () -> {
            wwTp(0.5, 0, 20.5, 0, 0);
            wwMobs(0.5, 32.5, 1, 0);
        });
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.BRACELET_GUARD));
        step(4, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww06_bracelets_guard"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 2);
        });
        step(2, () -> wwArrows(3));
        step(5, () -> shot("ww06b_arrows_incoming"));
        step(3, () -> shot("ww06c_deflect"));
        step(6, () -> shot("ww06d_arrows_back"));
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.BRACELET_GUARD));
        step(20, () -> clearMobs());
        // The shockwave: the bracelets clash in a ring of husks, by the glass and ice.
        step(10, () -> {
            wwTp(0.5, 0, 40.5, 0, 15);
            wwMobs(0.5, 40.5, 8, 3.5);
        });
        step(10, () -> shot("ww07_before_shockwave"));
        step(1, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.BRACELET_SHOCKWAVE));
        step(2, () -> shot("ww07b_shockwave"));
        step(4, () -> shot("ww07c_shockwave_ring"));
        step(12, () -> shot("ww07d_shockwave_after"));
        step(30, () -> clearMobs());
        // Sword and shield.
        step(10, () -> {
            wwTp(0.5, 0, 60.5, 0, 10);
            mc().player.getInventory().setSelectedSlot(1);
            server(sp -> sp.getInventory().setSelectedSlot(1));
        });
        step(5, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.SWORD_AND_SHIELD));
        step(10, () -> shot("ww08_sword_shield"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww08b_sword_shield_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
        });
        step(4, () -> shot("ww08c_sword_shield_first_person"));
        step(1, () -> key(mc().options.keyUse, true));
        step(8, () -> shot("ww08d_shield_block_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww08e_shield_block_front"));
        step(1, () -> {
            key(mc().options.keyUse, false);
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        // The shield thrown: it bounces between the husks and comes back.
        step(10, () -> {
            wwMobs(0.5, 70.5, 1, 0);
            wwMobs(-3.5, 74.5, 1, 0);
            wwMobs(4.5, 73.5, 1, 0);
            look(0, 4);
        });
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.SHIELD_THROW));
        step(3, () -> shot("ww09_shield_throw"));
        step(5, () -> shot("ww09b_shield_bounce"));
        step(6, () -> shot("ww09c_shield_bounce"));
        watch(60, () -> com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.shieldSlot(mc().player) >= 0, "ww09d_shield_back");
        step(4, () -> shot("ww09e_shield_caught"));
        step(20, () -> clearMobs());
        step(2, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.SWORD_AND_SHIELD));
        // Flight: take off and accelerate (slower than the Lantern, never supersonic).
        step(10, () -> {
            mc().player.getInventory().setSelectedSlot(0);
            server(sp -> sp.getInventory().setSelectedSlot(0));
            wwTp(0.5, 0, 100.5, 0, 5);
        });
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(4, () -> key(mc().options.keyJump, true));
        step(10, () -> {
            key(mc().options.keyJump, false);
            look(0, 0);
        });
        step(10, () -> clean("ww10_hover"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww10b_hover_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, -6);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(20, () -> shot("ww11_accelerating"));
        step(60, () -> shot("ww11b_top_speed"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww11c_flying_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        for (int i = 0; i < 24; i++) {
            final float yaw = (i + 1) * 3.75F;
            step(1, () -> look(yaw, 0));
            if (i == 14) step(0, () -> shot("ww12_turn_trail"));
        }
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            mc().player.getAbilities().flying = false;
            mc().player.onUpdateAbilities();
        });
        watch(80, () -> mc().player.onGround(), "ww12b_landing");
        // The Invisible Jet.
        step(10, () -> wwTp(0.5, 0, 140.5, 0, 10));
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.INVISIBLE_JET));
        step(20, () -> shot("ww13_invisible_jet"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww13b_jet_front"));
        step(1, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.InvisibleJetEntity jet) {
                sp.stopRiding();
                sp.teleportTo(jet.getX() + 6.5, jet.getY() + 1.0, jet.getZ() + 1.0);
            }
        }));
        step(3, () -> {
            camera(CameraType.FIRST_PERSON);
            look(90, 15);
        });
        step(4, () -> clean("ww13c_jet_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.INVISIBLE_JET);
        });
        step(6, () -> {
            look(0, -12);
            key(mc().options.keyUp, true);
        });
        step(30, () -> shot("ww14_jet_flight"));
        step(1, () -> key(mc().options.keySprint, true));
        step(20, () -> shot("ww14b_jet_afterburner"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("ww14c_jet_first_person"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keySprint, false);
            look(0, 0);
        });
        step(10, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.JetCloakPacket()));
        step(20, () -> shot("ww15_jet_cloaked"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("ww15b_jet_cloaked_front"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, false);
            com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.JetCloakPacket());
        });
        step(20, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.INVISIBLE_JET));
        step(10, () -> wwTp(0.5, 0, 160.5, 0, 10));
        // Power wheel.
        step(20, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("ww16_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power selected={}",
                com.danrod505.greenlantern.wonderwoman.AmazonPower.selected(com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.findTiara(mc().player)).id()));
        // The guide.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("wonder_woman")));
        step(10, () -> shot("ww17_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("wonder_woman_powers")));
        step(10, () -> shot("ww17b_guide_powers"));
        step(20, () -> mc().stop());
    }

    private static com.danrod505.greenlantern.entity.KrakenEntity clientKraken() {
        var list = mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.KrakenEntity.class, mc().player.getBoundingBox().inflate(96));
        return list.isEmpty() ? null : list.getFirst();
    }

    private static void kraken(java.util.function.Consumer<com.danrod505.greenlantern.entity.KrakenEntity> action) {
        server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken != null) action.accept(kraken);
        });
    }

    /** Climbs off the Kraken and watches it from a spot given relative to it (forward, up, left of its facing). */
    private static void krakenView(double forward, double up, double left) {
        camera(CameraType.FIRST_PERSON);
        server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken == null) return;
            if (sp.getVehicle() == kraken) sp.stopRiding();
            Vec3 f = Vec3.directionFromRotation(0, kraken.getYRot());
            Vec3 l = new Vec3(f.z, 0, -f.x);
            Vec3 at = kraken.position().add(f.scale(forward)).add(l.scale(-left)).add(0, up, 0);
            Vec3 target = kraken.bodyPoint(0, -1.5, 0, 1.0F);
            Vec3 to = target.subtract(at.add(0, sp.getEyeHeight(), 0));
            float yaw = (float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI);
            float pitch = (float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI);
            sp.teleportTo(sp.level(), at.x, at.y, at.z, java.util.Set.of(), yaw, pitch, true);
            sp.setYHeadRot(yaw);
        });
    }

    private static void rideKraken() {
        server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken != null && sp.getVehicle() != kraken) sp.startRiding(kraken);
        });
        camera(CameraType.THIRD_PERSON_BACK);
    }

    /** Moves the Kraken (and Aquaman off its back) to a spot relative to where the script started, facing south. */
    private static void placeKraken(double dx, double dy, double dz) {
        server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken == null || aquaBase == null) return;
            if (sp.getVehicle() == kraken) sp.stopRiding();
            kraken.setDeltaMovement(Vec3.ZERO);
            kraken.teleportTo(sp.level(), aquaBase.getX() + 0.5 + dx, aquaBase.getY() + dy, aquaBase.getZ() + 0.5 + dz,
                    java.util.Set.of(), 0.0F, 0.0F, true);
            kraken.setYRot(0.0F);
        });
    }

    /** The Kraken: rising out of the ground, poses, walking, tentacle slam, water jet, swimming, wounds and death. */
    private static void buildKrakenSteps() {
        step(60, () -> {
            mc().options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, com.danrod505.greenlantern.item.AquamanEmblemItem.charged(new ItemStack(ModItems.AQUAMAN_EMBLEM.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                aquaBase = base;
                var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
                var wall = net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState();
                var sand = net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
                var light = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
                // A deep sea for the Kraken: 49 wide, 80 long, 18 deep, behind the shore where it starts.
                for (int x = -24; x <= 24; x++) {
                    for (int z = 40; z <= 120; z++) {
                        boolean edge = x == -24 || x == 24 || z == 40 || z == 120;
                        level.setBlock(base.offset(x, -1, z), (x * 7 + z * 3) % 19 == 0 ? light : sand, 2);
                        for (int y = 0; y < 18; y++) {
                            level.setBlock(base.offset(x, y, z), edge ? wall : water, 2);
                        }
                    }
                }
                var corals = List.of(net.minecraft.world.level.block.Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
                        net.minecraft.world.level.block.Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                        net.minecraft.world.level.block.Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
                        net.minecraft.world.level.block.Blocks.HORN_CORAL_BLOCK.defaultBlockState());
                for (int z = 46; z < 116; z += 7) {
                    for (int side = -1; side <= 1; side += 2) {
                        int h = 2 + (z / 7) % 5;
                        for (int y = 0; y < h; y++) level.setBlock(base.offset(side * 15, y, z), corals.get((z / 7 + y) % 4), 2);
                    }
                }
                // A few trees on the shore, for scale.
                var log = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
                var leaves = net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState();
                for (int[] t : new int[][] {{-14, 6}, {16, 14}, {-18, 26}}) {
                    for (int y = 0; y < 5; y++) level.setBlock(base.offset(t[0], y, t[1]), log, 2);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            for (int y = 3; y <= 5; y++) {
                                if (Math.abs(dx) + Math.abs(dz) < 4 && !(dx == 0 && dz == 0 && y < 5)) level.setBlock(base.offset(t[0] + dx, y, t[1] + dz), leaves, 2);
                            }
                        }
                    }
                }
                com.danrod505.greenlantern.aquaman.AquamanSuit.summon(sp);
            });
            look(0, 10);
        });
        step(30, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mc().gui.getChat().clearMessages(false);
        });
        // Rising out of the ground.
        step(10, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.KRAKEN));
        step(12, () -> clean("kr00_emerge"));
        step(12, () -> clean("kr00b_emerge"));
        step(30, () -> clean("kr01_ride_back"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 15);
        });
        step(6, () -> clean("kr02_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        // From the outside: in front, from the side, from behind.
        step(4, () -> krakenView(20, 0, 3));
        step(12, () -> clean("kr03_view_front"));
        step(2, () -> krakenView(2, 0, 22));
        step(12, () -> clean("kr03b_view_side"));
        step(2, () -> krakenView(-18, 4, -10));
        step(12, () -> clean("kr03c_view_back"));
        // Walking towards Aquaman, heavy steps, seen from the side.
        step(2, () -> krakenView(30, 0, 18));
        step(30, () -> clean("kr04_walk_a"));
        step(9, () -> clean("kr04b_walk_b"));
        step(9, () -> clean("kr04c_walk_c"));
        step(9, () -> clean("kr04d_walk_d"));
        // Riding it across the shore.
        step(20, () -> {
            rideKraken();
            look(0, 15);
        });
        step(4, () -> key(mc().options.keyUp, true));
        step(26, () -> mc().options.hideGui = false);
        step(4, () -> shot("kr05_ride_walk_hud"));
        step(10, () -> clean("kr05b_ride_walk"));
        step(1, () -> key(mc().options.keyUp, false));
        // The tentacle slam, at two husks in front of it (seen from the side).
        step(25, () -> server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken == null) return;
            Vec3 at = kraken.position().add(Vec3.directionFromRotation(0, kraken.getYRot()).scale(9.5));
            for (int i = 0; i < 2; i++) {
                var husk = EntityType.HUSK.create(sp.level(), EntitySpawnReason.COMMAND);
                husk.snapTo(at.x + i * 1.5 - 0.75, at.y, at.z, 180, 0);
                husk.setNoAi(true);
                husk.setPersistenceRequired();
                sp.level().addFreshEntity(husk);
            }
        }));
        step(2, () -> krakenView(4, 0, 15.5));
        step(10, () -> kraken(k -> k.tentacleSlam()));
        step(3, () -> clean("kr06_slam_windup"));
        step(3, () -> clean("kr06b_slam_raised"));
        step(3, () -> clean("kr06c_slam_strike"));
        step(2, () -> clean("kr06d_slam_impact"));
        step(30, () -> rideKraken());
        // The rider's view of a slam and of the water jet.
        step(10, () -> look(0, 20));
        step(2, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(
                new com.danrod505.greenlantern.network.KrakenAttackPacket(com.danrod505.greenlantern.network.KrakenAttackPacket.SLAM)));
        step(6, () -> clean("kr06e_slam_rider_view"));
        step(30, () -> look(-20, 25));
        step(2, () -> key(mc().options.keyUse, true));
        step(10, () -> clean("kr07_jet"));
        step(10, () -> look(20, 30));
        step(10, () -> clean("kr07b_jet_sweep"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 25);
        });
        step(6, () -> clean("kr07c_jet_front"));
        step(2, () -> {
            key(mc().options.keyUse, false);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        // Into the deep sea: it stretches out and races through the water.
        step(20, () -> {
            kraken(k -> k.retreat());
            aquaTeleport(0.5, 6, 50.5, 0, 10);
        });
        step(20, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.KRAKEN));
        step(40, () -> clean("kr08_swim_idle"));
        step(2, () -> key(mc().options.keyUp, true));
        step(20, () -> clean("kr09_swim"));
        step(1, () -> key(mc().options.keySprint, true));
        step(20, () -> clean("kr09b_swim_fast"));
        step(2, () -> look(-50, 20));
        step(10, () -> clean("kr09c_swim_turn"));
        step(2, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            look(0, 0);
        });
        step(10, () -> placeKraken(0, 4, 80));
        step(6, () -> rideKraken());
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(6, () -> clean("kr10_swim_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(2, () -> krakenView(4, 0, 16));
        step(14, () -> clean("kr11_swim_side"));
        // Wounds and death (seen from the side, the HUD showing its life).
        step(10, () -> kraken(k -> k.hurtServer((ServerLevel) k.level(), k.level().damageSources().generic(), 150.0F)));
        step(1, () -> mc().options.hideGui = false);
        step(3, () -> shot("kr12_hurt_hud"));
        step(20, () -> kraken(k -> k.hurtServer((ServerLevel) k.level(), k.level().damageSources().generic(), 10000.0F)));
        step(10, () -> clean("kr13_death_a"));
        step(25, () -> clean("kr13b_death_b"));
        step(20, () -> clean("kr13c_death_c"));
        step(30, () -> server(sp -> com.danrod505.greenlantern.entity.KrakenEntity.clearRecovery(sp)));
        // Power wheel, hovering the Kraken (fifth of five).
        step(10, () -> {
            mc().options.hideGui = false;
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true);
        });
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 288);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("kr14_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("kraken")));
        step(10, () -> shot("kr15_guide"));
        step(20, () -> mc().stop());
    }


    private static com.danrod505.greenlantern.atlantis.Atlantis.Site atlantis;

    /** Teleports to a spot given relative to the center of Atlantis (y relative to the city floor). */
    private static void atTp(double dx, double dy, double dz, float yaw, float pitch) {
        if (atlantis == null) return;
        command(String.format(java.util.Locale.ROOT, "execute in minecraft:overworld run tp @a %.2f %.2f %.2f %.1f %.1f",
                atlantis.x() + 0.5 + dx, atlantis.floor() + dy, atlantis.z() + 0.5 + dz, yaw, pitch));
    }

    /** Teleports next to the nearest Atlantean (citizen or guard), looking at them. */
    private static void nearAtlantean(boolean guard) {
        var people = new java.util.ArrayList<>(mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.AtlanteanEntity.class, mc().player.getBoundingBox().inflate(120)));
        people.removeIf(a -> a.isGuard() != guard);
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT atlanteans guard={} near={}", guard, people.size());
        if (people.isEmpty()) return;
        var a = people.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(mc().player))).get();
        double x = a.getX() + 2.6;
        double z = a.getZ() + 2.6;
        float yaw = (float) Math.toDegrees(Math.atan2(-(a.getX() - x), a.getZ() - z));
        command(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, a.getY() + 0.4, z, yaw, 8.0F));
    }

    /** Atlantis: the gate, the arrival pavilion, the respirator on a hero, the city, its people and the way home. */
    private static void buildAtlantisSteps() {
        step(80, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            command("effect give @a night_vision infinite 0 true");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getInventory().setItem(9, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                Uniform.summon(sp);
                sp.getInventory().setItem(10, new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
                sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ATLANTIS_GATE.get()));
                atlantis = com.danrod505.greenlantern.atlantis.Atlantis.site(sp.level().getServer());
                com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT atlantis site={}", atlantis);
            });
            look(0, 8);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, ClientScript::use);
        step(25, () -> clean("a00_gate_portal"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("a00b_gate_portal_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mc().options.hideGui = false;
            key(mc().options.keyUp, true);
        });
        step(30, () -> key(mc().options.keyUp, false));
        // If the walk missed the opening (uneven ground), step right into it.
        step(20, () -> server(sp -> {
            if (atlantis == null || sp.position().distanceTo(atlantis.arrival()) < 3) return;
            var portals = sp.level().getEntitiesOfClass(com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, sp.getBoundingBox().inflate(8));
            if (portals.isEmpty()) return;
            var portal = portals.getFirst();
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT walk missed the portal: player={} portal={}", sp.position(), portal.position());
            sp.teleportTo(portal.getX(), portal.getY(), portal.getZ());
        }));
        // The first trip builds the whole city at once: wait until the player stands in the pavilion.
        waitFor(1200, () -> atlantis != null && mc().player != null && mc().player.position().distanceTo(atlantis.arrival()) < 3);
        step(60, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT arrived dimension={} pos={}", mc().level.dimension(), mc().player.position());
            camera(CameraType.FIRST_PERSON);
            look(180, 2);
        });
        step(40, () -> clean("a01_pavilion"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("a01b_arrival_lantern"));
        // Out in the water: the respirator breathes for the Lantern (the mask stays on).
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            mc().options.hideGui = false;
            atTp(4.5, 1, 32, 160, 0);
        });
        step(60, () -> {
            mc().gui.getChat().clearMessages(false);
            shot("a02_respirator_hud");
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT underwater={} air={} respirator={}", mc().player.isUnderWater(), mc().player.getAirSupply(),
                    com.danrod505.greenlantern.aquaman.Respirator.air(com.danrod505.greenlantern.aquaman.Respirator.find(mc().player)));
        });
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(6, () -> clean("a03_respirator_face"));
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            server(sp -> {
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            nearAtlantean(false);
        });
        step(40, () -> clean("a04_citizen"));
        step(2, () -> nearAtlantean(true));
        step(20, () -> clean("a05_guard"));
        // Let the eyes get used to the deep (vanilla water vision takes 30 seconds underwater).
        step(2, () -> atTp(0, 2, 8, 180, 4));
        step(80, () -> clean("a06_throne_hall"));
        step(2, () -> atTp(-22, 6, 30, 200, 4));
        step(80, () -> clean("a07_statue"));
        step(2, () -> atTp(23, 4, 42, 180, 6));
        step(80, () -> clean("a08_garden"));
        step(2, () -> atTp(-18, 14, 50, 200, 14));
        step(200, () -> clean("a09_towers_houses"));
        step(2, () -> atTp(0, 10, 54, 180, -6));
        step(120, () -> clean("a10_palace"));
        step(2, () -> atTp(36, 34, 36, 135, 30));
        step(120, () -> clean("a11_city_aerial"));
        step(2, () -> atTp(-64, 30, -20, 290, 18));
        step(120, () -> clean("a12_kelp_slopes"));
        step(2, () -> atTp(24, 60, 44, 150, 30));
        step(120, () -> clean("a13_beacon_from_surface"));
        // Aquaman opens the way home with his own power.
        step(2, () -> {
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.getInventory().setItem(11, com.danrod505.greenlantern.item.AquamanEmblemItem.charged(new ItemStack(ModItems.AQUAMAN_EMBLEM.get())));
                com.danrod505.greenlantern.aquaman.AquamanSuit.summon(sp);
            });
            atTp(0, 0, 40, 180, 6);
        });
        step(30, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.ATLANTIS_PORTAL);
        });
        step(25, () -> clean("a14_aquaman_portal_home"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
            server(sp -> {
                // Swim into the whirlpool.
                var portals = sp.level().getEntitiesOfClass(com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, sp.getBoundingBox().inflate(8));
                if (!portals.isEmpty()) sp.teleportTo(portals.getFirst().getX(), portals.getFirst().getY() + 0.2, portals.getFirst().getZ());
            });
        });
        step(80, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT back home dimension={} pos={}", mc().level.dimension(), mc().player.position());
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, () -> clean("a15_back_home"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("atlantis"));
        });
        step(10, () -> shot("a16_guide"));
        step(20, () -> mc().stop());
    }

    // ---- v1.13: the Trench ------------------------------------------------------------------------

    private static volatile com.danrod505.greenlantern.trench.Trench.Nest trenchNest;
    private static final List<com.danrod505.greenlantern.entity.TrenchCreatureEntity> TRENCH_POSED = new ArrayList<>();

    /** Teleports to a point (absolute), looking at another. */
    private static void tpLook(Vec3 from, Vec3 at) {
        double dx = at.x - from.x;
        double dy = at.y - from.y;
        double dz = at.z - from.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        command(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", from.x, from.y, from.z, yaw, pitch));
    }

    /** Like {@link #tpLook}, but rises from the point until the camera is in open water (not inside a wall or a spire). */
    private static void tpLookWater(Vec3 from, Vec3 at) {
        server(sp -> {
            Vec3 p = from;
            for (int i = 0; i < 30; i++) {
                BlockPos b = BlockPos.containing(p);
                if (sp.level().getBlockState(b).is(net.minecraft.world.level.block.Blocks.WATER)
                        && sp.level().getBlockState(b.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                        && sp.level().getBlockState(b.below()).is(net.minecraft.world.level.block.Blocks.WATER)) break;
                p = p.add(0, 1, 0);
            }
            tpLook(p, at);
        });
    }

    /** Relative to the nest's center (on its floor). */
    private static Vec3 atNest(double dx, double dy, double dz) {
        var n = trenchNest;
        return new Vec3(n.x() + 0.5 + dx, n.floor() + dy, n.z() + 0.5 + dz);
    }

    /** Poses a creature (or brute) with no AI at a point, facing a direction; returns it for more posing. */
    private static void poseCreature(Vec3 pos, boolean brute, float yaw, boolean carrying) {
        server(sp -> {
            var c = com.danrod505.greenlantern.entity.TrenchCreatureEntity.spawn(sp.level(), pos, brute, -1, 0);
            if (c == null) return;
            c.setNoAi(true);
            c.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
            c.setYHeadRot(yaw);
            c.setYBodyRot(yaw);
            TRENCH_POSED.add(c);
            if (carrying) {
                var villager = net.minecraft.world.entity.EntityType.VILLAGER.create(sp.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (villager == null) return;
                villager.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
                villager.setNoAi(true);
                sp.level().addFreshEntity(villager);
                villager.startRiding(c, true, true);
            }
        });
    }

    private static void clearTrenchPosed() {
        server(sp -> {
            for (var c : TRENCH_POSED) {
                if (c.getFirstPassenger() != null) c.getFirstPassenger().discard();
                c.discard();
            }
            TRENCH_POSED.clear();
        });
    }

    /** The Trench: the dark territory, the pit and its nest, the creatures, a cocoon and its rescue, a raid on Atlantis, the guide. */
    private static void buildTrenchSteps() {
        step(80, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule advance_weather false");
            command("gamerule spawn_mobs false");
            server(sp -> {
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.getInventory().setItem(10, new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
                sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                var server = sp.level().getServer();
                // The trip builds Atlantis; the nests are dug around it right away.
                boolean ok = com.danrod505.greenlantern.atlantis.AtlantisTravel.sendToAtlantis(sp);
                atlantis = com.danrod505.greenlantern.atlantis.Atlantis.site(server);
                boolean built = com.danrod505.greenlantern.trench.TrenchBuilder.ensureBuilt(server);
                var nests = com.danrod505.greenlantern.trench.Trench.nests(server);
                com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench travel={} built={} atlantis={} nests={}", ok, built, atlantis, nests);
                if (!nests.isEmpty()) trenchNest = nests.getFirst();
            });
        });
        waitFor(2400, () -> trenchNest != null && com.danrod505.greenlantern.trench.Trench.isComplete(mc().getSingleplayerServer()));
        // From the edge: the dark water of the Trench beyond the last fish of the open sea.
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            var n = trenchNest;
            tpLookWater(atNest(com.danrod505.greenlantern.trench.Trench.TERRITORY + 6, n.rim() - n.floor() + 4, 0), atNest(com.danrod505.greenlantern.trench.Trench.PIT_RADIUS, n.rim() - n.floor() - 4, 0));
        });
        step(200, () -> clean("t00_territory_edge"));
        // Crossing in: the warning and the clicks from the caves.
        step(2, () -> {
            mc().options.hideGui = false;
            var n = trenchNest;
            tpLookWater(atNest(com.danrod505.greenlantern.trench.Trench.PIT_RADIUS + 10, n.rim() - n.floor() + 2, 4), atNest(0, 8, 0));
        });
        step(25, () -> shot("t01_territory_warning"));
        step(160, () -> clean("t02_territory_dark"));
        // With night vision from here on, to see the details.
        step(2, () -> {
            command("effect give @a night_vision infinite 0 true");
            var n = trenchNest;
            tpLookWater(atNest(9, 24, 9), atNest(0, 2, 0));
        });
        step(120, () -> clean("t03_pit_from_above"));
        step(2, () -> tpLookWater(atNest(15, 10, -4), atNest(0, 5, 0)));
        step(120, () -> clean("t04_brood_mound_ribs"));
        // The creatures, posed close, in the dark water of their own sea (their eyes glow).
        step(2, () -> {
            command("effect clear @a night_vision");
            Vec3 eye = atNest(8, 12, 8);
            poseCreature(eye.add(-3.2, -1.2, 0.0), false, -90.0F, false);
            tpLook(eye, eye.add(-3.2, -0.6, 0.0));
        });
        step(60, () -> clean("t05_creature"));
        step(2, () -> {
            clearTrenchPosed();
            Vec3 eye = atNest(8, 12, 8);
            poseCreature(eye.add(-4.5, -2.0, 0.0), true, -90.0F, false);
            poseCreature(eye.add(-5.5, -1.0, 2.5), false, -115.0F, false);
            poseCreature(eye.add(-5.5, -1.5, -2.5), false, -65.0F, false);
            tpLook(eye, eye.add(-4.5, -0.8, 0.0));
        });
        step(60, () -> clean("t06_brute_and_pack"));
        step(2, () -> {
            clearTrenchPosed();
            Vec3 eye = atNest(8, 12, 8);
            command("effect give @a night_vision infinite 0 true");
            poseCreature(eye.add(-3.5, -1.2, 0.0), false, -60.0F, true);
            tpLook(eye, eye.add(-3.5, -0.4, 0.0));
        });
        step(60, () -> clean("t07_carrying_villager"));
        // The living nest: its own packs on patrol.
        step(2, () -> {
            clearTrenchPosed();
            command("effect give @a night_vision infinite 0 true");
            tpLookWater(atNest(-10, 14, -10), atNest(0, 6, 0));
        });
        step(100, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench creatures near={}", mc().level.getEntitiesOfClass(
                    com.danrod505.greenlantern.entity.TrenchCreatureEntity.class, mc().player.getBoundingBox().inflate(64)).size());
            clean("t08_nest_alive");
        });
        // A cocoon in a chamber, with a villager inside.
        step(2, () -> server(sp -> {
            var cocoons = com.danrod505.greenlantern.trench.TrenchLife.cocoons(sp.level(), trenchNest);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench cocoons={}", cocoons.size());
            Vec3 at;
            if (cocoons.isEmpty()) {
                at = com.danrod505.greenlantern.trench.TrenchNest.cocoonSpots(trenchNest).getFirst();
            } else {
                at = cocoons.getFirst().position();
            }
            Vec3 center = atNest(0, at.y - trenchNest.floor(), 0);
            Vec3 out = center.subtract(at).multiply(1, 0, 1).normalize();
            Vec3 eye = at.add(out.scale(3.2)).add(0, 1.4, 0);
            tpLook(eye, at.add(0, 1.1, 0));
        }));
        step(80, () -> clean("t09_cocoon"));
        step(2, () -> {
            mc().options.hideGui = false;
            server(sp -> {
                var cocoons = sp.level().getEntitiesOfClass(com.danrod505.greenlantern.entity.TrenchCocoonEntity.class, sp.getBoundingBox().inflate(6));
                if (!cocoons.isEmpty()) cocoons.getFirst().hurtServer(sp.level(), sp.level().damageSources().playerAttack(sp), 50.0F);
            });
        });
        step(6, () -> shot("t10_rescue_burst"));
        step(20, () -> clean("t11_villager_freed"));
        // A raid on Atlantis: the war party at the wall, under the boss bar.
        step(2, () -> {
            mc().options.hideGui = false;
            var a = atlantis;
            if (a == null) return;
            var n = trenchNest;
            double ang = Math.atan2(n.z() - a.z(), n.x() - a.x());
            Vec3 inside = new Vec3(a.x() + 0.5 + Math.cos(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS - 14), a.floor() + 14,
                    a.z() + 0.5 + Math.sin(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS - 14));
            Vec3 wall = new Vec3(a.x() + 0.5 + Math.cos(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS + 6), a.floor() + 12,
                    a.z() + 0.5 + Math.sin(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS + 6));
            tpLook(inside, wall);
        });
        step(40, () -> server(sp -> {
            if (atlantis == null) return;
            var raiders = com.danrod505.greenlantern.trench.TrenchLife.startRaid(sp.level(), atlantis, List.of(trenchNest));
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench raid raiders={}", raiders.size());
        }));
        step(30, () -> shot("t12_raid_warning"));
        step(60, () -> shot("t13_raid_at_the_wall"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("trench"));
        });
        step(10, () -> shot("t14_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("trench_captives")));
        step(10, () -> shot("t15_guide_captives"));
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
    // ---- v1.12: the swim build-up and its music, the shark's lunge, the creatures of Atlantis, the guide ----

    private static final List<com.danrod505.greenlantern.entity.AtlanteanMountEntity> POSED = new ArrayList<>();

    /** A glass-walled sea, 41 wide, 100 long and 12 deep, with coral pillars (same as the Aquaman script). */
    private static void buildSea(ServerPlayer sp) {
        ServerLevel level = sp.level();
        BlockPos base = sp.blockPosition();
        aquaBase = base;
        var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        var wall = net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState();
        var sand = net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
        var light = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
        for (int x = -21; x <= 21; x++) {
            for (int z = 3; z <= 105; z++) {
                boolean edge = x == -21 || x == 21 || z == 3 || z == 105;
                level.setBlock(base.offset(x, -1, z), (x * 7 + z * 3) % 23 == 0 ? light : sand, 2);
                for (int y = 0; y < 12; y++) {
                    level.setBlock(base.offset(x, y, z), edge ? wall : water, 2);
                }
            }
        }
        var corals = List.of(net.minecraft.world.level.block.Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.HORN_CORAL_BLOCK.defaultBlockState());
        for (int z = 10; z < 100; z += 9) {
            for (int side = -1; side <= 1; side += 2) {
                int h = 2 + (z / 9) % 4;
                for (int y = 0; y < h; y++) level.setBlock(base.offset(side * 16, y, z), corals.get((z / 9 + y) % 4), 2);
            }
        }
    }

    /** Puts a creature of Atlantis in the sea (posed: no AI) and returns it. */
    private static <T extends com.danrod505.greenlantern.entity.AtlanteanMountEntity> void pose(
            java.util.function.Supplier<EntityType<T>> type, double dx, double dy, double dz, float yaw, int variant, boolean posed) {
        server(sp -> {
            T mount = type.get().create(sp.level(), EntitySpawnReason.COMMAND);
            mount.snapTo(aquaBase.getX() + dx, aquaBase.getY() + dy, aquaBase.getZ() + dz, yaw, 0);
            mount.setYBodyRot(yaw);
            mount.setYHeadRot(yaw);
            mount.setVariant(variant);
            mount.setNoAi(posed);
            mount.setPersistenceRequired();
            sp.level().addFreshEntity(mount);
            POSED.add(mount);
        });
    }

    private static void clearPosed() {
        server(sp -> {
            for (var m : POSED) m.discard();
            POSED.clear();
        });
    }

    private static void rideNearest(Class<? extends com.danrod505.greenlantern.entity.AtlanteanMountEntity> type) {
        server(sp -> {
            var list = sp.level().getEntitiesOfClass(type, sp.getBoundingBox().inflate(40));
            list.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(sp)));
            if (!list.isEmpty()) {
                list.getFirst().setNoAi(false);
                sp.startRiding(list.getFirst());
            }
        });
    }

    private static void logSwim(String what) {
        var p = mc().player;
        String vehicle = p.getVehicle() == null ? "-" : String.format(java.util.Locale.ROOT, "%.2f",
                Math.sqrt(p.getVehicle().distanceToSqr(p.getVehicle().xo, p.getVehicle().yo, p.getVehicle().zo)));
        com.danrod505.greenlantern.GreenLantern.LOGGER.info(String.format(java.util.Locale.ROOT,
                "CLIENTSCRIPT %s swim speed=%.2f boost=%.2f vehicle=%s music=%s", what,
                com.danrod505.greenlantern.client.aqua.SwimController.speed(), com.danrod505.greenlantern.client.aqua.SwimController.boost(),
                vehicle, com.danrod505.greenlantern.client.aqua.SwimAudio.describeMusic()));
    }

    private static com.danrod505.greenlantern.entity.GreatWhiteSharkEntity clientShark() {
        return mc().player.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark ? shark : null;
    }

    private static boolean lunge(float from, float to) {
        var shark = clientShark();
        if (shark == null) return false;
        float p = shark.lungeProgress(0.0F) * com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.LUNGE_TICKS;
        return p >= from && p < to;
    }

    /** v1.12: gradual sprint swimming and its music, the shark's lunge and bite, the creatures of Atlantis and the guide. */
    private static void buildMountsSteps() {
        step(60, () -> {
            mc().options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc().getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, com.danrod505.greenlantern.item.AquamanEmblemItem.charged(new ItemStack(ModItems.AQUAMAN_EMBLEM.get())));
                sp.getInventory().setItem(1, new ItemStack(ModItems.MANTA_RAY_EGG.get(), 4));
                sp.getInventory().setItem(2, new ItemStack(ModItems.GIANT_SEAHORSE_EGG.get(), 4));
                sp.getInventory().setItem(3, new ItemStack(ModItems.ATLANTEAN_DOLPHIN_EGG.get(), 4));
                sp.getInventory().setItem(4, new ItemStack(ModItems.GUIDE_BOOK.get()));
                buildSea(sp);
                com.danrod505.greenlantern.aquaman.AquamanSuit.summon(sp);
            });
            look(0, 5);
        });
        // 1. The swim builds up gradually: log the speed every few ticks while sprinting.
        step(30, () -> aquaTeleport(0.5, 5, 6.5, 0, 0));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, true);
        });
        step(15, () -> logSwim("cruise"));
        step(1, () -> key(mc().options.keySprint, true));
        for (int i = 0; i < 10; i++) {
            final int n = i;
            step(5, () -> logSwim("ramp t=" + (n + 1) * 5));
            if (i == 5) step(0, () -> shot("mt01_swim_ramp"));
        }
        step(1, () -> shot("mt02_swim_top"));
        step(1, () -> key(mc().options.keySprint, false));
        step(0, () -> aquaTeleport(0.5, 5, 10.5, 0, 0));
        for (int i = 0; i < 8; i++) {
            final int n = i;
            step(5, () -> logSwim("ease t=" + (n + 1) * 5));
        }
        step(1, () -> key(mc().options.keyUp, false));
        // 2. The shark: lunge and bite, seen from the front.
        step(20, () -> aquaTeleport(0.5, 5, 14.5, 0, 0));
        step(10, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.SHARK));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, -8);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("mt03_shark_front_idle"));
        step(1, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark) {
                Vec3 mouth = shark.mouth().add(shark.facing().scale(2.0));
                var drowned = EntityType.DROWNED.create(sp.level(), EntitySpawnReason.COMMAND);
                drowned.snapTo(mouth.x, mouth.y - 0.8, mouth.z, 180, 0);
                drowned.setNoAi(true);
                sp.level().addFreshEntity(drowned);
            }
        }));
        watch(() -> lunge(3, 5), "mt04_shark_jaws_open");
        watch(() -> lunge(6, 8), "mt04b_shark_lunge");
        watch(() -> lunge(9, 11), "mt04c_shark_snap");
        watch(() -> lunge(12, 14), "mt04d_shark_thrash");
        step(4, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.SharkBitePacket()));
        step(30, () -> camera(CameraType.THIRD_PERSON_BACK));
        // From the side: off the shark, let it lunge at a drowned on its own.
        step(4, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark) {
                sp.stopRiding();
                sp.teleportTo(shark.getX() + 6.5, shark.getY() + 0.3, shark.getZ() + 1.5);
            }
        }));
        step(3, () -> {
            look(90, 5);
            camera(CameraType.FIRST_PERSON);
            mc().options.hideGui = true;
        });
        step(2, () -> aquaMobs(EntityType.DROWNED, -4, 3, 22, 2, true));
        watch(() -> mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.class, mc().player.getBoundingBox().inflate(30))
                .stream().anyMatch(s -> s.lungeProgress(0) > 0.15F && s.lungeProgress(0) < 0.35F), "mt04e_shark_side_lunge");
        step(80, () -> server(sp -> {
            var shark = com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.find(sp);
            if (shark != null) shark.swimAway();
            sp.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Drowned.class, sp.getBoundingBox().inflate(40)).forEach(e -> e.discard());
        }));
        // 3. The creatures of Atlantis, posed: seahorses (8 colors, 3 patterns), dolphins (6 colors), the manta.
        step(10, () -> {
            for (int i = 0; i < 8; i++) {
                pose(com.danrod505.greenlantern.registry.ModEntities.GIANT_SEAHORSE, -15.75 + i * 4.5, 2, 26.5, 180, i * 3 + i % 3, true);
            }
        });
        step(20, () -> aquaTeleport(0.5, 5, 5.5, 0, 3));
        step(20, () -> clean("mt05_seahorses"));
        step(2, () -> aquaTeleport(-9.5, 4.5, 19.5, 0, 5));
        step(10, () -> clean("mt05b_seahorses_close"));
        step(2, () -> {
            clearPosed();
            for (int i = 0; i < 6; i++) {
                pose(com.danrod505.greenlantern.registry.ModEntities.ATLANTEAN_DOLPHIN, -9.5 + i * 3.8, 4 + (i % 2) * 2, 24.5, 90, i, true);
            }
        });
        step(20, () -> clean("mt06_dolphins"));
        step(2, () -> {
            command("time set 18000");
            command("effect clear @a");
        });
        step(20, () -> clean("mt06b_dolphins_night_glow"));
        step(2, () -> command("time set 6000"));
        step(2, () -> {
            clearPosed();
            pose(com.danrod505.greenlantern.registry.ModEntities.MANTA_RAY, 0.5, 4, 26.5, 160, 0, true);
            pose(com.danrod505.greenlantern.registry.ModEntities.MANTA_RAY, -10.5, 7, 34.5, 200, 1, true);
            pose(com.danrod505.greenlantern.registry.ModEntities.MANTA_RAY, 11.5, 6, 36.5, 180, 2, true);
        });
        step(20, () -> clean("mt07_mantas"));
        step(2, () -> aquaTeleport(0.5, 10.5, 19.5, 0, 40));
        step(10, () -> clean("mt07b_manta_from_above"));
        // 4. Riding: the manta, swimming and then gliding out of the sea.
        step(2, () -> {
            mc().options.hideGui = false;
            aquaTeleport(0.5, 5, 25.5, 0, 0);
        });
        step(10, () -> rideNearest(com.danrod505.greenlantern.entity.MantaRayEntity.class));
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 5);
        });
        step(5, () -> shot("mt08_manta_ride"));
        step(1, () -> {
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(40, () -> {
            logSwim("manta");
            shot("mt08b_manta_ride_fast");
        });
        step(1, () -> look(0, -40));
        watch(80, () -> mc().player.getVehicle() != null && !mc().player.getVehicle().isInWater()
                && mc().player.getVehicle().getY() > aquaBase.getY() + 12.8, "mt09_manta_leap");
        step(1, () -> look(0, 10));
        step(10, () -> shot("mt09b_manta_glide"));
        step(30, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            server(sp -> sp.stopRiding());
            clearPosed();
        });
        // The seahorse.
        step(10, () -> {
            pose(com.danrod505.greenlantern.registry.ModEntities.GIANT_SEAHORSE, 0.5, 3, 40.5, 0, 4, false);
            aquaTeleport(0.5, 4, 40.5, 0, 5);
        });
        step(10, () -> rideNearest(com.danrod505.greenlantern.entity.GiantSeahorseEntity.class));
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            mc().options.hideGui = true;
        });
        step(5, () -> clean("mt10_seahorse_ride_front"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(30, () -> {
            logSwim("seahorse");
            shot("mt10b_seahorse_ride_fast");
        });
        step(2, () -> look(-60, 0));
        step(10, () -> shot("mt10c_seahorse_turn"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            server(sp -> sp.stopRiding());
            clearPosed();
        });
        // The dolphin: racing with its trail of light, then a leap.
        step(10, () -> {
            pose(com.danrod505.greenlantern.registry.ModEntities.ATLANTEAN_DOLPHIN, 0.5, 4, 30.5, 0, 1, false);
            aquaTeleport(0.5, 5, 30.5, 0, 0);
        });
        step(10, () -> rideNearest(com.danrod505.greenlantern.entity.AtlanteanDolphinEntity.class));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 5);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(35, () -> {
            logSwim("dolphin");
            shot("mt11_dolphin_race");
        });
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            mc().options.hideGui = true;
        });
        step(3, () -> clean("mt11c_dolphin_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mc().options.hideGui = false;
            look(0, -45);
        });
        watch(60, () -> mc().player.getVehicle() != null && !mc().player.getVehicle().isInWater()
                && mc().player.getVehicle().getY() > aquaBase.getY() + 13.0, "mt11b_dolphin_leap");
        step(30, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            server(sp -> sp.stopRiding());
            clearPosed();
        });
        // 5. The eggs in the hotbar and hatching one on the water.
        step(10, () -> {
            aquaTeleport(0.5, 12.2, 60.5, 0, 50);
            mc().player.getInventory().setSelectedSlot(3);
        });
        step(10, () -> server(sp -> {
            sp.getInventory().setSelectedSlot(3);
            BlockPos floor = aquaBase.offset(0, 10, 63);
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor), net.minecraft.core.Direction.UP, floor, false);
            sp.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(sp, InteractionHand.MAIN_HAND, hit));
        }));
        step(20, () -> shot("mt12_eggs_hotbar"));
        // 6. The Heroes' Guide: sections and subsections.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("welcome")));
        step(10, () -> shot("mt13_guide_welcome"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("atlantis_creatures")));
        step(10, () -> shot("mt13b_guide_creatures"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("aquaman_shark")));
        step(10, () -> shot("mt13c_guide_shark"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("recipes_atlantis")));
        step(10, () -> shot("mt13d_guide_recipes"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("controls_lantern")));
        step(10, () -> shot("mt13e_guide_lantern"));
        step(20, () -> mc().stop());
    }

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
                // Atlantis (and the Trench around it) needs a real ocean: a normal world for those scripts, a flat one for the others.
                boolean normal = "atlantis".equals(System.getenv("GL_CLIENT_SCRIPT")) || "trench".equals(System.getenv("GL_CLIENT_SCRIPT"));
                mc.createWorldOpenFlows().createFreshLevel("gltest-" + System.currentTimeMillis(), settings,
                        new WorldOptions(2814L, false, false), normal ? WorldPresets::createNormalWorldDimensions : WorldPresets::createFlatWorldDimensions, mc.screen);
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
        if (mc.player.tickCount % 10 == 0 && "aquaman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} swim={} speed={} water={} pos={} vehicle={}", mc.player.tickCount,
                    com.danrod505.greenlantern.client.aqua.SwimController.isSwimming(),
                    String.format("%.2f", com.danrod505.greenlantern.client.aqua.SwimController.speed()),
                    mc.player.isInWater(), mc.player.blockPosition(), mc.player.getVehicle());
        }
        if (mc.player.tickCount % 10 == 0 && "kraken".equals(System.getenv("GL_CLIENT_SCRIPT"))
                && mc.player.getVehicle() instanceof com.danrod505.greenlantern.entity.KrakenEntity k) {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} kraken pos={} swimming={} walk={} swimPower={} health={} jet={} rider dy={}",
                    mc.player.tickCount, k.blockPosition(), k.isSwimmingMode(), String.format("%.2f", k.walkAmount),
                    String.format("%.2f", k.swimPower), k.getHealth(), k.isJetting(), String.format("%.2f", mc.player.getY() - k.getY()));
        }
        if (mc.player.tickCount % 10 == 0 && "batman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} glide={} pull={} car={}", mc.player.tickCount, mc.player.blockPosition(),
                    com.danrod505.greenlantern.client.batman.GlideController.isGliding(),
                    com.danrod505.greenlantern.client.batman.GrappleController.isPulling(),
                    mc.player.getVehicle() instanceof com.danrod505.greenlantern.entity.BatmobileEntity car
                            ? car.blockPosition() + " speed=" + String.format("%.2f", car.drivingSpeed()) + " boost=" + car.isBoosting() : "-");
        }
        if (mc.player.tickCount % 10 == 0 && "superman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            var crystal = com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(mc.player);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} flying={} mach={} flags={} solar={} music: {}", mc.player.tickCount,
                    mc.player.blockPosition(), com.danrod505.greenlantern.client.flight.FlightController.isPowerFlying(),
                    String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.mach()),
                    com.danrod505.greenlantern.client.superman.SupermanVisuals.flags(mc.player.getId()),
                    crystal.isEmpty() ? -1 : com.danrod505.greenlantern.superman.SolarEnergy.get(crystal).stored(),
                    com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
        }
        if (mc.player.tickCount % 10 == 0 && "wonderwoman".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            var tiara = com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.findTiara(mc.player);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} flying={} speed={} flags={} divine={} jet={} music: {}", mc.player.tickCount,
                    mc.player.blockPosition(), com.danrod505.greenlantern.client.flight.FlightController.isPowerFlying(),
                    String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.speed()),
                    com.danrod505.greenlantern.client.wonderwoman.WonderWomanVisuals.flags(mc.player.getId()),
                    tiara.isEmpty() ? -1 : com.danrod505.greenlantern.wonderwoman.DivinePower.get(tiara).stored(),
                    mc.player.getVehicle() instanceof com.danrod505.greenlantern.entity.InvisibleJetEntity jet
                            ? jet.blockPosition() + " boost=" + jet.isBoosting() + " cloak=" + jet.isCloaked() : "-",
                    com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
        }
        if (mc.player.tickCount % 10 == 0 && "flight".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} mach={} music: {}", mc.player.tickCount,
                    String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.mach()),
                    com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
        }
        tickWatchers();
        if (hold != null) {
            if (hold.getAsBoolean() || --holdLeft <= 0) {
                hold = null;
            } else {
                return;
            }
        }
        if (stepIndex >= STEPS.size()) return;
        if (++wait >= STEPS.get(stepIndex).delay()) {
            wait = 0;
            STEPS.get(stepIndex++).action().run();
        }
    }
}
