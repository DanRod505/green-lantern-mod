package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of Cyborg, in wheel order (hold the wheel key to pick, the power key or right click on
 * the Mother Box to use).
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/cyborg_powers.png} at the
 * same index, before the emblem), its cost to {@link CyborgConfig}, its name and description to
 * {@code src/main/heroes/cyborg/lang} and its behaviour to {@link CyborgServer#usePower}.
 */
public enum CyborgPower implements HeroPower {
    /** Sonic Cannon: The arm becomes a cannon and fires a sonic wave in a cone that damages, knocks back enemies and shatters glass and ice. */
    SONIC_CANNON("sonic_cannon"),
    /** Shoulder Missiles: Four micro-rockets launch from the shoulder and chase the nearest enemies, exploding without breaking blocks. */
    SHOULDER_MISSILES("shoulder_missiles"),
    /** Tech Scan: While on, highlights enemies and ores within 24 blocks through walls and shows the health of the target in sight. Costs per second. */
    TECH_SCAN("tech_scan"),
    /** Machine Hack: Hacks the machine in sight: an iron golem becomes an ally for a minute, and doors, trapdoors, pistons and rails obey the touch. */
    MACHINE_HACK("machine_hack"),
    /** EMP Burst: A pulse around him stuns enemies for a few seconds, grounds flying mobs and switches off nearby redstone for a moment. */
    EMP_BURST("emp_burst"),
    /** Self Repair: Nanobots fix body and suit: heals over a few seconds and restores the suit's durability. */
    SELF_REPAIR("self_repair"),
    /** Boom Tube: Opens a tunnel of light that takes Cyborg and anyone near him to the marked point (sneak and use to mark it; without a mark, to the bed or spawn). */
    BOOM_TUBE("boom_tube");

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<CyborgPower> POWERS = new PowerSet<>(values());

    private final String id;

    CyborgPower(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int cost() {
        return switch (this) {
            case SONIC_CANNON -> CyborgConfig.SONIC_CANNON_COST.get();
            case SHOULDER_MISSILES -> CyborgConfig.SHOULDER_MISSILES_COST.get();
            case TECH_SCAN -> CyborgConfig.TECH_SCAN_COST.get();
            case MACHINE_HACK -> CyborgConfig.MACHINE_HACK_COST.get();
            case EMP_BURST -> CyborgConfig.EMP_BURST_COST.get();
            case SELF_REPAIR -> CyborgConfig.SELF_REPAIR_COST.get();
            case BOOM_TUBE -> CyborgConfig.BOOM_TUBE_COST.get();
        };
    }
}
