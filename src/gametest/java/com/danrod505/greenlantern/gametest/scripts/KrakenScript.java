package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;
import static com.danrod505.greenlantern.gametest.scripts.AquamanScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Screenshot script "kraken": The Kraken, Aquaman's giant mount. */
public final class KrakenScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("kraken", KrakenScript::build, KrakenScript::log, false);

    private KrakenScript() {}

    public static com.danrod505.greenlantern.entity.KrakenEntity clientKraken() {
        var list = mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.KrakenEntity.class, mc().player.getBoundingBox().inflate(96));
        return list.isEmpty() ? null : list.getFirst();
    }

    public static void kraken(java.util.function.Consumer<com.danrod505.greenlantern.entity.KrakenEntity> action) {
        server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken != null) action.accept(kraken);
        });
    }

    /** Climbs off the Kraken and watches it from a spot given relative to it (forward, up, left of its facing). */
    public static void krakenView(double forward, double up, double left) {
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

    public static void rideKraken() {
        server(sp -> {
            var kraken = com.danrod505.greenlantern.entity.KrakenEntity.find(sp);
            if (kraken != null && sp.getVehicle() != kraken) sp.startRiding(kraken);
        });
        camera(CameraType.THIRD_PERSON_BACK);
    }

    /** Moves the Kraken (and Aquaman off its back) to a spot relative to where the script started, facing south. */
    public static void placeKraken(double dx, double dy, double dz) {
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
    private static void build() {
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
                AquamanHero.INSTANCE.summonSuit(sp);
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

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        if (!(mc.player.getVehicle() instanceof com.danrod505.greenlantern.entity.KrakenEntity k)) return;
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} kraken pos={} swimming={} walk={} swimPower={} health={} jet={} rider dy={}",
                mc.player.tickCount, k.blockPosition(), k.isSwimmingMode(), String.format("%.2f", k.walkAmount),
                String.format("%.2f", k.swimPower), k.getHealth(), k.isJetting(), String.format("%.2f", mc.player.getY() - k.getY()));
    }
}
