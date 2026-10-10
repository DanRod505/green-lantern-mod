package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of Superman, picked on the power wheel (hold the wheel key) and used with the power key or
 * right click on the Kryptonian Crystal.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/superman_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link SupermanServer#usePower}.
 */
public enum SuperPower implements HeroPower {
    /** Twin red beams from the eyes: they burn whatever they touch. */
    HEAT_VISION("heat_vision"),
    /** A punch that shakes the ground: a shockwave that hurts and throws everything around. */
    SUPER_PUNCH("super_punch"),
    /** A freezing gale: it pushes back, chills and hurts, puts out fires and freezes water. */
    SUPER_BREATH("super_breath"),
    /** Sees through walls and the ground: creatures, ores and chests light up. */
    XRAY_VISION("xray_vision");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<SuperPower> POWERS = new PowerSet<>(values());

    private final String id;

    SuperPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    /** Solar energy needed to use the power (per second for the continuous ones). */
    @Override
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
}
