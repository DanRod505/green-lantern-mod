package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.superman.SuperPower;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.superman.SupermanServer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;

/** Superman: suit, solar energy, heat vision, super punch, super breath and X-ray vision. */
public final class SupermanTests {
    private SupermanTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("superman_suit_summon_and_swap", () -> SupermanTests::supermanSuitSummonAndSwap);
        tests.register("superman_solar_recharge", () -> SupermanTests::supermanSolarRecharge);
        tests.register("superman_heat_vision", () -> SupermanTests::supermanHeatVision);
        tests.register("superman_super_punch_area", () -> SupermanTests::supermanSuperPunchArea);
        tests.register("superman_super_breath", () -> SupermanTests::supermanSuperBreath);
        tests.register("superman_xray_toggle", () -> SupermanTests::supermanXrayToggle);
    }

    public static void supermanSuitSummonAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        // Batman first, then Superman replaces him (one hero at a time).
        player.getInventory().add(new ItemStack(ModItems.UTILITY_BELT.get()));
        BatmanHero.INSTANCE.summonSuit(player);
        helper.assertTrue(BatmanHelper.isSuited(player), "batman suit should be on");
        giveCrystal(player, 1000);
        use(player); // not suited as Superman: right click summons the suit
        helper.assertTrue(SupermanHelper.isSuited(player), "superman suit should be summoned");
        helper.assertFalse(BatmanHelper.isSuited(player), "the batman suit should be gone");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SUPERMAN_HAIR.get()), "the hair replaces the helmet");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SUPERMAN_LEGGINGS.get()), "leggings should be worn");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SUPERMAN_BOOTS.get()), "boots should be worn");
        helper.assertTrue(player.getAbilities().mayfly, "Superman can fly");
        SupermanHero.INSTANCE.updateSuitModifiers(player, true);
        helper.assertTrue(player.getMaxHealth() > 20.0F, "the suit should give extra health");
        SupermanHero.INSTANCE.dismissSuit(player, false);
        helper.assertFalse(SupermanHelper.isSuited(player), "superman suit should be dismissed");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "iron helmet should be restored");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "iron chestplate should be restored");
        helper.assertFalse(player.getAbilities().mayfly, "survival player should not fly without the suit");
        remove(player);
        helper.succeed();
    }

    public static void supermanSolarRecharge(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 0);
        SupermanHero.INSTANCE.summonSuit(player);
        // The cells only charge in direct sunlight: make sure it is daytime.
        if (SupermanServer.sunlight(player) <= 0.0F) helper.getLevel().setDayTime(6000);
        helper.assertTrue(SupermanServer.sunlight(player) > 0.0F, "the arena should be under the open sky in daylight");
        helper.assertFalse(SupermanServer.usePower(player, crystal, SuperPower.SUPER_PUNCH), "no energy, no power");
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = SupermanHero.SOLAR_ENERGY.get(crystal).stored();
                    helper.assertTrue(stored >= 10, "the sun should recharge the crystal, got " + stored);
                    remove(player);
                })
                .thenSucceed();
    }

    public static void supermanHeatVision(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.HEAT_VISION);
        net.minecraft.world.entity.animal.pig.Pig pig = dummyPig(helper, 7.5, 1, 7.5);
        float health = pig.getHealth();
        player.setXRot(8.0F);
        use(player);
        helper.assertTrue(SupermanServer.isUsingHeatVision(player), "the beams should be on");
        helper.startSequence()
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the beams should burn the pig");
                    helper.assertTrue(SupermanHero.SOLAR_ENERGY.get(crystal).stored() < 3000, "heat vision should cost solar energy");
                    SupermanServer.usePower(player, crystal, SuperPower.HEAT_VISION);
                    helper.assertFalse(SupermanServer.isUsingHeatVision(player), "using it again stops the beams");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void supermanSuperPunchArea(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 4.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.SUPER_PUNCH);
        // One pig right in front and two more off to the sides: the blast hits them all.
        net.minecraft.world.entity.animal.pig.Pig front = dummyPig(helper, 7.5, 1, 6.5);
        net.minecraft.world.entity.animal.pig.Pig left = dummyPig(helper, 10.5, 1, 7.5);
        net.minecraft.world.entity.animal.pig.Pig right = dummyPig(helper, 4.5, 1, 7.5);
        float health = front.getHealth();
        use(player);
        helper.assertTrue(SupermanHero.SOLAR_ENERGY.get(crystal).stored() == 3000 - SuperPower.SUPER_PUNCH.cost(), "the punch should cost solar energy");
        for (net.minecraft.world.entity.animal.pig.Pig pig : List.of(front, left, right)) {
            helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the punch should hit every pig around");
        }
        remove(player);
        helper.succeed();
    }

    public static void supermanSuperBreath(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.SUPER_BREATH);
        net.minecraft.world.entity.animal.pig.Pig pig = dummyPig(helper, 7.5, 1, 6.5);
        float health = pig.getHealth();
        use(player);
        helper.assertTrue(SupermanServer.isBreathing(player), "the breath should blow");
        helper.startSequence()
                .thenExecuteFor(12, player::doTick)
                .thenExecute(() -> {
                    helper.assertTrue(pig.getHealth() < health || pig.isDeadOrDying(), "the breath should hurt the pig");
                    helper.assertTrue(pig.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "the breath should slow the pig");
                    SupermanServer.usePower(player, crystal, SuperPower.SUPER_BREATH);
                    helper.assertFalse(SupermanServer.isBreathing(player), "using it again stops the breath");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void supermanXrayToggle(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack crystal = giveCrystal(player, 3000);
        SupermanHero.INSTANCE.summonSuit(player);
        SuperPower.POWERS.select(crystal, SuperPower.XRAY_VISION);
        use(player);
        helper.assertTrue(SupermanServer.isXray(player), "X-ray vision should be on");
        helper.startSequence()
                .thenExecuteFor(40, player::doTick)
                .thenExecute(() -> {
                    SupermanServer.usePower(player, crystal, SuperPower.XRAY_VISION);
                    helper.assertFalse(SupermanServer.isXray(player), "using it again turns X-ray vision off");
                    SupermanHero.INSTANCE.dismissSuit(player, false);
                    helper.assertFalse(SupermanServer.isXray(player), "taking the suit off ends every power");
                    remove(player);
                })
                .thenSucceed();
    }
}
