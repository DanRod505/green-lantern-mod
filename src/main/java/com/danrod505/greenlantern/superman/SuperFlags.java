package com.danrod505.greenlantern.superman;

/** Bit flags of the powers a Superman is using right now (synced to the players around him). */
public final class SuperFlags {
    public static final int HEAT_VISION = 1;
    public static final int SUPER_BREATH = 1 << 1;
    public static final int XRAY = 1 << 2;

    /** One-shot events sent along with the flags. */
    public static final int EVENT_NONE = -1;
    public static final int EVENT_PUNCH = 0;
    public static final int EVENT_SUIT_UP = 1;

    private SuperFlags() {}

    public static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
