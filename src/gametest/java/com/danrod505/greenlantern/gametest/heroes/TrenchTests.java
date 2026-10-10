package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.TrenchCocoonEntity;
import com.danrod505.greenlantern.entity.TrenchCreatureEntity;
import com.danrod505.greenlantern.trench.Trench;
import com.danrod505.greenlantern.trench.TrenchBuilder;
import com.danrod505.greenlantern.trench.TrenchLife;
import com.danrod505.greenlantern.trench.TrenchNest;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/** The Trench: nests, hunting creatures, cocoons and raids. */
public final class TrenchTests {
    private TrenchTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("trench_nest_build", () -> TrenchTests::trenchNestBuild);
        tests.register("trench_creature_hunts", () -> TrenchTests::trenchCreatureHunts);
        tests.register("trench_creature_grabs_villager", () -> TrenchTests::trenchCreatureGrabsVillager);
        tests.register("trench_cocoon_rescue", () -> TrenchTests::trenchCocoonRescue);
        tests.register("trench_raid", () -> TrenchTests::trenchRaid);
    }

    /** A nest is dug out: the flooded pit, the brood mound crowned by its catalyst, the chambers, the biome. */
    public static void trenchNestBuild(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        Trench.Nest nest = new Trench.Nest(7, 7000, 7000, -36, -10, 20);
        long start = System.nanoTime();
        TrenchBuilder.buildNestAt(overworld, nest);
        GreenLantern.LOGGER.info("GAMETEST trench nest built in {} ms", (System.nanoTime() - start) / 1_000_000);
        boolean catalyst = false;
        for (int y = nest.floor(); y <= nest.floor() + 8; y++) {
            if (overworld.getBlockState(new BlockPos(nest.x(), y, nest.z())).is(Blocks.SCULK_CATALYST)) catalyst = true;
        }
        helper.assertTrue(catalyst, "the brood mound is crowned by a sculk catalyst");
        int water = 0;
        int total = 0;
        for (int y = nest.floor() + 8; y <= nest.rim(); y++) {
            total++;
            if (overworld.getFluidState(new BlockPos(nest.x() + 13, y, nest.z() + 2)).is(net.minecraft.tags.FluidTags.WATER)) water++;
        }
        helper.assertTrue(water * 10 >= total * 7, "the pit is open and flooded (" + water + "/" + total + ")");
        for (Vec3 spot : TrenchNest.cocoonSpots(nest)) {
            BlockPos pos = BlockPos.containing(spot.x, spot.y + 1, spot.z);
            helper.assertTrue(overworld.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER), "the chamber is hollow and flooded at " + pos);
        }
        helper.assertTrue(overworld.getBiome(nest.center().above(4)).is(Trench.BIOME), "the nest lies in the Trench's own sea");
        helper.assertTrue(overworld.getBiome(new BlockPos(nest.x() + Trench.TERRITORY - 6, nest.rim(), nest.z())).is(Trench.BIOME),
                "the territory is the Trench's own sea too");
        helper.succeed();
    }

    /** A creature of the Trench hunts a player who swims close, and bites. */
    public static void trenchCreatureHunts(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 3.5, 0, 0);
        helper.startSequence()
                .thenExecuteFor(65, player::doTick) // wait out the spawn invulnerability
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    TrenchCreatureEntity creature = TrenchCreatureEntity.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(7.5, 2, 11.5)), false, -1, 0);
                    helper.assertTrue(creature != null, "the creature should spawn");
                })
                .thenWaitUntil(() -> {
                    player.doTick();
                    helper.assertTrue(player.getHealth() < player.getMaxHealth(), "the creature should bite the swimmer");
                })
                .thenExecute(() -> {
                    TrenchCreatureEntity creature = around(player, TrenchCreatureEntity.class, 16).getFirst();
                    helper.assertTrue(creature.getTarget() == player, "it hunts the player");
                    creature.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    private static Villager trenchVillager;

    /** A creature seizes a villager in the water to carry it away. */
    public static void trenchCreatureGrabsVillager(GameTestHelper helper) {
        flood(helper, 6);
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(7.5, 2, 3.5));
        villager.setNoAi(true);
        trenchVillager = villager;
        TrenchCreatureEntity creature = TrenchCreatureEntity.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(7.5, 2, 11.5)), false, -1, 0);
        helper.assertTrue(creature != null, "the creature should spawn");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(trenchVillager.getVehicle() instanceof TrenchCreatureEntity, "the creature should seize the villager"))
                .thenExecute(() -> {
                    helper.assertTrue(trenchVillager.hasEffect(MobEffects.WATER_BREATHING), "the captive breathes through the grip");
                    helper.assertTrue(trenchVillager.getHealth() == trenchVillager.getMaxHealth(), "captives are taken alive");
                    TrenchCreatureEntity taker = (TrenchCreatureEntity) trenchVillager.getVehicle();
                    trenchVillager.stopRiding();
                    taker.discard();
                    trenchVillager.discard();
                })
                .thenSucceed();
    }

    /** Breaking a cocoon frees the villager inside, who swims off breathing water and leaves emeralds. */
    public static void trenchCocoonRescue(GameTestHelper helper) {
        flood(helper, 6);
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(7.5, 2, 9.5));
        villager.setNoAi(true);
        TrenchCocoonEntity cocoon = TrenchCocoonEntity.encase(level, helper.absoluteVec(new Vec3(7.5, 2, 9.5)), villager, -1);
        helper.assertTrue(cocoon != null && villager.getVehicle() == cocoon, "the villager is shut in the cocoon");
        ServerPlayer player = player(helper, 7.5, 2, 7.0, 0, 0);
        int rescued = Trench.rescued(level.getServer());
        helper.startSequence()
                .thenExecuteFor(65, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(cocoon.hurtServer(level, level.damageSources().drown(), 50.0F), "the sea doesn't break a cocoon");
                    helper.assertTrue(cocoon.isAlive(), "the cocoon holds");
                    cocoon.hurtServer(level, level.damageSources().playerAttack(player), 6.0F);
                    helper.assertTrue(cocoon.isAlive() && villager.isPassenger(), "it takes more than one hit");
                })
                .thenIdle(12)
                .thenExecute(() -> cocoon.hurtServer(level, level.damageSources().playerAttack(player), 20.0F))
                .thenWaitUntil(() -> helper.assertTrue(cocoon.isRemoved(), "the cocoon bursts"))
                .thenExecute(() -> {
                    helper.assertFalse(villager.isPassenger(), "the villager is free");
                    helper.assertTrue(villager.isAlive(), "and alive");
                    helper.assertTrue(villager.hasEffect(MobEffects.WATER_BREATHING), "it can breathe on its way up");
                    helper.assertTrue(Trench.rescued(level.getServer()) == rescued + 1, "the rescue is counted");
                    helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(villager.blockPosition()).inflate(4),
                            e -> e.getItem().is(Items.EMERALD)).isEmpty(), "emeralds in thanks");
                    villager.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    /** A war party leaves a nest for the walls of Atlantis, under a boss bar, and goes for the Atlanteans. */
    public static void trenchRaid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        com.danrod505.greenlantern.atlantis.Atlantis.Site site = new com.danrod505.greenlantern.atlantis.Atlantis.Site(-8000, 8000, -30, 20);
        Trench.Nest nest = new Trench.Nest(0, -8000 + Trench.DISTANCE, 8000, -40, -12, 20);
        TrenchLife.stopRaidForTesting();
        List<TrenchCreatureEntity> raiders = TrenchLife.startRaid(level, site, List.of(nest));
        try {
            helper.assertTrue(raiders.size() >= 5, "a whole war party comes, got " + raiders.size());
            helper.assertTrue(TrenchLife.raidActive(), "the raid is on");
            helper.assertTrue(raiders.stream().allMatch(TrenchCreatureEntity::isRaider), "every one of them is a raider");
            helper.assertTrue(raiders.stream().anyMatch(TrenchCreatureEntity::isBrute), "a brute leads them");
            TrenchCreatureEntity first = raiders.getFirst();
            double dx = first.getX() - (site.x() + 0.5);
            double dz = first.getZ() - (site.z() + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz);
            helper.assertTrue(d > com.danrod505.greenlantern.atlantis.Atlantis.RADIUS - 4 && d < com.danrod505.greenlantern.atlantis.Atlantis.RADIUS + 16,
                    "they strike at the city wall, " + d + " blocks from the center");
            helper.assertTrue(first.getX() > site.x(), "from the side of their nest");
            helper.assertTrue(TrenchLife.startRaid(level, site, List.of(nest)).isEmpty(), "one raid at a time");
        } finally {
            TrenchLife.stopRaidForTesting();
            for (TrenchCreatureEntity raider : raiders) raider.discard();
        }
        helper.succeed();
    }
}
