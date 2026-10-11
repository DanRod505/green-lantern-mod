package com.danrod505.greenlantern.cyborg;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings of Cyborg (config/greenlantern-cyborg.toml): the Cyborg Battery and what each power costs. */
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
        builder.pop();
        SPEC = builder.build();
    }

    private CyborgConfig() {}
}
