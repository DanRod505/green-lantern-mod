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
    public static final ForgeConfigSpec.BooleanValue GIVE_GUIDE_ON_FIRST_JOIN;

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
    public static final ForgeConfigSpec.IntValue PORTAL_COST;
    public static final ForgeConfigSpec.BooleanValue OA_BATTERY_RECHARGES;
    public static final ForgeConfigSpec.IntValue MECHA_COST;
    public static final ForgeConfigSpec.IntValue MECHA_COST_PER_SECOND;
    public static final ForgeConfigSpec.IntValue MECHA_FLIGHT_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MECHA_DAMAGE_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue MECHA_WALK_SPEED;
    public static final ForgeConfigSpec.DoubleValue MECHA_FLIGHT_SPEED;
    public static final ForgeConfigSpec.IntValue MECHA_LASER_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MECHA_LASER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue MECHA_LASER_RANGE;
    public static final ForgeConfigSpec.IntValue MECHA_MISSILE_COST;
    public static final ForgeConfigSpec.IntValue MECHA_MISSILES_PER_SALVO;
    public static final ForgeConfigSpec.DoubleValue MECHA_MISSILE_POWER;
    public static final ForgeConfigSpec.BooleanValue MECHA_MISSILES_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue MECHA_LANDING_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue MECHA_TRAMPLES_LEAVES;

    // ---- The Flash -----------------------------------------------------------------------------
    public static final ForgeConfigSpec.IntValue SPEED_FORCE_CAPACITY;
    public static final ForgeConfigSpec.IntValue SPEED_FORCE_REGEN;
    public static final ForgeConfigSpec.IntValue SPEED_FORCE_RUN_CHARGE;
    public static final ForgeConfigSpec.DoubleValue RUN_START_SPEED;
    public static final ForgeConfigSpec.DoubleValue RUN_SOUND_BARRIER_SPEED;
    public static final ForgeConfigSpec.DoubleValue RUN_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue RUN_SECONDS_TO_SOUND_BARRIER;
    public static final ForgeConfigSpec.DoubleValue RUN_HUNGER;
    public static final ForgeConfigSpec.DoubleValue SUPER_JUMP_POWER;
    public static final ForgeConfigSpec.IntValue TORNADO_COST;
    public static final ForgeConfigSpec.IntValue TORNADO_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue TORNADO_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue TORNADO_SECONDS;
    public static final ForgeConfigSpec.DoubleValue TORNADO_RADIUS;
    public static final ForgeConfigSpec.IntValue PHASE_COST;
    public static final ForgeConfigSpec.IntValue PHASE_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue PHASE_MAX_SECONDS;
    public static final ForgeConfigSpec.IntValue LIGHTNING_COST;
    public static final ForgeConfigSpec.DoubleValue LIGHTNING_DAMAGE;
    public static final ForgeConfigSpec.IntValue LIGHTNING_CHAIN;

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
        GIVE_GUIDE_ON_FIRST_JOIN = BUILDER.comment("Give every player the Corps Manual (guide book) the first time they join a world.")
                .define("giveGuideOnFirstJoin", true);
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

        BUILDER.comment("Oa: the home planet of the Corps, reached through the Portal construct.").push("oa");
        PORTAL_COST = BUILDER.comment("Energy cost to open a portal to Oa (or back home).").defineInRange("portalCost", 250, 0, 100_000);
        OA_BATTERY_RECHARGES = BUILDER.comment("Whether rings recharge on their own near the Central Power Battery on Oa.").define("centralBatteryRecharges", true);
        BUILDER.pop();

        BUILDER.comment("Giant mecha: a 10 block tall armored suit the ring bearer pilots from the cockpit in its chest.").push("mecha");
        MECHA_COST = BUILDER.comment("Energy cost to summon the mecha.").defineInRange("mechaCost", 150, 0, 100_000);
        MECHA_COST_PER_SECOND = BUILDER.comment("Energy drained per second while piloting the mecha.").defineInRange("mechaCostPerSecond", 8, 0, 100_000);
        MECHA_FLIGHT_COST_PER_SECOND = BUILDER.comment("Extra energy drained per second while the mecha flies.").defineInRange("mechaFlightCostPerSecond", 8, 0, 100_000);
        MECHA_DAMAGE_ENERGY_COST = BUILDER.comment("Energy drained per point of damage the mecha absorbs for its pilot (the pilot takes no damage).").defineInRange("mechaDamageEnergyCost", 2.0, 0.0, 1000.0);
        MECHA_WALK_SPEED = BUILDER.comment("Top walking speed in blocks per tick (sprinting is 60% faster).").defineInRange("mechaWalkSpeed", 0.24, 0.05, 2.0);
        MECHA_FLIGHT_SPEED = BUILDER.comment("Top flight speed in blocks per tick (the afterburner, on sprint, is 70% faster).").defineInRange("mechaFlightSpeed", 0.75, 0.1, 4.0);
        MECHA_LASER_COST_PER_SECOND = BUILDER.comment("Energy drained per second while the laser fires.").defineInRange("mechaLaserCostPerSecond", 10, 0, 100_000);
        MECHA_LASER_DAMAGE = BUILDER.comment("Laser damage, dealt 4 times per second (targets also catch fire).").defineInRange("mechaLaserDamage", 5.0, 0.0, 1000.0);
        MECHA_LASER_RANGE = BUILDER.comment("Laser range in blocks.").defineInRange("mechaLaserRange", 64.0, 4.0, 256.0);
        MECHA_MISSILE_COST = BUILDER.comment("Energy cost of a missile salvo.").defineInRange("mechaMissileCost", 30, 0, 100_000);
        MECHA_MISSILES_PER_SALVO = BUILDER.comment("Homing missiles launched per salvo.").defineInRange("mechaMissilesPerSalvo", 6, 1, 32);
        MECHA_MISSILE_POWER = BUILDER.comment("Explosion power of each missile (creeper = 3, TNT = 4).").defineInRange("mechaMissilePower", 2.0, 0.5, 8.0);
        MECHA_MISSILES_BREAK_BLOCKS = BUILDER.comment("Whether missile explosions break blocks.").define("mechaMissilesBreakBlocks", false);
        MECHA_LANDING_DAMAGE = BUILDER.comment("Damage of the shockwave when the mecha lands hard.").defineInRange("mechaLandingDamage", 12.0, 0.0, 1000.0);
        MECHA_TRAMPLES_LEAVES = BUILDER.comment("Whether the mecha bursts through leaves in its way.").define("mechaTramplesLeaves", true);
        BUILDER.pop();

        BUILDER.comment("The Flash: super speed, the Speed Force and the speedster powers.").push("flash");
        SPEED_FORCE_CAPACITY = BUILDER.comment("Maximum Speed Force stored in the Flash ring.").defineInRange("speedForceCapacity", 1000, 100, 1_000_000);
        SPEED_FORCE_REGEN = BUILDER.comment("Speed Force regained per second while wearing the suit.").defineInRange("speedForceRegen", 4, 0, 100_000);
        SPEED_FORCE_RUN_CHARGE = BUILDER.comment("Extra Speed Force gained per second while running at top speed (running charges the ring).").defineInRange("speedForceRunCharge", 30, 0, 100_000);
        RUN_START_SPEED = BUILDER.comment("Speed (blocks/tick) when a super speed run starts (vanilla sprinting is about 0.28).").defineInRange("runStartSpeed", 0.6, 0.2, 5.0);
        RUN_SOUND_BARRIER_SPEED = BUILDER.comment("Running speed (blocks/tick) of the sound barrier: crossing it triggers the speedster boom.").defineInRange("runSoundBarrierSpeed", 2.4, 0.5, 9.0);
        RUN_MAX_SPEED = BUILDER.comment("Top running speed (blocks/tick). Keep it below 9 so servers don't reject the movement.").defineInRange("runMaxSpeed", 4.2, 0.5, 9.0);
        RUN_SECONDS_TO_SOUND_BARRIER = BUILDER.comment("Seconds of running (holding forward) needed to reach the sound barrier.").defineInRange("runSecondsToSoundBarrier", 4.0, 0.5, 60.0);
        RUN_HUNGER = BUILDER.comment("Food exhaustion per second while running at top speed (a speedster's metabolism; vanilla sprinting would be far more).").defineInRange("runHunger", 0.12, 0.0, 40.0);
        SUPER_JUMP_POWER = BUILDER.comment("Multiplier of the super jump height (jumping while running).").defineInRange("superJumpPower", 1.0, 0.1, 3.0);
        TORNADO_COST = BUILDER.comment("Speed Force cost to start the tornado.").defineInRange("tornadoCost", 120, 0, 100_000);
        TORNADO_COST_PER_SECOND = BUILDER.comment("Speed Force drained per second while the tornado spins.").defineInRange("tornadoCostPerSecond", 12, 0, 100_000);
        TORNADO_DAMAGE = BUILDER.comment("Damage dealt by the tornado twice per second to everything caught in it.").defineInRange("tornadoDamage", 2.5, 0.0, 1000.0);
        TORNADO_SECONDS = BUILDER.comment("How long the tornado lasts (seconds).").defineInRange("tornadoSeconds", 8.0, 1.0, 120.0);
        TORNADO_RADIUS = BUILDER.comment("Radius (blocks) of the area the tornado pulls in.").defineInRange("tornadoRadius", 7.0, 2.0, 24.0);
        PHASE_COST = BUILDER.comment("Speed Force cost to start vibrating your molecules.").defineInRange("phaseCost", 50, 0, 100_000);
        PHASE_COST_PER_SECOND = BUILDER.comment("Speed Force drained per second while phasing through solid matter.").defineInRange("phaseCostPerSecond", 12, 0, 100_000);
        PHASE_MAX_SECONDS = BUILDER.comment("Longest a phase can last (seconds).").defineInRange("phaseMaxSeconds", 10.0, 1.0, 120.0);
        LIGHTNING_COST = BUILDER.comment("Speed Force cost of a lightning bolt.").defineInRange("lightningCost", 60, 0, 100_000);
        LIGHTNING_DAMAGE = BUILDER.comment("Damage of a lightning bolt thrown standing still (up to 75% more when thrown at top speed).").defineInRange("lightningDamage", 12.0, 0.0, 1000.0);
        LIGHTNING_CHAIN = BUILDER.comment("How many extra creatures the lightning arcs to after the first hit.").defineInRange("lightningChain", 2, 0, 16);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private GLConfig() {}
}
