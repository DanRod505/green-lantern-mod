package com.danrod505.greenlantern.wonderwoman;

/** Bit flags of what a Wonder Woman is doing right now (synced to the players around her), and one-shot events. */
public final class AmazonFlags {
    /** The bracelets are raised, crossed in front of her: blows are blocked and projectiles bounce off. */
    public static final int GUARD = 1;
    /** The lasso whirls around her (or swings the creature caught in it). */
    public static final int SPIN = 1 << 1;

    public static final int EVENT_NONE = -1;
    /** The bracelets clash: a shockwave. */
    public static final int EVENT_SHOCKWAVE = 0;
    public static final int EVENT_SUIT_UP = 1;
    /** A projectile bounced off a bracelet. */
    public static final int EVENT_DEFLECT = 2;
    /** The lasso is thrown (the arm swings). */
    public static final int EVENT_LASSO_THROW = 3;

    private AmazonFlags() {}

    public static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
