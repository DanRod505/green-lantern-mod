package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
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
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isRing(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The ring held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findRing(Player player) {
        ItemStack held = heldRing(player);
        if (!held.isEmpty()) return held;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isRing(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** A player is "suited up" while wearing the Green Lantern uniform chest piece. */
    public static boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LANTERN_SUIT.get());
    }
}
