package com.danrod505.greenlantern.flight;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import net.minecraft.world.entity.player.Player;

/**
 * How a hero power-flies. The Green Lantern, Superman and Wonder Woman share the same power flight
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
    /** Whether the player power-flies right now (wears the Lantern uniform, Superman's suit or Wonder Woman's armor). */
    public static boolean canPowerFly(Player player) {
        return RingHelper.isSuited(player) || SupermanHelper.isSuited(player) || WonderWomanHelper.isSuited(player);
    }

    public static boolean isWonderWoman(Player player) {
        return WonderWomanHelper.isSuited(player);
    }

    public static boolean isSuperman(Player player) {
        return SupermanHelper.isSuited(player);
    }

    public static FlightProfile of(Player player) {
        return isSuperman(player) ? superman() : isWonderWoman(player) ? wonderWoman() : lantern();
    }

    public static FlightProfile lantern() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        return new FlightProfile(GLConfig.CRUISE_SPEED.get(), barrier, Math.max(barrier, GLConfig.MAX_FLIGHT_SPEED.get()),
                GLConfig.SECONDS_TO_SOUND_BARRIER.get(), 1.6, 1.0);
    }

    public static FlightProfile superman() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        return new FlightProfile(GLConfig.SUPERMAN_CRUISE_SPEED.get(), barrier, Math.max(barrier, GLConfig.SUPERMAN_MAX_SPEED.get()),
                GLConfig.SUPERMAN_SECONDS_TO_SOUND_BARRIER.get(), 2.0, GLConfig.SUPERMAN_LANDING_MULTIPLIER.get());
    }

    /** Wonder Woman: a strong, steady flight, but slower than the Lantern's. */
    public static FlightProfile wonderWoman() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        double cruise = GLConfig.WONDER_WOMAN_CRUISE_SPEED.get();
        double max = Math.max(cruise + 0.1, GLConfig.WONDER_WOMAN_MAX_SPEED.get());
        double toMax = GLConfig.WONDER_WOMAN_SECONDS_TO_MAX.get();
        // The flight accelerates at (barrier - cruise) / seconds: scaled so top speed comes after toMax seconds.
        double seconds = max < barrier ? toMax * (barrier - cruise) / (max - cruise) : toMax;
        return new FlightProfile(cruise, barrier, max, seconds, 1.4, 0.8);
    }
}
