package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.engine.AbilityFunctions;
import com.danrod505.greenlantern.engine.AbilityHero;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.OwnPowers;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.entity.LassoEntity;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.ring.LanternHero;
import com.danrod505.greenlantern.ring.RingConstructs;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.superman.SupermanServer;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The 9 heroes of before in the new model of abilities (stage 3, lot 1): every function of their
 * files runs something that exists, and the wheel, right click and holding the key reach their powers.
 */
public final class AdaptationTests {
    private AdaptationTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("adaptacao_funcoes_dos_nove", () -> AdaptationTests::functionsOfTheNine);
        tests.register("adaptacao_toque_e_segurar", () -> AdaptationTests::tapAndHold);
        tests.register("adaptacao_metralhadora_do_anel", () -> AdaptationTests::ringGatling);
    }

    /** Every function names a power the hero has or a preset of the engine; the supreme one is last. */
    public static void functionsOfTheNine(GameTestHelper helper) {
        int heroes = 0;
        for (HeroDefinition definition : HeroRegistry.all()) {
            if (!(definition instanceof AbilityHero hero) || definition instanceof EngineHero) continue;
            heroes++;
            String who = "hero " + hero.id() + ": ";
            int count = hero.powers().set().count();
            helper.assertTrue(count >= 5 && count <= 8, who + count + " abilities, the model asks 5 to 8");
            for (int i = 0; i < count; i++) {
                EngineAbility ability = hero.powers().set().byIndex(i);
                CharacterData.Ability data = ability.data();
                helper.assertTrue(data.supreme() == (i == count - 1), who + data.id() + ": the supreme ability goes last, and only it");
                for (CharacterData.Function function : data.allFunctions()) {
                    String power = OwnPowers.power(function, data);
                    if (OwnPowers.PRESET.equals(function.preset())) {
                        helper.assertTrue(power != null && hero.ownPowers().has(power), who + data.id() + " runs the power " + power + ", which the hero lacks");
                    } else {
                        helper.assertTrue(Presets.exists(function.preset()), who + data.id() + " runs the unknown preset " + function.preset());
                    }
                }
                String own = OwnPowers.power(data.defaultFunction(), data);
                if (own != null) helper.assertTrue(hero.abilityOf(own) == i, who + "abilityOf(" + own + ") should find " + data.id());
            }
        }
        helper.assertTrue(heroes == 9, "the 9 heroes of before should be in the new model, found " + heroes);
        helper.succeed();
    }

    /** Right click runs the tap of the chosen ability; holding the key runs its other function. */
    public static void tapAndHold(GameTestHelper helper) {
        ServerPlayer superman = player(helper, 4.5, 1, 2.5, 0, 0);
        ItemStack crystal = giveCrystal(superman, 3000);
        helper.assertTrue(SupermanHero.INSTANCE.summonSuit(superman), "Superman's suit should go on");
        int heat = SupermanHero.INSTANCE.abilityOf("heat_vision");
        helper.assertTrue(heat >= 0, "an ability of Superman should fire heat vision on a tap");
        SupermanHero.INSTANCE.powers().select(superman, superman.getMainHandItem(), heat, false);
        use(superman);
        helper.assertTrue(SupermanServer.isUsingHeatVision(superman), "right click with heat vision chosen should turn the beams on");
        SupermanServer.usePower(superman, superman.getMainHandItem(), com.danrod505.greenlantern.superman.SuperPower.HEAT_VISION);

        ServerPlayer diana = player(helper, 10.5, 1, 2.5, 0, 0);
        giveTiara(diana, 1000);
        helper.assertTrue(WonderWomanHero.INSTANCE.summonSuit(diana), "Wonder Woman's suit should go on");
        int lasso = WonderWomanHero.INSTANCE.abilityOf("lasso_pull");
        helper.assertTrue(lasso >= 0, "an ability of Wonder Woman should pull with the lasso on a tap");
        EngineAbility ability = WonderWomanHero.INSTANCE.powers().set().byIndex(lasso);
        helper.assertTrue(AbilityFunctions.usesHold(ability.data()), "the lasso ability should have a function when held");
        EngineServer.useAbility(diana, diana.getMainHandItem(), ability, AbilityFunctions.Press.HOLD);
        helper.assertTrue(LassoEntity.find(diana) != null, "holding the key on the lasso should throw it to capture");
        helper.startSequence()
                .thenExecuteFor(5, () -> {
                    superman.doTick();
                    diana.doTick();
                })
                .thenExecute(() -> {
                    remove(superman);
                    remove(diana);
                })
                .thenSucceed();
    }

    /** The gatling gun of the ring, held before, now fires on its own for a while. */
    public static void ringGatling(GameTestHelper helper) {
        ServerPlayer player = player(helper, 7.5, 1, 2.5, 0, 0);
        giveRing(player, 5000);
        helper.assertTrue(LanternHero.INSTANCE.summonSuit(player), "the Lantern's suit should go on");
        int hammer = LanternHero.INSTANCE.abilityOf("hammer");
        helper.assertTrue(hammer >= 0, "an ability of the Lantern should make the hammer on a tap");
        EngineAbility constructs = LanternHero.INSTANCE.powers().set().byIndex(hammer);
        helper.assertTrue(EngineServer.useAbility(player, player.getMainHandItem(), constructs, AbilityFunctions.Press.HOLD),
                "holding the key on the constructs should make the gatling gun");
        helper.assertTrue(RingConstructs.autoFiring(player), "the gatling gun should fire on its own");
        helper.startSequence()
                .thenExecuteFor(RingConstructs.AUTO_FIRE_TICKS + 10, player::doTick)
                .thenExecute(() -> {
                    helper.assertFalse(RingConstructs.autoFiring(player), "the gatling gun should stop after a while");
                    remove(player);
                })
                .thenSucceed();
    }
}
