package com.danrod505.greenlantern.supergirl;

import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of Supergirl, in wheel order (hold the wheel key to pick, the power key or right click on
 * the Argo Pendant to use).
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/supergirl_powers.png} at the
 * same index, before the emblem), its cost to {@link SupergirlConfig}, its name and description to
 * {@code src/main/heroes/supergirl/lang} and its behaviour to {@link SupergirlServer#usePower}.
 */
public enum SupergirlPower implements HeroPower {
    /** Heat Bolts: Instead of Superman's steady beam, her eyes fire quick heat bolts, one per click, that burst on the target and light fires without breaking blocks. */
    HEAT_BOLTS("heat_bolts"),
    /** Meteor Dash: She launches herself in a straight line like a comet for 12 blocks, on the ground or in the air, ramming and flinging everything in the way. */
    METEOR_DASH("meteor_dash"),
    /** Thunder Clap: She claps her hands and sends a shockwave in a cone that stuns enemies, snuffs torches and shatters glass. */
    THUNDER_CLAP("thunder_clap"),
    /** Frost Wall: A freezing breath that freezes water, puts out fire and raises a wall of ice in front of her, which melts on its own after 30 seconds. */
    FROST_WALL("frost_wall"),
    /** Super Hearing: While on, shows sound ripples around every creature within 40 blocks (even behind walls) and arrows on the HUD pointing to footsteps. Unlike Superman's X-ray, it shows no ores. Costs per second. */
    SUPER_HEARING("super_hearing"),
    /** Kryptonian Throw: Grabs the creature or loose block in sight, lifts it overhead and hurls it far on the next use. */
    KRYPTONIAN_THROW("kryptonian_throw"),
    /** Solar Flare: Unleashes all her energy at once in a burst of light around her, stronger the more energy is left. Afterwards she has no powers or flight for 20 seconds while she recharges. */
    SOLAR_FLARE("solar_flare");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<SupergirlPower> POWERS = new PowerSet<>(values());

    private final String id;

    SupergirlPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int cost() {
        return switch (this) {
            case HEAT_BOLTS -> SupergirlConfig.HEAT_BOLTS_COST.get();
            case METEOR_DASH -> SupergirlConfig.METEOR_DASH_COST.get();
            case THUNDER_CLAP -> SupergirlConfig.THUNDER_CLAP_COST.get();
            case FROST_WALL -> SupergirlConfig.FROST_WALL_COST.get();
            case SUPER_HEARING -> SupergirlConfig.SUPER_HEARING_COST.get();
            case KRYPTONIAN_THROW -> SupergirlConfig.KRYPTONIAN_THROW_COST.get();
            case SOLAR_FLARE -> SupergirlConfig.SOLAR_FLARE_COST.get();
        };
    }
}
