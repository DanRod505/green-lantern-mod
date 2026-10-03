package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.item.FlashRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
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
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isRing(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The Flash ring held in a hand or, failing that, the first one found in the inventory. */
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

    /** A player is the Flash while wearing the Flash suit chest piece. */
    public static boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.FLASH_SUIT.get());
    }
}
