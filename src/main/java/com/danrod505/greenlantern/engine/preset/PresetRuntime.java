package com.danrod505.greenlantern.engine.preset;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * The presets that go on for a while after the key (a shot in flight, a dash, a beam, a rain of
 * blows): each one is a task ticked every server tick until it says it is done, or until its player
 * leaves or dies. Tasks never outlive the server.
 */
public final class PresetRuntime {
    /** One tick of a running preset ({@code age} counts from 1); returns whether it goes on. */
    @FunctionalInterface
    public interface Task {
        boolean tick(int age);
    }

    private static final class Running {
        final ServerPlayer player;
        final UUID owner;
        final Task task;
        int age;

        Running(ServerPlayer player, Task task) {
            this.player = player;
            this.owner = player.getUUID();
            this.task = task;
        }
    }

    private static final List<Running> RUNNING = new ArrayList<>();

    private PresetRuntime() {}

    public static void register() {
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> tick(event.server()));
    }

    /** Starts a task for {@code player}; it gets its first tick on the next server tick. */
    public static void start(ServerPlayer player, Task task) {
        RUNNING.add(new Running(player, task));
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
            ServerPlayer player = running.player;
            if (player.isRemoved() || !player.isAlive() || server.getPlayerList().getPlayer(running.owner) != player) {
                done.add(running);
                continue;
            }
            running.age++;
            if (!running.task.tick(running.age)) done.add(running);
        }
        RUNNING.removeAll(done);
    }
}
