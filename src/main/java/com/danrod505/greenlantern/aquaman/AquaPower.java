package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of Aquaman, picked on the power wheel (hold the wheel key) and used with the power key or
 * right click on the Atlantean Emblem.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/aquaman_powers.png} at
 * the same index), its name / description to the lang files and its behaviour to
 * {@link AquamanServer#usePower}.
 */
public enum AquaPower implements HeroPower {
    /** The trident of Atlantis appears in your hand: a heavy weapon that can be thrown and comes back. */
    TRIDENT("trident"),
    /** A great white shark answers: ride it through the water, it bites your enemies. */
    SHARK("shark"),
    /** The creatures of the sea nearby follow and defend you for a while. */
    SEA_CALL("sea_call"),
    /** A whirlpool portal to Atlantis; opened in Atlantis, it leads back to where you came from. */
    ATLANTIS_PORTAL("atlantis_portal"),
    /** A 15 block tall Kraken rises from the deep and carries you on its head, on land and in the sea. */
    KRAKEN("kraken");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<AquaPower> POWERS = new PowerSet<>(values());

    private final String id;

    AquaPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    /** Power of the Seas needed to activate the power. */
    @Override
    public int cost() {
        return switch (this) {
            case TRIDENT -> GLConfig.TRIDENT_COST.get();
            case SHARK -> GLConfig.SHARK_COST.get();
            case SEA_CALL -> GLConfig.SEA_CALL_COST.get();
            case ATLANTIS_PORTAL -> GLConfig.ATLANTIS_PORTAL_COST.get();
            case KRAKEN -> GLConfig.KRAKEN_COST.get();
        };
    }
}
