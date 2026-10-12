package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.engine.AbilityHero;
import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Traits;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The automatic test of every character of the engine (stage 2, lot 7): whatever character file is
 * in the mod, generated from the catalog or written by hand, is checked the same way.
 * <ul>
 *     <li>the item is registered and, for a character of the mod, has a recipe that loads;</li>
 *     <li>the energy bar has room, the traits are known, there are 5 to 8 abilities and, for a
 *     character of the mod, every function names a preset that exists;</li>
 *     <li>the suit goes on and the skins go round (in creative, all of them);</li>
 *     <li>every function of every ability goes off with nothing in the way (no cooldown, energy or
 *     condition), throws nothing and never adds energy (other than the class's energy per hit).</li>
 * </ul>
 * Each function gets a fresh target in front of the player, and what it leaves (lasting presets,
 * creatures, weather) is cleaned up before the next one, so the other tests do not see it.
 */
public final class EveryCharacterTests {
    /** The test characters of src/gametest: they have no recipe. */
    static final Set<String> TEST_CHARACTERS = Set.of("prototipo", "prototipo_alado", "prototipo_colosso");
    private static final int TICKS_PER_FUNCTION = 3;

    private EveryCharacterTests() {}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
        tests.register("engine_every_character", () -> EveryCharacterTests::everyCharacter);
    }

    /** The characters of the engine and, since stage 3, the 9 heroes (their wheel is the engine's). */
    private static List<HeroDefinition> characters() {
        return HeroRegistry.all().stream().filter(EveryCharacterTests::covers).toList();
    }

    /** The basics of one character, before its abilities are used. */
    private static void basics(GameTestHelper helper, AbilityHero hero) {
        CharacterData data = hero.data();
        String who = "character " + data.id() + ": ";
        // The 9 heroes keep their own items and recipes (stage 3, lot 3 gives them the catalog's).
        boolean engine = hero instanceof EngineHero;
        helper.assertTrue(!engine || BuiltInRegistries.ITEM.containsKey(GreenLantern.id(data.id())), who + "the item is not registered");
        helper.assertTrue(engine || data.legacy(), who + "a hero with its own code must be marked legado");
        helper.assertTrue(Characters.check(data, data.id()).isEmpty(), who + Characters.check(data, data.id()));
        if (engine && !TEST_CHARACTERS.contains(data.id())) {
            var recipe = ResourceKey.create(Registries.RECIPE, GreenLantern.id(data.id()));
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(recipe).isPresent(), who + "no recipe for the item loads");
        }
        helper.assertTrue(hero.energy().capacity() > 0, who + "the energy bar is empty");
        int count = data.abilities().size();
        helper.assertTrue(count >= 5 && count <= 8, who + "has " + count + " abilities, needs 5 to 8");
        helper.assertTrue(hero.powers().set().count() == count, who + "the wheel has " + hero.powers().set().count() + " abilities, the file " + count);
        for (CharacterData.Trait trait : data.traits()) {
            helper.assertTrue(Traits.TYPES.contains(trait.type()), who + "unknown passiva " + trait.type());
        }
        Set<String> ids = new HashSet<>();
        for (CharacterData.Ability ability : data.abilities()) {
            helper.assertTrue(ids.add(ability.id()), who + "two abilities with the id " + ability.id());
            // The test characters keep one preset that is not built on purpose (the "not built yet" message).
            if (TEST_CHARACTERS.contains(data.id())) continue;
            helper.assertTrue(Presets.exists(ability.preset()), who + "ability " + ability.id() + ": no preset " + ability.preset());
            for (CharacterData.Function function : ability.functions()) {
                helper.assertTrue(Presets.exists(function.preset()), who + "ability " + ability.id() + ": no preset " + function.preset());
            }
        }
        helper.assertFalse(data.skins().isEmpty(), who + "has no skin");
    }

    /** Puts the suit on, then turns the skins round once in creative: each one comes up and the first comes back. */
    private static ServerPlayer suitUp(GameTestHelper helper, HeroDefinition definition) {
        String who = "character " + definition.id() + ": ";
        ServerPlayer player = player(helper, 7.5, 1, 4.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, HeroContractTests.chargedItem(definition));
        helper.assertTrue(definition.summonSuit(player), who + "the suit should go on");
        helper.assertTrue(definition.isSuited(player), who + "should be suited");
        // The skins of the 9 heroes come in stage 3, lot 2.
        if (!(definition instanceof EngineHero hero)) return player;
        ItemStack item = player.getMainHandItem();
        List<CharacterData.Skin> skins = hero.data().skins();
        helper.assertTrue(EngineServer.skin(hero, item).selected().equals(skins.getFirst().id()), who + "the first skin is the default");
        player.setGameMode(GameType.CREATIVE);
        for (int i = 1; i <= skins.size(); i++) {
            EngineServer.cycleSkin(player, hero, item);
            String expected = skins.get(i % skins.size()).id();
            helper.assertTrue(EngineServer.skin(hero, item).selected().equals(expected), who + "skin " + expected + " should come up");
        }
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Removes what the abilities left in the arena (creatures, blocks thrown, projectiles), keeping the players. */
    private static void sweep(GameTestHelper helper) {
        for (Entity entity : helper.getLevel().getEntities((Entity) null, helper.getBounds(), e -> !(e instanceof Player))) {
            entity.discard();
        }
    }

    public static void everyCharacter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean raining = level.isRaining();
        boolean thundering = level.isThundering();
        GameTestSequence sequence = helper.startSequence();
        for (HeroDefinition definition : characters()) {
            AbilityHero hero = (AbilityHero) definition;
            ServerPlayer[] player = new ServerPlayer[1];
            sequence.thenExecute(() -> {
                basics(helper, hero);
                player[0] = suitUp(helper, definition);
            });
            for (int a = 0; a < hero.powers().set().count(); a++) {
                EngineAbility ability = hero.powers().set().byIndex(a);
                for (int f = 0; f <= ability.data().functions().size(); f++) {
                    int index = f;
                    String preset = index == 0 ? ability.data().preset() : ability.data().functions().get(index - 1).preset();
                    String who = "character " + hero.data().id() + ", ability " + ability.data().id() + ", function " + index + " (" + preset + "): ";
                    sequence.thenExecute(() -> {
                        if (player[0] == null) return; // the basics already failed the test
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
                        // Only the class's energy per hit may come back (a warrior's blow on the target).
                        int perHit = hero.data().classRule().energy().perHit();
                        boolean hits = perHit > 0 && (after - before) % perHit == 0;
                        helper.assertTrue(after <= before || hits, who + "using it must not add energy (" + before + " -> " + after + ")");
                    })
                            .thenExecuteFor(TICKS_PER_FUNCTION, () -> {
                                if (player[0] != null) player[0].doTick();
                            })
                            .thenExecute(() -> {
                                if (player[0] != null) PresetRuntime.stopLasting(player[0]);
                                sweep(helper);
                                // The weather goes back to what it was: the sun tests run next to this one.
                                if (level.isRaining() != raining || level.isThundering() != thundering) {
                                    level.setWeatherParameters(raining ? 0 : 6000, raining ? 6000 : 0, raining, thundering);
                                }
                            });
                }
            }
            sequence.thenExecute(() -> {
                if (player[0] == null) return;
                if (hero.isSuited(player[0])) definition.dismissSuit(player[0], false);
                remove(player[0]);
                sweep(helper);
            });
        }
        sequence.thenSucceed();
    }

    /** Whether the hero is one this test covers (the contract of the powers leaves it to this test). */
    public static boolean covers(HeroDefinition hero) {
        return hero instanceof AbilityHero;
    }
}
