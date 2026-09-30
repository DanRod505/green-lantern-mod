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
    public static final ForgeConfigSpec.IntValue HAMMER_COST;
    public static final ForgeConfigSpec.DoubleValue HAMMER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HAMMER_RADIUS;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("ring");
        MAX_ENERGY = BUILDER.comment("Maximum energy stored in a Power Ring.")
                .defineInRange("maxEnergy", 1000, 100, 1_000_000);
        CHARGE_PER_TICK = BUILDER.comment("Energy restored per tick while charging at a Power Battery.")
                .defineInRange("chargePerTick", 10, 1, 100_000);
        FLIGHT_COST_PER_SECOND = BUILDER.comment("Energy drained per second while flying.")
                .defineInRange("flightCostPerSecond", 2, 0, 10_000);
        FLIGHT_SPEED = BUILDER.comment("Flying speed while wearing the uniform (vanilla creative flight is 0.05).")
                .defineInRange("flightSpeed", 0.1, 0.01, 1.0);
        SHOW_OATH = BUILDER.comment("Show the Green Lantern oath while recharging the ring.")
                .define("showOath", true);
        BUILDER.pop();

        BUILDER.push("constructs");
        BLAST_COST = BUILDER.comment("Energy cost of an energy blast.").defineInRange("blastCost", 20, 0, 100_000);
        BLAST_DAMAGE = BUILDER.comment("Damage of an energy blast.").defineInRange("blastDamage", 9.0, 0.0, 1000.0);
        MINIGUN_COST_PER_SHOT = BUILDER.comment("Energy cost of each minigun bullet.").defineInRange("minigunCostPerShot", 2, 0, 100_000);
        MINIGUN_DAMAGE = BUILDER.comment("Damage of each minigun bullet.").defineInRange("minigunDamage", 2.5, 0.0, 1000.0);
        BUBBLE_COST = BUILDER.comment("Energy cost to raise the protection bubble.").defineInRange("bubbleCost", 50, 0, 100_000);
        BUBBLE_COST_PER_SECOND = BUILDER.comment("Energy drained per second while the bubble is up.").defineInRange("bubbleCostPerSecond", 6, 0, 100_000);
        BUBBLE_DAMAGE_REDUCTION = BUILDER.comment("Fraction of incoming damage blocked by the bubble (0-1).").defineInRange("bubbleDamageReduction", 0.85, 0.0, 1.0);
        SAW_COST = BUILDER.comment("Energy cost to summon the giant saw.").defineInRange("sawCost", 60, 0, 100_000);
        SAW_COST_PER_SECOND = BUILDER.comment("Energy drained per second while riding the saw.").defineInRange("sawCostPerSecond", 8, 0, 100_000);
        SAW_DAMAGE = BUILDER.comment("Damage dealt by the saw blade per hit.").defineInRange("sawDamage", 7.0, 0.0, 1000.0);
        SAW_CUTS_PLANTS = BUILDER.comment("Whether the saw cuts through leaves, logs and plants.").define("sawCutsPlants", true);
        HAMMER_COST = BUILDER.comment("Energy cost of the hammer slam.").defineInRange("hammerCost", 120, 0, 100_000);
        HAMMER_DAMAGE = BUILDER.comment("Damage at the center of the hammer slam.").defineInRange("hammerDamage", 16.0, 0.0, 1000.0);
        HAMMER_RADIUS = BUILDER.comment("Radius of the hammer shockwave in blocks.").defineInRange("hammerRadius", 5.5, 1.0, 32.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private GLConfig() {}
}
