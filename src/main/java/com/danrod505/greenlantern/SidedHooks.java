package com.danrod505.greenlantern;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
    /** Client handler for the powers / events of a Wonder Woman (the local one included). */
    public static java.util.function.Consumer<com.danrod505.greenlantern.network.WonderWomanSyncPacket> wonderWomanSync = packet -> {};

    /** Opens the Corps Manual (guide book) screen on the local client. */
    public static Runnable openGuide = () -> {};

    /** Client handlers of newer heroes' packets, by packet class (see {@link #onClient}). */
    private static final Map<Class<?>, Consumer<?>> CLIENT_HANDLERS = new ConcurrentHashMap<>();

    /** Lets a hero's client half handle one of its server -> client packets (call it from {@code HeroClient#registerEvents}). */
    public static <P> void onClient(Class<P> type, Consumer<P> handler) {
        CLIENT_HANDLERS.put(type, handler);
    }

    /** Hands a server -> client packet to its client handler; nothing happens on a dedicated server. */
    @SuppressWarnings("unchecked")
    public static <P> void handleOnClient(P packet) {
        Consumer<P> handler = (Consumer<P>) CLIENT_HANDLERS.get(packet.getClass());
        if (handler != null) handler.accept(packet);
    }

    @FunctionalInterface
    public interface ShakeHandler {
        void shake(float intensity, int duration);
    }

    private SidedHooks() {}
}
