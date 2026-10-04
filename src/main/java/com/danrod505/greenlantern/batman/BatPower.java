package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Gadgets of Batman, picked on the power wheel (hold the wheel key) and used with the power key or
 * right click on the Utility Belt.
 * <p>
 * To add a gadget: add a constant here (its icon goes in {@code textures/gui/batman_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link BatmanServer#usePower}.
 */
public enum BatPower {
    /** A batarang flies from the hand, hits whatever it meets and curves back to the belt. */
    BATARANG("batarang"),
    /** The grapnel gun: the hook bites into a wall or a ledge and the cable reels Batman up to it. */
    GRAPPLE("grapple"),
    /** A cloud of bats pours out of the dark and defends Batman for a while. */
    BAT_SWARM("bat_swarm"),
    /** The Batmobile roars up: drive it, boost with sprint and fire missiles with left click. */
    BATMOBILE("batmobile");

    private static final BatPower[] VALUES = values();

    private final String id;

    BatPower(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public int iconIndex() {
        return ordinal();
    }

    public Component displayName() {
        return Component.translatable("power.greenlantern." + id);
    }

    public Component description() {
        return Component.translatable("power.greenlantern." + id + ".desc");
    }

    /** Belt charge needed to use the gadget. */
    public int cost() {
        return switch (this) {
            case BATARANG -> GLConfig.BATARANG_COST.get();
            case GRAPPLE -> GLConfig.GRAPPLE_COST.get();
            case BAT_SWARM -> GLConfig.BAT_SWARM_COST.get();
            case BATMOBILE -> GLConfig.BATMOBILE_COST.get();
        };
    }

    public static BatPower byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, VALUES.length - 1)];
    }

    public static int count() {
        return VALUES.length;
    }

    public static BatPower selected(ItemStack belt) {
        Integer index = belt.get(ModDataComponents.SELECTED_POWER.get());
        return byIndex(index == null ? 0 : index);
    }

    public static void select(ItemStack belt, BatPower power) {
        belt.set(ModDataComponents.SELECTED_POWER.get(), power.ordinal());
    }

    public static BatPower cycle(ItemStack belt, int offset) {
        BatPower next = VALUES[Math.floorMod(selected(belt).ordinal() + offset, VALUES.length)];
        select(belt, next);
        return next;
    }
}
