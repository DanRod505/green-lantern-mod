package com.danrod505.greenlantern.hero;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A hero suit: the pieces it puts on (a null head leaves the player's own helmet alone, like
 * Aquaman's), the message when the hero's item is missing, the suit up / down sounds and the
 * bonuses it gives while worn. The pieces are {@code SuitArmorItem}s: they only exist while worn.
 */
public record SuitSet(Supplier<? extends Item> head, Supplier<? extends Item> chest, Supplier<? extends Item> legs, Supplier<? extends Item> feet,
                      String missingItemKey, Supplier<SoundEvent> upSound, Supplier<SoundEvent> downSound, List<SuitModifier> modifiers) {
    static ItemStack piece(Supplier<? extends Item> item) {
        return item == null ? ItemStack.EMPTY : item.get().getDefaultInstance();
    }
}
