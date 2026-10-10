package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Powers of Superman, picked on the power wheel (hold the wheel key) and used with the power key or
 * right click on the Kryptonian Crystal.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/superman_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link SupermanServer#usePower}.
 */
public enum SuperPower {
    /** Twin red beams from the eyes: they burn whatever they touch. */
    HEAT_VISION("heat_vision"),
    /** A punch that shakes the ground: a shockwave that hurts and throws everything around. */
    SUPER_PUNCH("super_punch"),
    /** A freezing gale: it pushes back, chills and hurts, puts out fires and freezes water. */
    SUPER_BREATH("super_breath"),
    /** Sees through walls and the ground: creatures, ores and chests light up. */
    XRAY_VISION("xray_vision");

    private static final SuperPower[] VALUES = values();

    private final String id;

    SuperPower(String id) {
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

    /** Solar energy needed to use the power (per second for the continuous ones). */
    public int cost() {
        return switch (this) {
            case HEAT_VISION -> GLConfig.HEAT_VISION_COST_PER_SECOND.get();
            case SUPER_PUNCH -> GLConfig.SUPER_PUNCH_COST.get();
            case SUPER_BREATH -> GLConfig.SUPER_BREATH_COST_PER_SECOND.get();
            case XRAY_VISION -> GLConfig.XRAY_COST_PER_SECOND.get();
        };
    }

    /** Whether the cost is paid every second (heat vision, breath, X-ray) or once (the punch). */
    public boolean continuous() {
        return this != SUPER_PUNCH;
    }

    public static SuperPower byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, VALUES.length - 1)];
    }

    public static int count() {
        return VALUES.length;
    }

    public static SuperPower selected(ItemStack crystal) {
        Integer index = crystal.get(ModDataComponents.SELECTED_POWER.get());
        return byIndex(index == null ? 0 : index);
    }

    public static void select(ItemStack crystal, SuperPower power) {
        crystal.set(ModDataComponents.SELECTED_POWER.get(), power.ordinal());
    }

    public static SuperPower cycle(ItemStack crystal, int offset) {
        SuperPower next = VALUES[Math.floorMod(selected(crystal).ordinal() + offset, VALUES.length)];
        select(crystal, next);
        return next;
    }
}
