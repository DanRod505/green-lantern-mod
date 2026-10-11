package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Screenshot script "flight": The Lantern's power flight: acceleration, sound barrier, turns, dive and hero landing. */
public final class FlightScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("flight", FlightScript::build, FlightScript::log, false);

    private FlightScript() {}

    public static int afterLanding;

    public static boolean landingShot;

    /** Power flight showcase: take-off, acceleration, sonic boom, barrel roll and hero landing. */
    private static void build() {
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
                LanternHero.INSTANCE.summonSuit(sp);
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

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} mach={} music: {}", mc.player.tickCount,
                String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.mach()),
                com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
    }
}
