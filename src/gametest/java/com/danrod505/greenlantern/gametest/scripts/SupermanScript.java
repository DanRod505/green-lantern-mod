package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.superman.SupermanHero;
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

/** Screenshot script "superman": Superman: suit, heat vision, punch, breath, X-ray and flight. */
public final class SupermanScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("superman", SupermanScript::build, SupermanScript::log, false);

    private SupermanScript() {}

    public static BlockPos supBase = BlockPos.ZERO;

    public static void supPower(com.danrod505.greenlantern.superman.SuperPower power) {
        server(sp -> {
            ItemStack crystal = com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(sp);
            com.danrod505.greenlantern.superman.SuperPower.POWERS.select(crystal, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(crystal));
            com.danrod505.greenlantern.superman.SupermanServer.usePower(sp, crystal, power);
        });
    }

    public static void supTp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            sp.teleportTo(supBase.getX() + dx, supBase.getY() + dy, supBase.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    public static void supMobs(double dx, double dz, int count, double spread) {
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
    private static void build() {
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
                SupermanHero.INSTANCE.summonSuit(sp);
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
                com.danrod505.greenlantern.superman.SuperPower.POWERS.selected(com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(mc().player)).id()));
        // The guide.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("superman")));
        step(10, () -> shot("sup13_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("superman_powers")));
        step(10, () -> shot("sup13b_guide_powers"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        var crystal = com.danrod505.greenlantern.superman.SupermanHelper.findCrystal(mc.player);
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} flying={} mach={} flags={} solar={} music: {}", mc.player.tickCount,
                mc.player.blockPosition(), com.danrod505.greenlantern.client.flight.FlightController.isPowerFlying(),
                String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.mach()),
                com.danrod505.greenlantern.client.superman.SupermanVisuals.flags(mc.player.getId()),
                crystal.isEmpty() ? -1 : com.danrod505.greenlantern.superman.SupermanHero.SOLAR_ENERGY.get(crystal).stored(),
                com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
    }
}
