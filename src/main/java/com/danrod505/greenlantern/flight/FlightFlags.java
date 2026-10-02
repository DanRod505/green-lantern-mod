package com.danrod505.greenlantern.flight;

/** Bit flags describing a player's power-flight state (synced to nearby clients). */
public final class FlightFlags {
    /** Power flight active (accelerating flight following the look direction). */
    public static final int POWER = 1;
    /** Faster than the sound barrier. */
    public static final int SUPERSONIC = 1 << 1;
    /** Air brake flare in progress. */
    public static final int BRAKING = 1 << 2;

    private FlightFlags() {}

    public static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
