package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.supergirl.SupergirlConfig;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.supergirl.SupergirlPower;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Supergirl. The hero contract tests already check the suit, the texts and that every power runs;
 * these check what is particular to the hero. Give each finished power its own test here (and a
 * {@code test_instance/<name>.json} next to the others).
 */
public final class SupergirlTests {
    private SupergirlTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("supergirl_energy_recharges", () -> SupergirlTests::energyRecharges);
        tests.register("supergirl_powers_spend_energy", () -> SupergirlTests::powersSpendEnergy);
    }

    private static ItemStack giveItem(ServerPlayer player, int energy) {
        ItemStack item = HeroContractTests.chargedItem(SupergirlHero.INSTANCE);
        SupergirlHero.ENERGY.set(item, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        return player.getMainHandItem();
    }

    public static void energyRecharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack item = giveItem(player, 0);
        SupergirlHero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {
                    int stored = SupergirlHero.ENERGY.get(item).stored();
                    int expected = 2 * SupergirlConfig.RECHARGE_PER_SECOND.get();
                    helper.assertTrue(stored >= expected, "the energy should come back every second, got " + stored + " (expected " + expected + ")");
                    remove(player);
                })
                .thenSucceed();
    }

    public static void powersSpendEnergy(GameTestHelper helper) {
        for (SupergirlPower power : SupergirlPower.values()) {
            ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
            ItemStack item = giveItem(player, SupergirlHero.ENERGY.capacity());
            SupergirlHero.INSTANCE.summonSuit(player);
            helper.assertTrue(SupergirlHero.INSTANCE.powers().use(player, player.getMainHandItem(), power.ordinal()), power.id() + " should go off");
            int stored = SupergirlHero.ENERGY.get(player.getMainHandItem()).stored();
            helper.assertTrue(stored == SupergirlHero.ENERGY.capacity() - power.cost(), power.id() + " should cost " + power.cost() + ", left " + stored);
            remove(player);
        }
        helper.succeed();
    }
}
