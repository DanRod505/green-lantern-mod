package com.danrod505.greenlantern.flash;

/** One-off speedster moves, sent by the running client and relayed to everyone nearby. */
public enum SpeedAction {
    /** A super speed run starts. */
    START,
    /** Crossing the sound barrier while running. */
    BOOM,
    /** Jumping while running. */
    SUPER_JUMP,
    /** Hard stop: sliding on the ground. */
    SKID,
    /** Kicking off a wall while running up it. */
    WALL_JUMP,
    /** Landing from a super jump. */
    LANDING;

    private static final SpeedAction[] VALUES = values();

    public static SpeedAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : START;
    }
}
