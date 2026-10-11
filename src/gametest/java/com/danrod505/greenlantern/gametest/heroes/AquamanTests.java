package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.aquaman.AquaPower;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.aquaman.AquamanServer;
import com.danrod505.greenlantern.aquaman.SeaCall;
import com.danrod505.greenlantern.entity.AquaTridentEntity;
import com.danrod505.greenlantern.entity.GreatWhiteSharkEntity;
import com.danrod505.greenlantern.entity.KrakenEntity;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.item.AquaTridentItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.ring.LanternHero;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/** Aquaman: suit, breathing, trident, shark, the call of the sea, the Kraken, the Atlantean mounts, the respirator and Atlantis. */
public final class AquamanTests {
    private AquamanTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("aquaman_suit_summon_and_swap", () -> AquamanTests::aquamanSuitSummonAndSwap);
        tests.register("aquaman_breathes_and_regens", () -> AquamanTests::aquamanBreathesAndRegens);
        tests.register("aquaman_trident_throw_returns", () -> AquamanTests::aquamanTridentThrowReturns);
        tests.register("aquaman_shark_bite", () -> AquamanTests::aquamanSharkBite);
        tests.register("aquaman_sea_call", () -> AquamanTests::aquamanSeaCall);
        tests.register("atlantean_mounts_hatch_and_ride", () -> AquamanTests::atlanteanMountsHatchAndRide);
        tests.register("aquaman_kraken_call", () -> AquamanTests::aquamanKrakenCall);
        tests.register("aquaman_kraken_fights_and_falls", () -> AquamanTests::aquamanKrakenFightsAndFalls);
        tests.register("respirator_breathes_with_any_suit", () -> AquamanTests::respiratorBreathesWithAnySuit);
        tests.register("respirator_refills_out_of_water", () -> AquamanTests::respiratorRefillsOutOfWater);
        tests.register("atlantis_build_and_travel", () -> AquamanTests::atlantisBuildAndTravel);
        tests.register("atlantis_portal_power", () -> AquamanTests::atlantisPortalPower);
    }

    public static void aquamanSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        // The Flash first, then Aquaman replaces him (one hero at a time).
        ItemStack ring = new ItemStack(ModItems.FLASH_RING.get());
        player.getInventory().add(ring);
        FlashHero.INSTANCE.summonSuit(player);
        helper.assertTrue(FlashHelper.isSuited(player), "flash suit should be on");
        giveEmblem(player, 0);
        use(player); // not suited as Aquaman: right click summons the suit
        helper.assertTrue(AquamanHelper.isSuited(player), "aquaman suit should be summoned");
        helper.assertFalse(FlashHelper.isSuited(player), "the flash suit should be gone");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Aquaman has no mask: the helmet stays on");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.AQUAMAN_LEGGINGS.get()), "leggings should be worn");
        helper.assertTrue(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) >= 1.0, "the suit should make swimming easy");
        AquamanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(AquamanHelper.isSuited(player), "aquaman suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet still on");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "iron chestplate should be restored");
        helper.assertTrue(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) < 1.0, "swimming back to normal");
        remove(player);
        helper.succeed();
    }

    public static void aquamanBreathesAndRegens(GameTestHelper helper) {
        flood(helper, 5);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 0);
        AquamanHero.INSTANCE.summonSuit(player);
        player.setAirSupply(0);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    helper.assertTrue(player.getAirSupply() == player.getMaxAirSupply(), "Aquaman breathes underwater, air " + player.getAirSupply());
                    int stored = AquamanHero.SEA_FORCE.get(emblem).stored();
                    helper.assertTrue(stored >= 30, "the Power of the Seas should refill quickly in water, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanTridentThrowReturns(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.TRIDENT);
        Zombie zombie = dummy(helper, 7.5, 1, 7.5);
        float health = zombie.getHealth();
        use(player);
        ItemStack trident = player.getMainHandItem();
        helper.assertTrue(AquamanHelper.isTrident(trident), "the trident should appear in the main hand");
        helper.assertFalse(AquamanHelper.findEmblem(player).isEmpty(), "the emblem should move to the inventory");
        helper.assertTrue(AquamanHero.SEA_FORCE.get(AquamanHelper.findEmblem(player)).stored() == 1000 - AquaPower.TRIDENT.cost(), "the trident should cost Power of the Seas");
        player.setXRot(10.0F);
        AquaTridentItem.throwTrident(player, trident);
        helper.assertTrue(AquamanHelper.tridentSlot(player) < 0, "the thrown trident leaves the hand");
        helper.assertTrue(AquaTridentEntity.find(player) != null, "the trident should be in flight");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "zombie should be hit"))
                .thenWaitUntil(() -> helper.assertTrue(AquamanHelper.tridentSlot(player) >= 0, "the trident should come back"))
                .thenExecute(() -> {
                    helper.assertTrue(AquaTridentEntity.find(player) == null, "no trident left in flight");
                    remove(player);
                })
                .thenSucceed();
    }

    private static Zombie sharkPrey;
    private static float sharkPreyHealth;

    public static void aquamanSharkBite(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 3.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.SHARK);
        helper.startSequence()
                .thenExecuteFor(3, player::doTick) // notice the water
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    use(player);
                    GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.find(player);
                    helper.assertTrue(shark != null, "a shark should be summoned");
                    helper.assertTrue(player.getVehicle() == shark, "Aquaman should ride the shark");
                    Vec3 mouth = shark.mouth();
                    Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    zombie.snapTo(mouth.x, mouth.y - 0.8, mouth.z, 180, 0);
                    zombie.setNoAi(true);
                    helper.getLevel().addFreshEntity(zombie);
                    sharkPrey = zombie;
                    sharkPreyHealth = zombie.getHealth();
                    helper.assertTrue(shark.bite(), "the shark should lunge at the zombie in front of its jaws");
                    helper.assertTrue(shark.isLunging(), "the bite starts with a lunge");
                    helper.assertTrue(sharkPrey.getHealth() >= sharkPreyHealth, "the jaws close only after the wind-up");
                    helper.assertFalse(shark.bite(), "no second bite in the middle of a lunge");
                })
                .thenWaitUntil(() -> helper.assertTrue(sharkPrey.getHealth() < sharkPreyHealth || sharkPrey.isDeadOrDying(), "the bite should hurt"))
                .thenWaitUntil(() -> helper.assertFalse(GreatWhiteSharkEntity.find(player).isLunging(), "the lunge ends"))
                .thenExecute(() -> {
                    GreatWhiteSharkEntity shark = GreatWhiteSharkEntity.find(player);
                    AquamanServer.usePower(player, emblem, AquaPower.SHARK);
                    helper.assertTrue(shark.isRemoved(), "using the power again sends the shark away");
                    sharkPrey.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    /** The eggs hatch the three creatures of Atlantis; anyone can ride them and breathes while riding. */
    public static void atlanteanMountsHatchAndRide(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        var eggs = List.of(com.danrod505.greenlantern.registry.ModItems.MANTA_RAY_EGG.get(),
                com.danrod505.greenlantern.registry.ModItems.GIANT_SEAHORSE_EGG.get(),
                com.danrod505.greenlantern.registry.ModItems.ATLANTEAN_DOLPHIN_EGG.get());
        int[][] spots = {{7, 3}, {3, 11}, {11, 11}};
        for (int i = 0; i < eggs.size(); i++) {
            BlockPos floor = helper.absolutePos(new BlockPos(spots[i][0], 0, spots[i][1]));
            ItemStack egg = new ItemStack(eggs.get(i), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, egg);
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false);
            var result = eggs.get(i).useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            helper.assertTrue(result.consumesAction(), "the egg should hatch: " + eggs.get(i));
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "hatching uses one egg");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var mounts = around(player, com.danrod505.greenlantern.entity.AtlanteanMountEntity.class, 12);
        helper.assertTrue(mounts.size() == 3, "three creatures should hatch, got " + mounts.size());
        helper.assertTrue(mounts.stream().anyMatch(m -> m instanceof com.danrod505.greenlantern.entity.MantaRayEntity), "a manta ray");
        helper.assertTrue(mounts.stream().anyMatch(m -> m instanceof com.danrod505.greenlantern.entity.GiantSeahorseEntity), "a seahorse");
        helper.assertTrue(mounts.stream().anyMatch(m -> m instanceof com.danrod505.greenlantern.entity.AtlanteanDolphinEntity), "a dolphin");
        for (var mount : mounts) {
            helper.assertTrue(mount.isPersistenceRequired(), "hatched creatures don't despawn");
            helper.assertTrue(mount.variant() >= 0 && mount.variant() < mount.variants(), "valid look");
        }
        var seahorse = mounts.stream().filter(m -> m instanceof com.danrod505.greenlantern.entity.GiantSeahorseEntity).findFirst().orElseThrow();
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    seahorse.interact(player, InteractionHand.MAIN_HAND);
                    helper.assertTrue(player.getVehicle() == seahorse, "anyone can ride a creature of Atlantis");
                    helper.assertTrue(seahorse.getControllingPassenger() == player, "the rider steers it");
                    player.setAirSupply(10);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(player.getAirSupply() > 200, "the rider breathes through the creature, air=" + player.getAirSupply());
                    player.stopRiding();
                    helper.assertTrue(player.getVehicle() == null, "climbed off");
                    helper.assertTrue(seahorse.isAlive(), "the seahorse is fine");
                    for (var mount : mounts) mount.discard();
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanKrakenCall(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.KRAKEN);
        helper.startSequence()
                .thenExecuteFor(3, player::doTick) // notice the water
                .thenExecute(() -> {
                    helper.assertTrue(player.isInWater(), "the player should be in the water");
                    use(player);
                    KrakenEntity kraken = KrakenEntity.find(player);
                    helper.assertTrue(kraken != null, "the Kraken should rise");
                    helper.assertTrue(player.getVehicle() == kraken, "Aquaman should ride the Kraken");
                    helper.assertTrue(kraken.isSwimmingMode(), "called in deep water, the Kraken swims");
                    helper.assertTrue(kraken.getHealth() == KrakenEntity.maxHealth(), "the Kraken starts with full life");
                    int stored = AquamanHero.SEA_FORCE.get(AquamanHelper.findEmblem(player)).stored();
                    helper.assertTrue(stored <= 1000 - AquaPower.KRAKEN.cost(), "the Kraken should cost Power of the Seas, left " + stored);
                })
                .thenExecuteFor(5, player::doTick)
                .thenExecute(() -> {
                    KrakenEntity kraken = KrakenEntity.find(player);
                    helper.assertTrue(kraken != null && player.getVehicle() == kraken, "still riding the Kraken");
                    AquamanServer.usePower(player, AquamanHelper.findEmblem(player), AquaPower.KRAKEN);
                    helper.assertTrue(kraken.isRemoved(), "using the power again sends the Kraken back to the deep");
                    helper.assertTrue(player.getVehicle() == null, "Aquaman is off its head");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanKrakenFightsAndFalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        KrakenEntity kraken = KrakenEntity.create(level, player, false);
        level.addFreshEntity(kraken);
        player.startRiding(kraken);
        // The tentacle comes down about nine blocks in front of the Kraken (it faces south, +Z).
        Zombie slamTarget = dummy(helper, 7.5, 1, 12.0);
        float slamHealth = slamTarget.getHealth();
        Zombie[] jetTarget = new Zombie[1];
        float[] before = new float[2];
        helper.startSequence()
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.getVehicle() == kraken, "Aquaman should ride the Kraken");
                    helper.assertTrue(player.getY() - kraken.getY() > 8.0, "Aquaman sits on top of its head, got " + (player.getY() - kraken.getY()));
                    helper.assertFalse(kraken.isSwimmingMode(), "on dry land the Kraken walks");
                    helper.assertTrue(kraken.tentacleSlam(), "the tentacle slam should start");
                    helper.assertFalse(kraken.tentacleSlam(), "the tentacle needs a moment before the next slam");
                })
                .thenExecuteFor(KrakenEntity.SLAM_HIT + 2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(slamTarget.isDeadOrDying() || slamTarget.getHealth() < slamHealth, "the slam should crush the zombie");
                    jetTarget[0] = dummy(helper, 3.5, 1, 12.5);
                    before[0] = jetTarget[0].getHealth();
                    lookAt(player, jetTarget[0]);
                    kraken.setJetFiring(true);
                })
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(kraken.isJetting(), "the water jet should be firing");
                    helper.assertTrue(jetTarget[0].isDeadOrDying() || jetTarget[0].getHealth() < before[0], "the water jet should batter the zombie");
                    helper.assertTrue(AquamanHero.SEA_FORCE.get(emblem).stored() < 1000, "the jet drains Power of the Seas");
                    kraken.setJetFiring(false);
                })
                // Past the newcomer's spawn protection, so hits reach the rider.
                .thenExecuteFor(50, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(kraken.isJetting(), "the jet stops when released");
                    Zombie attacker = dummy(helper, 9.5, 1, 4.5);
                    float riderHealth = player.getHealth();
                    before[1] = kraken.getHealth();
                    player.hurtServer(level, level.damageSources().mobAttack(attacker), 6.0F);
                    helper.assertTrue(player.getHealth() == riderHealth, "the Kraken should take the blow aimed at its rider");
                    helper.assertTrue(kraken.getHealth() < before[1], "the Kraken loses life instead, " + kraken.getHealth() + " / " + before[1]);
                    attacker.discard();
                })
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    kraken.hurtServer(level, level.damageSources().generic(), KrakenEntity.maxHealth() * 2);
                    helper.assertTrue(kraken.isDying(), "out of life, the Kraken dies");
                    helper.assertTrue(player.getVehicle() == null, "its rider falls off");
                    helper.assertTrue(KrakenEntity.recoverySeconds(player) > 0, "a fallen Kraken needs time to recover");
                })
                .thenExecuteFor(KrakenEntity.DEATH_TICKS + 5, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(kraken.isRemoved(), "the dead Kraken is gone after its death throes");
                    AquaPower.POWERS.select(emblem, AquaPower.KRAKEN);
                    helper.assertFalse(AquamanServer.usePower(player, emblem, AquaPower.KRAKEN), "it cannot be called while recovering");
                    KrakenEntity.clearRecovery(player);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void aquamanSeaCall(GameTestHelper helper) {
        flood(helper, 6);
        ServerPlayer player = player(helper, 7.5, 2, 3.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        AquaPower.POWERS.select(emblem, AquaPower.SEA_CALL);
        helper.spawn(EntityType.COD, new Vec3(5.5, 3, 4.5));
        helper.spawn(EntityType.SQUID, new Vec3(9.5, 3, 4.5));
        Zombie zombie = dummy(helper, 7.5, 2, 9.5);
        float health = zombie.getHealth();
        helper.startSequence()
                .thenExecuteFor(3, player::doTick) // notice the water
                .thenExecute(() -> {
                    use(player);
                    helper.assertTrue(SeaCall.isActive(player), "the call should be active");
                    int allies = SeaCall.allies(player);
                    helper.assertTrue(allies >= GLConfig.SEA_CALL_HELPERS.get(), "dolphins should make up the numbers, got " + allies);
                })
                .thenExecuteFor(120, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the sea creatures should attack the zombie");
                    AquamanServer.usePower(player, emblem, AquaPower.SEA_CALL);
                    helper.assertFalse(SeaCall.isActive(player), "using it again releases the creatures");
                    helper.assertTrue(around(player, net.minecraft.world.entity.animal.dolphin.Dolphin.class, 20).isEmpty(),
                            "the dolphins from the deep go back");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void respiratorBreathesWithAnySuit(GameTestHelper helper) {
        flood(helper, 5);
        ServerPlayer player = player(helper, 7.5, 2, 7.5, 0, 0);
        giveRing(player, 3000);
        LanternHero.INSTANCE.summonSuit(player); // the Lantern mask holds the helmet slot: the respirator works anyway
        player.getInventory().setItem(20, new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()));
        player.setAirSupply(0);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER), "the player's head should be underwater");
                    helper.assertTrue(player.getAirSupply() == player.getMaxAirSupply(), "the respirator breathes for the player, air " + player.getAirSupply());
                    helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LANTERN_MASK.get()), "the mask stays on");
                    ItemStack respirator = player.getInventory().getItem(20);
                    int air = com.danrod505.greenlantern.aquaman.Respirator.air(respirator);
                    helper.assertTrue(air < com.danrod505.greenlantern.aquaman.Respirator.capacity(), "the respirator should spend air, has " + air);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void respiratorRefillsOutOfWater(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack respirator = new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get());
        com.danrod505.greenlantern.aquaman.Respirator.setAir(respirator, 0);
        player.getInventory().setItem(9, respirator);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int air = com.danrod505.greenlantern.aquaman.Respirator.air(player.getInventory().getItem(9));
                    helper.assertTrue(air > 0, "the respirator should refill out of the water, has " + air);
                    remove(player);
                })
                .thenSucceed();
    }

    /** Raises a whole Atlantis far from the tests, checks its landmarks and makes the round trip. */
    public static void atlantisBuildAndTravel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        com.danrod505.greenlantern.atlantis.Atlantis.Site site = new com.danrod505.greenlantern.atlantis.Atlantis.Site(6000, 6000, -30, 20);
        long start = System.nanoTime();
        com.danrod505.greenlantern.atlantis.AtlantisBuilder.buildAllAt(overworld, site);
        GreenLantern.LOGGER.info("GAMETEST atlantis built in {} ms", (System.nanoTime() - start) / 1_000_000);
        BlockPos c = site.center();
        helper.assertTrue(overworld.getBlockState(c.above(24)).is(Blocks.BEACON), "the palace beacon");
        helper.assertTrue(overworld.getBlockState(c.above(23)).is(Blocks.GOLD_BLOCK), "the beacon stands on gold");
        BlockPos pavilion = c.offset(0, 1, com.danrod505.greenlantern.atlantis.Atlantis.PAVILION_Z);
        helper.assertTrue(overworld.getBlockState(pavilion).isAir(), "the pavilion is full of air");
        helper.assertTrue(overworld.getBlockState(c.offset(0, 0, com.danrod505.greenlantern.atlantis.Atlantis.PAVILION_Z - com.danrod505.greenlantern.atlantis.Atlantis.PAVILION_RADIUS)).is(Blocks.WAXED_OXIDIZED_COPPER_DOOR), "the pavilion door");
        helper.assertTrue(overworld.getBlockState(c.offset(0, 1, 15)).is(Blocks.WATER), "the plaza is under water");
        helper.assertTrue(overworld.getBlockState(c.offset(30, 3, 40)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                || !overworld.getBlockState(c.offset(30, 3, 40)).isAir(), "no air pockets in the city");
        helper.assertTrue(overworld.getBlockState(c.offset(0, 20, 70)).is(Blocks.WATER), "the slopes are flooded");
        // The pavilion's chest holds a respirator for visitors.
        boolean found = false;
        for (int dx = -6; dx <= 6 && !found; dx++) {
            for (int dz = -6; dz <= 6 && !found; dz++) {
                if (overworld.getBlockEntity(pavilion.offset(dx, -1, dz)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    for (int i = 0; i < chest.getContainerSize(); i++) {
                        if (chest.getItem(i).is(ModItems.ATLANTEAN_RESPIRATOR.get())) found = true;
                    }
                }
            }
        }
        helper.assertTrue(found, "a respirator waits in the pavilion");

        com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, site, true);
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        Vec3 home = player.position();
        try {
            helper.assertTrue(com.danrod505.greenlantern.atlantis.AtlantisTravel.sendToAtlantis(player), "the trip to Atlantis");
            helper.assertTrue(player.level() == overworld && player.position().distanceTo(site.arrival()) < 1.0,
                    "arrives in the pavilion, at " + player.position());
            helper.assertTrue(com.danrod505.greenlantern.atlantis.AtlantisTravel.leadsHome(overworld, player.position()), "in Atlantis, portals lead home");
            com.danrod505.greenlantern.atlantis.AtlantisTravel.returnHome(player);
            helper.assertTrue(player.position().distanceTo(home) < 1.0, "back to the exact spot, at " + player.position());
        } finally {
            com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, null, false);
            remove(player);
        }
        helper.succeed();
    }

    /** Without an Atlantis in the world, the power says so and costs nothing; the gate opens nothing. */
    public static void atlantisPortalPower(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, null, false);
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack emblem = giveEmblem(player, 1000);
        AquamanHero.INSTANCE.summonSuit(player);
        boolean used = AquamanServer.usePower(player, emblem, AquaPower.ATLANTIS_PORTAL);
        helper.assertFalse(used, "no Atlantis in this world: no portal");
        helper.assertTrue(AquamanHero.SEA_FORCE.get(emblem).stored() == 1000, "a failed portal costs nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ATLANTIS_GATE.get()));
        use(player);
        helper.assertTrue(around(player, com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, 8.0).isEmpty(), "the gate opens nothing either");

        // With an Atlantis (pretend), the power opens the whirlpool and pays for it.
        var site = new com.danrod505.greenlantern.atlantis.Atlantis.Site(9000, 9000, -30, 20);
        com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, site, true);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, emblem);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(emblem));
            helper.assertTrue(AquamanServer.usePower(player, emblem, AquaPower.ATLANTIS_PORTAL), "the power opens a portal");
            var portals = around(player, com.danrod505.greenlantern.entity.AtlantisPortalEntity.class, 8.0);
            helper.assertTrue(portals.size() == 1, "one whirlpool, found " + portals.size());
            helper.assertFalse(portals.getFirst().leadsHome(), "far from Atlantis, the portal leads there");
            helper.assertTrue(AquamanHero.SEA_FORCE.get(emblem).stored() == 1000 - AquaPower.ATLANTIS_PORTAL.cost(), "the portal costs Power of the Seas");
            portals.forEach(p -> p.discard());
        } finally {
            com.danrod505.greenlantern.atlantis.Atlantis.setSiteForTesting(server, null, false);
            remove(player);
        }
        helper.succeed();
    }
}
