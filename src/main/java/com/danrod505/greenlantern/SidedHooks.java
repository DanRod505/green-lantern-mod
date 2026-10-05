package com.danrod505.greenlantern;

import java.util.function.BooleanSupplier;

/**
 * Tiny bridge that lets common code read client-only state without referencing client classes.
 * The client setup replaces these suppliers; on a dedicated server they keep their defaults.
 */
public final class SidedHooks {
    /** Whether the local player is holding the jump key. */
    public static BooleanSupplier jumpKeyDown = () -> false;

    /** Whether the local player is holding the sprint key. */
    public static BooleanSupplier sprintKeyDown = () -> false;

    /** Starts a camera shake on the local client (intensity 0-1, duration in ticks). */
    public static ShakeHandler cameraShake = (intensity, duration) -> {};

    /** Client handler for flight state / moves of other players. */
    public static java.util.function.Consumer<com.danrod505.greenlantern.network.FlightSyncPacket> flightSync = packet -> {};

    /** Client handler for the speedster state / moves of players (the local one included). */
    public static java.util.function.Consumer<com.danrod505.greenlantern.network.SpeedSyncPacket> speedSync = packet -> {};

    /** Client handler for the powers / events of a Superman (the local one included). */
    public static java.util.function.Consumer<com.danrod505.greenlantern.network.SupermanSyncPacket> supermanSync = packet -> {};

    /** Opens the Corps Manual (guide book) screen on the local client. */
    public static Runnable openGuide = () -> {};

    @FunctionalInterface
    public interface ShakeHandler {
        void shake(float intensity, int duration);
    }

    private SidedHooks() {}
}
