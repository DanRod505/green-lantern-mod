package com.danrod505.greenlantern;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common configuration (config/greenlantern-common.toml). All balance numbers live here so they
 * can be tuned without recompiling the mod.
 */
public final class GLConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---- Ring / energy ------------------------------------------------------------------------
    public static final ForgeConfigSpec.IntValue MAX_ENERGY;
    public static final ForgeConfigSpec.IntValue CHARGE_PER_TICK;
    public static final ForgeConfigSpec.IntValue FLIGHT_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue FLIGHT_SPEED;
    public static final ForgeConfigSpec.BooleanValue SHOW_OATH;

    // ---- Power flight ---------------------------------------------------------------------------
    public static final ForgeConfigSpec.DoubleValue CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue SOUND_BARRIER_SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_FLIGHT_SPEED;
    public static final ForgeConfigSpec.DoubleValue SECONDS_TO_SOUND_BARRIER;
    public static final ForgeConfigSpec.DoubleValue SUPERSONIC_COST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HERO_LANDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HERO_LANDING_RADIUS;

    // ---- Constructs ---------------------------------------------------------------------------
    public static final ForgeConfigSpec.IntValue BLAST_COST;
    public static final ForgeConfigSpec.DoubleValue BLAST_DAMAGE;
    public static final ForgeConfigSpec.IntValue MINIGUN_COST_PER_SHOT;
    public static final ForgeConfigSpec.DoubleValue MINIGUN_DAMAGE;
    public static final ForgeConfigSpec.IntValue BUBBLE_COST;
    public static final ForgeConfigSpec.IntValue BUBBLE_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue BUBBLE_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.IntValue SAW_COST;
    public static final ForgeConfigSpec.IntValue SAW_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SAW_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue SAW_CUTS_PLANTS;
    public static final ForgeConfigSpec.IntValue DRILL_COST;
    public static final ForgeConfigSpec.IntValue DRILL_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue DRILL_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue DRILL_DIGS_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue DRILL_SPEED;
    public static final ForgeConfigSpec.DoubleValue DRILL_MAX_HARDNESS;
    public static final ForgeConfigSpec.BooleanValue DRILL_COLLECTS_DROPS;
    public static final ForgeConfigSpec.IntValue HAMMER_COST;
    public static final ForgeConfigSpec.DoubleValue HAMMER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HAMMER_RADIUS;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("ring");
        MAX_ENERGY = BUILDER.comment("Maximum energy stored in a Power Ring.")
                .defineInRange("ringCapacity", 5000, 100, 1_000_000);
        CHARGE_PER_TICK = BUILDER.comment("Energy restored per tick while charging at a Power Battery.")
                .defineInRange("chargeRate", 30, 1, 100_000);
        FLIGHT_COST_PER_SECOND = BUILDER.comment("Energy drained per second while flying.")
                .defineInRange("flightCostPerSecond", 2, 0, 10_000);
        FLIGHT_SPEED = BUILDER.comment("Flying speed while wearing the uniform (vanilla creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.1, 0.01, 1.0);
        SHOW_OATH = BUILDER.comment("Show the Green Lantern oath while recharging the ring.")
                .define("showOath", true);
        BUILDER.pop();

        BUILDER.comment("Power flight: hold forward while flying to keep accelerating until you break the sound barrier.").push("flight");
        CRUISE_SPEED = BUILDER.comment("Speed (blocks/tick) when power flight starts.").defineInRange("cruiseSpeed", 0.7, 0.2, 5.0);
        SOUND_BARRIER_SPEED = BUILDER.comment("Speed (blocks/tick) considered Mach 1: crossing it triggers the sonic boom.").defineInRange("soundBarrierSpeed", 2.6, 0.5, 9.0);
        MAX_FLIGHT_SPEED = BUILDER.comment("Top speed (blocks/tick). Keep it below 9 so servers don't reject the movement.").defineInRange("maxSpeed", 4.0, 0.5, 9.0);
        SECONDS_TO_SOUND_BARRIER = BUILDER.comment("Seconds of continuous acceleration (holding forward) needed to reach Mach 1.").defineInRange("secondsToSoundBarrier", 5.0, 0.5, 60.0);
        SUPERSONIC_COST_MULTIPLIER = BUILDER.comment("Flight energy cost multiplier at top speed (scales linearly with speed).").defineInRange("supersonicCostMultiplier", 3.0, 1.0, 50.0);
        HERO_LANDING_DAMAGE = BUILDER.comment("Damage of the shockwave when diving into the ground at high speed.").defineInRange("heroLandingDamage", 10.0, 0.0, 1000.0);
        HERO_LANDING_RADIUS = BUILDER.comment("Radius of the hero landing shockwave.").defineInRange("heroLandingRadius", 4.5, 1.0, 16.0);
        BUILDER.pop();

        BUILDER.push("constructs");
        BLAST_COST = BUILDER.comment("Energy cost of an energy blast.").defineInRange("blastCost", 20, 0, 100_000);
        BLAST_DAMAGE = BUILDER.comment("Damage of an energy blast.").defineInRange("blastDamage", 9.0, 0.0, 1000.0);
        MINIGUN_COST_PER_SHOT = BUILDER.comment("Energy cost of each minigun bullet.").defineInRange("minigunShotCost", 1, 0, 100_000);
        MINIGUN_DAMAGE = BUILDER.comment("Damage of each minigun bullet.").defineInRange("minigunDamage", 2.5, 0.0, 1000.0);
        BUBBLE_COST = BUILDER.comment("Energy cost to raise the protection bubble.").defineInRange("bubbleCost", 50, 0, 100_000);
        BUBBLE_COST_PER_SECOND = BUILDER.comment("Energy drained per second while the bubble is up.").defineInRange("bubbleCostPerSecond", 6, 0, 100_000);
        BUBBLE_DAMAGE_REDUCTION = BUILDER.comment("Fraction of incoming damage blocked by the bubble (0-1).").defineInRange("bubbleDamageReduction", 0.85, 0.0, 1.0);
        SAW_COST = BUILDER.comment("Energy cost to summon the giant saw.").defineInRange("sawCost", 60, 0, 100_000);
        SAW_COST_PER_SECOND = BUILDER.comment("Energy drained per second while riding the saw.").defineInRange("sawCostPerSecond", 8, 0, 100_000);
        SAW_DAMAGE = BUILDER.comment("Damage dealt by the saw blade per hit.").defineInRange("sawDamage", 7.0, 0.0, 1000.0);
        SAW_CUTS_PLANTS = BUILDER.comment("Whether the saw cuts through leaves, logs and plants.").define("sawCutsPlants", true);
        DRILL_COST = BUILDER.comment("Energy cost to summon the giant drill.").defineInRange("drillCost", 70, 0, 100_000);
        DRILL_COST_PER_SECOND = BUILDER.comment("Energy drained per second while riding the drill.").defineInRange("drillCostPerSecond", 9, 0, 100_000);
        DRILL_DAMAGE = BUILDER.comment("Damage dealt by the drill bit per hit.").defineInRange("drillDamage", 8.0, 0.0, 1000.0);
        DRILL_DIGS_BLOCKS = BUILDER.comment("Whether the drill bores through stone, dirt and ores.").define("drillDigsBlocks", true);
        DRILL_SPEED = BUILDER.comment("Mining speed of the drill, in block hardness per tick (stone has hardness 1.5, deepslate 3, obsidian 50).").defineInRange("drillSpeed", 0.6, 0.01, 1000.0);
        DRILL_MAX_HARDNESS = BUILDER.comment("Hardest block the drill can mine (bedrock and other unbreakable blocks are never mined).").defineInRange("drillMaxHardness", 50.0, 0.0, 1000.0);
        DRILL_COLLECTS_DROPS = BUILDER.comment("Whether mined blocks go straight to the rider's inventory (what doesn't fit drops at their feet).").define("drillCollectsDrops", true);
        HAMMER_COST = BUILDER.comment("Energy cost of the hammer slam.").defineInRange("hammerCost", 120, 0, 100_000);
        HAMMER_DAMAGE = BUILDER.comment("Damage at the center of the hammer slam.").defineInRange("hammerDamage", 16.0, 0.0, 1000.0);
        HAMMER_RADIUS = BUILDER.comment("Radius of the hammer shockwave in blocks.").defineInRange("hammerRadius", 5.5, 1.0, 32.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private GLConfig() {}
}
