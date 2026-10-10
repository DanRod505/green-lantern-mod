package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.item.AquamanEmblemItem;
import com.danrod505.greenlantern.item.AquaTridentItem;
import com.danrod505.greenlantern.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers to locate the Atlantean Emblem and the trident, and to ask whether a player is Aquaman. */
public final class AquamanHelper {
    private AquamanHelper() {}

    public static boolean isEmblem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AquamanEmblemItem;
    }

    public static boolean isTrident(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AquaTridentItem;
    }

    /** The emblem held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public static ItemStack heldEmblem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isEmblem(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The emblem held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findEmblem(Player player) {
        ItemStack held = heldEmblem(player);
        if (!held.isEmpty()) return held;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isEmblem(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Inventory slot holding the trident, or -1. */
    public static int tridentSlot(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isTrident(inventory.getItem(i))) return i;
        }
        return -1;
    }

    /** A player is Aquaman while wearing the Atlantean scale shirt. */
    public static boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.AQUAMAN_SUIT.get());
    }
}
