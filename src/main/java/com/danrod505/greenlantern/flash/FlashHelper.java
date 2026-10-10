package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.item.FlashRingItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers to locate the Flash ring and query whether a player wears the Flash suit. */
public final class FlashHelper {
    private FlashHelper() {}

    public static boolean isRing(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof FlashRingItem;
    }

    /** The Flash ring held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public static ItemStack heldRing(Player player) {
        return FlashHero.INSTANCE.heldItem(player);
    }

    /** The Flash ring held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findRing(Player player) {
        return FlashHero.INSTANCE.findItem(player);
    }

    /** A player is the Flash while wearing the Flash suit chest piece. */
    public static boolean isSuited(Player player) {
        return FlashHero.INSTANCE.isSuited(player);
    }
}
