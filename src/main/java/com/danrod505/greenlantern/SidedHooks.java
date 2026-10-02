package com.danrod505.greenlantern;

import java.util.function.BooleanSupplier;

/**
 * Tiny bridge that lets common code read client-only state without referencing client classes.
 * The client setup replaces these suppliers; on a dedicated server they keep their defaults.
 */
public final class SidedHooks {
    /** Whether the local player is holding the jump key. */
    public static BooleanSupplier jumpKeyDown = () -> false;

    /** Starts a camera shake on the local client (intensity 0-1, duration in ticks). */
    public static ShakeHandler cameraShake = (intensity, duration) -> {};

    /** Client handler for flight state / moves of other players. */
    public static java.util.function.Consumer<com.danrod505.greenlantern.network.FlightSyncPacket> flightSync = packet -> {};

    @FunctionalInterface
    public interface ShakeHandler {
        void shake(float intensity, int duration);
    }

    private SidedHooks() {}
}
