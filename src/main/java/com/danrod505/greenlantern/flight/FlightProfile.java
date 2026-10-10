package com.danrod505.greenlantern.flight;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.superman.SupermanHelper;
import net.minecraft.world.entity.player.Player;

/**
 * How a hero power-flies. The Green Lantern and Superman share the same power flight (hold forward
 * to keep accelerating, break the sound barrier, barrel rolls, hero landings); Superman's is much
 * faster and stronger.
 *
 * @param cruise       speed (blocks/tick) when power flight starts
 * @param barrier      speed of the sound barrier (Mach 1)
 * @param max          top speed
 * @param seconds      seconds of acceleration to break the sound barrier
 * @param sprintBoost  acceleration multiplier while sprinting
 * @param landingPower damage and radius multiplier of the hero landing
 */
public record FlightProfile(double cruise, double barrier, double max, double seconds, double sprintBoost, double landingPower) {
    /** Whether the player power-flies right now (wears the Lantern uniform or Superman's suit). */
    public static boolean canPowerFly(Player player) {
        return RingHelper.isSuited(player) || SupermanHelper.isSuited(player);
    }

    public static boolean isSuperman(Player player) {
        return SupermanHelper.isSuited(player);
    }

    public static FlightProfile of(Player player) {
        return isSuperman(player) ? superman() : lantern();
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
}
