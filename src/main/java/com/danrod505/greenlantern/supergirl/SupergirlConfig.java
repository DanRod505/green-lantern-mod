package com.danrod505.greenlantern.supergirl;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings of Supergirl (config/greenlantern-supergirl.toml): the Supergirl's Solar Energy and what each power costs. */
public final class SupergirlConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CAPACITY;
    public static final ForgeConfigSpec.IntValue RECHARGE_PER_SECOND;
    public static final ForgeConfigSpec.IntValue HEAT_BOLTS_COST;
    public static final ForgeConfigSpec.IntValue METEOR_DASH_COST;
    public static final ForgeConfigSpec.IntValue THUNDER_CLAP_COST;
    public static final ForgeConfigSpec.IntValue FROST_WALL_COST;
    public static final ForgeConfigSpec.IntValue SUPER_HEARING_COST;
    public static final ForgeConfigSpec.IntValue KRYPTONIAN_THROW_COST;
    public static final ForgeConfigSpec.IntValue SOLAR_FLARE_COST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("supergirl");
        CAPACITY = builder.comment("How much Supergirl's Solar Energy the Argo Pendant holds")
                .defineInRange("capacity", 900, 1, 1_000_000);
        RECHARGE_PER_SECOND = builder.comment("Supergirl's Solar Energy that comes back every second")
                .defineInRange("recharge_per_second", 7, 0, 1_000_000);
        HEAT_BOLTS_COST = builder.comment("Heat Bolts: Supergirl's Solar Energy it costs")
                .defineInRange("heat_bolts_cost", 35, 0, 1_000_000);
        METEOR_DASH_COST = builder.comment("Meteor Dash: Supergirl's Solar Energy it costs")
                .defineInRange("meteor_dash_cost", 70, 0, 1_000_000);
        THUNDER_CLAP_COST = builder.comment("Thunder Clap: Supergirl's Solar Energy it costs")
                .defineInRange("thunder_clap_cost", 80, 0, 1_000_000);
        FROST_WALL_COST = builder.comment("Frost Wall: Supergirl's Solar Energy it costs")
                .defineInRange("frost_wall_cost", 60, 0, 1_000_000);
        SUPER_HEARING_COST = builder.comment("Super Hearing: Supergirl's Solar Energy it costs")
                .defineInRange("super_hearing_cost", 10, 0, 1_000_000);
        KRYPTONIAN_THROW_COST = builder.comment("Kryptonian Throw: Supergirl's Solar Energy it costs")
                .defineInRange("kryptonian_throw_cost", 50, 0, 1_000_000);
        SOLAR_FLARE_COST = builder.comment("Solar Flare: Supergirl's Solar Energy it costs")
                .defineInRange("solar_flare_cost", 400, 0, 1_000_000);
        builder.pop();
        SPEC = builder.build();
    }

    private SupergirlConfig() {}
}
