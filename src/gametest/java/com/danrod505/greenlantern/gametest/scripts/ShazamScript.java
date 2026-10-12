package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.client.flight.FlightAudio;
import com.danrod505.greenlantern.client.flight.FlightController;
import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.shazam.ShazamContent;
import com.danrod505.greenlantern.shazam.ShazamHero;
import com.danrod505.greenlantern.shazam.ShazamPower;
import com.danrod505.greenlantern.shazam.ShazamServer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Screenshot script "shazam": the lightning of "Shazam!" forming the suit, the suit (front, back,
 * side), the HUD, the power wheel, every power, the flight above the sound barrier, the lightning
 * taking the suit off and the guide.
 */
public final class ShazamScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("shazam", ShazamScript::build, ShazamScript::log, false);

    private static BlockPos base = BlockPos.ZERO;

    private ShazamScript() {}

    private static void power(ShazamPower power) {
        server(sp -> {
            ItemStack item = ShazamHero.INSTANCE.findItem(sp);
            ShazamHero.INSTANCE.selectOwn(item, power.id());
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(item));
            boolean used = ShazamHero.INSTANCE.ownPowers().use(sp, item, power.ordinal());
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

    /** Still mobs in a ring around (dx, dz). */
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

    private static int lightningTicks;

    /** Whether a lightning bolt has been falling right by the player for a couple of ticks (so it is on screen). */
    private static boolean lightningNear() {
        boolean near = !mc().level.getEntitiesOfClass(LightningBolt.class, mc().player.getBoundingBox().inflate(4)).isEmpty();
        lightningTicks = near ? lightningTicks + 1 : 0;
        return lightningTicks >= 3;
    }

    private static void refill() {
        server(sp -> ShazamHero.ENERGY.set(ShazamHero.INSTANCE.findItem(sp), ShazamHero.ENERGY.capacity()));
    }

    private static void build() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            command("kill @e[type=!minecraft:player]");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, ShazamContent.ITEM.get().charged(new ItemStack(ShazamContent.ITEM.get())));
                ServerLevel level = sp.level();
                base = sp.blockPosition();
                BlockState stone = Blocks.STONE.defaultBlockState();
                // Glass for the Thunder Call, 30 blocks east.
                for (int x = 28; x <= 32; x++) {
                    for (int y = 0; y <= 2; y++) level.setBlock(base.offset(x, y, 10), Blocks.GLASS.defaultBlockState(), 2);
                }
                // Torches for the Courage of Achilles, 30 blocks west.
                for (int x = -32; x <= -28; x += 2) level.setBlock(base.offset(x, 0, 5), Blocks.TORCH.defaultBlockState(), 2);
                // A wall to the south, with creatures behind it for the Wisdom of Solomon.
                for (int x = -6; x <= 6; x++) {
                    for (int y = 0; y <= 4; y++) level.setBlock(base.offset(x, y, -34), stone, 2);
                }
                // Pillars along the flight path give a sense of speed.
                for (int z = 60; z < 900; z += 16) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 8 + (z / 16) % 5; y++) level.setBlock(base.offset(side * 14, y, z), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                    }
                }
            });
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        // "Shazam!": the lightning falls on him and forms the suit.
        waitFor(200, () -> !ShazamHero.INSTANCE.findItem(mc().player).isEmpty());
        step(10, () -> {
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
            server(sp -> ShazamHero.INSTANCE.summonSuit(sp));
        });
        watch(60, ShazamScript::lightningNear, "sz05_transform");
        step(2, () -> clean("sz05c_transform_flash"));
        step(40, () -> clean("sz00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("sz01_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("sz02_suit_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("sz03_hud"));
        // Power wheel.
        step(10, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            screen.mouseMoved(screen.width / 2.0 + Math.cos(angle) * 60, screen.height * 0.46 + Math.sin(angle) * 60);
        });
        step(6, () -> shot("sz04_wheel"));
        step(1, () -> {
            key(KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });

        // Zeus's Lightning: one bolt, three zombies.
        step(10, () -> {
            tp(0.5, 0, 0.5, 0, 4);
            mobs(EntityType.HUSK, 0.5, 9.5, 3, 2.5);
        });
        step(20, () -> power(ShazamPower.ZEUS_LIGHTNING));
        step(2, () -> shot("sz06_zeus_lightning"));
        step(2, () -> shot("sz06b_zeus_jumps"));
        step(30, () -> {
            clearMobs();
            refill();
        });

        // Thunder Call: lightning on the husks by the glass.
        step(5, () -> {
            tp(30.5, 0, 0.5, 0, 12);
            mobs(EntityType.HUSK, 30.5, 8.0, 3, 1.5);
        });
        step(20, () -> power(ShazamPower.THUNDER_CALL));
        step(2, () -> shot("sz07_thunder_call"));
        step(4, () -> shot("sz07b_thunder_call_after"));
        step(30, () -> {
            clearMobs();
            refill();
        });

        // Speed of Mercury: through a group.
        step(5, () -> {
            tp(0.5, 0, 20.5, 0, 6);
            mobs(EntityType.HUSK, 0.5, 26.5, 4, 1.2);
        });
        step(20, () -> power(ShazamPower.MERCURY_DASH));
        step(3, () -> shot("sz08_mercury_dash"));
        step(30, () -> {
            clearMobs();
            refill();
        });

        // Strength of Hercules: a ring of husks thrown away.
        step(5, () -> {
            tp(0.5, 0, 50.5, 0, 25);
            mobs(EntityType.HUSK, 0.5, 50.5, 6, 3.0);
        });
        step(20, () -> power(ShazamPower.HERCULES_SLAM));
        step(3, () -> shot("sz09_hercules_slam"));
        step(30, () -> {
            clearMobs();
            refill();
        });

        // Courage of Achilles: a cry that stuns and puts the torches out.
        step(5, () -> {
            command("kill @e[type=minecraft:slime]");
            tp(-29.5, 0, 0.5, 0, 8);
            mobs(EntityType.HUSK, -29.5, 7.5, 3, 2.0);
        });
        step(20, () -> power(ShazamPower.ACHILLES_ROAR));
        step(2, () -> shot("sz10_achilles_roar"));
        step(30, () -> {
            clearMobs();
            refill();
        });

        // Stamina of Atlas: the golden weight around him.
        step(5, () -> tp(0.5, 0, 0.5, 0, 10));
        step(10, () -> power(ShazamPower.ATLAS_STAMINA));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(10, () -> shot("sz11_atlas_stamina"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            power(ShazamPower.ATLAS_STAMINA);
        });

        // Wisdom of Solomon: creatures glowing behind the wall, health under the crosshair.
        step(5, () -> {
            command("kill @e[type=minecraft:slime]");
            tp(0.5, 0, -20.5, 180, 4);
            mobs(EntityType.HUSK, 0.5, -40.5, 4, 2.5);
            camera(CameraType.FIRST_PERSON);
        });
        step(20, () -> power(ShazamPower.SOLOMON_WISDOM));
        step(10, () -> shot("sz12_solomon_wisdom"));
        step(2, () -> {
            power(ShazamPower.SOLOMON_WISDOM);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, () -> {
            clearMobs();
            refill();
        });

        // Flight: take off, accelerate, break the sound barrier.
        step(10, () -> tp(0.5, 0, 40.5, 0, 5));
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(4, () -> key(mc().options.keyJump, true));
        step(10, () -> {
            key(mc().options.keyJump, false);
            look(0, -6);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        watch(80, FlightController::isSupersonic, "sz13b_sound_barrier");
        step(15, () -> shot("sz13_flight"));
        step(1, () -> {
            mc().options.hideGui = true;
            camera(CameraType.THIRD_PERSON_FRONT);
        });
        step(4, () -> clean("sz13c_flight_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 60);
        });
        watch(80, () -> mc().player.onGround(), "sz13d_landing");
        step(3, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
        });
        step(20, () -> {
            mc().player.getAbilities().flying = false;
            mc().player.onUpdateAbilities();
            look(0, 10);
            camera(CameraType.THIRD_PERSON_FRONT);
        });

        // Back to Billy: the lightning again.
        step(20, () -> {
            mc().options.hideGui = true;
            server(sp -> ShazamHero.INSTANCE.dismissSuit(sp, true));
        });
        watch(60, () -> !mc().level.getEntitiesOfClass(LightningBolt.class, mc().player.getBoundingBox().inflate(4)).isEmpty(), "sz05b_transform_back");
        step(10, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });

        // The guide.
        step(5, () -> mc().setScreen(GuideScreen.atChapter("shazam")));
        step(10, () -> shot("sz14_guide"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("shazam_powers")));
        step(10, () -> shot("sz14b_guide_powers"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        ItemStack item = ShazamHero.INSTANCE.findItem(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} suited={} energy={} atlas={} solomon={} flying={} mach={} music: {}", mc.player.tickCount,
                mc.player.blockPosition(), ShazamHero.INSTANCE.isSuited(mc.player), item.isEmpty() ? -1 : ShazamHero.ENERGY.get(item).stored(),
                !item.isEmpty() && ShazamServer.isAtlas(item), !item.isEmpty() && ShazamServer.isSolomon(item), FlightController.isPowerFlying(),
                String.format("%.2f", FlightController.mach()), FlightAudio.describeMusic());
    }
}
