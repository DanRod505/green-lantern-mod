package com.danrod505.greenlantern.cyborg;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings of Cyborg (config/greenlantern-cyborg.toml): the Cyborg Battery, the thrusters and the powers. */
public final class CyborgConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CAPACITY;
    public static final ForgeConfigSpec.IntValue RECHARGE_PER_SECOND;
    public static final ForgeConfigSpec.IntValue SONIC_CANNON_COST;
    public static final ForgeConfigSpec.IntValue SHOULDER_MISSILES_COST;
    public static final ForgeConfigSpec.IntValue TECH_SCAN_COST;
    public static final ForgeConfigSpec.IntValue MACHINE_HACK_COST;
    public static final ForgeConfigSpec.IntValue EMP_BURST_COST;
    public static final ForgeConfigSpec.IntValue SELF_REPAIR_COST;
    public static final ForgeConfigSpec.IntValue BOOM_TUBE_COST;
    public static final ForgeConfigSpec.DoubleValue REDSTONE_RECHARGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue SECONDS_TO_MAX;
    public static final ForgeConfigSpec.IntValue FLIGHT_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SONIC_CANNON_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SONIC_CANNON_RANGE;
    public static final ForgeConfigSpec.BooleanValue SONIC_CANNON_SHATTERS;
    public static final ForgeConfigSpec.IntValue MISSILE_COUNT;
    public static final ForgeConfigSpec.DoubleValue MISSILE_POWER;
    public static final ForgeConfigSpec.DoubleValue SCAN_RADIUS;
    public static final ForgeConfigSpec.DoubleValue HACK_RANGE;
    public static final ForgeConfigSpec.DoubleValue HACK_ALLY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue EMP_RADIUS;
    public static final ForgeConfigSpec.DoubleValue EMP_STUN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue REPAIR_HEALTH;
    public static final ForgeConfigSpec.DoubleValue BOOM_TUBE_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("cyborg");
        CAPACITY = builder.comment("How much Cyborg Battery the Mother Box holds")
                .defineInRange("capacity", 1200, 1, 1_000_000);
        RECHARGE_PER_SECOND = builder.comment("Cyborg Battery that comes back every second")
                .defineInRange("recharge_per_second", 6, 0, 1_000_000);
        SONIC_CANNON_COST = builder.comment("Sonic Cannon: Cyborg Battery it costs")
                .defineInRange("sonic_cannon_cost", 60, 0, 1_000_000);
        SHOULDER_MISSILES_COST = builder.comment("Shoulder Missiles: Cyborg Battery it costs")
                .defineInRange("shoulder_missiles_cost", 90, 0, 1_000_000);
        TECH_SCAN_COST = builder.comment("Tech Scan: Cyborg Battery it costs")
                .defineInRange("tech_scan_cost", 15, 0, 1_000_000);
        MACHINE_HACK_COST = builder.comment("Machine Hack: Cyborg Battery it costs")
                .defineInRange("machine_hack_cost", 80, 0, 1_000_000);
        EMP_BURST_COST = builder.comment("EMP Burst: Cyborg Battery it costs")
                .defineInRange("emp_burst_cost", 120, 0, 1_000_000);
        SELF_REPAIR_COST = builder.comment("Self Repair: Cyborg Battery it costs")
                .defineInRange("self_repair_cost", 100, 0, 1_000_000);
        BOOM_TUBE_COST = builder.comment("Boom Tube: Cyborg Battery it costs")
                .defineInRange("boom_tube_cost", 250, 0, 1_000_000);
        REDSTONE_RECHARGE_MULTIPLIER = builder.comment("The battery recharges this many times faster near powered redstone (within 4 blocks)")
                .defineInRange("redstone_recharge_multiplier", 2.0, 1.0, 100.0);
        CRUISE_SPEED = builder.comment("Speed (blocks/tick) when the thrusters start")
                .defineInRange("cruise_speed", 0.7, 0.2, 5.0);
        MAX_SPEED = builder.comment("Top flying speed (blocks/tick): between Wonder Woman's and Superman's, below the sound barrier by default")
                .defineInRange("max_speed", 2.5, 0.5, 9.0);
        SECONDS_TO_MAX = builder.comment("Seconds of acceleration (holding forward) to reach top speed")
                .defineInRange("seconds_to_top_speed", 5.0, 0.5, 60.0);
        FLIGHT_COST_PER_SECOND = builder.comment("Cyborg Battery the thrusters burn per second of flight (hovering is free)")
                .defineInRange("flight_cost_per_second", 4, 0, 1_000_000);
        DAMAGE_REDUCTION = builder.comment("Fraction of the damage the armored body shrugs off (0-1)")
                .defineInRange("damage_reduction", 0.3, 0.0, 1.0);
        SONIC_CANNON_DAMAGE = builder.comment("Sonic Cannon: damage up close (less at the end of the cone)")
                .defineInRange("sonic_cannon_damage", 10.0, 0.0, 1000.0);
        SONIC_CANNON_RANGE = builder.comment("Sonic Cannon: length of the cone (blocks)")
                .defineInRange("sonic_cannon_range", 14.0, 2.0, 64.0);
        SONIC_CANNON_SHATTERS = builder.comment("Sonic Cannon: shatters glass and ice in the cone")
                .define("sonic_cannon_shatters", true);
        MISSILE_COUNT = builder.comment("Shoulder Missiles: rockets per launch")
                .defineInRange("missile_count", 4, 1, 16);
        MISSILE_POWER = builder.comment("Shoulder Missiles: explosion power of each rocket (never breaks blocks)")
                .defineInRange("missile_power", 1.6, 0.1, 6.0);
        SCAN_RADIUS = builder.comment("Tech Scan: how far enemies and ores are seen (blocks)")
                .defineInRange("scan_radius", 24.0, 4.0, 64.0);
        HACK_RANGE = builder.comment("Machine Hack: how far the hack reaches (blocks)")
                .defineInRange("hack_range", 16.0, 2.0, 64.0);
        HACK_ALLY_SECONDS = builder.comment("Machine Hack: how long a hacked iron golem fights for Cyborg (seconds)")
                .defineInRange("hack_ally_seconds", 60.0, 1.0, 3600.0);
        EMP_RADIUS = builder.comment("EMP Burst: radius (blocks)")
                .defineInRange("emp_radius", 9.0, 2.0, 32.0);
        EMP_STUN_SECONDS = builder.comment("EMP Burst: how long enemies stay stunned (seconds)")
                .defineInRange("emp_stun_seconds", 4.0, 0.5, 60.0);
        REPAIR_HEALTH = builder.comment("Self Repair: health restored over the repair (2 = one heart)")
                .defineInRange("repair_health", 12.0, 0.0, 1000.0);
        BOOM_TUBE_RADIUS = builder.comment("Boom Tube: everyone this close to Cyborg goes through with him (blocks)")
                .defineInRange("boom_tube_radius", 4.0, 0.0, 16.0);
        builder.pop();
        SPEC = builder.build();
    }

    private CyborgConfig() {}
}
