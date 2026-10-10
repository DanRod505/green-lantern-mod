package com.danrod505.greenlantern.flight;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * How fast a hero power-flies (each hero with power flight gives its own: see {@code HeroDefinition#flightProfile}; the
 * looks and handling are in {@link FlightStyle}). The Green Lantern, Superman and Wonder Woman share the same power flight
 * (hold forward to keep accelerating, break the sound barrier, barrel rolls, hero landings);
 * Superman's is much faster and stronger, Wonder Woman's is slower than the Lantern's (by default
 * she never reaches the sound barrier).
 *
 * @param cruise       speed (blocks/tick) when power flight starts
 * @param barrier      speed of the sound barrier (Mach 1)
 * @param max          top speed
 * @param seconds      seconds of acceleration to break the sound barrier
 * @param sprintBoost  acceleration multiplier while sprinting
 * @param landingPower damage and radius multiplier of the hero landing
 */
public record FlightProfile(double cruise, double barrier, double max, double seconds, double sprintBoost, double landingPower) {
    /** Whether the player power-flies right now (wears the suit of a hero with a flight profile). */
    public static boolean canPowerFly(Player player) {
        return HeroRegistry.suited(player).map(HeroDefinition::flightProfile).isPresent();
    }

    /** The suited hero's flight profile (the Lantern's when none). */
    public static FlightProfile of(Player player) {
        FlightProfile profile = HeroRegistry.suited(player).map(HeroDefinition::flightProfile).orElse(null);
        return profile != null ? profile : lantern();
    }

    public static FlightProfile lantern() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        return new FlightProfile(GLConfig.CRUISE_SPEED.get(), barrier, Math.max(barrier, GLConfig.MAX_FLIGHT_SPEED.get()),
                GLConfig.SECONDS_TO_SOUND_BARRIER.get(), 1.6, 1.0);
    }
}
