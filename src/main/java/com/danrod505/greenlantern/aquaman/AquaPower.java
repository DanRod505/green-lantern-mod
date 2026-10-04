package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Powers of Aquaman, picked on the power wheel (hold the wheel key) and used with the power key or
 * right click on the Atlantean Emblem.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/aquaman_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link AquamanServer#usePower}.
 */
public enum AquaPower {
    /** The trident of Atlantis appears in your hand: a heavy weapon that can be thrown and comes back. */
    TRIDENT("trident"),
    /** A great white shark answers: ride it through the water, it bites your enemies. */
    SHARK("shark"),
    /** The creatures of the sea nearby follow and defend you for a while. */
    SEA_CALL("sea_call");

    private static final AquaPower[] VALUES = values();

    private final String id;

    AquaPower(String id) {
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

    /** Power of the Seas needed to activate the power. */
    public int cost() {
        return switch (this) {
            case TRIDENT -> GLConfig.TRIDENT_COST.get();
            case SHARK -> GLConfig.SHARK_COST.get();
            case SEA_CALL -> GLConfig.SEA_CALL_COST.get();
        };
    }

    public static AquaPower byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, VALUES.length - 1)];
    }

    public static int count() {
        return VALUES.length;
    }

    public static AquaPower selected(ItemStack emblem) {
        Integer index = emblem.get(ModDataComponents.SELECTED_POWER.get());
        return byIndex(index == null ? 0 : index);
    }

    public static void select(ItemStack emblem, AquaPower power) {
        emblem.set(ModDataComponents.SELECTED_POWER.get(), power.ordinal());
    }

    public static AquaPower cycle(ItemStack emblem, int offset) {
        AquaPower next = VALUES[Math.floorMod(selected(emblem).ordinal() + offset, VALUES.length)];
        select(emblem, next);
        return next;
    }
}
