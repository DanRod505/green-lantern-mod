package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitSet;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A hero whose wheel holds abilities of the engine (the new model: 5 to 8 abilities with their
 * functions by context, the supreme one last, the traits off the wheel): every character of the
 * engine ({@link EngineHero}) and, since the adaptation of the 9 heroes (stage 3, lot 1), the first
 * 9 heroes too. Those keep their own item, suit, energy and look ({@link LegacyWheel}); their powers
 * of today run as functions of the new abilities (the preset {@code poder_de_hoje}).
 * <p>
 * The methods with the names of {@code HeroDefinition}'s are the hero's own.
 */
public interface AbilityHero {
    String id();

    CharacterData data();

    HeroEnergy energy();

    HeroPowers<EngineAbility> powers();

    ItemStack findItem(Player player);

    ItemStack heldItem(Player player);

    boolean isSuited(Player player);

    SuitSet suit();

    /** Whether all four suit pieces are on: only then the skin is the look. */
    default boolean fullSuit(Player player) {
        SuitSet suit = suit();
        return isSuited(player) && player.getItemBySlot(EquipmentSlot.HEAD).is(suit.head().get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(suit.legs().get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(suit.feet().get());
    }

    /** The traits the hero's own code already does (flight, gliding...): the engine leaves them alone. */
    default Set<String> ownTraits() {
        return Set.of();
    }
}
