package com.danrod505.greenlantern.engine;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * The ability presets: a preset is a ready, configurable piece (a strong punch, a beam, a dash, a
 * bubble...) that an ability names in its file, with its numbers. The presets themselves come in
 * stage 2 ("Presets de personagem"); this is where they plug in. An ability whose preset doesn't
 * exist yet still spends its energy and starts its cooldown, shows a puff of the character's colours
 * and says it is not built yet, so a character can be tried out before its presets are done.
 */
public final class Presets {
    /** What a preset does with the ability that names it. */
    @FunctionalInterface
    public interface Preset {
        void use(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability);
    }

    private static final Map<String, Preset> FIGHT = new HashMap<>();
    private static final Map<String, Preset> WORLD = new HashMap<>();

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

    static void useInFight(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability) {
        Preset preset = FIGHT.get(ability.data().preset());
        if (preset != null) {
            preset.use(player, hero, item, ability);
        } else {
            notBuilt(player, hero, ability, "message.greenlantern.engine_not_built");
        }
    }

    static void useInWorld(ServerPlayer player, EngineHero hero, ItemStack item, EngineAbility ability) {
        Preset preset = ability.data().worldPreset().map(WORLD::get).orElse(null);
        if (preset != null) {
            preset.use(player, hero, item, ability);
        } else {
            notBuilt(player, hero, ability, "message.greenlantern.engine_world_not_built");
        }
    }

    private static void notBuilt(ServerPlayer player, EngineHero hero, EngineAbility ability, String key) {
        EngineServer.puff(player, hero);
        EngineServer.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F + 0.1F * ability.ordinal());
        player.displayClientMessage(Component.translatable(key, ability.displayName()), true);
    }
}
