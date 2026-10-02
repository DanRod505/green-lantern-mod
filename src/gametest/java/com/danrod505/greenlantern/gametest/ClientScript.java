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
        if ("flight".equals(System.getenv("GL_CLIENT_SCRIPT"))) {
            buildFlightSteps();
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
        if (stepIndex >= STEPS.size()) return;
        if (++wait >= STEPS.get(stepIndex).delay()) {
            wait = 0;
            STEPS.get(stepIndex++).action().run();
        }
    }
}
