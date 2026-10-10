package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.item.KryptonianCrystalItem;
import com.danrod505.greenlantern.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small helpers to locate the Kryptonian Crystal and to ask whether a player is Superman. */
public final class SupermanHelper {
    private SupermanHelper() {}

    public static boolean isCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof KryptonianCrystalItem;
    }

    /** The crystal held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public static ItemStack heldCrystal(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isCrystal(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The crystal held in a hand or, failing that, the first one found in the inventory. */
    public static ItemStack findCrystal(Player player) {
        ItemStack held = heldCrystal(player);
        if (!held.isEmpty()) return held;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCrystal(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** A player is Superman while wearing the suit with the S. */
    public static boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SUPERMAN_SUIT.get());
    }

    /** Superman with solar energy left in his cells (his powers need it; the suit stays on without it). */
    public static boolean isPowered(Player player) {
        if (!isSuited(player)) return false;
        if (player.isCreative()) return true;
        ItemStack crystal = findCrystal(player);
        return !crystal.isEmpty() && SolarEnergy.get(crystal).stored() > 0;
    }
}
