package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.flash.FlashHero;
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

/** Screenshot script "flash": The Flash: suit, running, sound barrier, water and wall running, every power. */
public final class FlashScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("flash", FlashScript::build, FlashScript::log, false);

    private FlashScript() {}

    public static BlockPos flashBase = BlockPos.ZERO;

    public static void flashPower(com.danrod505.greenlantern.flash.SpeedsterPower power) {
        server(sp -> {
            com.danrod505.greenlantern.flash.SpeedsterPower.POWERS.select(sp.getMainHandItem(), power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(sp.getMainHandItem()));
            com.danrod505.greenlantern.flash.SpeedsterServer.usePower(sp, sp.getMainHandItem(), power);
        });
    }

    public static void flashZombies(int dx, int dz, int count) {
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
    private static void build() {
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
                FlashHero.INSTANCE.summonSuit(sp);
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
                com.danrod505.greenlantern.flash.SpeedsterPower.POWERS.selected(mc().player.getMainHandItem()).id()));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} speed={} mach={} flags={} pos={} music: {}", mc.player.tickCount,
                String.format("%.2f", com.danrod505.greenlantern.client.speed.SpeedController.speed()),
                String.format("%.2f", com.danrod505.greenlantern.client.speed.SpeedController.mach()),
                com.danrod505.greenlantern.client.speed.SpeedController.flags(), mc.player.blockPosition(),
                com.danrod505.greenlantern.client.speed.SpeedAudio.describeMusic());
    }
}
