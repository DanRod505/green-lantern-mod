package com.danrod505.greenlantern.flash;

/** Bit flags describing a speedster's state (sent by the running client, synced to the players nearby). */
public final class SpeedFlags {
    /** Super speed run in progress. */
    public static final int RUNNING = 1;
    /** Faster than the sound barrier. */
    public static final int SUPERSONIC = 1 << 1;
    /** Running up a wall. */
    public static final int WALL = 1 << 2;
    /** Running on water. */
    public static final int WATER = 1 << 3;
    /** In the air after a super jump. */
    public static final int AIR = 1 << 4;
    /** Bits the client may set; the ones below belong to the server. */
    public static final int CLIENT_MASK = 0x1F;
    /** Molecules vibrating: passing through solid matter (set by the server only). */
    public static final int PHASING = 1 << 5;
    /** Running in circles around a tornado (set by the server only). */
    public static final int TORNADO = 1 << 6;

    private SpeedFlags() {}

    public static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
