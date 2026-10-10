package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Puts hero suits on and takes them off (see {@code HeroDefinition#summonSuit}). Whatever armor the
 * player was wearing is kept safe in the player's persistent data and given back when the suit comes off.
 */
public final class Uniform {
    private static final String STASH_KEY = GreenLantern.MODID + ".stashed_armor";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Codec<List<ItemStack>> STASH_CODEC = ItemStack.OPTIONAL_CODEC.listOf();

    private Uniform() {}

    // ---- shared by every hero suit -----------------------------------------------------------------

    /**
     * Puts on a hero suit (head, chest, legs, feet pieces). Whatever armor the player wore is stashed
     * in their persistent data and given back by {@link #removeSuit}. An empty piece leaves that slot
     * alone (Aquaman has no mask: the player keeps their own helmet).
     */
    public static void equipSuit(ServerPlayer player, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        ItemStack[] pieces = {head, chest, legs, feet};
        List<ItemStack> stash = new ArrayList<>();
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack worn = player.getItemBySlot(SLOTS[i]);
            boolean replaced = !pieces[i].isEmpty() || worn.getItem() instanceof SuitArmorItem;
            stash.add(!replaced || worn.getItem() instanceof SuitArmorItem ? ItemStack.EMPTY : worn.copy());
        }
        writeStash(player, stash);
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack worn = player.getItemBySlot(SLOTS[i]);
            if (!pieces[i].isEmpty()) {
                player.setItemSlot(SLOTS[i], createPiece(player, pieces[i]));
            } else if (worn.getItem() instanceof SuitArmorItem) {
                player.setItemSlot(SLOTS[i], ItemStack.EMPTY);
            }
        }
    }

    /** Takes off any hero suit pieces and gives back the stashed armor. */
    public static void removeSuit(ServerPlayer player) {
        for (EquipmentSlot slot : SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof SuitArmorItem) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        List<ItemStack> stash = readStash(player);
        for (int i = 0; i < SLOTS.length && i < stash.size(); i++) {
            ItemStack stored = stash.get(i);
            if (stored.isEmpty()) continue;
            if (player.getItemBySlot(SLOTS[i]).isEmpty()) {
                player.setItemSlot(SLOTS[i], stored);
            } else if (!player.getInventory().add(stored)) {
                player.drop(stored, false);
            }
        }
        player.getPersistentData().remove(STASH_KEY);
        removeStrayPieces(player);
    }

    /** Uniform pieces only exist while worn: remove any that ended up elsewhere in the inventory. */
    public static void removeStrayPieces(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof SuitArmorItem) {
                boolean worn = false;
                for (EquipmentSlot slot : SLOTS) {
                    if (player.getItemBySlot(slot) == stack) {
                        worn = true;
                        break;
                    }
                }
                if (!worn) inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().getItem() instanceof SuitArmorItem) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static ItemStack createPiece(ServerPlayer player, ItemStack stack) {
        // Curse of Binding keeps the uniform on, Curse of Vanishing makes sure it never drops.
        var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> binding = enchantments.getOrThrow(Enchantments.BINDING_CURSE);
        Holder<Enchantment> vanishing = enchantments.getOrThrow(Enchantments.VANISHING_CURSE);
        stack.enchant(binding, 1);
        stack.enchant(vanishing, 1);
        return stack;
    }

    private static void writeStash(ServerPlayer player, List<ItemStack> stash) {
        CompoundTag data = player.getPersistentData();
        STASH_CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stash)
                .resultOrPartial(error -> GreenLantern.LOGGER.error("Could not save stashed armor: {}", error))
                .ifPresent(tag -> data.put(STASH_KEY, tag));
    }

    private static List<ItemStack> readStash(ServerPlayer player) {
        Tag tag = player.getPersistentData().get(STASH_KEY);
        if (tag == null) return List.of();
        return STASH_CODEC.parse(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> GreenLantern.LOGGER.error("Could not load stashed armor: {}", error))
                .orElse(List.of());
    }
}
