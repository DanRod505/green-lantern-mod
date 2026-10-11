package com.danrod505.greenlantern.supergirl;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings of Supergirl (config/greenlantern-supergirl.toml): the solar energy, her flight, the powers and Krypto. */
public final class SupergirlConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CAPACITY;
    public static final ForgeConfigSpec.IntValue RECHARGE_PER_SECOND;
    public static final ForgeConfigSpec.IntValue SHADE_RECHARGE_PER_SECOND;
    public static final ForgeConfigSpec.IntValue PUNCH_ENERGY;
    public static final ForgeConfigSpec.IntValue HEAT_BOLTS_COST;
    public static final ForgeConfigSpec.IntValue METEOR_DASH_COST;
    public static final ForgeConfigSpec.IntValue THUNDER_CLAP_COST;
    public static final ForgeConfigSpec.IntValue FROST_WALL_COST;
    public static final ForgeConfigSpec.IntValue SUPER_HEARING_COST;
    public static final ForgeConfigSpec.IntValue KRYPTONIAN_THROW_COST;
    public static final ForgeConfigSpec.IntValue SOLAR_FLARE_COST;
    public static final ForgeConfigSpec.DoubleValue CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue SECONDS_TO_SOUND_BARRIER;
    public static final ForgeConfigSpec.DoubleValue LANDING_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue FLIGHT_COST_PER_SECOND;
    public static final ForgeConfigSpec.IntValue ROLL_COST;
    public static final ForgeConfigSpec.IntValue ROLL_DODGE_TICKS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue HEAT_BOLT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAT_BOLT_RANGE;
    public static final ForgeConfigSpec.BooleanValue HEAT_BOLT_IGNITES;
    public static final ForgeConfigSpec.DoubleValue DASH_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue DASH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue CLAP_RANGE;
    public static final ForgeConfigSpec.DoubleValue CLAP_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue CLAP_STUN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FROST_WALL_SECONDS;
    public static final ForgeConfigSpec.IntValue FROST_WALL_WIDTH;
    public static final ForgeConfigSpec.IntValue FROST_WALL_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue HEARING_RADIUS;
    public static final ForgeConfigSpec.DoubleValue THROW_RANGE;
    public static final ForgeConfigSpec.DoubleValue THROW_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue THROW_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FLARE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue FLARE_MAX_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FLARE_WEAK_SECONDS;
    public static final ForgeConfigSpec.DoubleValue KRYPTO_HEALTH;
    public static final ForgeConfigSpec.DoubleValue KRYPTO_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue KRYPTO_RETURN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue KRYPTO_HEAT_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("supergirl");
        CAPACITY = builder.comment("How much Supergirl's Solar Energy the Argo Pendant holds")
                .defineInRange("capacity", 900, 1, 1_000_000);
        RECHARGE_PER_SECOND = builder.comment("Solar energy that comes back every second under the open sky in daylight")
                .defineInRange("recharge_per_second", 7, 0, 1_000_000);
        SHADE_RECHARGE_PER_SECOND = builder.comment("Solar energy that comes back every second in the shade or at night (none underground, in the Nether or the End)")
                .defineInRange("shade_recharge_per_second", 2, 0, 1_000_000);
        PUNCH_ENERGY = builder.comment("Solar energy every punch that lands gives back")
                .defineInRange("punch_energy", 5, 0, 1_000_000);
        HEAT_BOLTS_COST = builder.comment("Heat Bolts: Supergirl's Solar Energy it costs")
                .defineInRange("heat_bolts_cost", 35, 0, 1_000_000);
        METEOR_DASH_COST = builder.comment("Meteor Dash: Supergirl's Solar Energy it costs")
                .defineInRange("meteor_dash_cost", 70, 0, 1_000_000);
        THUNDER_CLAP_COST = builder.comment("Thunder Clap: Supergirl's Solar Energy it costs")
                .defineInRange("thunder_clap_cost", 80, 0, 1_000_000);
        FROST_WALL_COST = builder.comment("Frost Wall: Supergirl's Solar Energy it costs")
                .defineInRange("frost_wall_cost", 60, 0, 1_000_000);
        SUPER_HEARING_COST = builder.comment("Super Hearing: Supergirl's Solar Energy it costs per second while on")
                .defineInRange("super_hearing_cost", 10, 0, 1_000_000);
        KRYPTONIAN_THROW_COST = builder.comment("Kryptonian Throw: Supergirl's Solar Energy grabbing costs (the throw is free)")
                .defineInRange("kryptonian_throw_cost", 50, 0, 1_000_000);
        SOLAR_FLARE_COST = builder.comment("Solar Flare: the least energy it needs (it always pours out all she has)")
                .defineInRange("solar_flare_cost", 400, 0, 1_000_000);
        CRUISE_SPEED = builder.comment("Speed (blocks/tick) when her flight starts")
                .defineInRange("cruise_speed", 1.0, 0.2, 5.0);
        MAX_SPEED = builder.comment("Top flying speed (blocks/tick): above the Lantern's and the sound barrier, below Superman's")
                .defineInRange("max_speed", 5.5, 0.5, 9.0);
        SECONDS_TO_SOUND_BARRIER = builder.comment("Seconds of acceleration to break the sound barrier (Superman needs 1.6)")
                .defineInRange("seconds_to_sound_barrier", 1.0, 0.2, 60.0);
        LANDING_MULTIPLIER = builder.comment("Hero landing: damage and radius multiplier over the Lantern's (Superman's is 1.8)")
                .defineInRange("hero_landing_multiplier", 1.2, 0.0, 10.0);
        FLIGHT_COST_PER_SECOND = builder.comment("Solar energy her flight drains per second (more above the sound barrier)")
                .defineInRange("flight_cost_per_second", 2, 0, 1_000_000);
        ROLL_COST = builder.comment("Solar energy a barrel roll costs (double tap left or right while flying)")
                .defineInRange("roll_cost", 20, 0, 1_000_000);
        ROLL_DODGE_TICKS = builder.comment("Ticks after a barrel roll during which projectiles miss her")
                .defineInRange("roll_dodge_ticks", 10, 0, 200);
        DAMAGE_REDUCTION = builder.comment("Fraction of the damage she shrugs off while she has solar energy (Superman shrugs off 0.6)")
                .defineInRange("damage_reduction", 0.4, 0.0, 1.0);
        HEAT_BOLT_DAMAGE = builder.comment("Heat Bolts: damage of each bolt (it also sets the target on fire)")
                .defineInRange("heat_bolt_damage", 7.0, 0.0, 1000.0);
        HEAT_BOLT_RANGE = builder.comment("Heat Bolts: how far a bolt flies (blocks)")
                .defineInRange("heat_bolt_range", 48.0, 4.0, 128.0);
        HEAT_BOLT_IGNITES = builder.comment("Heat Bolts: light fires where they hit (never breaks blocks)")
                .define("heat_bolt_ignites", true);
        DASH_DISTANCE = builder.comment("Meteor Dash: how far she goes (blocks)")
                .defineInRange("dash_distance", 12.0, 2.0, 48.0);
        DASH_DAMAGE = builder.comment("Meteor Dash: damage to whatever is in the way")
                .defineInRange("dash_damage", 10.0, 0.0, 1000.0);
        CLAP_RANGE = builder.comment("Thunder Clap: length of the cone (blocks)")
                .defineInRange("clap_range", 10.0, 2.0, 48.0);
        CLAP_DAMAGE = builder.comment("Thunder Clap: damage up close")
                .defineInRange("clap_damage", 4.0, 0.0, 1000.0);
        CLAP_STUN_SECONDS = builder.comment("Thunder Clap: how long enemies stay stunned (seconds)")
                .defineInRange("clap_stun_seconds", 3.0, 0.0, 60.0);
        FROST_WALL_SECONDS = builder.comment("Frost Wall: seconds before the wall melts")
                .defineInRange("frost_wall_seconds", 30.0, 1.0, 3600.0);
        FROST_WALL_WIDTH = builder.comment("Frost Wall: width of the wall (blocks)")
                .defineInRange("frost_wall_width", 5, 1, 15);
        FROST_WALL_HEIGHT = builder.comment("Frost Wall: height of the wall (blocks)")
                .defineInRange("frost_wall_height", 3, 1, 8);
        HEARING_RADIUS = builder.comment("Super Hearing: how far she hears creatures (blocks)")
                .defineInRange("hearing_radius", 40.0, 4.0, 64.0);
        THROW_RANGE = builder.comment("Kryptonian Throw: how far she reaches to grab (blocks)")
                .defineInRange("throw_range", 8.0, 2.0, 32.0);
        THROW_STRENGTH = builder.comment("Kryptonian Throw: speed of the throw (blocks/tick)")
                .defineInRange("throw_strength", 2.6, 0.5, 8.0);
        THROW_DAMAGE = builder.comment("Kryptonian Throw: damage to the thrown creature and to whatever it crashes into")
                .defineInRange("throw_damage", 8.0, 0.0, 1000.0);
        FLARE_RADIUS = builder.comment("Solar Flare: radius (blocks)")
                .defineInRange("flare_radius", 10.0, 2.0, 32.0);
        FLARE_MAX_DAMAGE = builder.comment("Solar Flare: damage up close with a full pendant (less with less energy)")
                .defineInRange("flare_max_damage", 36.0, 0.0, 1000.0);
        FLARE_WEAK_SECONDS = builder.comment("Solar Flare: seconds without powers, flight or recharge afterwards")
                .defineInRange("flare_weak_seconds", 20.0, 0.0, 600.0);
        KRYPTO_HEALTH = builder.comment("Krypto: health (2 = one heart)")
                .defineInRange("krypto_health", 40.0, 1.0, 1000.0);
        KRYPTO_DAMAGE = builder.comment("Krypto: damage of a bite")
                .defineInRange("krypto_damage", 8.0, 0.0, 1000.0);
        KRYPTO_RETURN_SECONDS = builder.comment("Krypto: seconds he takes to come back after his health runs out")
                .defineInRange("krypto_return_seconds", 60.0, 0.0, 3600.0);
        KRYPTO_HEAT_SECONDS = builder.comment("Krypto: seconds between his heat vision blasts in a fight")
                .defineInRange("krypto_heat_seconds", 5.0, 0.5, 600.0);
        builder.pop();
        SPEC = builder.build();
    }

    private SupergirlConfig() {}
}
