package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Screenshot script "mecha": The giant Mecha construct. */
public final class MechaScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("mecha", MechaScript::build, null, false);

    private MechaScript() {}

    public static void mecha(java.util.function.Consumer<com.danrod505.greenlantern.entity.MechaEntity> action) {
        server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.MechaEntity mecha) action.accept(mecha);
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
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, PowerRingItem.charged(new ItemStack(ModItems.POWER_RING.get())));
                LanternHero.INSTANCE.summonSuit(sp);
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                for (int i = -1; i <= 1; i++) {
                    Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                    zombie.snapTo(base.getX() + 0.5 + i * 3.0, base.getY(), base.getZ() + 16.5, 180, 0);
                    zombie.setNoAi(true);
                    zombie.setPersistenceRequired();
                    zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                    level.addFreshEntity(zombie);
                }
                // A little stone wall and a lantern post for scale.
                for (int x = -6; x <= -3; x++) {
                    for (int y = 0; y < 2; y++) {
                        level.setBlockAndUpdate(base.offset(x, y, 6), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                ConstructRegistry.select(sp.getMainHandItem(), ConstructRegistry.MECHA);
            });
            look(0, 10);
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(10, ClientScript::use);
        step(8, () -> clean("m00_assembling"));
        step(40, () -> clean("m01_back"));
        step(5, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> clean("m02_front"));
        step(2, () -> look(35, 25));
        step(5, () -> clean("m02b_front_low"));
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            mc().options.hideGui = false;
            look(0, 15);
        });
        step(5, () -> shot("m03_cockpit"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 16);
            mecha(m -> m.setLaserFiring(true));
        });
        step(12, () -> clean("m04_laser"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("m04b_laser_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mecha(m -> m.setLaserFiring(false));
        });
        step(25, () -> {
            look(-20, 10);
            mecha(m -> m.fireMissiles());
        });
        step(7, () -> clean("m05_missiles_launch"));
        step(8, () -> clean("m05b_missiles_fly"));
        step(10, () -> clean("m05c_missiles_hit"));
        // Walking.
        step(20, () -> {
            look(90, 12);
            key(mc().options.keyUp, true);
        });
        step(30, () -> clean("m06_walk"));
        step(7, () -> clean("m06b_walk"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> clean("m06c_walk_front"));
        step(6, () -> clean("m06d_walk_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, false);
        });
        // Take off and fly.
        step(20, () -> key(mc().options.keyJump, true));
        step(16, () -> clean("m07_takeoff"));
        step(30, () -> {
            clean("m07b_climb");
            key(mc().options.keyUp, true);
        });
        step(10, () -> key(mc().options.keyJump, false));
        step(25, () -> clean("m08_flying"));
        step(2, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(5, () -> clean("m08b_flying_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keySprint, true);
        });
        step(25, () -> clean("m08c_afterburner"));
        // Dive into the ground for the landing shockwave.
        step(2, () -> {
            key(mc().options.keySprint, false);
            look(mc().player.getYRot(), 70);
        });
        for (int i = 0; i < 6; i++) {
            String name = "m09_dive_" + i;
            step(8, () -> shot(name));
        }
        step(2, () -> key(mc().options.keyUp, false));
        step(6, () -> clean("m09z_after_landing"));
        step(20, () -> mc().stop());
    }
}
