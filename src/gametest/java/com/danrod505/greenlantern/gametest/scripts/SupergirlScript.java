package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.client.flight.FlightAudio;
import com.danrod505.greenlantern.client.flight.FlightController;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.supergirl.KryptoEntity;
import com.danrod505.greenlantern.supergirl.SupergirlContent;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.supergirl.SupergirlPower;
import com.danrod505.greenlantern.supergirl.SupergirlServer;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "supergirl": Krypto arriving, her suit (front, back, side), the HUD, every power,
 * her flight with Krypto beside her, the barrel roll, the hero landing, Krypto fighting while she
 * recovers from the Solar Flare, the power wheel and the guide.
 */
public final class SupergirlScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("supergirl", SupergirlScript::build, SupergirlScript::log, false);

    private static BlockPos base = BlockPos.ZERO;

    private SupergirlScript() {}

    private static void power(SupergirlPower power) {
        server(sp -> {
            ItemStack item = SupergirlHero.INSTANCE.findItem(sp);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(item));
            boolean used = SupergirlHero.INSTANCE.powers().use(sp, item, power.ordinal());
            GreenLantern.LOGGER.info("CLIENTSCRIPT power {} used={}", power.id(), used);
        });
    }

    private static void tp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            sp.teleportTo(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    /** Still mobs in a ring around (dx, dz), facing her. */
    private static void mobs(EntityType<? extends Mob> type, double dx, double dz, int count, double spread) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                Mob mob = type.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                mob.snapTo(base.getX() + dx + Math.cos(a) * spread, base.getY(), base.getZ() + dz + Math.sin(a) * spread, 180, 0);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        });
    }

    private static void refill() {
        server(sp -> SupergirlHero.ENERGY.set(SupergirlHero.INSTANCE.findItem(sp), SupergirlHero.ENERGY.capacity()));
    }

    private static KryptoEntity krypto() {
        var player = mc().player;
        return mc().level.getEntitiesOfClass(KryptoEntity.class, player.getBoundingBox().inflate(40)).stream().findFirst().orElse(null);
    }

    private static void build() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, SupergirlContent.ITEM.get().charged(new ItemStack(SupergirlContent.ITEM.get())));
                ServerLevel level = sp.level();
                base = sp.blockPosition();
                BlockState stone = Blocks.STONE.defaultBlockState();
                // Glass and a campfire for the Thunder Clap, 30 blocks east.
                for (int x = 28; x <= 32; x++) {
                    for (int y = 0; y <= 2; y++) level.setBlock(base.offset(x, y, 6), Blocks.GLASS.defaultBlockState(), 2);
                }
                level.setBlock(base.offset(26, 0, 4), Blocks.CAMPFIRE.defaultBlockState(), 2);
                level.setBlock(base.offset(34, 0, 4), Blocks.TORCH.defaultBlockState(), 2);
                // A thick wall to the west, with creatures behind it for the Super Hearing.
                for (int x = -40; x <= -34; x++) {
                    for (int y = 0; y <= 5; y++) {
                        for (int z = -6; z <= 6; z++) level.setBlock(base.offset(x, y, z), stone, 2);
                    }
                }
                // Pillars along the flight path give a sense of speed.
                for (int z = 60; z < 900; z += 16) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 8 + (z / 16) % 5; y++) level.setBlock(base.offset(side * 14, y, z), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                    }
                }
            });
            camera(CameraType.THIRD_PERSON_BACK);
            look(20, -25);
        });
        // She puts on the suit for the first time: Krypto dives down from the sky.
        step(10, () -> server(sp -> SupergirlHero.INSTANCE.summonSuit(sp)));
        watch(60, () -> {
            KryptoEntity krypto = krypto();
            return krypto != null && krypto.getY() > mc().player.getY() + 3 && krypto.getY() < mc().player.getY() + 10;
        }, "sg14_krypto_arrives");
        step(60, () -> look(0, 10));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("sg00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("sg01_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("sg02_suit_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("sg03_hud"));
        // Heat bolts at a few husks.
        step(10, () -> {
            tp(0.5, 0, 2.5, 0, 6);
            mobs(EntityType.HUSK, 0.5, 13.5, 3, 1.8);
        });
        step(10, () -> power(SupergirlPower.HEAT_BOLTS));
        step(3, () -> power(SupergirlPower.HEAT_BOLTS));
        step(2, () -> shot("sg05_heat_bolts"));
        step(3, () -> power(SupergirlPower.HEAT_BOLTS));
        step(5, () -> shot("sg05b_heat_bolts_hit"));
        step(30, () -> clearMobs());
        // The Meteor Dash through a crowd.
        step(10, () -> {
            tp(0.5, 0, 20.5, 0, 6);
            mobs(EntityType.HUSK, 0.5, 26.5, 6, 2.0);
        });
        step(10, () -> shot("sg06a_before_dash"));
        step(1, () -> power(SupergirlPower.METEOR_DASH));
        step(3, () -> shot("sg06_meteor_dash"));
        step(8, () -> shot("sg06b_meteor_dash_after"));
        step(30, () -> clearMobs());
        // The Thunder Clap: glass shatters, the torch goes out, husks reel.
        step(10, () -> {
            tp(30.5, 0, 0.5, 0, 8);
            mobs(EntityType.HUSK, 30.5, 9.5, 3, 2.0);
        });
        step(10, () -> power(SupergirlPower.THUNDER_CLAP));
        step(2, () -> shot("sg07_thunder_clap"));
        step(10, () -> shot("sg07b_thunder_clap_after"));
        step(30, () -> clearMobs());
        // The Frost Wall between her and skeletons.
        step(10, () -> {
            tp(-15.5, 0, 0.5, 0, 6);
            mobs(EntityType.SKELETON, -15.5, 10.5, 3, 2.2);
        });
        step(10, () -> power(SupergirlPower.FROST_WALL));
        step(4, () -> shot("sg08_frost_wall"));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(180, 10);
        });
        step(4, () -> shot("sg08b_frost_wall_front"));
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            clearMobs();
        });
        // Super Hearing: creatures behind the thick wall.
        step(10, () -> {
            tp(-28.5, 0, 0.5, 90, 4);
            mobs(EntityType.HUSK, -44.5, 0.5, 4, 3.0);
            mobs(EntityType.CREEPER, -46.5, -4.5, 1, 0);
            mobs(EntityType.PIG, -26.5, 9.5, 2, 1.5);
        });
        step(10, () -> power(SupergirlPower.SUPER_HEARING));
        step(30, () -> camera(CameraType.FIRST_PERSON));
        step(6, () -> shot("sg09_super_hearing"));
        step(2, () -> look(30, 4));
        step(6, () -> shot("sg09b_super_hearing_arrows"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            power(SupergirlPower.SUPER_HEARING);
        });
        step(20, () -> clearMobs());
        // The Kryptonian Throw: a husk over her head, then far away.
        step(10, () -> {
            tp(0.5, 0, -15.5, 0, 20);
            mobs(EntityType.HUSK, 0.5, -12.5, 1, 0);
        });
        step(10, () -> power(SupergirlPower.KRYPTONIAN_THROW));
        step(8, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(180, 10);
        });
        step(4, () -> shot("sg10_kryptonian_throw"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, -10);
        });
        step(4, () -> power(SupergirlPower.KRYPTONIAN_THROW));
        step(3, () -> shot("sg10b_kryptonian_throw_hurl"));
        step(30, () -> {
            clearMobs();
            refill();
        });
        // Flight: take off with Krypto, accelerate, break the sound barrier, barrel roll.
        step(10, () -> tp(0.5, 0, 40.5, 0, 5));
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(4, () -> key(mc().options.keyJump, true));
        step(10, () -> {
            key(mc().options.keyJump, false);
            look(0, 0);
        });
        step(40, () -> shot("sg15b_krypto_flight"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("sg15c_krypto_flight_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, -6);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        watch(60, FlightController::isSupersonic, "sg12b_sound_barrier");
        step(10, () -> shot("sg12_flight"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("sg12c_flight_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        // Barrel roll: double tap left.
        step(2, () -> key(mc().options.keyLeft, true));
        step(2, () -> key(mc().options.keyLeft, false));
        step(2, () -> key(mc().options.keyLeft, true));
        step(3, () -> {
            key(mc().options.keyLeft, false);
            shot("sg13_barrel_roll");
        });
        step(4, () -> shot("sg13b_barrel_roll"));
        // Dive into the ground: the hero landing.
        step(10, () -> look(0, 60));
        watch(80, () -> mc().player.onGround(), "sg12d_landing");
        step(3, () -> shot("sg12e_landing_after"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        // The Solar Flare in the middle of husks; then she is worn out and Krypto stands guard.
        step(20, () -> {
            refill();
            server(sp -> {
                ServerLevel level = sp.level();
                for (int i = 0; i < 6; i++) {
                    Mob mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                    double a = i * Math.PI / 3;
                    mob.snapTo(sp.getX() + Math.cos(a) * 4, sp.getY(), sp.getZ() + Math.sin(a) * 4, 0, 0);
                    mob.setNoAi(true);
                    mob.setPersistenceRequired();
                    level.addFreshEntity(mob);
                }
            });
            look(0, 15);
        });
        step(10, () -> power(SupergirlPower.SOLAR_FLARE));
        step(4, () -> shot("sg11_solar_flare"));
        step(6, () -> shot("sg11b_solar_flare_wave"));
        step(60, () -> look(180, 20));
        watch(120, () -> {
            KryptoEntity krypto = krypto();
            return krypto != null && !mc().level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,
                    krypto.getBoundingBox().inflate(1.2)).isEmpty();
        }, "sg15_krypto_fight");
        step(10, () -> shot("sg15b_krypto_fight_after"));
        step(10, () -> clearMobs());
        // Power wheel.
        step(20, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            screen.mouseMoved(screen.width / 2.0 + Math.cos(angle) * 60, screen.height * 0.46 + Math.sin(angle) * 60);
        });
        step(6, () -> shot("sg04_wheel"));
        step(1, () -> {
            key(KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        // The guide.
        step(5, () -> mc().setScreen(GuideScreen.atChapter("supergirl")));
        step(10, () -> shot("sg16_guide"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("supergirl_powers")));
        step(10, () -> shot("sg16b_guide_powers"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("krypto")));
        step(10, () -> shot("sg16c_guide_krypto"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        ItemStack item = SupergirlHero.INSTANCE.findItem(mc.player);
        KryptoEntity krypto = krypto();
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={} energy={} flying={} mach={} krypto={} weak={} music: {}", mc.player.tickCount,
                mc.player.blockPosition(), SupergirlHero.INSTANCE.isSuited(mc.player), item.isEmpty() ? -1 : SupergirlHero.ENERGY.get(item).stored(),
                FlightController.isPowerFlying(), String.format("%.2f", FlightController.mach()),
                krypto == null ? "none" : krypto.blockPosition() + (krypto.isFlying() ? " flying" : ""), SupergirlServer.isWeak(mc.player),
                FlightAudio.describeMusic());
    }
}
