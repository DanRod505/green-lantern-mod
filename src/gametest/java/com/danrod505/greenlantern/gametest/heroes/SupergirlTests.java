package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.supergirl.KryptoData;
import com.danrod505.greenlantern.supergirl.KryptoEntity;
import com.danrod505.greenlantern.supergirl.SupergirlConfig;
import com.danrod505.greenlantern.supergirl.SupergirlContent;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.supergirl.SupergirlPower;
import com.danrod505.greenlantern.supergirl.SupergirlServer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Supergirl: the solar energy (sun and punches), her flight and barrel roll, each power, and Krypto
 * (his arrival, fighting beside her, resting when hurt, the whistle and fetching). The hero contract
 * tests check the suit, the texts and that every power runs.
 */
public final class SupergirlTests {
    private SupergirlTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("supergirl_energy_recharges", () -> SupergirlTests::energyRecharges);
        tests.register("supergirl_punch_energy", () -> SupergirlTests::punchEnergy);
        tests.register("supergirl_powers_spend_energy", () -> SupergirlTests::powersSpendEnergy);
        tests.register("supergirl_flight", () -> SupergirlTests::flight);
        tests.register("supergirl_barrel_roll", () -> SupergirlTests::barrelRoll);
        tests.register("supergirl_heat_bolts", () -> SupergirlTests::heatBolts);
        tests.register("supergirl_meteor_dash", () -> SupergirlTests::meteorDash);
        tests.register("supergirl_thunder_clap", () -> SupergirlTests::thunderClap);
        tests.register("supergirl_frost_wall", () -> SupergirlTests::frostWall);
        tests.register("supergirl_super_hearing", () -> SupergirlTests::superHearing);
        tests.register("supergirl_kryptonian_throw", () -> SupergirlTests::kryptonianThrow);
        tests.register("supergirl_throw_block", () -> SupergirlTests::throwBlock);
        tests.register("supergirl_solar_flare", () -> SupergirlTests::solarFlare);
        tests.register("supergirl_krypto_arrives", () -> SupergirlTests::kryptoArrives);
        tests.register("supergirl_krypto_fights", () -> SupergirlTests::kryptoFights);
        tests.register("supergirl_krypto_rests", () -> SupergirlTests::kryptoRests);
        tests.register("supergirl_krypto_whistle", () -> SupergirlTests::kryptoWhistle);
        tests.register("supergirl_krypto_fetches", () -> SupergirlTests::kryptoFetches);
    }

    private static ItemStack giveItem(ServerPlayer player, int energy) {
        ItemStack item = HeroContractTests.chargedItem(SupergirlHero.INSTANCE);
        SupergirlHero.ENERGY.set(item, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        return player.getMainHandItem();
    }

    /** A suited Supergirl with a full pendant. */
    private static ServerPlayer supergirl(GameTestHelper helper, double x, double z, float yaw) {
        ServerPlayer player = player(helper, x, 1, z, yaw, 0);
        giveItem(player, SupergirlHero.ENERGY.capacity());
        helper.assertTrue(SupergirlHero.INSTANCE.summonSuit(player), "the suit should go on");
        return player;
    }

    private static boolean use(ServerPlayer player, SupergirlPower power) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(player.getMainHandItem()));
        return SupergirlHero.INSTANCE.powers().use(player, player.getMainHandItem(), power.ordinal());
    }

    private static int energy(ServerPlayer player) {
        return SupergirlHero.ENERGY.get(player.getMainHandItem()).stored();
    }

    private static void done(ServerPlayer player) {
        if (SupergirlHero.INSTANCE.isSuited(player)) SupergirlHero.INSTANCE.dismissSuit(player, false);
        remove(player);
    }

    private static DamageSource damage(GameTestHelper helper, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type) {
        return new DamageSource(helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
    }

    /** Daylight over the arena (the pendant charges from the sun). */
    private static void daylight(GameTestHelper helper, ServerPlayer player) {
        if (SupergirlServer.rechargeRate(player) < SupergirlConfig.RECHARGE_PER_SECOND.get()) helper.getLevel().setDayTime(6000);
    }

    public static void energyRecharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack item = giveItem(player, 0);
        SupergirlHero.INSTANCE.summonSuit(player);
        daylight(helper, player);
        float rate = SupergirlServer.rechargeRate(player);
        helper.assertTrue(rate > SupergirlConfig.SHADE_RECHARGE_PER_SECOND.get(), "under the open sky by day the sun should charge her fast, rate " + rate);
        // A roof right over her head (once the light settles): the shade rate.
        helper.setBlock(new BlockPos(7, 3, 7), Blocks.STONE);
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    float shade = SupergirlServer.rechargeRate(player);
                    helper.assertTrue(shade < rate, "under a roof she should charge slower, " + shade + " vs " + rate);
                    helper.setBlock(new BlockPos(7, 3, 7), Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = SupergirlHero.ENERGY.get(item).stored();
                    int expected = (int) (2 * rate) - 1;
                    helper.assertTrue(stored >= expected, "the sun should bring energy back every second, got " + stored + " (expected " + expected + ")");
                    done(player);
                })
                .thenSucceed();
    }

    public static void punchEnergy(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 5.5, 0);
        SupergirlHero.ENERGY.set(player.getMainHandItem(), 100);
        Zombie zombie = dummy(helper, 7.5, 1, 6.8);
        player.attack(zombie);
        helper.assertTrue(energy(player) == 100 + SupergirlConfig.PUNCH_ENERGY.get(), "a punch that lands gives energy back, got " + energy(player));
        int before = energy(player);
        zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().magic(), 1.0F);
        helper.assertTrue(energy(player) == before, "only her own punches give energy back");
        done(player);
        helper.succeed();
    }

    public static void powersSpendEnergy(GameTestHelper helper) {
        for (SupergirlPower power : SupergirlPower.values()) {
            ServerPlayer player = supergirl(helper, 7.5, 3.5, 0);
            // Something loose to grab for the throw.
            if (power == SupergirlPower.KRYPTONIAN_THROW) {
                helper.setBlock(new BlockPos(7, 2, 6), Blocks.DIRT);
            }
            helper.assertTrue(use(player, power), power.id() + " should go off");
            int stored = energy(player);
            int expected = power == SupergirlPower.SOLAR_FLARE ? 0 : SupergirlHero.ENERGY.capacity() - power.cost();
            helper.assertTrue(stored == expected, power.id() + " should leave " + expected + ", left " + stored);
            done(player);
        }
        helper.setBlock(new BlockPos(7, 2, 6), Blocks.AIR);
        // With nothing in reach, the throw gives its energy back.
        ServerPlayer player = supergirl(helper, 7.5, 3.5, 180);
        player.setXRot(-60);
        helper.assertTrue(use(player, SupergirlPower.KRYPTONIAN_THROW), "the throw should still answer");
        helper.assertTrue(energy(player) == SupergirlHero.ENERGY.capacity(), "a throw with nothing to grab costs nothing, left " + energy(player));
        SupergirlHero.ENERGY.set(player.getMainHandItem(), 10);
        helper.assertFalse(use(player, SupergirlPower.HEAT_BOLTS), "without energy no bolt goes off");
        helper.assertTrue(energy(player) == 10, "a power that doesn't go off costs nothing");
        done(player);
        helper.succeed();
    }

    public static void flight(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 7.5, 0);
        var mine = SupergirlHero.INSTANCE.flightProfile();
        var superman = SupermanHero.INSTANCE.flightProfile();
        helper.assertTrue(mine != null, "Supergirl should power-fly");
        helper.assertTrue(mine.max() > mine.barrier(), "she breaks the sound barrier");
        helper.assertTrue(mine.max() < superman.max(), "her top speed is below Superman's");
        helper.assertTrue(mine.seconds() < superman.seconds(), "she reaches the sound barrier quicker than Superman");
        helper.assertTrue(SupergirlHero.INSTANCE.canFly(player, player.getMainHandItem()), "with sunlight in the pendant she flies");
        SupergirlHero.ENERGY.set(player.getMainHandItem(), 0);
        helper.assertFalse(SupergirlHero.INSTANCE.canFly(player, player.getMainHandItem()), "with an empty pendant she can't fly");
        helper.assertTrue(SupergirlHero.INSTANCE.fallDamageMultiplier(player, 30) == 0.0F, "she never takes fall damage");
        done(player);
        helper.succeed();
    }

    public static void barrelRoll(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 7.5, 0);
        DamageSource arrow = damage(helper, DamageTypes.ARROW);
        DamageSource hit = damage(helper, DamageTypes.MOB_ATTACK);
        float tough = 1.0F - SupergirlConfig.DAMAGE_REDUCTION.get().floatValue();
        helper.assertTrue(Math.abs(SupergirlHero.INSTANCE.damageTakenMultiplier(player, hit) - tough) < 1.0E-4F, "her skin shrugs off part of every blow");
        helper.assertTrue(SupergirlHero.INSTANCE.damageTakenMultiplier(player, arrow) > 0.0F, "without a roll arrows hit her");
        int before = energy(player);
        SupergirlHero.INSTANCE.onFlightAction(player, FlightAction.ROLL_LEFT);
        helper.assertTrue(energy(player) == before - SupergirlConfig.ROLL_COST.get(), "the roll costs energy, left " + energy(player));
        helper.assertTrue(SupergirlServer.isDodging(player), "the roll should make her dodge");
        helper.assertTrue(SupergirlHero.INSTANCE.damageTakenMultiplier(player, arrow) == 0.0F, "arrows miss her during the roll");
        helper.assertTrue(SupergirlHero.INSTANCE.damageTakenMultiplier(player, hit) > 0.0F, "the roll doesn't stop a punch");
        helper.startSequence()
                .thenExecuteFor(SupergirlConfig.ROLL_DODGE_TICKS.get() + 2, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(SupergirlServer.isDodging(player), "the dodge should end");
                    // Without energy her skin is plain again.
                    SupergirlHero.ENERGY.set(player.getMainHandItem(), 0);
                    helper.assertTrue(SupergirlHero.INSTANCE.damageTakenMultiplier(player, hit) == 1.0F, "with no sunlight she takes full damage");
                    done(player);
                })
                .thenSucceed();
    }

    public static void heatBolts(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 1.5, 0);
        Husk husk = helper.spawn(EntityType.HUSK, new Vec3(7.5, 1, 9.5));
        husk.setNoAi(true);
        lookAt(player, husk);
        float before = husk.getHealth();
        helper.assertTrue(use(player, SupergirlPower.HEAT_BOLTS), "the bolts should fire");
        helper.assertTrue(SupergirlServer.boltsInFlight(player) == 1, "one bolt per use");
        helper.assertTrue(use(player, SupergirlPower.HEAT_BOLTS), "another bolt right away");
        helper.startSequence()
                .thenExecuteFor(8, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(SupergirlServer.boltsInFlight(player) == 0, "the bolts should have landed");
                    helper.assertTrue(!husk.isAlive() || husk.getHealth() < before, "the bolts should hurt the husk");
                    helper.assertTrue(husk.isOnFire(), "the bolts set the husk on fire");
                    husk.discard();
                    // At a wall: fire on its face, the wall itself stays.
                    helper.setBlock(new BlockPos(7, 1, 8), Blocks.STONE);
                    Vec3 to = helper.absoluteVec(new Vec3(7.5, 1.5, 8.0)).subtract(player.getEyePosition());
                    player.setYRot(0);
                    player.setYHeadRot(0);
                    player.setXRot((float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI));
                    use(player, SupergirlPower.HEAT_BOLTS);
                })
                .thenExecuteFor(6, player::doTick)
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(7, 1, 8));
                    helper.assertBlockPresent(Blocks.FIRE, new BlockPos(7, 1, 7));
                    helper.setBlock(new BlockPos(7, 1, 7), Blocks.AIR);
                    done(player);
                })
                .thenSucceed();
    }

    public static void meteorDash(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 1.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 6.5);
        float before = zombie.getHealth();
        Vec3 start = player.position();
        helper.assertTrue(use(player, SupergirlPower.METEOR_DASH), "the dash should start");
        helper.startSequence()
                .thenExecuteFor(10, player::doTick)
                .thenExecute(() -> {
                    double went = player.position().distanceTo(start);
                    helper.assertTrue(went >= 8.0, "she should streak forward, went " + went);
                    helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < before, "the zombie in the way should be rammed");
                    done(player);
                })
                .thenSucceed();
    }

    public static void thunderClap(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 2, 5), Blocks.GLASS);
        helper.setBlock(new BlockPos(8, 1, 6), Blocks.TORCH);
        ServerPlayer player = supergirl(helper, 7.5, 1.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 7.5);
        float before = zombie.getHealth();
        helper.assertTrue(use(player, SupergirlPower.THUNDER_CLAP), "the clap should go off");
        helper.assertBlockNotPresent(Blocks.GLASS, new BlockPos(7, 2, 5));
        helper.assertBlockNotPresent(Blocks.TORCH, new BlockPos(8, 1, 6));
        helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "the zombie should be stunned");
        helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < before, "the shockwave should hurt the zombie");
        // Behind her, nothing happens.
        ServerPlayer back = supergirl(helper, 3.5, 7.5, 180);
        Zombie behind = dummy(helper, 3.5, 1, 10.5);
        use(back, SupergirlPower.THUNDER_CLAP);
        helper.assertFalse(behind.hasEffect(MobEffects.SLOWNESS), "the clap is a cone in front of her");
        done(player);
        done(back);
        helper.succeed();
    }

    public static void frostWall(GameTestHelper helper) {
        helper.setBlock(new BlockPos(7, 1, 5), Blocks.FIRE);
        ServerPlayer player = supergirl(helper, 7.5, 2.5, 0);
        int walls = SupergirlServer.wallBlocks();
        helper.assertTrue(use(player, SupergirlPower.FROST_WALL), "the breath should go off");
        helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(7, 1, 5));
        int width = SupergirlConfig.FROST_WALL_WIDTH.get();
        int height = SupergirlConfig.FROST_WALL_HEIGHT.get();
        helper.assertTrue(SupergirlServer.wallBlocks() - walls == width * height, "a whole wall should rise, " + (SupergirlServer.wallBlocks() - walls) + " blocks");
        for (int h = 1; h <= height; h++) helper.assertBlockPresent(Blocks.PACKED_ICE, new BlockPos(7, h, 6));
        int melt = (int) Math.ceil(SupergirlConfig.FROST_WALL_SECONDS.get() * 20.0);
        helper.startSequence()
                .thenIdle(melt + 5)
                .thenExecute(() -> {
                    for (int h = 1; h <= height; h++) helper.assertBlockNotPresent(Blocks.PACKED_ICE, new BlockPos(7, h, 6));
                    done(player);
                })
                .thenSucceed();
    }

    public static void superHearing(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 7.5, 0);
        int cost = SupergirlPower.SUPER_HEARING.cost();
        helper.assertTrue(use(player, SupergirlPower.SUPER_HEARING), "the hearing should switch on");
        helper.assertTrue(SupergirlServer.isHearing(player.getMainHandItem()), "the pendant should say the hearing is on");
        float rate = SupergirlServer.rechargeRate(player);
        int[] atStart = new int[1];
        helper.startSequence()
                .thenExecute(() -> atStart[0] = energy(player))
                .thenExecuteFor(40, player::doTick)
                .thenExecute(() -> {
                    // About two seconds of listening, minus what the sun gave back meanwhile.
                    int spent = atStart[0] - energy(player) + (int) Math.ceil(2 * rate);
                    helper.assertTrue(spent >= 2 * cost - 2, "the hearing should keep costing per second, spent " + spent);
                    int before = energy(player);
                    helper.assertTrue(use(player, SupergirlPower.SUPER_HEARING), "using it again switches it off");
                    helper.assertFalse(SupergirlServer.isHearing(player.getMainHandItem()), "the hearing should be off");
                    helper.assertTrue(energy(player) == before, "switching off is free");
                    done(player);
                })
                .thenSucceed();
    }

    public static void kryptonianThrow(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 3.5, 0);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(7.5, 1, 6.5));
        lookAt(player, pig);
        helper.assertTrue(use(player, SupergirlPower.KRYPTONIAN_THROW), "she should grab the pig");
        helper.assertTrue(SupergirlServer.held(player) == pig, "the pig should be in her hands");
        float health = pig.getHealth();
        helper.startSequence()
                .thenExecuteFor(5, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(pig.getY() > player.getY() + player.getBbHeight(), "she carries it over her head");
                    int before = energy(player);
                    player.setYRot(0);
                    player.setXRot(35);
                    helper.assertTrue(use(player, SupergirlPower.KRYPTONIAN_THROW), "the throw should go off");
                    helper.assertTrue(SupergirlServer.held(player) == null, "her hands should be free");
                    helper.assertTrue(energy(player) == before, "the throw itself is free");
                    helper.assertTrue(pig.getDeltaMovement().length() > 1.0, "the pig should fly off");
                })
                .thenExecuteFor(30, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(!pig.isAlive() || pig.getHealth() < health, "the pig should get hurt where it lands");
                    done(player);
                })
                .thenSucceed();
    }

    public static void throwBlock(GameTestHelper helper) {
        BlockPos dirt = new BlockPos(7, 1, 6);
        helper.setBlock(dirt, Blocks.DIRT);
        helper.setBlock(new BlockPos(9, 1, 6), Blocks.OBSIDIAN);
        ServerPlayer player = supergirl(helper, 7.5, 3.5, 0);
        Vec3 to = helper.absoluteVec(Vec3.atCenterOf(dirt)).subtract(player.getEyePosition());
        player.setYRot(0);
        player.setXRot((float) (-Math.atan2(to.y, to.horizontalDistance()) * 180.0 / Math.PI));
        helper.assertTrue(use(player, SupergirlPower.KRYPTONIAN_THROW), "she should tear the dirt loose");
        helper.assertBlockNotPresent(Blocks.DIRT, dirt);
        Entity held = SupergirlServer.held(player);
        helper.assertTrue(held instanceof FallingBlockEntity block && block.getBlockState().is(Blocks.DIRT), "she should hold the dirt");
        use(player, SupergirlPower.KRYPTONIAN_THROW);
        // Obsidian is too hard to tear loose: nothing to grab, energy back.
        Vec3 to2 = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(9, 1, 6))).subtract(player.getEyePosition());
        player.setYRot((float) (Math.atan2(-to2.x, to2.z) * 180.0 / Math.PI));
        player.setXRot((float) (-Math.atan2(to2.y, to2.horizontalDistance()) * 180.0 / Math.PI));
        int before = energy(player);
        use(player, SupergirlPower.KRYPTONIAN_THROW);
        helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(9, 1, 6));
        helper.assertTrue(energy(player) == before, "nothing grabbed, nothing spent");
        helper.startSequence()
                .thenExecuteFor(30, player::doTick)
                .thenExecute(() -> {
                    helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, player.getBoundingBox().inflate(30)).forEach(Entity::discard);
                    done(player);
                })
                .thenSucceed();
    }

    public static void solarFlare(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 7.5, 0);
        daylight(helper, player);
        Zombie zombie = dummy(helper, 7.5, 1, 10.5);
        float before = zombie.getHealth();
        helper.assertTrue(use(player, SupergirlPower.SOLAR_FLARE), "the flare should go off");
        helper.assertTrue(energy(player) == 0, "the flare pours out all her energy");
        helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < before, "the flare should hurt the zombie");
        helper.assertTrue(SupergirlServer.isWeak(player), "she should be worn out");
        helper.assertFalse(use(player, SupergirlPower.HEAT_BOLTS), "no powers while worn out");
        SupergirlHero.ENERGY.set(player.getMainHandItem(), 100);
        helper.assertFalse(SupergirlHero.INSTANCE.canFly(player, player.getMainHandItem()), "no flight while worn out");
        SupergirlHero.ENERGY.set(player.getMainHandItem(), 0);
        int weak = (int) Math.ceil(SupergirlConfig.FLARE_WEAK_SECONDS.get() * 20.0);
        helper.startSequence()
                .thenExecuteFor(weak / 2, player::doTick)
                .thenExecute(() -> helper.assertTrue(energy(player) == 0, "no recharge while worn out, got " + energy(player)))
                .thenExecuteFor(weak / 2 + 25, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(SupergirlServer.isWeak(player), "she should recover");
                    helper.assertTrue(energy(player) > 0, "the sun should charge her again");
                    done(player);
                })
                .thenSucceed();
    }

    /** Ticks until Krypto has landed next to her after the suit goes on. */
    private static final int ARRIVAL = 110;

    public static void kryptoArrives(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 7.5, 0);
        KryptoEntity[] his = new KryptoEntity[1];
        helper.startSequence()
                .thenExecuteFor(11, player::doTick)
                .thenExecute(() -> {
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    helper.assertTrue(krypto != null, "Krypto should come when she puts on the suit");
                    helper.assertTrue(krypto.isOwnedBy(player), "Krypto is hers");
                    KryptoData data = player.getMainHandItem().get(SupergirlContent.KRYPTO_DATA.get());
                    helper.assertTrue(data != null && data.met(), "the pendant should remember meeting him");
                })
                .thenExecuteFor(ARRIVAL, player::doTick)
                .thenExecute(() -> {
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    helper.assertTrue(krypto != null && krypto.distanceTo(player) < 4.0, "he should land beside her");
                    helper.assertTrue(krypto.getMaxHealth() == SupergirlConfig.KRYPTO_HEALTH.get().floatValue(), "Krypto's health comes from the config");
                    his[0] = krypto;
                    // The suit comes off: he flies away, the pendant keeps him.
                    SupergirlHero.INSTANCE.dismissSuit(player, false);
                    player.doTick();
                })
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(his[0].isRemoved(), "without the suit Krypto flies off");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void kryptoFights(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 4.5, 4.5, 0);
        Husk husk = helper.spawn(EntityType.HUSK, new Vec3(4.5, 1, 6.0));
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        float[] after = new float[1];
        helper.startSequence()
                .thenExecuteFor(ARRIVAL, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(SupergirlServer.krypto(player) != null, "Krypto should be with her");
                    lookAt(player, husk);
                    player.attack(husk);
                    after[0] = husk.getHealth();
                })
                .thenExecuteFor(100, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(!husk.isAlive() || husk.getHealth() < after[0], "Krypto should bite whoever she punches, husk at " + husk.getHealth());
                    // Never another player.
                    ServerPlayer friend = player(helper, 10.5, 1, 10.5, 0, 0);
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    helper.assertTrue(krypto != null && !krypto.canAttack(friend), "Krypto never attacks players");
                    remove(friend);
                    husk.discard();
                    done(player);
                })
                .thenSucceed();
    }

    public static void kryptoRests(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 7.5, 7.5, 0);
        helper.startSequence()
                .thenExecuteFor(ARRIVAL, player::doTick)
                .thenExecute(() -> {
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    helper.assertTrue(krypto != null, "Krypto should be with her");
                    krypto.setCustomName(net.minecraft.network.chat.Component.literal("Rex"));
                    krypto.hurtServer(helper.getLevel(), helper.getLevel().damageSources().magic(), 1000.0F);
                    helper.assertTrue(krypto.isRemoved(), "with no health left he flies off instead of dying");
                    helper.assertTrue(SupergirlServer.krypto(player) == null, "he is gone for now");
                    KryptoData data = player.getMainHandItem().get(SupergirlContent.KRYPTO_DATA.get());
                    helper.assertTrue(data != null && data.backAt() > helper.getLevel().getGameTime(), "the pendant should know when he comes back");
                    helper.assertTrue("Rex".equals(data.name()), "the pendant keeps his name");
                })
                .thenExecuteFor(30, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(SupergirlServer.krypto(player) == null, "he rests before coming back");
                    player.setShiftKeyDown(true);
                    helper.assertTrue(use(player, SupergirlPower.HEAT_BOLTS), "the whistle answers");
                    player.setShiftKeyDown(false);
                    helper.assertTrue(SupergirlServer.krypto(player) == null, "a whistle doesn't wake him before he has rested");
                    // Rest over.
                    KryptoData data = player.getMainHandItem().get(SupergirlContent.KRYPTO_DATA.get());
                    player.getMainHandItem().set(SupergirlContent.KRYPTO_DATA.get(),
                            new KryptoData(true, data.name(), 0.0F, helper.getLevel().getGameTime() + 1));
                })
                .thenExecuteFor(25, player::doTick)
                .thenExecute(() -> {
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    helper.assertTrue(krypto != null, "after resting he comes back");
                    helper.assertTrue(krypto.hasCustomName() && "Rex".equals(krypto.getCustomName().getString()), "with his name");
                    done(player);
                })
                .thenSucceed();
    }

    public static void kryptoWhistle(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 2.5, 2.5, 0);
        helper.startSequence()
                .thenExecuteFor(ARRIVAL, player::doTick)
                .thenExecute(() -> {
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    helper.assertTrue(krypto != null, "Krypto should be with her");
                    krypto.setOrderedToSit(true);
                    Vec3 far = helper.absoluteVec(new Vec3(12.5, 1, 12.5));
                    player.teleportTo(far.x, far.y, far.z);
                    int before = energy(player);
                    player.setShiftKeyDown(true);
                    helper.assertTrue(use(player, SupergirlPower.THUNDER_CLAP), "sneaking she whistles instead");
                    player.setShiftKeyDown(false);
                    helper.assertTrue(energy(player) == before, "the whistle is free");
                    helper.assertFalse(krypto.isOrderedToSit(), "the whistle gets him up");
                    helper.assertTrue(krypto.distanceTo(player) < 4.0, "he comes to her at once, " + krypto.distanceTo(player));
                    done(player);
                })
                .thenSucceed();
    }

    public static void kryptoFetches(GameTestHelper helper) {
        ServerPlayer player = supergirl(helper, 3.5, 3.5, 0);
        ItemEntity[] loot = new ItemEntity[1];
        helper.startSequence()
                .thenExecuteFor(ARRIVAL, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(SupergirlServer.krypto(player) != null, "Krypto should be with her");
                    Vec3 at = helper.absoluteVec(new Vec3(8.5, 1, 8.5));
                    loot[0] = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.EMERALD, 3));
                    loot[0].setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(loot[0]);
                })
                .thenExecuteFor(200, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(loot[0].isRemoved(), "Krypto should pick the loot up");
                    var near = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2.0),
                            e -> e.getItem().is(Items.EMERALD));
                    KryptoEntity krypto = SupergirlServer.krypto(player);
                    boolean carrying = krypto != null && krypto.carried().is(Items.EMERALD);
                    boolean delivered = !near.isEmpty() || player.getInventory().countItem(Items.EMERALD) > 0;
                    helper.assertTrue(delivered || carrying, "he should bring it to her");
                    near.forEach(Entity::discard);
                    done(player);
                })
                .thenSucceed();
    }
}
