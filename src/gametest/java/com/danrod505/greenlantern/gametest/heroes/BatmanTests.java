package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.batman.BatPower;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.batman.BatmanServer;
import com.danrod505.greenlantern.entity.BatDefenderEntity;
import com.danrod505.greenlantern.entity.BatarangEntity;
import com.danrod505.greenlantern.entity.BatmobileEntity;
import com.danrod505.greenlantern.entity.BatmobileMissileEntity;
import com.danrod505.greenlantern.entity.GrappleHookEntity;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.DeferredRegister;

/** Batman: suit, belt charge, batarang, grapple, bat swarm and the Batmobile. */
public final class BatmanTests {
    private BatmanTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("batman_suit_summon_and_swap", () -> BatmanTests::batmanSuitSummonAndSwap);
        tests.register("batman_belt_recharges", () -> BatmanTests::batmanBeltRecharges);
        tests.register("batman_batarang_returns", () -> BatmanTests::batmanBatarangReturns);
        tests.register("batman_grapple_hooks_and_releases", () -> BatmanTests::batmanGrappleHooksAndReleases);
        tests.register("batman_bat_swarm", () -> BatmanTests::batmanBatSwarm);
        tests.register("batman_batmobile", () -> BatmanTests::batmanBatmobile);
    }

    public static void batmanSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        // Aquaman first, then Batman replaces him (one hero at a time).
        player.getInventory().add(new ItemStack(ModItems.AQUAMAN_EMBLEM.get()));
        AquamanHero.INSTANCE.summonSuit(player);
        helper.assertTrue(AquamanHelper.isSuited(player), "aquaman suit should be on");
        giveBelt(player, 0);
        use(player); // not suited as Batman: right click summons the suit
        helper.assertTrue(BatmanHelper.isSuited(player), "batman suit should be summoned");
        helper.assertFalse(AquamanHelper.isSuited(player), "the aquaman suit should be gone");
        helper.assertTrue(BatmanHelper.hasCowl(player), "the cowl replaces the helmet");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BATMAN_LEGGINGS.get()), "leggings should be worn");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BATMAN_BOOTS.get()), "boots should be worn");
        BatmanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(BatmanHelper.isSuited(player), "batman suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "iron chestplate should be restored");
        remove(player);
        helper.succeed();
    }

    public static void batmanBeltRecharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack belt = giveBelt(player, 0);
        BatmanHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = BatmanHero.BAT_CHARGE.get(belt).stored();
                    helper.assertTrue(stored >= 10, "the belt should recharge on its own, got " + stored);
                    helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "the cowl gives night vision");
                    BatPower.POWERS.select(belt, BatPower.BAT_SWARM);
                    BatmanHero.BAT_CHARGE.set(belt, 0);
                    helper.assertFalse(BatmanServer.usePower(player, belt, BatPower.BAT_SWARM), "no charge, no gadget");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void batmanBatarangReturns(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.BATARANG);
        Zombie zombie = dummy(helper, 7.5, 1, 7.5);
        float health = zombie.getHealth();
        player.setXRot(8.0F);
        use(player);
        helper.assertTrue(BatarangEntity.findAll(player).size() == 1, "a batarang should be in flight");
        helper.assertTrue(BatmanHero.BAT_CHARGE.get(belt).stored() == 1000 - BatPower.BATARANG.cost(), "the batarang should cost charge");
        helper.startSequence()
                // Waits for the batarang's own hit (it slows the target): the zombie may also burn in the sun.
                .thenWaitUntil(() -> helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) || zombie.isDeadOrDying(),
                        "the batarang should hit and slow the zombie"))
                .thenExecute(() -> helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the batarang should hurt the zombie"))
                .thenWaitUntil(() -> helper.assertTrue(BatarangEntity.findAll(player).isEmpty(), "the batarang should come back"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void batmanGrappleHooksAndReleases(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        for (int x = 5; x <= 10; x++) {
            for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(x, y, 11), Blocks.STONE);
        }
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.GRAPPLE);
        use(player);
        helper.assertTrue(GrappleHookEntity.find(player) != null, "the hook should be fired");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    GrappleHookEntity hook = GrappleHookEntity.find(player);
                    helper.assertTrue(hook != null && hook.isAttached(), "the hook should bite into the wall");
                })
                .thenExecute(() -> {
                    GrappleHookEntity hook = GrappleHookEntity.find(player);
                    BatmanServer.usePower(player, belt, BatPower.GRAPPLE);
                    helper.assertTrue(hook.isRemoved(), "using the gadget again lets go of the cable");
                    helper.assertTrue(BatmanHero.BAT_CHARGE.get(belt).stored() == 1000 - BatPower.GRAPPLE.cost(), "letting go is free");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void batmanBatSwarm(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.BAT_SWARM);
        Zombie zombie = dummy(helper, 7.5, 1, 11.5);
        float health = zombie.getHealth();
        use(player);
        helper.assertTrue(BatDefenderEntity.findAll(player).size() == GLConfig.BAT_SWARM_COUNT.get(), "the whole swarm should come");
        helper.assertTrue(BatmanServer.isSwarmActive(player), "the swarm is active");
        helper.startSequence()
                // A bat's bite both hurts and blinds (a zombie in the sun can lose health on its own, so wait for the blindness).
                .thenWaitUntil(() -> helper.assertTrue(zombie.isDeadOrDying() || zombie.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                        "the bats should attack and blind the zombie"))
                .thenExecute(() -> {
                    helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the bats' attack should hurt");
                    BatmanServer.usePower(player, belt, BatPower.BAT_SWARM);
                    helper.assertFalse(BatmanServer.isSwarmActive(player), "using the gadget again scatters the bats");
                })
                .thenWaitUntil(() -> helper.assertTrue(around(player, BatDefenderEntity.class, 64).isEmpty(), "the scattered bats fly away"))
                .thenExecute(() -> remove(player))
                .thenSucceed();
    }

    public static void batmanBatmobile(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 3.5, 0, 0);
        ItemStack belt = giveBelt(player, 1000);
        BatmanHero.INSTANCE.summonSuit(player);
        BatPower.POWERS.select(belt, BatPower.BATMOBILE);
        Zombie zombie = dummy(helper, 7.5, 1, 12.5);
        float health = zombie.getHealth();
        use(player);
        BatmobileEntity car = BatmobileEntity.find(player);
        helper.assertTrue(car != null, "the Batmobile should arrive");
        helper.assertTrue(player.getVehicle() == car, "Batman should be in the Batmobile");
        helper.assertTrue(BatmanHero.BAT_CHARGE.get(belt).stored() == 1000 - BatPower.BATMOBILE.cost(), "the Batmobile should cost charge");
        helper.startSequence()
                .thenExecuteFor(2, player::doTick)
                .thenExecute(() -> helper.assertTrue(car.fireMissiles(), "the missiles should fire"))
                .thenWaitUntil(() -> helper.assertTrue(!around(player, BatmobileMissileEntity.class, 32).isEmpty()
                        || zombie.getHealth() < health || zombie.isDeadOrDying(), "missiles should launch"))
                .thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < health || zombie.isDeadOrDying(), "the missiles should hit the zombie"))
                .thenExecute(() -> {
                    helper.assertTrue(player.isAlive() && player.getVehicle() == car, "the driver is safe from his own missiles");
                    BatmanServer.usePower(player, BatmanHelper.findBelt(player), BatPower.BATMOBILE);
                    helper.assertTrue(car.isRemoved(), "using the gadget again sends the car away");
                    helper.assertFalse(player.isPassenger(), "Batman gets out");
                    remove(player);
                })
                .thenSucceed();
    }
}
