package com.danrod505.greenlantern.flight;

/** Special moves and events of power flight, sent by the flying client and relayed to everyone nearby. */
public enum FlightAction {
    /** Explosive launch when the player starts flying close to the ground. */
    TAKEOFF,
    /** Crossing the sound barrier. */
    SONIC_BOOM,
    /** Barrel roll dodge to the left / right (double tap A or D). */
    ROLL_LEFT,
    ROLL_RIGHT,
    /** Hard stop flare (hold back while flying fast). */
    AIR_BRAKE,
    /** Diving into the ground at high speed: shockwave. */
    HERO_LANDING;

    private static final FlightAction[] VALUES = values();

    public static FlightAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : TAKEOFF;
    }
}
