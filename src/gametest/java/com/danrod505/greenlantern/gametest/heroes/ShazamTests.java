package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.gametest.GameTestKit;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.shazam.ShazamConfig;
import com.danrod505.greenlantern.shazam.ShazamHero;
import com.danrod505.greenlantern.shazam.ShazamPower;
import java.util.function.Consumer;
import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.shazam.ShazamServer;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Shazam. The hero contract tests already check the suit, the texts and that every power runs;
 * these check what is particular to the hero. Give each finished power its own test here (and a
 * {@code test_instance/<name>.json} next to the others).
 */
public final class ShazamTests {
    private ShazamTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("shazam_energy_recharges", () -> ShazamTests::energyRecharges);
        tests.register("shazam_powers_spend_energy", () -> ShazamTests::powersSpendEnergy);
        tests.register("shazam_transform", () -> ShazamTests::transform);
        tests.register("shazam_lightning_fills_magic", () -> ShazamTests::lightningFillsMagic);
        tests.register("shazam_flight", () -> ShazamTests::flight);
        tests.register("shazam_zeus_lightning", () -> ShazamTests::zeusLightning);
        tests.register("shazam_thunder_call", () -> ShazamTests::thunderCall);
        tests.register("shazam_mercury_dash", () -> ShazamTests::mercuryDash);
        tests.register("shazam_hercules_slam", () -> ShazamTests::herculesSlam);
        tests.register("shazam_achilles_roar", () -> ShazamTests::achillesRoar);
        tests.register("shazam_atlas_stamina", () -> ShazamTests::atlasStamina);
        tests.register("shazam_solomon_wisdom", () -> ShazamTests::solomonWisdom);
    }

    /** A suited Shazam with a full Wizard's Bolt. */
    private static ServerPlayer shazam(GameTestHelper helper, double x, double z, float yaw) {
        ServerPlayer player = player(helper, x, 1, z, yaw, 0);
        giveItem(player, ShazamHero.ENERGY.capacity());
        helper.assertTrue(ShazamHero.INSTANCE.summonSuit(player), "the suit should go on");
        return player;
    }

    private static boolean use(ServerPlayer player, ShazamPower power) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(player.getMainHandItem()));
        return ShazamHero.INSTANCE.legacyPowers().use(player, player.getMainHandItem(), power.ordinal());
    }

    private static int energy(ServerPlayer player) {
        return ShazamHero.ENERGY.get(player.getMainHandItem()).stored();
    }

    private static void done(ServerPlayer player) {
        if (ShazamHero.INSTANCE.isSuited(player)) ShazamHero.INSTANCE.dismissSuit(player, false);
        remove(player);
    }

    private static boolean hurt(LivingEntity entity, float before) {
        return !entity.isAlive() || entity.getHealth() < before;
    }

    public static void transform(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        giveItem(player, 100);
        Zombie close = dummy(helper, 8.5, 1, 8.5);
        float before = close.getHealth();
        float health = player.getHealth();
        // "Shazam!": the lightning falls on him, the suit forms and the magic comes back.
        GameTestKit.use(player);
        helper.assertTrue(ShazamHero.INSTANCE.isSuited(player), "the Wizard's Bolt should call the suit");
        helper.assertTrue(around(player, LightningBolt.class, 3).stream().anyMatch(b -> b.getDamage() >= 0), "a lightning bolt should fall on him");
        int expected = 100 + ShazamConfig.TRANSFORM_RECHARGE.get();
        helper.assertTrue(energy(player) >= Math.min(expected, ShazamHero.ENERGY.capacity()), "the lightning should bring magic back, got " + energy(player));
        helper.assertTrue(hurt(close, before), "the lightning should hurt the enemy right next to him");
        helper.assertTrue(player.getHealth() >= health && !player.isOnFire(), "the lightning never hurts him");
        // Sneaking with the suit on, the lightning comes again and he is Billy.
        player.setShiftKeyDown(true);
        GameTestKit.use(player);
        helper.assertFalse(ShazamHero.INSTANCE.isSuited(player), "sneaking, the Wizard's Bolt should take the suit off");
        player.setShiftKeyDown(false);
        // Right away again: the suit forms, but no magic comes back until the wait is over.
        int now = energy(player);
        GameTestKit.use(player);
        helper.assertTrue(ShazamHero.INSTANCE.isSuited(player), "the suit should form again");
        helper.assertTrue(energy(player) == now, "a second \"Shazam!\" right away brings no magic back, " + now + " -> " + energy(player));
        close.discard();
        done(player);
        helper.succeed();
    }

    public static void lightningFillsMagic(GameTestHelper helper) {
        ServerPlayer player = shazam(helper, 7.5, 7.5, 0);
        ShazamHero.ENERGY.set(player.getMainHandItem(), 50);
        DamageSource lightning = helper.getLevel().damageSources().lightningBolt();
        helper.assertTrue(ShazamHero.INSTANCE.damageTakenMultiplier(player, lightning) == 0.0F, "lightning should not hurt him");
        helper.assertTrue(energy(player) == ShazamHero.ENERGY.capacity(), "lightning should fill his magic, got " + energy(player));
        helper.assertTrue(ShazamHero.INSTANCE.damageTakenMultiplier(player, helper.getLevel().damageSources().generic()) == 1.0F, "other blows hurt as usual");
        done(player);
        helper.succeed();
    }

    public static void flight(GameTestHelper helper) {
        FlightProfile shazam = ShazamHero.INSTANCE.flightProfile();
        FlightProfile supergirl = SupergirlHero.INSTANCE.flightProfile();
        helper.assertTrue(shazam.max() > GLConfig.MAX_FLIGHT_SPEED.get() && shazam.max() < supergirl.max(),
                "his top speed should be between the Lantern's and Supergirl's, " + shazam.max());
        helper.assertTrue(shazam.max() > shazam.barrier(), "he breaks the sound barrier");
        ServerPlayer player = shazam(helper, 7.5, 7.5, 0);
        helper.assertTrue(player.getAbilities().mayfly, "the suit lets him fly");
        ShazamHero.ENERGY.set(player.getMainHandItem(), 0);
        helper.assertFalse(ShazamHero.INSTANCE.canFly(player, player.getMainHandItem()), "with no magic he can't fly");
        done(player);
        helper.succeed();
    }

    public static void zeusLightning(GameTestHelper helper) {
        ServerPlayer player = shazam(helper, 7.5, 1.5, 0);
        Zombie first = dummy(helper, 7.5, 1, 9.5);
        Zombie second = dummy(helper, 10.5, 1, 11.5);
        Zombie third = dummy(helper, 4.5, 1, 12.5);
        float before = first.getHealth();
        lookAt(player, first);
        helper.assertTrue(use(player, ShazamPower.ZEUS_LIGHTNING), "the bolt should fire");
        helper.assertTrue(ShazamServer.boltsInFlight(player), "the bolt flies for a moment");
        helper.startSequence()
                .thenExecuteFor(8, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(ShazamServer.boltsInFlight(player), "the bolt should have landed");
                    helper.assertTrue(hurt(first, before), "the bolt should hurt the first zombie");
                    helper.assertTrue(hurt(second, before), "the bolt should jump to the second zombie");
                    helper.assertTrue(hurt(third, before), "the bolt should jump to the third zombie");
                    first.discard();
                    second.discard();
                    third.discard();
                    done(player);
                })
                .thenSucceed();
    }

    public static void thunderCall(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 1, 12), Blocks.GLASS);
        ServerPlayer player = shazam(helper, 7.5, 1.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 12.5);
        float before = zombie.getHealth();
        lookAt(player, zombie);
        float health = player.getHealth();
        helper.assertTrue(use(player, ShazamPower.THUNDER_CALL), "the lightning should be called");
        helper.assertTrue(hurt(zombie, before), "the lightning should hurt the zombie");
        helper.assertBlockNotPresent(Blocks.GLASS, new BlockPos(8, 1, 12));
        helper.assertTrue(around(player, LightningBolt.class, 14).size() >= 2, "a lightning bolt should fall where he aims");
        helper.assertTrue(player.getHealth() >= health, "his own lightning never hurts him");
        zombie.discard();
        done(player);
        helper.succeed();
    }

    public static void mercuryDash(GameTestHelper helper) {
        ServerPlayer player = shazam(helper, 7.5, 0.5, 0);
        Zombie zombie = dummy(helper, 7.5, 1, 6.5);
        float before = zombie.getHealth();
        double startZ = player.getZ();
        helper.assertTrue(use(player, ShazamPower.MERCURY_DASH), "the dash should start");
        helper.assertTrue(ShazamServer.isDashing(player), "he should be dashing");
        helper.startSequence()
                .thenExecuteFor(10, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(ShazamServer.isDashing(player), "the dash should be over");
                    helper.assertTrue(player.getZ() - startZ >= 10, "he should dash far ahead, moved " + (player.getZ() - startZ));
                    helper.assertTrue(hurt(zombie, before), "the dash should ram the zombie");
                    zombie.discard();
                    done(player);
                })
                .thenSucceed();
    }

    public static void herculesSlam(GameTestHelper helper) {
        ServerPlayer player = shazam(helper, 7.5, 7.5, 0);
        Zombie near = dummy(helper, 7.5, 1, 10.5);
        Zombie far = dummy(helper, 7.5, 1, 15.0);
        float before = near.getHealth();
        helper.assertTrue(use(player, ShazamPower.HERCULES_SLAM), "the slam should go off");
        helper.assertTrue(hurt(near, before), "the shockwave should hurt the near zombie");
        helper.assertTrue(near.getDeltaMovement().z > 0.3, "the shockwave should throw the zombie away, " + near.getDeltaMovement());
        helper.assertTrue(far.getHealth() >= before, "the shockwave has a reach");
        near.discard();
        far.discard();
        done(player);
        helper.succeed();
    }

    public static void achillesRoar(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 1, 6), Blocks.TORCH);
        ServerPlayer player = shazam(helper, 7.5, 1.5, 0);
        Zombie ahead = dummy(helper, 7.5, 1, 7.5);
        Zombie behind = dummy(helper, 7.5, 1, 0.5);
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
        helper.assertTrue(use(player, ShazamPower.ACHILLES_ROAR), "the battle cry should go off");
        helper.assertTrue(ahead.hasEffect(MobEffects.SLOWNESS), "the zombie ahead should be stunned");
        helper.assertFalse(behind.hasEffect(MobEffects.SLOWNESS), "the cry is a cone in front of him");
        helper.assertBlockNotPresent(Blocks.TORCH, new BlockPos(8, 1, 6));
        ahead.discard();
        behind.discard();
        done(player);
        helper.succeed();
    }

    public static void atlasStamina(GameTestHelper helper) {
        ServerPlayer player = shazam(helper, 7.5, 7.5, 0);
        DamageSource blow = helper.getLevel().damageSources().generic();
        helper.assertTrue(ShazamHero.INSTANCE.damageTakenMultiplier(player, blow) == 1.0F, "without Atlas he takes full damage");
        helper.assertTrue(use(player, ShazamPower.ATLAS_STAMINA), "Atlas should switch on");
        helper.assertTrue(ShazamServer.isAtlas(player.getMainHandItem()), "Atlas should be on");
        helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "nothing should push him");
        float share = ShazamHero.INSTANCE.damageTakenMultiplier(player, blow);
        helper.assertTrue(Math.abs(share - ShazamConfig.ATLAS_DAMAGE_TAKEN.get().floatValue()) < 1.0E-4F, "Atlas should halve the damage, share " + share);
        int before = energy(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(energy(player) < before + 2 * ShazamConfig.RECHARGE_PER_SECOND.get(), "Atlas should cost magic every second");
                    helper.assertTrue(use(player, ShazamPower.ATLAS_STAMINA), "Atlas should switch off");
                    helper.assertFalse(ShazamServer.isAtlas(player.getMainHandItem()), "Atlas should be off");
                    helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1.0, "the weight goes away");
                    done(player);
                })
                .thenSucceed();
    }

    public static void solomonWisdom(GameTestHelper helper) {
        ServerPlayer player = shazam(helper, 7.5, 1.5, 0);
        // Behind a wall, far away: it still shows.
        for (int x = 5; x <= 10; x++) {
            for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
        }
        Zombie hidden = dummy(helper, 7.5, 1, 13.5);
        helper.assertTrue(use(player, ShazamPower.SOLOMON_WISDOM), "Solomon should switch on");
        helper.assertTrue(ShazamServer.isSolomon(player.getMainHandItem()), "Solomon should be on");
        helper.assertTrue(hidden.hasEffect(MobEffects.GLOWING), "the zombie behind the wall should glow");
        helper.assertTrue(use(player, ShazamPower.SOLOMON_WISDOM), "Solomon should switch off");
        helper.assertFalse(ShazamServer.isSolomon(player.getMainHandItem()), "Solomon should be off");
        hidden.discard();
        done(player);
        helper.succeed();
    }

    private static ItemStack giveItem(ServerPlayer player, int energy) {
        ItemStack item = HeroContractTests.chargedItem(ShazamHero.INSTANCE);
        ShazamHero.ENERGY.set(item, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        return player.getMainHandItem();
    }

    public static void energyRecharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack item = giveItem(player, 0);
        ShazamHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = ShazamHero.ENERGY.get(item).stored();
                    int expected = 2 * ShazamConfig.RECHARGE_PER_SECOND.get();
                    helper.assertTrue(stored >= expected, "the energy should come back every second, got " + stored + " (expected " + expected + ")");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void powersSpendEnergy(GameTestHelper helper) {
        for (ShazamPower power : ShazamPower.values()) {
            ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
            ItemStack item = giveItem(player, ShazamHero.ENERGY.capacity());
            ShazamHero.INSTANCE.summonSuit(player);
            helper.assertTrue(ShazamHero.INSTANCE.legacyPowers().use(player, player.getMainHandItem(), power.ordinal()), power.id() + " should go off");
            int stored = ShazamHero.ENERGY.get(player.getMainHandItem()).stored();
            helper.assertTrue(stored == ShazamHero.ENERGY.capacity() - power.cost(), power.id() + " should cost " + power.cost() + ", left " + stored);
            remove(player);
        }
        helper.succeed();
    }
}
