package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A hero whose wheel is built from its character file, in the new model of abilities: a character
 * of the engine ({@link EngineHero}) or one of the 9 heroes that already existed ({@link LegacyHero}).
 * The presets, the choice of function and the cooldowns work the same for both; only the 9 heroes
 * have powers of their own code, which the preset {@value #OWN_POWER} runs.
 */
public interface CharacterHero {
    /** The preset that runs a power of the hero's own code, named by {@code "poder"} in its settings. */
    String OWN_POWER = "poder_do_heroi";

    String id();

    CharacterData data();

    /** The energy that pays for the abilities. */
    HeroEnergy energy();

    SuitSet suit();

    boolean isSuited(Player player);

    ItemStack findItem(Player player);

    HeroPowers<EngineAbility> powers();

    /** Whether the hero's own code has a power with this id. */
    default boolean hasOwnPower(String power) {
        return false;
    }

    /** Uses a power of the hero's own code, exactly as before (its own energy, cooldown and checks); whether it went off. */
    default boolean ownPower(ServerPlayer player, ItemStack item, String power) {
        return false;
    }
}
