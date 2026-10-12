package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The powers of today of the first 9 heroes, as presets (stage 3, lot 1: "Adaptação dos 9 heróis").
 * Their abilities in the new model call the power they come from by name, so the Mecha, the Kraken,
 * the Batmobile or the Boom Tube are the same as before, only now on the new wheel:
 * <ul>
 *     <li>poder_de_hoje: the power {@code poder} of the hero's wheel of today ({@code "batmobile"},
 *     {@code "kraken"}...), exactly as it was;</li>
 *     <li>construto_de_hoje: the Lantern's construct {@code construto} ({@code "hammer"},
 *     {@code "mecha"}...); a construct that fired while the ring was held (the minigun) fires by
 *     itself for {@code segundos} seconds (3), and pressing the ability again stops it.</li>
 * </ul>
 * A power of today pays its own energy and keeps its own cooldown (the numbers are balanced in lot 3),
 * so the engine takes no cost for these and waits only half a second; a power that could not go
 * off (no target, no water) already told the player why.
 */
public final class LegacyPresets {
    /** The short wait between two uses: a power of today keeps its own cooldown. */
    private static final int WAIT_TICKS = 10;
    /** The players whose held construct (the minigun) works by itself right now. */
    private static final Set<UUID> FIRING = ConcurrentHashMap.newKeySet();

    private LegacyPresets() {}

    public static void register() {
        Presets.registerFight("poder_de_hoje", LegacyPresets::power);
        Presets.registerFight("construto_de_hoje", LegacyPresets::construct);
    }

    // ---- poder_de_hoje -------------------------------------------------------------------------------

    private static Outcome power(PresetContext c) {
        String id = c.settings().text("poder", "");
        HeroPowers<?> legacy = HeroRegistry.byId(c.hero().id()).map(h -> h.legacyPowers()).orElse(null);
        if (legacy == null || legacy == c.hero().powers()) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_power", id));
        return run(legacy, c, id);
    }

    private static <P extends HeroPower> Outcome run(HeroPowers<P> legacy, PresetContext c, String id) {
        for (int i = 0; i < legacy.set().count(); i++) {
            P power = legacy.set().byIndex(i);
            if (!power.id().equals(id)) continue;
            return legacy.action().use(c.player(), c.item(), power) ? Outcome.cooldown(WAIT_TICKS) : Outcome.fail(null);
        }
        return Outcome.fail(Component.translatable("message.greenlantern.engine_no_power", id));
    }

    /** Whether the hero has a power of today with this id (for the tests and the check of the files). */
    public static boolean hasPower(String heroId, String id) {
        HeroPowers<?> legacy = HeroRegistry.byId(heroId).map(h -> h.legacyPowers()).orElse(null);
        if (legacy == null) return false;
        for (int i = 0; i < legacy.set().count(); i++) {
            if (legacy.power(i).id().equals(id)) return true;
        }
        return false;
    }

    // ---- construto_de_hoje ---------------------------------------------------------------------------

    /** The construct of the ring with this name ({@code hammer}, {@code minigun}, {@code mecha}...), or null. */
    public static Construct construct(String name) {
        for (Construct construct : ConstructRegistry.all()) {
            if (construct.id().equals(GreenLantern.id(name))) return construct;
        }
        return null;
    }

    private static Outcome construct(PresetContext c) {
        String name = c.settings().text("construto", "");
        Construct construct = construct(name);
        ServerPlayer player = c.player();
        ItemStack ring = c.item();
        if (construct == null || !RingHelper.isRing(ring)) return Outcome.fail(Component.translatable("message.greenlantern.engine_no_power", name));
        boolean toggle = construct.useMode() == Construct.UseMode.TOGGLE;
        // As the ring did: a construct that is up can always go away; only a new one needs energy.
        if (!toggle && !player.isCreative() && !RingEnergy.has(ring, Math.max(1, construct.activationCost()))) {
            PowerRingItem.notifyNoEnergy(player);
            return Outcome.fail(null);
        }
        if (construct.useMode() == Construct.UseMode.HOLD) return hold(c, construct);
        if (!construct.activate(player, ring)) return Outcome.fail(null);
        if (construct.cooldown() > 0) player.getCooldowns().addCooldown(ring, construct.cooldown());
        return Outcome.cooldown(Math.max(WAIT_TICKS, construct.cooldown()));
    }

    /** Whether the player's held construct works by itself now (the minigun stays up without the ring held). */
    public static boolean firesByItself(Player player) {
        return FIRING.contains(player.getUUID());
    }

    /** A construct that worked while the ring was held now works by itself for a few seconds. */
    private static Outcome hold(PresetContext c, Construct construct) {
        ServerPlayer player = c.player();
        FIRING.add(player.getUUID());
        if (!construct.activate(player, c.item())) {
            FIRING.remove(player.getUUID());
            return Outcome.fail(null);
        }
        int ticks = Math.max(20, (int) Math.round(c.settings().number("segundos", 3.0) * 20.0));
        int[] held = {0};
        return PresetRuntime.lasting(c, new PresetRuntime.Task() {
            @Override
            public boolean tick(int age) {
                ItemStack ring = RingHelper.findRing(player);
                if (ring.isEmpty() || !RingHelper.isSuited(player) || !player.isAlive()) return false;
                held[0] = age;
                construct.holdTick(player, ring, age);
                return age < ticks;
            }

            @Override
            public void stop() {
                FIRING.remove(player.getUUID());
                ItemStack ring = RingHelper.findRing(player);
                if (!ring.isEmpty()) construct.release(player, ring, held[0]);
            }
        });
    }
}
