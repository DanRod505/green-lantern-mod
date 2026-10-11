package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.cyborg.CyborgHero;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.ring.LanternHero;
import com.danrod505.greenlantern.shazam.ShazamHero;
import com.danrod505.greenlantern.supergirl.SupergirlHero;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The heroes of the mod, in priority order: the first 9 heroes, with their own packages, then every
 * character of the engine, built from its file in {@code data/greenlantern/characters} (see
 * {@link Characters}). Decides which hero the shared keys (suit up, wheel, power) talk to: the hero
 * whose suit is worn, else the one whose item is in hand, else the first item carried (earlier
 * heroes first).
 */
public final class HeroRegistry {
    private static final List<HeroDefinition> HEROES = build();

    private static List<HeroDefinition> build() {
        List<HeroDefinition> heroes = new ArrayList<>(List.of(
                LanternHero.INSTANCE,
                FlashHero.INSTANCE,
                AquamanHero.INSTANCE,
                BatmanHero.INSTANCE,
                SupermanHero.INSTANCE,
                WonderWomanHero.INSTANCE,
                CyborgHero.INSTANCE,
                SupergirlHero.INSTANCE,
                ShazamHero.INSTANCE));
        Characters.engine().forEach(data -> heroes.add(EngineHero.create(data)));
        return List.copyOf(heroes);
    }

    private HeroRegistry() {}

    public static List<HeroDefinition> all() {
        return HEROES;
    }

    public static Optional<HeroDefinition> byId(String id) {
        for (HeroDefinition hero : HEROES) {
            if (hero.id().equals(id)) return Optional.of(hero);
        }
        return Optional.empty();
    }

    /** The hero whose suit the player is wearing. */
    public static Optional<HeroDefinition> suited(Player player) {
        for (HeroDefinition hero : HEROES) {
            if (hero.isSuited(player)) return Optional.of(hero);
        }
        return Optional.empty();
    }

    /** The hero the shared keys control right now (see the class comment). */
    public static Optional<HeroDefinition> context(Player player) {
        Optional<HeroDefinition> suited = suited(player);
        if (suited.isPresent()) return suited;
        for (HeroDefinition hero : HEROES) {
            if (!hero.heldItem(player).isEmpty()) return Optional.of(hero);
        }
        for (HeroDefinition hero : HEROES) {
            if (!hero.findItem(player).isEmpty()) return Optional.of(hero);
        }
        return Optional.empty();
    }

    /** Only one hero suit at a time: takes off every suit except the given hero's. */
    public static void dismissOthers(ServerPlayer player, HeroDefinition keep) {
        for (HeroDefinition hero : HEROES) {
            if (hero != keep && hero.isSuited(player)) hero.dismissSuit(player, false);
        }
    }
}
