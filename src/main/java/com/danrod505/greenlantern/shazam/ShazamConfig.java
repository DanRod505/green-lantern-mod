package com.danrod505.greenlantern.shazam;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings of Shazam (config/greenlantern-shazam.toml): the Shazam's Magic and what each power costs. */
public final class ShazamConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CAPACITY;
    public static final ForgeConfigSpec.IntValue RECHARGE_PER_SECOND;
    public static final ForgeConfigSpec.IntValue ZEUS_LIGHTNING_COST;
    public static final ForgeConfigSpec.IntValue THUNDER_CALL_COST;
    public static final ForgeConfigSpec.IntValue MERCURY_DASH_COST;
    public static final ForgeConfigSpec.IntValue HERCULES_SLAM_COST;
    public static final ForgeConfigSpec.IntValue ACHILLES_ROAR_COST;
    public static final ForgeConfigSpec.IntValue ATLAS_STAMINA_COST;
    public static final ForgeConfigSpec.IntValue SOLOMON_WISDOM_COST;
    public static final ForgeConfigSpec.IntValue STORM_RECHARGE_PER_SECOND;
    public static final ForgeConfigSpec.IntValue TRANSFORM_RECHARGE;
    public static final ForgeConfigSpec.IntValue TRANSFORM_RECHARGE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue TRANSFORM_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ZEUS_DAMAGE;
    public static final ForgeConfigSpec.IntValue ZEUS_JUMPS;
    public static final ForgeConfigSpec.DoubleValue ZEUS_JUMP_RANGE;
    public static final ForgeConfigSpec.DoubleValue THUNDER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RADIUS;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE;
    public static final ForgeConfigSpec.DoubleValue DASH_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue DASH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SLAM_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SLAM_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ROAR_RANGE;
    public static final ForgeConfigSpec.DoubleValue ROAR_STUN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue ATLAS_DAMAGE_TAKEN;
    public static final ForgeConfigSpec.DoubleValue SOLOMON_RANGE;
    public static final ForgeConfigSpec.DoubleValue CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue SECONDS_TO_SOUND_BARRIER;
    public static final ForgeConfigSpec.DoubleValue LANDING_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue FLIGHT_COST_PER_SECOND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("shazam");
        CAPACITY = builder.comment("How much Shazam's Magic the Wizard's Bolt holds")
                .defineInRange("capacity", 1000, 1, 1_000_000);
        RECHARGE_PER_SECOND = builder.comment("Shazam's Magic that comes back every second")
                .defineInRange("recharge_per_second", 4, 0, 1_000_000);
        ZEUS_LIGHTNING_COST = builder.comment("Zeus's Lightning: Shazam's Magic it costs")
                .defineInRange("zeus_lightning_cost", 35, 0, 1_000_000);
        THUNDER_CALL_COST = builder.comment("Thunder Call: Shazam's Magic it costs")
                .defineInRange("thunder_call_cost", 90, 0, 1_000_000);
        MERCURY_DASH_COST = builder.comment("Speed of Mercury: Shazam's Magic it costs")
                .defineInRange("mercury_dash_cost", 60, 0, 1_000_000);
        HERCULES_SLAM_COST = builder.comment("Strength of Hercules: Shazam's Magic it costs")
                .defineInRange("hercules_slam_cost", 70, 0, 1_000_000);
        ACHILLES_ROAR_COST = builder.comment("Courage of Achilles: Shazam's Magic it costs")
                .defineInRange("achilles_roar_cost", 80, 0, 1_000_000);
        ATLAS_STAMINA_COST = builder.comment("Stamina of Atlas: Shazam's Magic it costs")
                .defineInRange("atlas_stamina_cost", 8, 0, 1_000_000);
        SOLOMON_WISDOM_COST = builder.comment("Wisdom of Solomon: Shazam's Magic it costs")
                .defineInRange("solomon_wisdom_cost", 6, 0, 1_000_000);
        STORM_RECHARGE_PER_SECOND = builder.comment("Shazam's Magic that comes back every second during a thunderstorm")
                .defineInRange("storm_recharge_per_second", 12, 0, 1_000_000);
        TRANSFORM_RECHARGE = builder.comment("Shazam's Magic the lightning of each \"Shazam!\" brings back")
                .defineInRange("transform_recharge", 300, 0, 1_000_000);
        TRANSFORM_RECHARGE_COOLDOWN_SECONDS = builder.comment("Seconds before another \"Shazam!\" brings magic back again")
                .defineInRange("transform_recharge_cooldown_seconds", 10, 0, 3600);
        TRANSFORM_DAMAGE = builder.comment("Damage of the transformation's lightning to enemies right next to him")
                .defineInRange("transform_damage", 6.0, 0.0, 1000.0);
        ZEUS_DAMAGE = builder.comment("Zeus's Lightning: damage to the first enemy (each jump does 3/4 of the one before)")
                .defineInRange("zeus_damage", 8.0, 0.0, 1000.0);
        ZEUS_JUMPS = builder.comment("Zeus's Lightning: how many more enemies the bolt jumps to")
                .defineInRange("zeus_jumps", 2, 0, 16);
        ZEUS_JUMP_RANGE = builder.comment("Zeus's Lightning: how far (blocks) the bolt jumps")
                .defineInRange("zeus_jump_range", 8.0, 1.0, 64.0);
        THUNDER_DAMAGE = builder.comment("Thunder Call: damage where the lightning strikes")
                .defineInRange("thunder_damage", 12.0, 0.0, 1000.0);
        THUNDER_RADIUS = builder.comment("Thunder Call: radius (blocks) of the strike")
                .defineInRange("thunder_radius", 3.5, 0.5, 16.0);
        THUNDER_RANGE = builder.comment("Thunder Call: how far (blocks) he can call the lightning")
                .defineInRange("thunder_range", 48.0, 4.0, 128.0);
        DASH_DISTANCE = builder.comment("Speed of Mercury: blocks of the dash")
                .defineInRange("dash_distance", 14.0, 2.0, 64.0);
        DASH_DAMAGE = builder.comment("Speed of Mercury: damage to each enemy rammed")
                .defineInRange("dash_damage", 7.0, 0.0, 1000.0);
        SLAM_DAMAGE = builder.comment("Strength of Hercules: damage right next to him (less further away)")
                .defineInRange("slam_damage", 10.0, 0.0, 1000.0);
        SLAM_RADIUS = builder.comment("Strength of Hercules: radius (blocks) of the shockwave")
                .defineInRange("slam_radius", 6.0, 1.0, 32.0);
        ROAR_RANGE = builder.comment("Courage of Achilles: reach (blocks) of the battle cry")
                .defineInRange("roar_range", 10.0, 1.0, 32.0);
        ROAR_STUN_SECONDS = builder.comment("Courage of Achilles: seconds the enemies stay stunned")
                .defineInRange("roar_stun_seconds", 4.0, 0.0, 60.0);
        ATLAS_DAMAGE_TAKEN = builder.comment("Stamina of Atlas: share of the damage he still takes while it is on")
                .defineInRange("atlas_damage_taken", 0.5, 0.0, 1.0);
        SOLOMON_RANGE = builder.comment("Wisdom of Solomon: radius (blocks) in which hostile creatures glow")
                .defineInRange("solomon_range", 32.0, 4.0, 128.0);
        CRUISE_SPEED = builder.comment("Speed (blocks/tick) when his flight starts")
                .defineInRange("cruise_speed", 1.0, 0.2, 5.0);
        MAX_SPEED = builder.comment("Top flying speed (blocks/tick): above the Lantern's (4.0), below Supergirl's (5.5) and Superman's (7.5)")
                .defineInRange("max_speed", 4.8, 0.5, 9.0);
        SECONDS_TO_SOUND_BARRIER = builder.comment("Seconds of acceleration to break the sound barrier")
                .defineInRange("seconds_to_sound_barrier", 1.3, 0.2, 60.0);
        LANDING_MULTIPLIER = builder.comment("Hero landing: damage and radius multiplier over the Lantern's")
                .defineInRange("hero_landing_multiplier", 1.4, 0.0, 10.0);
        FLIGHT_COST_PER_SECOND = builder.comment("Shazam's Magic his flight drains per second (more above the sound barrier)")
                .defineInRange("flight_cost_per_second", 2, 0, 1_000_000);
        builder.pop();
        SPEC = builder.build();
    }

    private ShazamConfig() {}
}
