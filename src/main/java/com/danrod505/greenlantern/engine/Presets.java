package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.engine.preset.BlastPresets;
import com.danrod505.greenlantern.engine.preset.DefensePresets;
import com.danrod505.greenlantern.engine.preset.EvasionPresets;
import com.danrod505.greenlantern.engine.preset.MovementPresets;
import com.danrod505.greenlantern.engine.preset.ShotPresets;
import com.danrod505.greenlantern.engine.preset.StrikePresets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * The ability presets: a preset is a ready, configurable piece (a strong punch, a beam, a dash, a
 * bubble...) that an ability names in its file, with its numbers and settings ({@code "ajustes"}).
 * They come in the batches of stage 2 ("Presets de personagem"), each family in its own class of
 * {@code engine.preset}. An ability whose preset doesn't exist yet still spends its energy and starts
 * its cooldown, shows a puff of the character's colours and says it is not built yet, so a character
 * can be tried out before its presets are done.
 */
public final class Presets {
    /** What a preset does with the ability that names it. */
    @FunctionalInterface
    public interface Preset {
        Outcome use(PresetContext context);
    }

    /**
     * How a use went: it went off (with the ability's cooldown, or another one, like the short wait
     * between the blows of a combo), or it could not (nothing to grab, no ammunition): then the energy
     * comes back, there is no cooldown and the player reads why.
     */
    public record Outcome(boolean went, int cooldownTicks, Component reason) {
        public static final Outcome DONE = new Outcome(true, -1, null);

        /** It went off and waits {@code ticks} instead of the ability's cooldown. */
        public static Outcome cooldown(int ticks) {
            return new Outcome(true, Math.max(1, ticks), null);
        }

        /** It could not go off; the energy comes back. */
        public static Outcome fail(Component reason) {
            return new Outcome(false, 0, reason);
        }
    }

    private static final Map<String, Preset> FIGHT = new TreeMap<>();
    private static final Map<String, Preset> WORLD = new TreeMap<>();

    static {
        StrikePresets.register();
        ShotPresets.register();
        BlastPresets.register();
        MovementPresets.register();
        DefensePresets.register();
        EvasionPresets.register();
    }

    private Presets() {}

    /** Adds a preset for the fight use of abilities ({@code "preset"} in the file). */
    public static void registerFight(String id, Preset preset) {
        if (FIGHT.putIfAbsent(id, preset) != null) throw new IllegalStateException("Preset " + id + " twice");
    }

    /** Adds a preset for the world use of abilities ({@code "uso_no_mundo"} in the file). */
    public static void registerWorld(String id, Preset preset) {
        if (WORLD.putIfAbsent(id, preset) != null) throw new IllegalStateException("World preset " + id + " twice");
    }

    public static boolean hasFight(String id) {
        return FIGHT.containsKey(id);
    }

    public static boolean hasWorld(String id) {
        return WORLD.containsKey(id);
    }

    /** The ids of the fight presets there are, in order. */
    public static Set<String> fightIds() {
        return Collections.unmodifiableSet(FIGHT.keySet());
    }

    static Outcome useInFight(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability) {
        Preset preset = FIGHT.get(ability.data().preset());
        if (preset == null) return notBuilt(player, hero, ability, "message.greenlantern.engine_not_built");
        return preset.use(new PresetContext(player, hero, item, ability));
    }

    static Outcome useInWorld(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability) {
        Preset preset = ability.data().worldPreset().map(WORLD::get).orElse(null);
        if (preset == null) return notBuilt(player, hero, ability, "message.greenlantern.engine_world_not_built");
        return preset.use(new PresetContext(player, hero, item, ability));
    }

    private static Outcome notBuilt(ServerPlayer player, EngineHero hero, EngineAbility ability, String key) {
        EngineServer.puff(player, hero);
        EngineServer.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F + 0.1F * ability.ordinal());
        player.displayClientMessage(Component.translatable(key, ability.displayName()), true);
        return Outcome.DONE;
    }
}
