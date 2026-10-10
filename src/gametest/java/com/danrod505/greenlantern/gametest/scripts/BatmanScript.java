package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.registry.ModItems;
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

/** Screenshot script "batman": Batman: suit, gadgets, grapple, gliding and the Batmobile. */
public final class BatmanScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("batman", BatmanScript::build, BatmanScript::log, false);

    private BatmanScript() {}

    public static BlockPos batBase = BlockPos.ZERO;

    public static void batPower(com.danrod505.greenlantern.batman.BatPower power) {
        server(sp -> {
            ItemStack belt = com.danrod505.greenlantern.batman.BatmanHelper.findBelt(sp);
            com.danrod505.greenlantern.batman.BatPower.POWERS.select(belt, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(belt));
            com.danrod505.greenlantern.batman.BatmanServer.usePower(sp, belt, power);
        });
    }

    public static void batTp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            sp.teleportTo(batBase.getX() + dx, batBase.getY() + dy, batBase.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    public static void batMobs(double dx, double dz, int count, boolean noAi) {
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

    /** Batman: suit and cape, the batarang, the grappling hook, gliding, the swarm of bats, the Batmobile, the wheel and the manual. */
    private static void build() {
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
                BatmanHero.INSTANCE.summonSuit(sp);
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
                com.danrod505.greenlantern.batman.BatPower.POWERS.selected(com.danrod505.greenlantern.batman.BatmanHelper.findBelt(mc().player)).id()));
        // The manual.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("batman")));
        step(10, () -> shot("bat11_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("batman_powers")));
        step(10, () -> shot("bat11b_guide_gadgets"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} glide={} pull={} car={}", mc.player.tickCount, mc.player.blockPosition(),
                com.danrod505.greenlantern.client.batman.GlideController.isGliding(),
                com.danrod505.greenlantern.client.batman.GrappleController.isPulling(),
                mc.player.getVehicle() instanceof com.danrod505.greenlantern.entity.BatmobileEntity car
                        ? car.blockPosition() + " speed=" + String.format("%.2f", car.drivingSpeed()) + " boost=" + car.isBoosting() : "-");
    }
}
