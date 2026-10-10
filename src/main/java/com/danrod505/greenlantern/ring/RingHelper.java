package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.item.PowerRingItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers to locate the Power Ring and query the uniform state of a player. */
public final class RingHelper {
    private RingHelper() {}

    public static boolean isRing(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PowerRingItem;
    }

    /** The ring held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public static ItemStack heldRing(Player player) {
        return LanternHero.INSTANCE.heldItem(player);
    }

    /** The ring held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findRing(Player player) {
        return LanternHero.INSTANCE.findItem(player);
    }

    /** A player is "suited up" while wearing the Green Lantern uniform chest piece. */
    public static boolean isSuited(Player player) {
        return LanternHero.INSTANCE.isSuited(player);
    }
}
