package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.item.UtilityBeltItem;
import com.danrod505.greenlantern.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers to locate the Utility Belt and to ask whether a player is Batman. */
public final class BatmanHelper {
    private BatmanHelper() {}

    public static boolean isBelt(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof UtilityBeltItem;
    }

    /** The belt held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public static ItemStack heldBelt(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isBelt(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The belt held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findBelt(Player player) {
        ItemStack held = heldBelt(player);
        if (!held.isEmpty()) return held;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isBelt(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** A player is Batman while wearing the batsuit. */
    public static boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BATMAN_SUIT.get());
    }

    /** Whether the player wears the cowl (the ears and the night lenses come with it). */
    public static boolean hasCowl(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BATMAN_COWL.get());
    }
}
