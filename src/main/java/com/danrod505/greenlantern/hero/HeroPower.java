package com.danrod505.greenlantern.hero;

import net.minecraft.network.chat.Component;

/**
 * A power on a hero's power wheel. Each hero keeps its powers in an enum that implements this; the
 * icon sits at the same index in the hero's wheel icon sheet, and the name and description come
 * from {@code power.greenlantern.<id>} and {@code power.greenlantern.<id>.desc} in the lang files.
 */
public interface HeroPower {
    /** Lang key suffix, e.g. {@code "lasso_capture"}. */
    String id();

    /** Position on the wheel (the enum ordinal). */
    int ordinal();

    /** Energy needed to use the power (per second for continuous ones), from the config. */
    int cost();

    default int iconIndex() {
        return ordinal();
    }

    default Component displayName() {
        return Component.translatable("power.greenlantern." + id());
    }

    default Component description() {
        return Component.translatable("power.greenlantern." + id() + ".desc");
    }
}
