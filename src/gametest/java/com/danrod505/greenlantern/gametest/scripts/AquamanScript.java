package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

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

/** Screenshot script "aquaman": Aquaman: suit, swimming, trident, shark and the call of the sea. */
public final class AquamanScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("aquaman", AquamanScript::build, AquamanScript::log, false);

    private AquamanScript() {}

    public static void aquaMobs(EntityType<?> type, double dx, double dy, double dz, int count, boolean noAi) {
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

    public static void aquaTeleport(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> sp.teleportTo(aquaBase.getX() + dx, aquaBase.getY() + dy, aquaBase.getZ() + dz));
        look(yaw, pitch);
    }

    public static void aquaPower(com.danrod505.greenlantern.aquaman.AquaPower power) {
        server(sp -> {
            ItemStack emblem = com.danrod505.greenlantern.aquaman.AquamanHelper.findEmblem(sp);
            com.danrod505.greenlantern.aquaman.AquaPower.POWERS.select(emblem, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(emblem));
            com.danrod505.greenlantern.aquaman.AquamanServer.usePower(sp, emblem, power);
        });
    }

    public static BlockPos aquaBase = BlockPos.ZERO;

    /** Aquaman: suit, fast 3D swimming with the trail, the leap, the trident, riding the shark, the call of the sea and the wheel. */
    private static void build() {
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
                AquamanHero.INSTANCE.summonSuit(sp);
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
                com.danrod505.greenlantern.aquaman.AquaPower.POWERS.selected(com.danrod505.greenlantern.aquaman.AquamanHelper.findEmblem(mc().player)).id()));
        // The manual.
        step(5, () -> mc().setScreen(new com.danrod505.greenlantern.client.GuideScreen()));
        step(10, () -> shot("aq13_guide"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} swim={} speed={} water={} pos={} vehicle={}", mc.player.tickCount,
                com.danrod505.greenlantern.client.aqua.SwimController.isSwimming(),
                String.format("%.2f", com.danrod505.greenlantern.client.aqua.SwimController.speed()),
                mc.player.isInWater(), mc.player.blockPosition(), mc.player.getVehicle());
    }
}
