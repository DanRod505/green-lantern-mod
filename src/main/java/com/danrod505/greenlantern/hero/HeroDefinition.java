package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.flight.FlightProfile;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One hero of the mod, described in one place: the item that calls the suit (ring, emblem, belt,
 * crystal, tiara...), the suit, the power wheel and the hooks the shared code calls for it. Each
 * hero package has one subclass, listed once in {@link HeroRegistry}; keys, packets, the power
 * wheel, death and logout handling, fall damage and power flight all go through the registry
 * instead of naming heroes.
 */
public abstract class HeroDefinition {
    private final String id;
    private final Predicate<ItemStack> activator;
    private final Supplier<? extends Item> suitChest;

    /**
     * @param id        short id, e.g. {@code "wonder_woman"}
     * @param activator whether a stack is this hero's item (it calls the suit and holds the energy)
     * @param suitChest the chest piece of the suit: wearing it is what "suited" means
     */
    protected HeroDefinition(String id, Predicate<ItemStack> activator, Supplier<? extends Item> suitChest) {
        this.id = id;
        this.activator = activator;
        this.suitChest = suitChest;
    }

    public final String id() {
        return id;
    }

    public final boolean isActivator(ItemStack stack) {
        return !stack.isEmpty() && activator.test(stack);
    }

    /** The hero's item held in either hand (main hand first), or {@link ItemStack#EMPTY}. */
    public final ItemStack heldItem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isActivator(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The hero's item held in a hand or, failing that, the first one found in the inventory. */
    public final ItemStack findItem(Player player) {
        ItemStack held = heldItem(player);
        if (!held.isEmpty()) return held;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isActivator(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Whether the player wears this hero's suit. */
    public boolean isSuited(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(suitChest.get());
    }

    /** Puts the suit on (taking off any other hero's first); false if the hero's item is missing. */
    public abstract boolean summonSuit(ServerPlayer player);

    /** Takes the suit off, with sound and particles if {@code effects}. */
    public abstract void dismissSuit(ServerPlayer player, boolean effects);

    /** The power wheel, or null for a hero without one (the Lantern picks constructs instead). */
    public HeroPowers<?> powers() {
        return null;
    }

    public final boolean hasPowers() {
        return powers() != null;
    }

    /**
     * The client half of the hero (HUD, visuals, renderers), or null. Only called on the physical
     * client: return a constructor reference ({@code FlashClient::new}) so the client classes are
     * never loaded on a dedicated server.
     */
    public Supplier<HeroClient> client() {
        return null;
    }

    /** How the hero power-flies while suited, or null if the hero doesn't. */
    public FlightProfile flightProfile() {
        return null;
    }

    // ---- Server hooks (called every tick / on events for every player) ----------------------------

    /**
     * Start of the player tick: keeps the suit honest. By default the suit goes away when the
     * hero's item is gone. Returning false skips the rest of this player's tick.
     */
    public boolean checkSuit(ServerPlayer player) {
        if (isSuited(player) && findItem(player).isEmpty()) {
            dismissSuit(player, true);
        }
        return true;
    }

    /** The hero's own server tick (energy, running powers), called every tick suited or not. */
    public void tick(ServerPlayer player) {
    }

    /** The player leaves the server: drop whatever the hero keeps for them. */
    public void onLogout(ServerPlayer player) {
    }

    /** Damage multiplier for a fall while suited, or a negative number to leave the fall alone. */
    public float fallDamageMultiplier(Player player, double distance) {
        return -1.0F;
    }

    /** Multiplier for damage the player takes (called suited or not; the hero checks its own condition). */
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        return 1.0F;
    }

    /** Items that belong to the suit or a power and must never lie around as dropped items. */
    public boolean neverDropped(ItemStack stack) {
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}
