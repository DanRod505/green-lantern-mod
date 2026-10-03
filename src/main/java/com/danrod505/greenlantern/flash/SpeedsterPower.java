package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Powers of the Flash, picked on the power wheel (hold the wheel key while wearing the suit) and
 * used with right click on the Flash ring or the power key.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/flash_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link SpeedsterServer#usePower}.
 */
public enum SpeedsterPower {
    /** Run in circles to create a vortex that lifts and batters everything around it. */
    TORNADO("tornado"),
    /** Vibrate your molecules to pass through solid matter. */
    PHASE("phase"),
    /** Throw a bolt of Speed Force lightning. */
    LIGHTNING("lightning");

    private static final SpeedsterPower[] VALUES = values();

    private final String id;

    SpeedsterPower(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public int iconIndex() {
        return ordinal();
    }

    public Component name() {
        return Component.translatable("power.greenlantern." + id);
    }

    public Component description() {
        return Component.translatable("power.greenlantern." + id + ".desc");
    }

    /** Speed Force needed to activate the power. */
    public int cost() {
        return switch (this) {
            case TORNADO -> GLConfig.TORNADO_COST.get();
            case PHASE -> GLConfig.PHASE_COST.get();
            case LIGHTNING -> GLConfig.LIGHTNING_COST.get();
        };
    }

    public static SpeedsterPower byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, VALUES.length - 1)];
    }

    public static int count() {
        return VALUES.length;
    }

    public static SpeedsterPower selected(ItemStack ring) {
        Integer index = ring.get(ModDataComponents.SELECTED_POWER.get());
        return byIndex(index == null ? 0 : index);
    }

    public static void select(ItemStack ring, SpeedsterPower power) {
        ring.set(ModDataComponents.SELECTED_POWER.get(), power.ordinal());
    }

    public static SpeedsterPower cycle(ItemStack ring, int offset) {
        SpeedsterPower next = VALUES[Math.floorMod(selected(ring).ordinal() + offset, VALUES.length)];
        select(ring, next);
        return next;
    }
}
