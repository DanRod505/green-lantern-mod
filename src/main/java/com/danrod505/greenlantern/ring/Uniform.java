package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.registry.ModParticles;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Summons and dismisses the Green Lantern uniform. Whatever armor the player was wearing is kept
 * safe in the player's persistent data and given back when the uniform is dismissed.
 */
public final class Uniform {
    private static final String STASH_KEY = GreenLantern.MODID + ".stashed_armor";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Codec<List<ItemStack>> STASH_CODEC = ItemStack.OPTIONAL_CODEC.listOf();

    private Uniform() {}

    public static void toggle(ServerPlayer player) {
        if (RingHelper.isSuited(player)) {
            dismiss(player, true);
        } else {
            summon(player);
        }
    }

    public static boolean summon(ServerPlayer player) {
        if (RingHelper.isSuited(player)) return true;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_ring"), true);
            return false;
        }
        if (RingEnergy.get(ring).stored() <= 0) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_energy"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            return false;
        }

        List<ItemStack> stash = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack worn = player.getItemBySlot(slot);
            stash.add(worn.getItem() instanceof SuitArmorItem ? ItemStack.EMPTY : worn.copy());
        }
        writeStash(player, stash);

        player.setItemSlot(EquipmentSlot.HEAD, createPiece(player, ModItems.LANTERN_MASK.get().getDefaultInstance()));
        player.setItemSlot(EquipmentSlot.CHEST, createPiece(player, ModItems.LANTERN_SUIT.get().getDefaultInstance()));
        player.setItemSlot(EquipmentSlot.LEGS, createPiece(player, ModItems.LANTERN_LEGGINGS.get().getDefaultInstance()));
        player.setItemSlot(EquipmentSlot.FEET, createPiece(player, ModItems.LANTERN_BOOTS.get().getDefaultInstance()));

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RING_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.9, 0.4, 0.02);
        level.sendParticles(ModParticles.SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 1.0, 0.5, 0.15);
        FlightHandler.refreshAbilities(player);
        return true;
    }

    public static void dismiss(ServerPlayer player, boolean effects) {
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
        FlightHandler.refreshAbilities(player);

        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RING_DEACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.9, 0.4, 0.01);
        }
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
