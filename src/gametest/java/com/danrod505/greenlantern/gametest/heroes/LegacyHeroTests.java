package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.CharacterHero;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.LegacyHero;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Traits;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.gametest.HeroContractTests;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.ring.LanternHero;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The 9 heroes in the new model of abilities (stage 3, lot 1).
 * <ul>
 *     <li>{@code legacy_heroes_wheel}: each one's wheel is the one of its character file, 5 to 8
 *     abilities with the supreme last, known traits and presets, every {@value CharacterHero#OWN_POWER}
 *     names a power of the hero's own code, and every power of the old wheel (every construct, for
 *     the Lantern) is still run by some function.</li>
 *     <li>{@code legacy_heroes_library_functions}: every function that is a preset of the library
 *     goes off, throws nothing and is paid from the hero's own energy. The powers of the hero's own
 *     code keep their own tests.</li>
 * </ul>
 */
public final class LegacyHeroTests {
    private static final int TICKS_PER_FUNCTION = 3;

    private LegacyHeroTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("legacy_heroes_wheel", () -> LegacyHeroTests::wheel);
        tests.register("legacy_heroes_library_functions", () -> LegacyHeroTests::libraryFunctions);
    }

    private static List<LegacyHero> heroes() {
        return HeroRegistry.all().stream().filter(h -> h instanceof LegacyHero).map(h -> (LegacyHero) h).toList();
    }

    /** The ids of the powers of the old wheel. */
    private static List<String> oldPowers(LegacyHero hero) {
        List<String> ids = new ArrayList<>();
        if (hero instanceof LanternHero) {
            for (Construct construct : ConstructRegistry.all()) ids.add(construct.id().getPath());
            return ids;
        }
        HeroPowers<?> own = hero.ownPowers();
        for (int i = 0; i < own.set().count(); i++) ids.add(own.power(i).id());
        return ids;
    }

    public static void wheel(GameTestHelper helper) {
        helper.assertTrue(heroes().size() == 9, "there should be 9 heroes of their own code, found " + heroes().size());
        for (LegacyHero hero : heroes()) {
            CharacterData data = hero.data();
            String who = "hero " + hero.id() + ": ";
            helper.assertTrue(data.legacy(), who + "the character file should say \"legado\": true");
            List<CharacterData.Ability> abilities = data.abilities();
            int count = abilities.size();
            helper.assertTrue(count >= 5 && count <= 8, who + "has " + count + " abilities, needs 5 to 8");
            helper.assertTrue(hero.powers().set().count() == count, who + "the wheel has " + hero.powers().set().count() + " abilities, the file " + count);
            helper.assertTrue(abilities.getLast().supreme(), who + "the supreme should be the last ability");
            helper.assertTrue(abilities.stream().filter(CharacterData.Ability::supreme).count() == 1, who + "needs exactly one supreme");
            for (CharacterData.Trait trait : data.traits()) {
                helper.assertTrue(Traits.TYPES.contains(trait.type()), who + "unknown passiva " + trait.type());
            }
            Set<String> ids = new HashSet<>();
            for (CharacterData.Ability ability : abilities) {
                helper.assertTrue(ids.add(ability.id()), who + "two abilities with the id " + ability.id());
                for (CharacterData.Function function : ability.allFunctions()) {
                    helper.assertTrue(Presets.exists(function.preset()), who + ability.id() + ": no preset " + function.preset());
                    LegacyHero.ownPowerOf(ability, function).ifPresent(power ->
                            helper.assertTrue(hero.hasOwnPower(power), who + ability.id() + ": no power " + power + " in the hero's code"));
                }
            }
            for (String power : oldPowers(hero)) {
                helper.assertTrue(hero.abilityIndexOf(power) >= 0, who + "no ability runs the old power " + power);
            }
        }
        helper.succeed();
    }

    /** Removes what the functions left in the arena (creatures, blocks thrown, projectiles), keeping the players. */
    private static void sweep(GameTestHelper helper) {
        for (Entity entity : helper.getLevel().getEntities((Entity) null, helper.getBounds(), e -> !(e instanceof Player))) {
            entity.discard();
        }
    }

    public static void libraryFunctions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The weather as it is set (a storm called now only shows a few seconds later).
        boolean raining = level.getLevelData().isRaining();
        boolean thundering = level.getLevelData().isThundering();
        GameTestSequence sequence = helper.startSequence();
        for (LegacyHero hero : heroes()) {
            ServerPlayer[] player = new ServerPlayer[1];
            sequence.thenExecute(() -> {
                player[0] = player(helper, 7.5, 1, 4.5, 0, 0);
                player[0].setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(hero));
                helper.assertTrue(hero.summonSuit(player[0]), "hero " + hero.id() + ": the suit should go on");
            });
            for (int a = 0; a < hero.powers().set().count(); a++) {
                EngineAbility ability = hero.powers().set().byIndex(a);
                List<CharacterData.Function> functions = ability.data().allFunctions();
                for (int f = 0; f < functions.size(); f++) {
                    if (LegacyHero.ownPowerOf(ability.data(), functions.get(f)).isPresent()) continue;
                    int index = f;
                    String who = "hero " + hero.id() + ", ability " + ability.id() + ", function " + index + " (" + functions.get(f).preset() + "): ";
                    sequence.thenExecute(() -> {
                        dummy(helper, 7.5, 1, 9.5);
                        ItemStack item = player[0].getMainHandItem();
                        hero.energy().set(item, hero.energy().capacity() / 2);
                        int before = hero.energy().get(item).stored();
                        try {
                            Presets.tryFunction(player[0], hero, item, ability, index);
                        } catch (RuntimeException e) {
                            GreenLantern.LOGGER.error("{}failed", who, e);
                            helper.fail(who + "threw " + e);
                        }
                        int after = hero.energy().get(player[0].getMainHandItem()).stored();
                        helper.assertTrue(after <= before, who + "using it must not add energy (" + before + " -> " + after + ")");
                    })
                            .thenExecuteFor(TICKS_PER_FUNCTION, () -> player[0].doTick())
                            .thenExecute(() -> {
                                // What the function built (a great work, a bridge) comes down, so the tests next to this one keep their sky.
                                PresetRuntime.undo(player[0], PresetRuntime.key(ability.id(), index));
                                PresetRuntime.stopLasting(player[0]);
                                sweep(helper);
                                // The weather goes back to what it was: the sun tests run next to this one.
                                if (level.getLevelData().isRaining() != raining || level.getLevelData().isThundering() != thundering) {
                                    level.setWeatherParameters(raining ? 0 : 6000, raining ? 6000 : 0, raining, thundering);
                                }
                            });
                }
            }
            sequence.thenExecute(() -> {
                if (hero.isSuited(player[0])) hero.dismissSuit(player[0], false);
                remove(player[0]);
                sweep(helper);
            });
        }
        sequence.thenSucceed();
    }
}
