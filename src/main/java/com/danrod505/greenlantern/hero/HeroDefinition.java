package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.flight.FlightStyle;
import com.danrod505.greenlantern.ring.FlightHandler;
import com.danrod505.greenlantern.ring.Uniform;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
    private final SuitSet suit;

    /**
     * @param id        short id, e.g. {@code "wonder_woman"}
     * @param activator whether a stack is this hero's item (it calls the suit and holds the energy)
     * @param suit      the suit; wearing its chest piece is what "suited" means
     */
    protected HeroDefinition(String id, Predicate<ItemStack> activator, SuitSet suit) {
        this.id = id;
        this.activator = activator;
        this.suit = suit;
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
        return player.getItemBySlot(EquipmentSlot.CHEST).is(suit.chest().get());
    }

    public final SuitSet suit() {
        return suit;
    }

    /** Puts the suit on (taking off any other hero's first); false if the hero's item is missing. */
    public final boolean summonSuit(ServerPlayer player) {
        if (isSuited(player)) return true;
        ItemStack item = findItem(player);
        if (item.isEmpty()) {
            player.displayClientMessage(Component.translatable(suit.missingItemKey()), true);
            return false;
        }
        if (!canSuitUp(player, item)) return false;
        // Only one hero suit at a time.
        HeroRegistry.dismissOthers(player, this);
        Uniform.equipSuit(player, SuitSet.piece(suit.head()), SuitSet.piece(suit.chest()), SuitSet.piece(suit.legs()), SuitSet.piece(suit.feet()));
        updateSuitModifiers(player, true);
        onSuitEquipped(player);
        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), suit.upSound().get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        suitUpEffects(level, player);
        return true;
    }

    /** Takes the suit off (giving back the armor worn before), with sound and particles if {@code effects}. */
    public final void dismissSuit(ServerPlayer player, boolean effects) {
        onSuitRemoving(player);
        Uniform.removeSuit(player);
        updateSuitModifiers(player, false);
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), suit.downSound().get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            suitDownEffects(level, player);
        }
    }

    /** Adds (suited) or removes the suit's bonuses. */
    public final void updateSuitModifiers(ServerPlayer player, boolean suited) {
        for (SuitModifier modifier : suit.modifiers()) modifier.apply(player, suited);
    }

    /** Last check before suiting up, with the hero's item in hand (the Lantern needs energy in the ring). */
    protected boolean canSuitUp(ServerPlayer player, ItemStack item) {
        return true;
    }

    /** Right after the pieces and bonuses are on, before the suit up sound. */
    protected void onSuitEquipped(ServerPlayer player) {
    }

    /** Particles (and client events) after the suit up sound. */
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
    }

    /** Before the suit comes off: put away what the hero's powers left in the world. */
    protected void onSuitRemoving(ServerPlayer player) {
    }

    /** Particles after the suit down sound (only when taken off with effects). */
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
    }

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

    /** How the hero's power flight looks, sounds and handles (null: the Lantern's). */
    public FlightStyle flightStyle() {
        return null;
    }

    /** Whether the suited hero may power-fly right now, given the item found by {@link #findItem}. */
    public boolean canFly(Player player, ItemStack item) {
        return !item.isEmpty();
    }

    /**
     * Pays one second of power flight from the hero's item ({@code multiplier} grows with the speed,
     * up to the supersonic cost). Free by default.
     */
    public void payFlight(ServerPlayer player, ItemStack item, float multiplier) {}

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
