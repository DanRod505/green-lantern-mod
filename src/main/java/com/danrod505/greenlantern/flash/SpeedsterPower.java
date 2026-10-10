package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of the Flash, picked on the power wheel (hold the wheel key while wearing the suit) and
 * used with right click on the Flash ring or the power key.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/flash_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link SpeedsterServer#usePower}.
 */
public enum SpeedsterPower implements HeroPower {
    /** Run in circles to create a vortex that lifts and batters everything around it. */
    TORNADO("tornado"),
    /** Vibrate your molecules to pass through solid matter. */
    PHASE("phase"),
    /** Throw a bolt of Speed Force lightning. */
    LIGHTNING("lightning");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<SpeedsterPower> POWERS = new PowerSet<>(values());

    private final String id;

    SpeedsterPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    /** Speed Force needed to activate the power. */
    @Override
    public int cost() {
        return switch (this) {
            case TORNADO -> GLConfig.TORNADO_COST.get();
            case PHASE -> GLConfig.PHASE_COST.get();
            case LIGHTNING -> GLConfig.LIGHTNING_COST.get();
        };
    }
}
