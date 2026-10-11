package com.danrod505.greenlantern.gametest;

import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.event.TickEvent;
import com.danrod505.greenlantern.gametest.scripts.ClientScripts;

/**
 * Development-only visual smoke test. Launch with {@code GL_CLIENT_SCRIPT=<script> ./gradlew runClient -Pgametests}:
 * creates a world, plays the script's steps and saves screenshots to {@code run/screenshots}.
 * <p>
 * Each script lives in its own class under {@code gametest/scripts} (one per hero or place) and is
 * listed in {@link ClientScripts}; this class runs it and holds the helpers every script uses.
 */
public final class ClientScript {
    /**
     * A screenshot script: its name (the {@code GL_CLIENT_SCRIPT} value), the method that queues its
     * steps, an optional logger called every 10 ticks (or null) and whether it needs a normal world
     * with real oceans instead of a flat one.
     */
    public record Script(String name, Runnable build, java.util.function.Consumer<Minecraft> log, boolean normalWorld) {}

    private static Script script;

    private record Step(int delay, Runnable action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static int stepIndex;
    private static int wait;
    private static boolean worldRequested;

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static ServerPlayer serverPlayer() {
        return mc().getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    }

    public static void server(java.util.function.Consumer<ServerPlayer> action) {
        mc().getSingleplayerServer().execute(() -> action.accept(serverPlayer()));
    }

    public static void command(String command) {
        var server = mc().getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    public static void look(float yaw, float pitch) {
        var player = mc().player;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        server(sp -> {
            sp.setYRot(yaw);
            sp.setXRot(pitch);
            sp.setYHeadRot(yaw);
        });
    }

    public static void shot(String name) {
        Screenshot.grab(mc().gameDirectory, "gl_" + name + ".png", mc().getMainRenderTarget(), 1, msg -> {});
    }

    public static void camera(CameraType type) {
        mc().options.setCameraType(type);
    }

    public static void select(Construct construct) {
        server(sp -> ConstructRegistry.select(sp.getMainHandItem(), construct));
    }

    public static void use() {
        server(sp -> sp.getMainHandItem().getItem().use(sp.level(), sp, InteractionHand.MAIN_HAND));
    }

    public static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static java.util.function.BooleanSupplier hold;
    private static int holdLeft;

    /** Holds the next steps until the condition holds (or for at most {@code maxTicks} ticks). */
    public static void waitFor(int maxTicks, java.util.function.BooleanSupplier condition) {
        step(0, () -> {
            hold = condition;
            holdLeft = maxTicks;
        });
    }

    public static void key(net.minecraft.client.KeyMapping mapping, boolean down) {
        mapping.setDown(down);
    }

    public static final java.util.Set<String> TAKEN = new java.util.HashSet<>();

    private record Watcher(java.util.function.BooleanSupplier condition, String name) {}

    private static final List<Watcher> WATCHERS = new ArrayList<>();

    /** From now on, takes the screenshot the first tick the condition holds (checked every tick, in parallel with the steps). */
    public static void watch(java.util.function.BooleanSupplier condition, String name) {
        step(0, () -> WATCHERS.add(new Watcher(condition, name)));
    }

    /** Watches the condition for the next {@code ticks} ticks, blocking the steps; shoots anyway at the end. */
    public static void watch(int ticks, java.util.function.BooleanSupplier condition, String name) {
        for (int i = 0; i < ticks; i++) {
            step(1, () -> {
                if (!TAKEN.contains(name) && condition.getAsBoolean()) {
                    TAKEN.add(name);
                    shot(name);
                }
            });
        }
        step(0, () -> {
            if (TAKEN.add(name)) shot(name + "_late");
        });
    }

    public static void tickWatchers() {
        WATCHERS.removeIf(w -> {
            if (TAKEN.contains(w.name())) return true;
            if (!w.condition().getAsBoolean()) return false;
            TAKEN.add(w.name());
            shot(w.name());
            return true;
        });
    }

    /** Screenshot without the HUD and chat, so the model is easy to see. */
    public static void clean(String name) {
        mc().gui.getChat().clearMessages(false);
        mc().options.hideGui = true;
        shot(name);
    }

    public static void clearMobs() {
        server(sp -> {
            for (var mob : sp.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, sp.getBoundingBox().inflate(80))) mob.discard();
        });
    }

    private ClientScript() {}

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientScript::tick);
        script = ClientScripts.byName(System.getenv("GL_CLIENT_SCRIPT"));
        script.build().run();
    }

    private static void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = mc();
        if (mc.level == null || mc.player == null) {
            if (!worldRequested && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                mc.options.onboardAccessibility = false;
                worldRequested = true;
                LevelSettings settings = new LevelSettings("gltest", GameType.CREATIVE, false, Difficulty.EASY, true,
                        new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
                // Atlantis (and the Trench around it) needs a real ocean: a normal world for those scripts, a flat one for the others.
                boolean normal = script.normalWorld();
                mc.createWorldOpenFlows().createFreshLevel("gltest-" + System.currentTimeMillis(), settings,
                        new WorldOptions(2814L, false, false), normal ? WorldPresets::createNormalWorldDimensions : WorldPresets::createFlatWorldDimensions, mc.screen);
            }
            return;
        }
        if (mc.player.tickCount % 10 == 0 && script.log() != null) script.log().accept(mc);
        tickWatchers();
        if (hold != null) {
            if (hold.getAsBoolean() || --holdLeft <= 0) {
                hold = null;
            } else {
                return;
            }
        }
        if (stepIndex >= STEPS.size()) return;
        if (++wait >= STEPS.get(stepIndex).delay()) {
            wait = 0;
            STEPS.get(stepIndex++).action().run();
        }
    }
}
