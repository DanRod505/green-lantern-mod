package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;
import static com.danrod505.greenlantern.gametest.scripts.AquamanScript.*;
import static com.danrod505.greenlantern.gametest.scripts.MechaScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Screenshot script "mounts": The Atlantean mounts and the Heroes' Guide. */
public final class MountsScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("mounts", MountsScript::build, null, false);

    private MountsScript() {}

    /** Giant mecha: summon, poses from several angles, lasers, missiles, walking, flight and landing. */

    public static final List<com.danrod505.greenlantern.entity.AtlanteanMountEntity> POSED = new ArrayList<>();

    /** A glass-walled sea, 41 wide, 100 long and 12 deep, with coral pillars (same as the Aquaman script). */
    public static void buildSea(ServerPlayer sp) {
        ServerLevel level = sp.level();
        BlockPos base = sp.blockPosition();
        aquaBase = base;
        var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        var wall = net.minecraft.world.level.block.Blocks.PRISMARINE_BRICKS.defaultBlockState();
        var sand = net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
        var light = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
        for (int x = -21; x <= 21; x++) {
            for (int z = 3; z <= 105; z++) {
                boolean edge = x == -21 || x == 21 || z == 3 || z == 105;
                level.setBlock(base.offset(x, -1, z), (x * 7 + z * 3) % 23 == 0 ? light : sand, 2);
                for (int y = 0; y < 12; y++) {
                    level.setBlock(base.offset(x, y, z), edge ? wall : water, 2);
                }
            }
        }
        var corals = List.of(net.minecraft.world.level.block.Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.HORN_CORAL_BLOCK.defaultBlockState());
        for (int z = 10; z < 100; z += 9) {
            for (int side = -1; side <= 1; side += 2) {
                int h = 2 + (z / 9) % 4;
                for (int y = 0; y < h; y++) level.setBlock(base.offset(side * 16, y, z), corals.get((z / 9 + y) % 4), 2);
            }
        }
    }

    /** Puts a creature of Atlantis in the sea (posed: no AI) and returns it. */
    public static <T extends com.danrod505.greenlantern.entity.AtlanteanMountEntity> void pose(
            java.util.function.Supplier<EntityType<T>> type, double dx, double dy, double dz, float yaw, int variant, boolean posed) {
        server(sp -> {
            T mount = type.get().create(sp.level(), EntitySpawnReason.COMMAND);
            mount.snapTo(aquaBase.getX() + dx, aquaBase.getY() + dy, aquaBase.getZ() + dz, yaw, 0);
            mount.setYBodyRot(yaw);
            mount.setYHeadRot(yaw);
            mount.setVariant(variant);
            mount.setNoAi(posed);
            mount.setPersistenceRequired();
            sp.level().addFreshEntity(mount);
            POSED.add(mount);
        });
    }

    public static void clearPosed() {
        server(sp -> {
            for (var m : POSED) m.discard();
            POSED.clear();
        });
    }

    public static void rideNearest(Class<? extends com.danrod505.greenlantern.entity.AtlanteanMountEntity> type) {
        server(sp -> {
            var list = sp.level().getEntitiesOfClass(type, sp.getBoundingBox().inflate(40));
            list.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(sp)));
            if (!list.isEmpty()) {
                list.getFirst().setNoAi(false);
                sp.startRiding(list.getFirst());
            }
        });
    }

    public static void logSwim(String what) {
        var p = mc().player;
        String vehicle = p.getVehicle() == null ? "-" : String.format(java.util.Locale.ROOT, "%.2f",
                Math.sqrt(p.getVehicle().distanceToSqr(p.getVehicle().xo, p.getVehicle().yo, p.getVehicle().zo)));
        com.danrod505.greenlantern.GreenLantern.LOGGER.info(String.format(java.util.Locale.ROOT,
                "CLIENTSCRIPT %s swim speed=%.2f boost=%.2f vehicle=%s music=%s", what,
                com.danrod505.greenlantern.client.aqua.SwimController.speed(), com.danrod505.greenlantern.client.aqua.SwimController.boost(),
                vehicle, com.danrod505.greenlantern.client.aqua.SwimAudio.describeMusic()));
    }

    public static com.danrod505.greenlantern.entity.GreatWhiteSharkEntity clientShark() {
        return mc().player.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark ? shark : null;
    }

    public static boolean lunge(float from, float to) {
        var shark = clientShark();
        if (shark == null) return false;
        float p = shark.lungeProgress(0.0F) * com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.LUNGE_TICKS;
        return p >= from && p < to;
    }

    /** v1.12: gradual sprint swimming and its music, the shark's lunge and bite, the creatures of Atlantis and the guide. */
    private static void build() {
        step(60, () -> {
            mc().options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc().getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, com.danrod505.greenlantern.item.AquamanEmblemItem.charged(new ItemStack(ModItems.AQUAMAN_EMBLEM.get())));
                sp.getInventory().setItem(1, new ItemStack(ModItems.MANTA_RAY_EGG.get(), 4));
                sp.getInventory().setItem(2, new ItemStack(ModItems.GIANT_SEAHORSE_EGG.get(), 4));
                sp.getInventory().setItem(3, new ItemStack(ModItems.ATLANTEAN_DOLPHIN_EGG.get(), 4));
                sp.getInventory().setItem(4, new ItemStack(ModItems.GUIDE_BOOK.get()));
                buildSea(sp);
                AquamanHero.INSTANCE.summonSuit(sp);
            });
            look(0, 5);
        });
        // 1. The swim builds up gradually: log the speed every few ticks while sprinting.
        step(30, () -> aquaTeleport(0.5, 5, 6.5, 0, 0));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, true);
        });
        step(15, () -> logSwim("cruise"));
        step(1, () -> key(mc().options.keySprint, true));
        for (int i = 0; i < 10; i++) {
            final int n = i;
            step(5, () -> logSwim("ramp t=" + (n + 1) * 5));
            if (i == 5) step(0, () -> shot("mt01_swim_ramp"));
        }
        step(1, () -> shot("mt02_swim_top"));
        step(1, () -> key(mc().options.keySprint, false));
        step(0, () -> aquaTeleport(0.5, 5, 10.5, 0, 0));
        for (int i = 0; i < 8; i++) {
            final int n = i;
            step(5, () -> logSwim("ease t=" + (n + 1) * 5));
        }
        step(1, () -> key(mc().options.keyUp, false));
        // 2. The shark: lunge and bite, seen from the front.
        step(20, () -> aquaTeleport(0.5, 5, 14.5, 0, 0));
        step(10, () -> aquaPower(com.danrod505.greenlantern.aquaman.AquaPower.SHARK));
        step(20, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, -8);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("mt03_shark_front_idle"));
        step(1, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark) {
                Vec3 mouth = shark.mouth().add(shark.facing().scale(2.0));
                var drowned = EntityType.DROWNED.create(sp.level(), EntitySpawnReason.COMMAND);
                drowned.snapTo(mouth.x, mouth.y - 0.8, mouth.z, 180, 0);
                drowned.setNoAi(true);
                sp.level().addFreshEntity(drowned);
            }
        }));
        watch(() -> lunge(3, 5), "mt04_shark_jaws_open");
        watch(() -> lunge(6, 8), "mt04b_shark_lunge");
        watch(() -> lunge(9, 11), "mt04c_shark_snap");
        watch(() -> lunge(12, 14), "mt04d_shark_thrash");
        step(4, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.SharkBitePacket()));
        step(30, () -> camera(CameraType.THIRD_PERSON_BACK));
        // From the side: off the shark, let it lunge at a drowned on its own.
        step(4, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.GreatWhiteSharkEntity shark) {
                sp.stopRiding();
                sp.teleportTo(shark.getX() + 6.5, shark.getY() + 0.3, shark.getZ() + 1.5);
            }
        }));
        step(3, () -> {
            look(90, 5);
            camera(CameraType.FIRST_PERSON);
            mc().options.hideGui = true;
        });
        step(2, () -> aquaMobs(EntityType.DROWNED, -4, 3, 22, 2, true));
        watch(() -> mc().level.getEntitiesOfClass(com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.class, mc().player.getBoundingBox().inflate(30))
                .stream().anyMatch(s -> s.lungeProgress(0) > 0.15F && s.lungeProgress(0) < 0.35F), "mt04e_shark_side_lunge");
        step(80, () -> server(sp -> {
            var shark = com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.find(sp);
            if (shark != null) shark.swimAway();
            sp.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Drowned.class, sp.getBoundingBox().inflate(40)).forEach(e -> e.discard());
        }));
        // 3. The creatures of Atlantis, posed: seahorses (8 colors, 3 patterns), dolphins (6 colors), the manta.
        step(10, () -> {
            for (int i = 0; i < 8; i++) {
                pose(com.danrod505.greenlantern.registry.ModEntities.GIANT_SEAHORSE, -15.75 + i * 4.5, 2, 26.5, 180, i * 3 + i % 3, true);
            }
        });
        step(20, () -> aquaTeleport(0.5, 5, 5.5, 0, 3));
        step(20, () -> clean("mt05_seahorses"));
        step(2, () -> aquaTeleport(-9.5, 4.5, 19.5, 0, 5));
        step(10, () -> clean("mt05b_seahorses_close"));
        step(2, () -> {
            clearPosed();
            for (int i = 0; i < 6; i++) {
                pose(com.danrod505.greenlantern.registry.ModEntities.ATLANTEAN_DOLPHIN, -9.5 + i * 3.8, 4 + (i % 2) * 2, 24.5, 90, i, true);
            }
        });
        step(20, () -> clean("mt06_dolphins"));
        step(2, () -> {
            command("time set 18000");
            command("effect clear @a");
        });
        step(20, () -> clean("mt06b_dolphins_night_glow"));
        step(2, () -> command("time set 6000"));
        step(2, () -> {
            clearPosed();
            pose(com.danrod505.greenlantern.registry.ModEntities.MANTA_RAY, 0.5, 4, 26.5, 160, 0, true);
            pose(com.danrod505.greenlantern.registry.ModEntities.MANTA_RAY, -10.5, 7, 34.5, 200, 1, true);
            pose(com.danrod505.greenlantern.registry.ModEntities.MANTA_RAY, 11.5, 6, 36.5, 180, 2, true);
        });
        step(20, () -> clean("mt07_mantas"));
        step(2, () -> aquaTeleport(0.5, 10.5, 19.5, 0, 40));
        step(10, () -> clean("mt07b_manta_from_above"));
        // 4. Riding: the manta, swimming and then gliding out of the sea.
        step(2, () -> {
            mc().options.hideGui = false;
            aquaTeleport(0.5, 5, 25.5, 0, 0);
        });
        step(10, () -> rideNearest(com.danrod505.greenlantern.entity.MantaRayEntity.class));
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 5);
        });
        step(5, () -> shot("mt08_manta_ride"));
        step(1, () -> {
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(40, () -> {
            logSwim("manta");
            shot("mt08b_manta_ride_fast");
        });
        step(1, () -> look(0, -40));
        watch(80, () -> mc().player.getVehicle() != null && !mc().player.getVehicle().isInWater()
                && mc().player.getVehicle().getY() > aquaBase.getY() + 12.8, "mt09_manta_leap");
        step(1, () -> look(0, 10));
        step(10, () -> shot("mt09b_manta_glide"));
        step(30, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            server(sp -> sp.stopRiding());
            clearPosed();
        });
        // The seahorse.
        step(10, () -> {
            pose(com.danrod505.greenlantern.registry.ModEntities.GIANT_SEAHORSE, 0.5, 3, 40.5, 0, 4, false);
            aquaTeleport(0.5, 4, 40.5, 0, 5);
        });
        step(10, () -> rideNearest(com.danrod505.greenlantern.entity.GiantSeahorseEntity.class));
        step(10, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            mc().options.hideGui = true;
        });
        step(5, () -> clean("mt10_seahorse_ride_front"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(30, () -> {
            logSwim("seahorse");
            shot("mt10b_seahorse_ride_fast");
        });
        step(2, () -> look(-60, 0));
        step(10, () -> shot("mt10c_seahorse_turn"));
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            server(sp -> sp.stopRiding());
            clearPosed();
        });
        // The dolphin: racing with its trail of light, then a leap.
        step(10, () -> {
            pose(com.danrod505.greenlantern.registry.ModEntities.ATLANTEAN_DOLPHIN, 0.5, 4, 30.5, 0, 1, false);
            aquaTeleport(0.5, 5, 30.5, 0, 0);
        });
        step(10, () -> rideNearest(com.danrod505.greenlantern.entity.AtlanteanDolphinEntity.class));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 5);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(35, () -> {
            logSwim("dolphin");
            shot("mt11_dolphin_race");
        });
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            mc().options.hideGui = true;
        });
        step(3, () -> clean("mt11c_dolphin_front"));
        step(2, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            mc().options.hideGui = false;
            look(0, -45);
        });
        watch(60, () -> mc().player.getVehicle() != null && !mc().player.getVehicle().isInWater()
                && mc().player.getVehicle().getY() > aquaBase.getY() + 13.0, "mt11b_dolphin_leap");
        step(30, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            server(sp -> sp.stopRiding());
            clearPosed();
        });
        // 5. The eggs in the hotbar and hatching one on the water.
        step(10, () -> {
            aquaTeleport(0.5, 12.2, 60.5, 0, 50);
            mc().player.getInventory().setSelectedSlot(3);
        });
        step(10, () -> server(sp -> {
            sp.getInventory().setSelectedSlot(3);
            BlockPos floor = aquaBase.offset(0, 10, 63);
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor), net.minecraft.core.Direction.UP, floor, false);
            sp.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(sp, InteractionHand.MAIN_HAND, hit));
        }));
        step(20, () -> shot("mt12_eggs_hotbar"));
        // 6. The Heroes' Guide: sections and subsections.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("welcome")));
        step(10, () -> shot("mt13_guide_welcome"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("atlantis_creatures")));
        step(10, () -> shot("mt13b_guide_creatures"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("aquaman_shark")));
        step(10, () -> shot("mt13c_guide_shark"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("recipes_atlantis")));
        step(10, () -> shot("mt13d_guide_recipes"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("controls_lantern")));
        step(10, () -> shot("mt13e_guide_lantern"));
        step(20, () -> mc().stop());
    }
}
