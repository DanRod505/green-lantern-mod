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

    // ---- Aquaman --------------------------------------------------------------------------------
    public static final ForgeConfigSpec.IntValue SEA_FORCE_CAPACITY;
    public static final ForgeConfigSpec.IntValue SEA_FORCE_REGEN_WATER;
    public static final ForgeConfigSpec.IntValue SEA_FORCE_REGEN_LAND;
    public static final ForgeConfigSpec.DoubleValue SWIM_SPEED;
    public static final ForgeConfigSpec.DoubleValue SWIM_SPRINT_SPEED;
    public static final ForgeConfigSpec.DoubleValue SWIM_SECONDS_TO_TOP_SPEED;
    public static final ForgeConfigSpec.IntValue TRIDENT_COST;
    public static final ForgeConfigSpec.DoubleValue TRIDENT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue TRIDENT_THROW_DAMAGE;
    public static final ForgeConfigSpec.IntValue SHARK_COST;
    public static final ForgeConfigSpec.DoubleValue SHARK_SPEED;
    public static final ForgeConfigSpec.DoubleValue SHARK_BITE_DAMAGE;
    public static final ForgeConfigSpec.IntValue KRAKEN_COST;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_HEALTH;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_REGEN_IN_WATER;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_RECOVERY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_WALK_SPEED;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_SWIM_SPEED;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_TENTACLE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_JET_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue KRAKEN_JET_RANGE;
    public static final ForgeConfigSpec.IntValue KRAKEN_JET_COST_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue KRAKEN_TRAMPLES_LEAVES;
    public static final ForgeConfigSpec.IntValue SEA_CALL_COST;
    public static final ForgeConfigSpec.DoubleValue SEA_CALL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue SEA_CALL_RADIUS;
    public static final ForgeConfigSpec.IntValue SEA_CALL_MAX_CREATURES;
    public static final ForgeConfigSpec.IntValue SEA_CALL_HELPERS;
    public static final ForgeConfigSpec.DoubleValue SEA_CALL_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ATLANTIS_PORTAL_COST;

    // Atlantis
    public static final ForgeConfigSpec.BooleanValue ATLANTIS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue TRENCH_ENABLED;
    public static final ForgeConfigSpec.BooleanValue TRENCH_RAIDS;
    public static final ForgeConfigSpec.IntValue TRENCH_RAID_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.DoubleValue TRENCH_RAID_CHANCE;
    public static final ForgeConfigSpec.IntValue TRENCH_NEST_POPULATION;
    public static final ForgeConfigSpec.IntValue ATLANTIS_GATE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue RESPIRATOR_SECONDS;
    public static final ForgeConfigSpec.IntValue RESPIRATOR_RECHARGE_SECONDS;

    // Batman
    public static final ForgeConfigSpec.IntValue BAT_CHARGE_CAPACITY;
    public static final ForgeConfigSpec.IntValue BAT_CHARGE_REGEN;
    public static final ForgeConfigSpec.DoubleValue GLIDE_SPEED;
    public static final ForgeConfigSpec.DoubleValue GLIDE_DIVE_SPEED;
    public static final ForgeConfigSpec.DoubleValue GLIDE_SINK;
    public static final ForgeConfigSpec.IntValue BATARANG_COST;
    public static final ForgeConfigSpec.DoubleValue BATARANG_DAMAGE;
    public static final ForgeConfigSpec.IntValue GRAPPLE_COST;
    public static final ForgeConfigSpec.DoubleValue GRAPPLE_RANGE;
    public static final ForgeConfigSpec.DoubleValue GRAPPLE_PULL_SPEED;
    public static final ForgeConfigSpec.IntValue BAT_SWARM_COST;
    public static final ForgeConfigSpec.IntValue BAT_SWARM_COUNT;
    public static final ForgeConfigSpec.DoubleValue BAT_SWARM_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BAT_SWARM_DAMAGE;
    public static final ForgeConfigSpec.IntValue BATMOBILE_COST;
    public static final ForgeConfigSpec.DoubleValue BATMOBILE_SPEED;
    public static final ForgeConfigSpec.DoubleValue BATMOBILE_BOOST_SPEED;
    public static final ForgeConfigSpec.IntValue BATMOBILE_BOOST_COST_PER_SECOND;
    public static final ForgeConfigSpec.IntValue BATMOBILE_MISSILE_COST;
    public static final ForgeConfigSpec.DoubleValue BATMOBILE_MISSILE_POWER;
    public static final ForgeConfigSpec.BooleanValue BATMOBILE_MISSILES_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue BATMOBILE_RAM_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BATMOBILE_DAMAGE_REDUCTION;

    // Superman
    public static final ForgeConfigSpec.IntValue SOLAR_CAPACITY;
    public static final ForgeConfigSpec.IntValue SOLAR_REGEN;
    public static final ForgeConfigSpec.DoubleValue SOLAR_RAIN_FACTOR;
    public static final ForgeConfigSpec.IntValue SUPERMAN_FLIGHT_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SUPERMAN_CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue SUPERMAN_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue SUPERMAN_SECONDS_TO_SOUND_BARRIER;
    public static final ForgeConfigSpec.DoubleValue SUPERMAN_LANDING_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SUPERMAN_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.IntValue HEAT_VISION_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue HEAT_VISION_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAT_VISION_RANGE;
    public static final ForgeConfigSpec.DoubleValue HEAT_VISION_SECONDS;
    public static final ForgeConfigSpec.BooleanValue HEAT_VISION_IGNITES;
    public static final ForgeConfigSpec.IntValue SUPER_PUNCH_COST;
    public static final ForgeConfigSpec.DoubleValue SUPER_PUNCH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SUPER_PUNCH_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SUPER_PUNCH_BREAKS_BLOCKS;
    public static final ForgeConfigSpec.IntValue SUPER_BREATH_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SUPER_BREATH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SUPER_BREATH_RANGE;
    public static final ForgeConfigSpec.DoubleValue SUPER_BREATH_SECONDS;
    public static final ForgeConfigSpec.BooleanValue SUPER_BREATH_FREEZES;
    public static final ForgeConfigSpec.IntValue XRAY_COST_PER_SECOND;
    public static final ForgeConfigSpec.IntValue XRAY_RADIUS;

    // Wonder Woman
    public static final ForgeConfigSpec.IntValue DIVINE_CAPACITY;
    public static final ForgeConfigSpec.IntValue DIVINE_REGEN;
    public static final ForgeConfigSpec.IntValue SWORD_HIT_CHARGE;
    public static final ForgeConfigSpec.DoubleValue WONDER_WOMAN_CRUISE_SPEED;
    public static final ForgeConfigSpec.DoubleValue WONDER_WOMAN_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue WONDER_WOMAN_SECONDS_TO_MAX;
    public static final ForgeConfigSpec.DoubleValue WONDER_WOMAN_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue AMAZON_SWORD_DAMAGE;
    public static final ForgeConfigSpec.IntValue LASSO_CAPTURE_COST;
    public static final ForgeConfigSpec.DoubleValue LASSO_CAPTURE_SECONDS;
    public static final ForgeConfigSpec.IntValue LASSO_PULL_COST;
    public static final ForgeConfigSpec.DoubleValue LASSO_RANGE;
    public static final ForgeConfigSpec.IntValue LASSO_SPIN_COST;
    public static final ForgeConfigSpec.DoubleValue LASSO_SPIN_DAMAGE;
    public static final ForgeConfigSpec.IntValue BRACELET_GUARD_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue BRACELET_GUARD_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BRACELET_PASSIVE_DEFLECT_CHANCE;
    public static final ForgeConfigSpec.IntValue BRACELET_SHOCKWAVE_COST;
    public static final ForgeConfigSpec.DoubleValue BRACELET_SHOCKWAVE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BRACELET_SHOCKWAVE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue BRACELET_SHOCKWAVE_BREAKS_GLASS;
    public static final ForgeConfigSpec.IntValue SWORD_AND_SHIELD_COST;
    public static final ForgeConfigSpec.IntValue SHIELD_THROW_COST;
    public static final ForgeConfigSpec.DoubleValue SHIELD_THROW_DAMAGE;
    public static final ForgeConfigSpec.IntValue SHIELD_BOUNCES;
    public static final ForgeConfigSpec.IntValue INVISIBLE_JET_COST;
    public static final ForgeConfigSpec.DoubleValue INVISIBLE_JET_SPEED;
    public static final ForgeConfigSpec.DoubleValue INVISIBLE_JET_BOOST_SPEED;
    public static final ForgeConfigSpec.DoubleValue INVISIBLE_JET_DAMAGE_REDUCTION;

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

        BUILDER.comment("Aquaman: breathing underwater, super fast 3D swimming, the trident, the great white shark and the call of the sea.").push("aquaman");
        SEA_FORCE_CAPACITY = BUILDER.comment("Maximum Power of the Seas stored in the Atlantean Emblem.").defineInRange("seaForceCapacity", 1000, 100, 1_000_000);
        SEA_FORCE_REGEN_WATER = BUILDER.comment("Power of the Seas regained per second while in water (half of it in the rain).").defineInRange("seaForceRegenInWater", 25, 0, 100_000);
        SEA_FORCE_REGEN_LAND = BUILDER.comment("Power of the Seas regained per second on dry land.").defineInRange("seaForceRegenOnLand", 3, 0, 100_000);
        SWIM_SPEED = BUILDER.comment("Swimming speed (blocks/tick) of Aquaman underwater (vanilla swimming is about 0.2).").defineInRange("swimSpeed", 0.9, 0.1, 9.0);
        SWIM_SPRINT_SPEED = BUILDER.comment("Top swimming speed (blocks/tick) holding sprint. Keep it below 9 so servers don't reject the movement.").defineInRange("swimSprintSpeed", 2.4, 0.1, 9.0);
        SWIM_SECONDS_TO_TOP_SPEED = BUILDER.comment("Seconds of sprint swimming needed to build up from the normal swimming speed to the top speed (eased in and out).").defineInRange("swimAccelerationSeconds", 3.5, 0.1, 30.0);
        TRIDENT_COST = BUILDER.comment("Power of the Seas cost to summon the trident.").defineInRange("tridentCost", 40, 0, 100_000);
        TRIDENT_DAMAGE = BUILDER.comment("Melee damage of the trident (a diamond sword does 7).").defineInRange("tridentDamage", 14.0, 0.0, 1000.0);
        TRIDENT_THROW_DAMAGE = BUILDER.comment("Damage of the thrown trident.").defineInRange("tridentThrowDamage", 16.0, 0.0, 1000.0);
        SHARK_COST = BUILDER.comment("Power of the Seas cost to summon the great white shark.").defineInRange("sharkCost", 150, 0, 100_000);
        SHARK_SPEED = BUILDER.comment("Top swimming speed (blocks/tick) of the shark with a rider (sprint is 30% faster).").defineInRange("sharkSpeed", 1.8, 0.1, 6.0);
        SHARK_BITE_DAMAGE = BUILDER.comment("Damage of a shark bite.").defineInRange("sharkBiteDamage", 12.0, 0.0, 1000.0);
        SEA_CALL_COST = BUILDER.comment("Power of the Seas cost of the call of the sea.").defineInRange("seaCallCost", 200, 0, 100_000);
        SEA_CALL_SECONDS = BUILDER.comment("How long the sea creatures follow and defend Aquaman (seconds).").defineInRange("seaCallSeconds", 30.0, 1.0, 600.0);
        SEA_CALL_RADIUS = BUILDER.comment("Radius (blocks) of the call: sea creatures this close answer it.").defineInRange("seaCallRadius", 32.0, 4.0, 96.0);
        SEA_CALL_MAX_CREATURES = BUILDER.comment("Most creatures that answer one call.").defineInRange("seaCallMaxCreatures", 24, 1, 256);
        SEA_CALL_HELPERS = BUILDER.comment("If fewer creatures than this answer (and Aquaman is in the water), dolphins come from the deep to make up the number (0 = never).").defineInRange("seaCallHelpers", 3, 0, 16);
        SEA_CALL_DAMAGE_MULTIPLIER = BUILDER.comment("Multiplier of the damage dealt by the called creatures (dolphins bite for 5, small fish for 2).").defineInRange("seaCallDamageMultiplier", 1.0, 0.0, 100.0);
        ATLANTIS_PORTAL_COST = BUILDER.comment("Power of the Seas cost of the portal to Atlantis (and back).").defineInRange("atlantisPortalCost", 250, 0, 100_000);
        KRAKEN_COST = BUILDER.comment("Power of the Seas cost to call the Kraken.").defineInRange("krakenCost", 400, 0, 100_000);
        KRAKEN_HEALTH = BUILDER.comment("Life of the Kraken (an iron golem has 100, the Wither 300).").defineInRange("krakenHealth", 400.0, 1.0, 100_000.0);
        KRAKEN_REGEN_IN_WATER = BUILDER.comment("Life the Kraken heals per second while in the water.").defineInRange("krakenRegenInWater", 1.0, 0.0, 1000.0);
        KRAKEN_RECOVERY_SECONDS = BUILDER.comment("Seconds a defeated Kraken needs before it can be called again.").defineInRange("krakenRecoverySeconds", 60.0, 0.0, 3600.0);
        KRAKEN_WALK_SPEED = BUILDER.comment("Walking speed (blocks/tick) of the Kraken on land (sprint is 35% faster).").defineInRange("krakenWalkSpeed", 0.16, 0.02, 2.0);
        KRAKEN_SWIM_SPEED = BUILDER.comment("Top swimming speed (blocks/tick) of the Kraken in the water (sprint is 35% faster).").defineInRange("krakenSwimSpeed", 1.4, 0.1, 6.0);
        KRAKEN_TENTACLE_DAMAGE = BUILDER.comment("Damage of the tentacle slam (less at the edge of the blow).").defineInRange("krakenTentacleDamage", 20.0, 0.0, 1000.0);
        KRAKEN_JET_DAMAGE = BUILDER.comment("Damage of the water jet, five times per second.").defineInRange("krakenJetDamage", 4.0, 0.0, 1000.0);
        KRAKEN_JET_RANGE = BUILDER.comment("Reach (blocks) of the water jet.").defineInRange("krakenJetRange", 32.0, 4.0, 96.0);
        KRAKEN_JET_COST_PER_SECOND = BUILDER.comment("Power of the Seas drained per second while the Kraken fires the water jet.").defineInRange("krakenJetCostPerSecond", 10, 0, 100_000);
        KRAKEN_TRAMPLES_LEAVES = BUILDER.comment("Whether the walking Kraken bursts through leaves (respects protected areas).").define("krakenTramplesLeaves", true);
        BUILDER.pop();

        BUILDER.comment("Atlantis: the sunken city at the bottom of a deep ocean, the Atlantean Gate and the Atlantean Respirator.").push("atlantis");
        ATLANTIS_ENABLED = BUILDER.comment("Whether Atlantis is raised in the overworld (in a deep ocean, some way from spawn).").define("enabled", true);
        ATLANTIS_GATE_COOLDOWN = BUILDER.comment("Seconds before the Atlantean Gate can open another portal.").defineInRange("gateCooldownSeconds", 10, 0, 3600);
        RESPIRATOR_SECONDS = BUILDER.comment("Seconds of air stored in the Atlantean Respirator.").defineInRange("respiratorSeconds", 480, 10, 100_000);
        RESPIRATOR_RECHARGE_SECONDS = BUILDER.comment("Seconds out of the water for an empty respirator to fill up again.").defineInRange("respiratorRechargeSeconds", 24, 1, 100_000);
        BUILDER.pop();

        BUILDER.comment("The Trench: hostile creatures of the deep living in nests around Atlantis, their dark territory, captives and raids.").push("trench");
        TRENCH_ENABLED = BUILDER.comment("Whether the nests of the Trench are dug around Atlantis (needs Atlantis).").define("enabled", true);
        TRENCH_RAIDS = BUILDER.comment("Whether the Trench raids Atlantis now and then while a player is there.").define("raids", true);
        TRENCH_RAID_INTERVAL_MINUTES = BUILDER.comment("Average minutes between chances of a raid on Atlantis.").defineInRange("raidIntervalMinutes", 20, 1, 100_000);
        TRENCH_RAID_CHANCE = BUILDER.comment("Chance (0-1) that a raid happens when its time comes and someone is in Atlantis.").defineInRange("raidChance", 0.5, 0.0, 1.0);
        TRENCH_NEST_POPULATION = BUILDER.comment("Creatures living in each nest (one of them a brute); the dead are slowly replaced.").defineInRange("nestPopulation", 9, 1, 40);
        BUILDER.pop();

        BUILDER.comment("Batman: the Utility Belt, gliding with the cape, the batarang, the grapnel gun, the swarm of bats and the Batmobile.").push("batman");
        BAT_CHARGE_CAPACITY = BUILDER.comment("Maximum charge stored in the Utility Belt.").defineInRange("beltChargeCapacity", 1000, 100, 1_000_000);
        BAT_CHARGE_REGEN = BUILDER.comment("Belt charge regained per second while the batsuit is worn (50% more in the dark).").defineInRange("beltChargeRegen", 8, 0, 100_000);
        GLIDE_SPEED = BUILDER.comment("Gliding speed (blocks/tick) with the cape spread, looking straight ahead.").defineInRange("glideSpeed", 0.6, 0.1, 4.0);
        GLIDE_DIVE_SPEED = BUILDER.comment("Top gliding speed (blocks/tick) when diving (looking down).").defineInRange("glideDiveSpeed", 1.5, 0.1, 6.0);
        GLIDE_SINK = BUILDER.comment("How fast (blocks/tick) Batman sinks while gliding level.").defineInRange("glideSink", 0.075, 0.0, 1.0);
        BATARANG_COST = BUILDER.comment("Belt charge cost of a batarang.").defineInRange("batarangCost", 10, 0, 100_000);
        BATARANG_DAMAGE = BUILDER.comment("Damage of a batarang (it also stuns for a moment).").defineInRange("batarangDamage", 7.0, 0.0, 1000.0);
        GRAPPLE_COST = BUILDER.comment("Belt charge cost of a shot of the grapnel gun.").defineInRange("grappleCost", 15, 0, 100_000);
        GRAPPLE_RANGE = BUILDER.comment("Reach (blocks) of the grapnel gun's cable.").defineInRange("grappleRange", 48.0, 4.0, 128.0);
        GRAPPLE_PULL_SPEED = BUILDER.comment("Top speed (blocks/tick) at which the cable reels Batman in.").defineInRange("grapplePullSpeed", 1.6, 0.2, 6.0);
        BAT_SWARM_COST = BUILDER.comment("Belt charge cost of calling the swarm of bats.").defineInRange("batSwarmCost", 200, 0, 100_000);
        BAT_SWARM_COUNT = BUILDER.comment("Bats in the swarm.").defineInRange("batSwarmCount", 16, 1, 64);
        BAT_SWARM_SECONDS = BUILDER.comment("How long the bats stay and defend Batman (seconds).").defineInRange("batSwarmSeconds", 25.0, 1.0, 600.0);
        BAT_SWARM_DAMAGE = BUILDER.comment("Damage of each bat's attack (they also blind and slow down their prey).").defineInRange("batSwarmDamage", 2.0, 0.0, 1000.0);
        BATMOBILE_COST = BUILDER.comment("Belt charge cost of calling the Batmobile.").defineInRange("batmobileCost", 250, 0, 100_000);
        BATMOBILE_SPEED = BUILDER.comment("Top speed (blocks/tick) of the Batmobile.").defineInRange("batmobileSpeed", 0.95, 0.1, 4.0);
        BATMOBILE_BOOST_SPEED = BUILDER.comment("Top speed (blocks/tick) with the jet booster on (hold sprint). Keep it below 9.").defineInRange("batmobileBoostSpeed", 1.9, 0.1, 8.0);
        BATMOBILE_BOOST_COST_PER_SECOND = BUILDER.comment("Belt charge drained per second while the booster burns.").defineInRange("batmobileBoostCostPerSecond", 12, 0, 100_000);
        BATMOBILE_MISSILE_COST = BUILDER.comment("Belt charge cost of a pair of missiles.").defineInRange("batmobileMissileCost", 30, 0, 100_000);
        BATMOBILE_MISSILE_POWER = BUILDER.comment("Explosion power of a Batmobile missile (TNT is 4).").defineInRange("batmobileMissilePower", 2.0, 0.0, 10.0);
        BATMOBILE_MISSILES_BREAK_BLOCKS = BUILDER.comment("Whether the Batmobile's missiles break blocks.").define("batmobileMissilesBreakBlocks", false);
        BATMOBILE_RAM_DAMAGE = BUILDER.comment("Damage when the Batmobile rams a creature at full speed (less when slower).").defineInRange("batmobileRamDamage", 14.0, 0.0, 1000.0);
        BATMOBILE_DAMAGE_REDUCTION = BUILDER.comment("Fraction of the damage the armored Batmobile takes for its driver (0-1).").defineInRange("batmobileDamageReduction", 0.8, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Superman: the Kryptonian Crystal, solar energy, super flight, heat vision, the super punch, super breath and X-ray vision.").push("superman");
        SOLAR_CAPACITY = BUILDER.comment("Maximum solar energy stored in Superman's cells (kept in the Kryptonian Crystal).").defineInRange("solarCapacity", 6000, 100, 1_000_000);
        SOLAR_REGEN = BUILDER.comment("Solar energy absorbed per second under the open sky in daylight (50% more high up, above y=150).").defineInRange("solarRegen", 60, 0, 100_000);
        SOLAR_RAIN_FACTOR = BUILDER.comment("Fraction of the sunlight that gets through the clouds when it rains.").defineInRange("solarRainFactor", 0.35, 0.0, 1.0);
        SUPERMAN_FLIGHT_COST_PER_SECOND = BUILDER.comment("Solar energy drained per second while flying (x supersonicCostMultiplier at top speed).").defineInRange("flightCostPerSecond", 2, 0, 10_000);
        SUPERMAN_CRUISE_SPEED = BUILDER.comment("Speed (blocks/tick) when Superman's power flight starts.").defineInRange("cruiseSpeed", 1.0, 0.2, 5.0);
        SUPERMAN_MAX_SPEED = BUILDER.comment("Superman's top speed (blocks/tick). Keep it below 9 so servers don't reject the movement.").defineInRange("maxSpeed", 7.5, 0.5, 9.0);
        SUPERMAN_SECONDS_TO_SOUND_BARRIER = BUILDER.comment("Seconds of acceleration Superman needs to break the sound barrier.").defineInRange("secondsToSoundBarrier", 1.6, 0.2, 60.0);
        SUPERMAN_LANDING_MULTIPLIER = BUILDER.comment("Superman's hero landing: damage and radius multiplier over the Lantern's.").defineInRange("heroLandingMultiplier", 1.8, 0.0, 10.0);
        SUPERMAN_DAMAGE_REDUCTION = BUILDER.comment("Fraction of the damage Superman shrugs off while he has solar energy (0-1).").defineInRange("damageReduction", 0.6, 0.0, 1.0);
        HEAT_VISION_COST_PER_SECOND = BUILDER.comment("Solar energy drained per second of heat vision.").defineInRange("heatVisionCostPerSecond", 60, 0, 100_000);
        HEAT_VISION_DAMAGE = BUILDER.comment("Damage of heat vision, dealt 5 times a second (it also sets the target on fire).").defineInRange("heatVisionDamage", 5.0, 0.0, 1000.0);
        HEAT_VISION_RANGE = BUILDER.comment("Reach of heat vision (blocks).").defineInRange("heatVisionRange", 64.0, 4.0, 160.0);
        HEAT_VISION_SECONDS = BUILDER.comment("How long a burst of heat vision lasts (seconds); press again to stop sooner.").defineInRange("heatVisionSeconds", 4.0, 0.5, 60.0);
        HEAT_VISION_IGNITES = BUILDER.comment("Whether heat vision sets blocks on fire and melts ice and snow.").define("heatVisionIgnites", true);
        SUPER_PUNCH_COST = BUILDER.comment("Solar energy cost of the super punch.").defineInRange("superPunchCost", 150, 0, 100_000);
        SUPER_PUNCH_DAMAGE = BUILDER.comment("Damage of the super punch at its center (less at the edge of the blast).").defineInRange("superPunchDamage", 28.0, 0.0, 1000.0);
        SUPER_PUNCH_RADIUS = BUILDER.comment("Radius of the super punch shockwave.").defineInRange("superPunchRadius", 6.0, 1.0, 24.0);
        SUPER_PUNCH_BREAKS_BLOCKS = BUILDER.comment("Whether the super punch smashes the weak blocks (dirt, sand, leaves, glass...) in front of it.").define("superPunchBreaksBlocks", false);
        SUPER_BREATH_COST_PER_SECOND = BUILDER.comment("Solar energy drained per second of super breath.").defineInRange("superBreathCostPerSecond", 45, 0, 100_000);
        SUPER_BREATH_DAMAGE = BUILDER.comment("Freezing damage of super breath, dealt 4 times a second.").defineInRange("superBreathDamage", 2.5, 0.0, 1000.0);
        SUPER_BREATH_RANGE = BUILDER.comment("Reach of super breath (blocks).").defineInRange("superBreathRange", 16.0, 2.0, 64.0);
        SUPER_BREATH_SECONDS = BUILDER.comment("How long a gust of super breath lasts (seconds); press again to stop sooner.").defineInRange("superBreathSeconds", 3.0, 0.5, 60.0);
        SUPER_BREATH_FREEZES = BUILDER.comment("Whether super breath freezes water into ice and puts out fires.").define("superBreathFreezes", true);
        XRAY_COST_PER_SECOND = BUILDER.comment("Solar energy drained per second of X-ray vision.").defineInRange("xrayCostPerSecond", 5, 0, 100_000);
        XRAY_RADIUS = BUILDER.comment("How far X-ray vision sees through walls (blocks): ores and chests at this distance, creatures at twice it.").defineInRange("xrayRadius", 20, 4, 48);
        BUILDER.pop();

        BUILDER.comment("Wonder Woman: the Tiara of Themyscira, divine power, flight, the Lasso of Truth, the Bracelets of Submission, the Amazon sword and shield and the Invisible Jet.").push("wonderWoman");
        DIVINE_CAPACITY = BUILDER.comment("Maximum divine power stored in the Tiara of Themyscira.").defineInRange("divineCapacity", 1000, 50, 1_000_000);
        DIVINE_REGEN = BUILDER.comment("Divine power regained per second while the armor is worn.").defineInRange("divineRegen", 8, 0, 100_000);
        SWORD_HIT_CHARGE = BUILDER.comment("Divine power regained with every blow of the Amazon sword.").defineInRange("swordHitCharge", 12, 0, 100_000);
        WONDER_WOMAN_CRUISE_SPEED = BUILDER.comment("Speed (blocks/tick) when Wonder Woman's flight starts.").defineInRange("cruiseSpeed", 0.6, 0.2, 5.0);
        WONDER_WOMAN_MAX_SPEED = BUILDER.comment("Wonder Woman's top flying speed (blocks/tick). Below the Green Lantern's, and below the sound barrier by default.").defineInRange("maxSpeed", 2.3, 0.5, 9.0);
        WONDER_WOMAN_SECONDS_TO_MAX = BUILDER.comment("Seconds of acceleration (holding forward) Wonder Woman needs to reach top speed.").defineInRange("secondsToTopSpeed", 6.0, 0.5, 60.0);
        WONDER_WOMAN_DAMAGE_REDUCTION = BUILDER.comment("Fraction of the damage Wonder Woman shrugs off with the armor on (0-1).").defineInRange("damageReduction", 0.35, 0.0, 1.0);
        AMAZON_SWORD_DAMAGE = BUILDER.comment("Attack damage of the Amazon sword (before the armor's strength bonus).").defineInRange("swordDamage", 11.0, 1.0, 1000.0);
        LASSO_CAPTURE_COST = BUILDER.comment("Divine power cost of catching a creature with the Lasso of Truth.").defineInRange("lassoCaptureCost", 40, 0, 100_000);
        LASSO_CAPTURE_SECONDS = BUILDER.comment("How long the lasso holds a creature (seconds).").defineInRange("lassoCaptureSeconds", 10.0, 1.0, 120.0);
        LASSO_PULL_COST = BUILDER.comment("Divine power cost of pulling with the lasso.").defineInRange("lassoPullCost", 25, 0, 100_000);
        LASSO_RANGE = BUILDER.comment("How far the lasso reaches (blocks).").defineInRange("lassoRange", 24.0, 4.0, 64.0);
        LASSO_SPIN_COST = BUILDER.comment("Divine power cost of whirling the lasso.").defineInRange("lassoSpinCost", 60, 0, 100_000);
        LASSO_SPIN_DAMAGE = BUILDER.comment("Damage of each sweep of the whirling lasso (and of the creature swung on it).").defineInRange("lassoSpinDamage", 6.0, 0.0, 1000.0);
        BRACELET_GUARD_COST_PER_SECOND = BUILDER.comment("Divine power drained per second while the bracelets are raised.").defineInRange("braceletGuardCostPerSecond", 12, 0, 100_000);
        BRACELET_GUARD_SECONDS = BUILDER.comment("How long the bracelets stay raised (seconds); press again to lower them sooner.").defineInRange("braceletGuardSeconds", 6.0, 0.5, 60.0);
        BRACELET_PASSIVE_DEFLECT_CHANCE = BUILDER.comment("Chance that an arrow coming from the front bounces off the bracelets even without raising them (0-1).").defineInRange("braceletPassiveDeflectChance", 0.35, 0.0, 1.0);
        BRACELET_SHOCKWAVE_COST = BUILDER.comment("Divine power cost of clashing the bracelets.").defineInRange("braceletShockwaveCost", 150, 0, 100_000);
        BRACELET_SHOCKWAVE_DAMAGE = BUILDER.comment("Damage of the bracelets' shockwave at its center (more after blocking hits, less at the edge).").defineInRange("braceletShockwaveDamage", 14.0, 0.0, 1000.0);
        BRACELET_SHOCKWAVE_RADIUS = BUILDER.comment("Radius of the bracelets' shockwave (it grows after blocking hits).").defineInRange("braceletShockwaveRadius", 7.0, 1.0, 24.0);
        BRACELET_SHOCKWAVE_BREAKS_GLASS = BUILDER.comment("Whether the bracelets' shockwave shatters the glass around.").define("braceletShockwaveBreaksGlass", true);
        SWORD_AND_SHIELD_COST = BUILDER.comment("Divine power cost of calling the Amazon sword and shield.").defineInRange("swordAndShieldCost", 20, 0, 100_000);
        SHIELD_THROW_COST = BUILDER.comment("Divine power cost of throwing the shield.").defineInRange("shieldThrowCost", 30, 0, 100_000);
        SHIELD_THROW_DAMAGE = BUILDER.comment("Damage of the thrown shield to each creature it hits.").defineInRange("shieldThrowDamage", 10.0, 0.0, 1000.0);
        SHIELD_BOUNCES = BUILDER.comment("How many creatures the thrown shield can bounce between before it flies back.").defineInRange("shieldBounces", 3, 1, 16);
        INVISIBLE_JET_COST = BUILDER.comment("Divine power cost of calling the Invisible Jet.").defineInRange("invisibleJetCost", 100, 0, 100_000);
        INVISIBLE_JET_SPEED = BUILDER.comment("Cruising speed of the Invisible Jet (blocks/tick).").defineInRange("invisibleJetSpeed", 1.6, 0.2, 6.0);
        INVISIBLE_JET_BOOST_SPEED = BUILDER.comment("Top speed of the Invisible Jet with the afterburner (sprint) on (blocks/tick).").defineInRange("invisibleJetBoostSpeed", 2.8, 0.2, 8.0);
        INVISIBLE_JET_DAMAGE_REDUCTION = BUILDER.comment("Fraction of the damage the Invisible Jet takes for its pilot (0-1).").defineInRange("invisibleJetDamageReduction", 0.7, 0.0, 1.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private GLConfig() {}
}
