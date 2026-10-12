package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * The presets that go on for a while after the key (a shot in flight, a dash, a beam, a rain of
 * blows, a bubble): each one is a task ticked every server tick until it says it is done, or until
 * its player leaves or dies. Tasks never outlive the server.
 * <p>
 * A lasting preset (a bubble, super speed, invisibility...) runs under its ability's name: pressing
 * the ability again turns it off, taking the suit off turns it off, and its cooldown starts when it
 * ends (see {@link #lasting}).
 */
public final class PresetRuntime {
    /** One tick of a running preset ({@code age} counts from 1); returns whether it goes on. */
    @FunctionalInterface
    public interface Task {
        boolean tick(int age);

        /** Called once when the task ends, however it ends: puts back what it changed. */
        default void stop() {}

        /** A task with something to put back when it ends. */
        static Task ending(Task task, Runnable end) {
            return new Task() {
                @Override
                public boolean tick(int age) {
                    return task.tick(age);
                }

                @Override
                public void stop() {
                    end.run();
                }
            };
        }
    }

    private static final class Running {
        final ServerPlayer player;
        final UUID owner;
        final Task task;
        final String key;
        int age;

        Running(ServerPlayer player, Task task, String key) {
            this.player = player;
            this.owner = player.getUUID();
            this.task = task;
            this.key = key;
        }
    }

    private static final List<Running> RUNNING = new ArrayList<>();
    /** What pressing an ability again takes back (a great work), by player and ability. */
    private static final Map<String, Runnable> UNDO = new HashMap<>();

    private PresetRuntime() {}

    public static void register() {
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> tick(event.server()));
        Guards.register();
        Summons.register();
        ControlPresets.events();
    }

    /** Starts a task for {@code player}; it gets its first tick on the next server tick. */
    public static void start(ServerPlayer player, Task task) {
        RUNNING.add(new Running(player, task, null));
    }

    /**
     * Starts a lasting preset under its ability's name. The ability's cooldown starts when it ends
     * (its time is up, it is pressed again, the suit comes off), so the use itself only waits a tick.
     */
    public static Outcome lasting(PresetContext c, Task task) {
        ServerPlayer player = c.player();
        String key = key(c);
        stop(player, key);
        RUNNING.add(new Running(player, Task.ending(task, () -> {
            task.stop();
            EngineServer.startCooldown(player, c.hero(), c.ability());
        }), key));
        return Outcome.cooldown(1);
    }

    /** The name a lasting preset runs under: the ability's (each other function of it has a name of its own). */
    public static String key(PresetContext c) {
        return key(c.ability().data().id(), c.chosen().index());
    }

    /**
     * Starts a task under a name of its own (a power of one of the 9 heroes that goes on for a while):
     * like a lasting preset, the same name stops the one before, and taking the suit off stops it.
     */
    public static void startNamed(ServerPlayer player, String key, Task task) {
        stop(player, key);
        RUNNING.add(new Running(player, task, key));
    }

    /** The name function {@code index} of an ability runs under (0 is the default one, named after the ability). */
    public static String key(String ability, int index) {
        return index == 0 ? ability : ability + "#" + index;
    }

    /** Whether a lasting preset of this ability is running for the player. */
    public static boolean running(ServerPlayer player, String key) {
        return RUNNING.stream().anyMatch(r -> key.equals(r.key) && r.owner.equals(player.getUUID()));
    }

    /** Ends the player's lasting preset of this ability; returns whether one was running. */
    public static boolean stop(ServerPlayer player, String key) {
        List<Running> ended = RUNNING.stream().filter(r -> key.equals(r.key) && r.owner.equals(player.getUUID())).toList();
        RUNNING.removeAll(ended);
        ended.forEach(r -> r.task.stop());
        return !ended.isEmpty();
    }

    /** Ends all the player's lasting presets (the suit came off). */
    public static void stopLasting(ServerPlayer player) {
        List<Running> ended = RUNNING.stream().filter(r -> r.key != null && r.owner.equals(player.getUUID())).toList();
        RUNNING.removeAll(ended);
        ended.forEach(r -> r.task.stop());
    }

    /** Ends one task of the player now, if it is still running. */
    public static void cancel(ServerPlayer player, Task task) {
        List<Running> ended = RUNNING.stream().filter(r -> r.task == task && r.owner.equals(player.getUUID())).toList();
        RUNNING.removeAll(ended);
        ended.forEach(r -> r.task.stop());
    }

    /**
     * Leaves something the ability can take back: the next press of it (with no need of energy,
     * cooldown) runs {@code undo} instead of using the ability again.
     */
    public static void undoable(PresetContext c, Runnable undo) {
        UNDO.put(c.player().getUUID() + "/" + key(c), undo);
    }

    /** Takes back what this ability of the player left, if anything; returns whether it did. */
    public static boolean undo(ServerPlayer player, String key) {
        Runnable undo = UNDO.remove(player.getUUID() + "/" + key);
        if (undo == null) return false;
        undo.run();
        return true;
    }

    /** Drops what this ability of the player left to take back, without taking it back. */
    public static void forget(ServerPlayer player, String key) {
        UNDO.remove(player.getUUID() + "/" + key);
    }

    /** Whether this ability of the player left something to take back. */
    public static boolean undoable(ServerPlayer player, String key) {
        return UNDO.containsKey(player.getUUID() + "/" + key);
    }

    /** How many tasks of this player are running (for the tests). */
    public static int running(ServerPlayer player) {
        return (int) RUNNING.stream().filter(r -> r.owner.equals(player.getUUID())).count();
    }

    private static void tick(MinecraftServer server) {
        if (RUNNING.isEmpty()) return;
        List<Running> now = List.copyOf(RUNNING);
        List<Running> done = new ArrayList<>();
        for (Running running : now) {
            if (!RUNNING.contains(running)) continue;
            ServerPlayer player = running.player;
            if (player.isRemoved() || !player.isAlive() || server.getPlayerList().getPlayer(running.owner) != player) {
                done.add(running);
                continue;
            }
            running.age++;
            if (!running.task.tick(running.age)) done.add(running);
        }
        RUNNING.removeAll(done);
        done.forEach(r -> r.task.stop());
    }
}
