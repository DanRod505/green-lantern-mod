package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;
import static com.danrod505.greenlantern.gametest.scripts.AtlantisScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Screenshot script "trench": The Trench, the hostile faction of the deep. */
public final class TrenchScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("trench", TrenchScript::build, null, true);

    private TrenchScript() {}

    public static volatile com.danrod505.greenlantern.trench.Trench.Nest trenchNest;
    public static final List<com.danrod505.greenlantern.entity.TrenchCreatureEntity> TRENCH_POSED = new ArrayList<>();

    /** Teleports to a point (absolute), looking at another. */
    public static void tpLook(Vec3 from, Vec3 at) {
        double dx = at.x - from.x;
        double dy = at.y - from.y;
        double dz = at.z - from.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        command(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", from.x, from.y, from.z, yaw, pitch));
    }

    /** Like {@link #tpLook}, but rises from the point until the camera is in open water (not inside a wall or a spire). */
    public static void tpLookWater(Vec3 from, Vec3 at) {
        server(sp -> {
            Vec3 p = from;
            for (int i = 0; i < 30; i++) {
                BlockPos b = BlockPos.containing(p);
                if (sp.level().getBlockState(b).is(net.minecraft.world.level.block.Blocks.WATER)
                        && sp.level().getBlockState(b.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                        && sp.level().getBlockState(b.below()).is(net.minecraft.world.level.block.Blocks.WATER)) break;
                p = p.add(0, 1, 0);
            }
            tpLook(p, at);
        });
    }

    /** Relative to the nest's center (on its floor). */
    public static Vec3 atNest(double dx, double dy, double dz) {
        var n = trenchNest;
        return new Vec3(n.x() + 0.5 + dx, n.floor() + dy, n.z() + 0.5 + dz);
    }

    /** Poses a creature (or brute) with no AI at a point, facing a direction; returns it for more posing. */
    public static void poseCreature(Vec3 pos, boolean brute, float yaw, boolean carrying) {
        server(sp -> {
            var c = com.danrod505.greenlantern.entity.TrenchCreatureEntity.spawn(sp.level(), pos, brute, -1, 0);
            if (c == null) return;
            c.setNoAi(true);
            c.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
            c.setYHeadRot(yaw);
            c.setYBodyRot(yaw);
            TRENCH_POSED.add(c);
            if (carrying) {
                var villager = net.minecraft.world.entity.EntityType.VILLAGER.create(sp.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (villager == null) return;
                villager.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
                villager.setNoAi(true);
                sp.level().addFreshEntity(villager);
                villager.startRiding(c, true, true);
            }
        });
    }

    public static void clearTrenchPosed() {
        server(sp -> {
            for (var c : TRENCH_POSED) {
                if (c.getFirstPassenger() != null) c.getFirstPassenger().discard();
                c.discard();
            }
            TRENCH_POSED.clear();
        });
    }

    /** The Trench: the dark territory, the pit and its nest, the creatures, a cocoon and its rescue, a raid on Atlantis, the guide. */
    private static void build() {
        step(80, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule advance_weather false");
            command("gamerule spawn_mobs false");
            server(sp -> {
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.getInventory().setItem(10, new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
                sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                var server = sp.level().getServer();
                // The trip builds Atlantis; the nests are dug around it right away.
                boolean ok = com.danrod505.greenlantern.atlantis.AtlantisTravel.sendToAtlantis(sp);
                atlantis = com.danrod505.greenlantern.atlantis.Atlantis.site(server);
                boolean built = com.danrod505.greenlantern.trench.TrenchBuilder.ensureBuilt(server);
                var nests = com.danrod505.greenlantern.trench.Trench.nests(server);
                com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench travel={} built={} atlantis={} nests={}", ok, built, atlantis, nests);
                if (!nests.isEmpty()) trenchNest = nests.getFirst();
            });
        });
        waitFor(2400, () -> trenchNest != null && com.danrod505.greenlantern.trench.Trench.isComplete(mc().getSingleplayerServer()));
        // From the edge: the dark water of the Trench beyond the last fish of the open sea.
        step(2, () -> {
            camera(CameraType.FIRST_PERSON);
            var n = trenchNest;
            tpLookWater(atNest(com.danrod505.greenlantern.trench.Trench.TERRITORY + 6, n.rim() - n.floor() + 4, 0), atNest(com.danrod505.greenlantern.trench.Trench.PIT_RADIUS, n.rim() - n.floor() - 4, 0));
        });
        step(200, () -> clean("t00_territory_edge"));
        // Crossing in: the warning and the clicks from the caves.
        step(2, () -> {
            mc().options.hideGui = false;
            var n = trenchNest;
            tpLookWater(atNest(com.danrod505.greenlantern.trench.Trench.PIT_RADIUS + 10, n.rim() - n.floor() + 2, 4), atNest(0, 8, 0));
        });
        step(25, () -> shot("t01_territory_warning"));
        step(160, () -> clean("t02_territory_dark"));
        // With night vision from here on, to see the details.
        step(2, () -> {
            command("effect give @a night_vision infinite 0 true");
            var n = trenchNest;
            tpLookWater(atNest(9, 24, 9), atNest(0, 2, 0));
        });
        step(120, () -> clean("t03_pit_from_above"));
        step(2, () -> tpLookWater(atNest(15, 10, -4), atNest(0, 5, 0)));
        step(120, () -> clean("t04_brood_mound_ribs"));
        // The creatures, posed close, in the dark water of their own sea (their eyes glow).
        step(2, () -> {
            command("effect clear @a night_vision");
            Vec3 eye = atNest(8, 12, 8);
            poseCreature(eye.add(-3.2, -1.2, 0.0), false, -90.0F, false);
            tpLook(eye, eye.add(-3.2, -0.6, 0.0));
        });
        step(60, () -> clean("t05_creature"));
        step(2, () -> {
            clearTrenchPosed();
            Vec3 eye = atNest(8, 12, 8);
            poseCreature(eye.add(-4.5, -2.0, 0.0), true, -90.0F, false);
            poseCreature(eye.add(-5.5, -1.0, 2.5), false, -115.0F, false);
            poseCreature(eye.add(-5.5, -1.5, -2.5), false, -65.0F, false);
            tpLook(eye, eye.add(-4.5, -0.8, 0.0));
        });
        step(60, () -> clean("t06_brute_and_pack"));
        step(2, () -> {
            clearTrenchPosed();
            Vec3 eye = atNest(8, 12, 8);
            command("effect give @a night_vision infinite 0 true");
            poseCreature(eye.add(-3.5, -1.2, 0.0), false, -60.0F, true);
            tpLook(eye, eye.add(-3.5, -0.4, 0.0));
        });
        step(60, () -> clean("t07_carrying_villager"));
        // The living nest: its own packs on patrol.
        step(2, () -> {
            clearTrenchPosed();
            command("effect give @a night_vision infinite 0 true");
            tpLookWater(atNest(-10, 14, -10), atNest(0, 6, 0));
        });
        step(100, () -> {
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench creatures near={}", mc().level.getEntitiesOfClass(
                    com.danrod505.greenlantern.entity.TrenchCreatureEntity.class, mc().player.getBoundingBox().inflate(64)).size());
            clean("t08_nest_alive");
        });
        // A cocoon in a chamber, with a villager inside.
        step(2, () -> server(sp -> {
            var cocoons = com.danrod505.greenlantern.trench.TrenchLife.cocoons(sp.level(), trenchNest);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench cocoons={}", cocoons.size());
            Vec3 at;
            if (cocoons.isEmpty()) {
                at = com.danrod505.greenlantern.trench.TrenchNest.cocoonSpots(trenchNest).getFirst();
            } else {
                at = cocoons.getFirst().position();
            }
            Vec3 center = atNest(0, at.y - trenchNest.floor(), 0);
            Vec3 out = center.subtract(at).multiply(1, 0, 1).normalize();
            Vec3 eye = at.add(out.scale(3.2)).add(0, 1.4, 0);
            tpLook(eye, at.add(0, 1.1, 0));
        }));
        step(80, () -> clean("t09_cocoon"));
        step(2, () -> {
            mc().options.hideGui = false;
            server(sp -> {
                var cocoons = sp.level().getEntitiesOfClass(com.danrod505.greenlantern.entity.TrenchCocoonEntity.class, sp.getBoundingBox().inflate(6));
                if (!cocoons.isEmpty()) cocoons.getFirst().hurtServer(sp.level(), sp.level().damageSources().playerAttack(sp), 50.0F);
            });
        });
        step(6, () -> shot("t10_rescue_burst"));
        step(20, () -> clean("t11_villager_freed"));
        // A raid on Atlantis: the war party at the wall, under the boss bar.
        step(2, () -> {
            mc().options.hideGui = false;
            var a = atlantis;
            if (a == null) return;
            var n = trenchNest;
            double ang = Math.atan2(n.z() - a.z(), n.x() - a.x());
            Vec3 inside = new Vec3(a.x() + 0.5 + Math.cos(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS - 14), a.floor() + 14,
                    a.z() + 0.5 + Math.sin(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS - 14));
            Vec3 wall = new Vec3(a.x() + 0.5 + Math.cos(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS + 6), a.floor() + 12,
                    a.z() + 0.5 + Math.sin(ang) * (com.danrod505.greenlantern.atlantis.Atlantis.RADIUS + 6));
            tpLook(inside, wall);
        });
        step(40, () -> server(sp -> {
            if (atlantis == null) return;
            var raiders = com.danrod505.greenlantern.trench.TrenchLife.startRaid(sp.level(), atlantis, List.of(trenchNest));
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT trench raid raiders={}", raiders.size());
        }));
        step(30, () -> shot("t12_raid_warning"));
        step(60, () -> shot("t13_raid_at_the_wall"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("trench"));
        });
        step(10, () -> shot("t14_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("trench_captives")));
        step(10, () -> shot("t15_guide_captives"));
        step(20, () -> mc().stop());
    }
}
