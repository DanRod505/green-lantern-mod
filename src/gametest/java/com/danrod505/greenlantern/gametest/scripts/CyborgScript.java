package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.cyborg.CyborgContent;
import com.danrod505.greenlantern.cyborg.CyborgHero;
import com.danrod505.greenlantern.cyborg.CyborgPower;
import com.danrod505.greenlantern.cyborg.CyborgServer;
import com.danrod505.greenlantern.gametest.ClientScript;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "cyborg": Cyborg's suit (front, back, side), the HUD, every power (the sonic
 * cannon through a glass wall, the missiles chasing husks, the scan showing ores underground, the
 * hacked golem, the EMP, the repair), the thrusters, the redstone recharge, the Boom Tube opening and
 * arriving, the power wheel and the guide.
 */
public final class CyborgScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("cyborg", CyborgScript::build, CyborgScript::log, false);

    private static BlockPos base = BlockPos.ZERO;

    private CyborgScript() {}

    private static void power(CyborgPower power) {
        server(sp -> {
            ItemStack item = CyborgHero.INSTANCE.findItem(sp);
            CyborgPower.POWERS.select(item, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(item));
            boolean used = CyborgHero.INSTANCE.powers().use(sp, item, power.ordinal());
            GreenLantern.LOGGER.info("CLIENTSCRIPT power {} used={}", power.id(), used);
        });
    }

    private static void tp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            sp.teleportTo(base.getX() + 0.5 + dx, base.getY() + dy, base.getZ() + 0.5 + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    /** Husks (they don't burn in the sun) standing still, {@code count} in a row across x. */
    private static void husks(double dx, double dz, int count, double spacing) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                var husk = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                husk.snapTo(base.getX() + 0.5 + dx + (i - (count - 1) * 0.5) * spacing, base.getY(), base.getZ() + 0.5 + dz, 180, 0);
                husk.setNoAi(true);
                husk.setPersistenceRequired();
                level.addFreshEntity(husk);
            }
        });
    }

    private static void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        server(sp -> {
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(x0, y0, z0), base.offset(x1, y1, z1))) {
                sp.level().setBlockAndUpdate(pos, block.defaultBlockState());
            }
        });
    }

    private static void build() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                base = sp.blockPosition();
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, CyborgContent.ITEM.get().charged(new ItemStack(CyborgContent.ITEM.get())));
                CyborgHero.INSTANCE.summonSuit(sp);
            });
            husks(0, 8, 3, 2.5);
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
        step(10, () -> clean("cyb00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("cyb00b_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("cyb00c_suit_side"));
        step(2, () -> look(-90, 15));
        step(4, () -> clean("cyb00d_suit_side_left"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("cyb01_hud"));

        // Sonic cannon: through a glass wall, into the husks.
        step(2, () -> fill(-3, 0, 4, 3, 2, 4, Blocks.GLASS));
        step(10, () -> look(0, 4));
        step(4, () -> shot("cyb02a_glass_wall"));
        step(2, () -> power(CyborgPower.SONIC_CANNON));
        step(3, () -> shot("cyb02_sonic_cannon"));
        step(8, () -> shot("cyb02b_sonic_cannon_after"));
        step(30, () -> {
            clearMobs();
            fill(-3, 0, 4, 3, 2, 4, Blocks.AIR);
        });

        // Shoulder missiles: four rockets chasing a spread of husks.
        step(4, () -> husks(0, 14, 4, 3.0));
        step(10, () -> look(0, -4));
        step(2, () -> power(CyborgPower.SHOULDER_MISSILES));
        step(5, () -> shot("cyb03_shoulder_missiles"));
        step(6, () -> shot("cyb03b_missiles_flight"));
        step(10, () -> shot("cyb03c_missiles_hit"));
        step(30, () -> clearMobs());

        // Tech scan: ores buried around show through the ground, husks behind a wall glow.
        step(2, () -> {
            fill(-6, -4, 4, -4, -3, 6, Blocks.DIAMOND_ORE);
            fill(4, -5, 3, 6, -4, 5, Blocks.GOLD_ORE);
            fill(-2, -6, 9, 1, -5, 10, Blocks.REDSTONE_ORE);
            fill(2, -3, 10, 3, -3, 12, Blocks.IRON_ORE);
            fill(-3, 0, 6, 3, 3, 6, Blocks.STONE_BRICKS);
        });
        step(4, () -> husks(0, 9, 3, 2.0));
        step(10, () -> look(0, 30));
        step(2, () -> power(CyborgPower.TECH_SCAN));
        step(30, () -> shot("cyb04_tech_scan"));
        step(2, () -> camera(CameraType.FIRST_PERSON));
        step(14, () -> shot("cyb04b_tech_scan_first_person"));
        step(2, () -> look(0, 4));
        step(14, () -> shot("cyb04c_tech_scan_wall"));
        step(2, () -> {
            power(CyborgPower.TECH_SCAN);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, () -> {
            clearMobs();
            fill(-3, 0, 6, 3, 3, 6, Blocks.AIR);
        });

        // Machine hack: an iron golem turns against the husks.
        step(2, () -> server(sp -> {
            ServerLevel level = sp.level();
            var golem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
            golem.snapTo(base.getX() + 2.5, base.getY(), base.getZ() + 7.5, 180, 0);
            golem.setPersistenceRequired();
            level.addFreshEntity(golem);
            var husk = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
            husk.snapTo(base.getX() - 1.5, base.getY(), base.getZ() + 9.5, 180, 0);
            husk.setNoAi(true);
            husk.setPersistenceRequired();
            level.addFreshEntity(husk);
        }));
        step(10, () -> server(sp -> {
            Mob golem = sp.level().getNearestEntity(net.minecraft.world.entity.animal.golem.IronGolem.class,
                    net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(), sp, sp.getX(), sp.getY(), sp.getZ(), sp.getBoundingBox().inflate(16));
            if (golem != null) {
                Vec3 to = golem.getBoundingBox().getCenter().subtract(sp.getEyePosition());
                float yaw = (float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI);
                float pitch = (float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI);
                mc().execute(() -> look(yaw, pitch));
            }
        }));
        step(4, () -> power(CyborgPower.MACHINE_HACK));
        step(3, () -> shot("cyb05_machine_hack"));
        step(30, () -> {
            look(0, 10);
            shot("cyb05b_golem_ally");
        });
        step(20, () -> clearMobs());

        // EMP: a ring of husks stunned all at once.
        step(2, () -> server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                var husk = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                husk.snapTo(base.getX() + 0.5 + Math.cos(a) * 4.5, base.getY(), base.getZ() + 0.5 + Math.sin(a) * 4.5, 0, 0);
                husk.setPersistenceRequired();
                level.addFreshEntity(husk);
            }
        }));
        step(6, () -> look(0, 30));
        step(2, () -> power(CyborgPower.EMP_BURST));
        step(3, () -> shot("cyb06_emp_burst"));
        step(15, () -> shot("cyb06b_emp_stunned"));
        step(20, () -> clearMobs());

        // Self repair.
        step(2, () -> server(sp -> sp.setHealth(8.0F)));
        step(4, () -> look(0, 10));
        step(2, () -> power(CyborgPower.SELF_REPAIR));
        step(20, () -> shot("cyb07_self_repair"));
        step(60, () -> shot("cyb07b_repaired"));

        // Thrusters: take off and speed up.
        step(10, () -> tp(0, 0, 30, 0, 5));
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(4, () -> key(mc().options.keyJump, true));
        step(10, () -> {
            key(mc().options.keyJump, false);
            look(0, 0);
        });
        step(10, () -> shot("cyb11_hover"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("cyb11b_hover_front"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, -6);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(20, () -> shot("cyb11c_flight"));
        step(30, () -> shot("cyb11d_top_speed"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            mc().player.getAbilities().flying = false;
            mc().player.onUpdateAbilities();
        });
        step(40, () -> tp(0, 0, 0, 0, 20));

        // Recharge next to redstone.
        step(4, () -> {
            fill(2, 0, 1, 2, 0, 1, Blocks.REDSTONE_BLOCK);
            fill(-2, 0, 1, -2, 0, 1, Blocks.REDSTONE_BLOCK);
            fill(-1, 0, 3, 1, 0, 3, Blocks.REDSTONE_WIRE);
            server(sp -> CyborgHero.ENERGY.set(CyborgHero.INSTANCE.findItem(sp), CyborgHero.ENERGY.capacity() / 4));
        });
        step(50, () -> shot("cyb12_recharge"));

        // Boom Tube: mark here, walk away with a friend, open the tube and come back.
        step(2, () -> server(sp -> {
            sp.setShiftKeyDown(true);
            ItemStack item = CyborgHero.INSTANCE.findItem(sp);
            CyborgHero.INSTANCE.powers().use(sp, item, CyborgPower.BOOM_TUBE.ordinal());
            sp.setShiftKeyDown(false);
        }));
        step(10, () -> shot("cyb13a_boom_tube_marked"));
        step(2, () -> {
            tp(-20, 0, -12, 0, 8);
            server(sp -> {
                var villager = EntityType.VILLAGER.create(sp.level(), EntitySpawnReason.COMMAND);
                villager.snapTo(sp.getX() + 1.5, sp.getY(), sp.getZ() + 0.5, 0, 0);
                villager.setNoAi(true);
                villager.setPersistenceRequired();
                sp.level().addFreshEntity(villager);
            });
        });
        step(20, () -> power(CyborgPower.BOOM_TUBE));
        step(10, () -> shot("cyb13_boom_tube_open"));
        step(8, () -> shot("cyb13b_boom_tube_wide"));
        waitFor(40, () -> CyborgServer.boomTubeTicks(serverPlayer()) == 0);
        step(3, () -> shot("cyb14_boom_tube_arrive"));
        step(10, () -> look(180, 10));
        step(4, () -> shot("cyb14b_arrived_with_friend"));
        step(10, () -> clearMobs());

        // Power wheel.
        step(20, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            screen.mouseMoved(screen.width / 2.0 + Math.cos(angle) * 60, screen.height * 0.46 + Math.sin(angle) * 60);
        });
        step(6, () -> shot("cyb09_power_wheel"));
        step(1, () -> {
            key(KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        // The guide.
        step(5, () -> mc().setScreen(GuideScreen.atChapter("cyborg")));
        step(10, () -> shot("cyb15_guide"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("cyborg_powers")));
        step(10, () -> shot("cyb15b_guide_powers"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        ItemStack item = CyborgHero.INSTANCE.findItem(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={} energy={} flying={} mach={} music: {}", mc.player.tickCount, mc.player.blockPosition(),
                CyborgHero.INSTANCE.isSuited(mc.player), item.isEmpty() ? -1 : CyborgHero.ENERGY.get(item).stored(),
                com.danrod505.greenlantern.client.flight.FlightController.isPowerFlying(),
                String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.mach()),
                com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
    }
}
