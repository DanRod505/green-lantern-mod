package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Gadgets of Batman, picked on the power wheel (hold the wheel key) and used with the power key or
 * right click on the Utility Belt.
 * <p>
 * To add a gadget: add a constant here (its icon goes in {@code textures/gui/batman_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link BatmanServer#usePower}.
 */
public enum BatPower implements HeroPower {
    /** A batarang flies from the hand, hits whatever it meets and curves back to the belt. */
    BATARANG("batarang"),
    /** The grapnel gun: the hook bites into a wall or a ledge and the cable reels Batman up to it. */
    GRAPPLE("grapple"),
    /** A cloud of bats pours out of the dark and defends Batman for a while. */
    BAT_SWARM("bat_swarm"),
    /** The Batmobile roars up: drive it, boost with sprint and fire missiles with left click. */
    BATMOBILE("batmobile");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<BatPower> POWERS = new PowerSet<>(values());

    private final String id;

    BatPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    /** Belt charge needed to use the gadget. */
    @Override
    public int cost() {
        return switch (this) {
            case BATARANG -> GLConfig.BATARANG_COST.get();
            case GRAPPLE -> GLConfig.GRAPPLE_COST.get();
            case BAT_SWARM -> GLConfig.BAT_SWARM_COST.get();
            case BATMOBILE -> GLConfig.BATMOBILE_COST.get();
        };
    }
}
