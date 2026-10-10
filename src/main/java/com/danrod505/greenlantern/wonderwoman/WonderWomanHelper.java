package com.danrod505.greenlantern.wonderwoman;

import com.danrod505.greenlantern.item.AmazonShieldItem;
import com.danrod505.greenlantern.item.AmazonSwordItem;
import com.danrod505.greenlantern.item.AmazonTiaraItem;
import com.danrod505.greenlantern.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers to locate the Tiara of Themyscira and the Amazon weapons, and to ask whether a player is Wonder Woman. */
public final class WonderWomanHelper {
    private WonderWomanHelper() {}

    public static boolean isTiara(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AmazonTiaraItem;
    }

    public static boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AmazonSwordItem;
    }

    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AmazonShieldItem;
    }

    /** The tiara held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public static ItemStack heldTiara(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isTiara(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The tiara held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findTiara(Player player) {
        ItemStack held = heldTiara(player);
        if (!held.isEmpty()) return held;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isTiara(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Inventory slot of the Amazon sword, or -1 (the off hand counts too: slot {@link Inventory#SLOT_OFFHAND}). */
    public static int swordSlot(Player player) {
        return slotOf(player, true);
    }

    /** Inventory slot of the Amazon shield, or -1. */
    public static int shieldSlot(Player player) {
        return slotOf(player, false);
    }

    private static int slotOf(Player player, boolean sword) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (sword ? isSword(stack) : isShield(stack)) return i;
        }
        return -1;
    }

    /** A player is Wonder Woman while wearing her armor with the golden eagle. */
    public static boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WONDER_WOMAN_SUIT.get());
    }
}
