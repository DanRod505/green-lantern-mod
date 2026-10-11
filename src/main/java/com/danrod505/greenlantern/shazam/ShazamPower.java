package com.danrod505.greenlantern.shazam;

import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of Shazam, in wheel order (hold the wheel key to pick, the power key or right click on
 * the Wizard's Bolt to use).
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/shazam_powers.png} at the
 * same index, before the emblem), its cost to {@link ShazamConfig}, its name and description to
 * {@code src/main/heroes/shazam/lang} and its behaviour to {@link ShazamServer#usePower}.
 */
public enum ShazamPower implements HeroPower {
    /** Zeus's Lightning: Fires a golden bolt from his hand that flies to the first enemy, chases it if it dodges and jumps to two more enemies nearby. */
    ZEUS_LIGHTNING("zeus_lightning"),
    /** Thunder Call: He raises his arm and lightning strikes from the sky where he aims, hurting and flinging the enemies around and shattering glass. Works even underground. */
    THUNDER_CALL("thunder_call"),
    /** Speed of Mercury: Dashes 14 blocks in a straight line in a red and gold blur, on the ground or in the air, ramming each enemy once and stopping at walls. */
    MERCURY_DASH("mercury_dash"),
    /** Strength of Hercules: He punches the ground and sends out a shockwave around him that throws enemies away and raises dust. */
    HERCULES_SLAM("hercules_slam"),
    /** Courage of Achilles: A battle cry in a cone that stuns the enemies ahead and puts out fire and torches. */
    ACHILLES_ROAR("achilles_roar"),
    /** Stamina of Atlas: While on, he is as heavy as a mountain: he can't be knocked back, takes half damage and doesn't get hungry. Costs per second. */
    ATLAS_STAMINA("atlas_stamina"),
    /** Wisdom of Solomon: While on, makes every hostile creature within 32 blocks glow, even behind walls, and shows the health of whoever he aims at on the HUD. Costs per second. */
    SOLOMON_WISDOM("solomon_wisdom");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<ShazamPower> POWERS = new PowerSet<>(values());

    private final String id;

    ShazamPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int cost() {
        return switch (this) {
            case ZEUS_LIGHTNING -> ShazamConfig.ZEUS_LIGHTNING_COST.get();
            case THUNDER_CALL -> ShazamConfig.THUNDER_CALL_COST.get();
            case MERCURY_DASH -> ShazamConfig.MERCURY_DASH_COST.get();
            case HERCULES_SLAM -> ShazamConfig.HERCULES_SLAM_COST.get();
            case ACHILLES_ROAR -> ShazamConfig.ACHILLES_ROAR_COST.get();
            case ATLAS_STAMINA -> ShazamConfig.ATLAS_STAMINA_COST.get();
            case SOLOMON_WISDOM -> ShazamConfig.SOLOMON_WISDOM_COST.get();
        };
    }
}
