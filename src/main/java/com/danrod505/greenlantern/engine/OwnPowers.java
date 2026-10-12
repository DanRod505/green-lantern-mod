package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The powers a hero had before the new model of abilities (heat vision, the batarang, the hammer of
 * the ring...), by id, so a function of an ability can run one: in the file it names the preset
 * {@value #PRESET} with {@code "poder": "<id>"} in its settings. Such a function runs exactly as the
 * power always did, paying its own energy and starting its own recharge; the engine adds no cost nor
 * cooldown of its own.
 */
public final class OwnPowers {
    /** The preset of a function that runs a power of before. */
    public static final String PRESET = "poder_de_hoje";
    public static final OwnPowers NONE = new OwnPowers();

    /** Uses a power; returns whether it went off. */
    @FunctionalInterface
    public interface Use {
        boolean use(ServerPlayer player, ItemStack item);
    }

    private record Entry(IntSupplier cost, Use use) {}

    private final Map<String, Entry> powers = new LinkedHashMap<>();

    public OwnPowers add(String id, IntSupplier cost, Use use) {
        if (powers.put(id, new Entry(cost, use)) != null) throw new IllegalStateException("Power " + id + " twice");
        return this;
    }

    /** The powers of a hero's enum, used as its power action did. */
    public static <P extends HeroPower> OwnPowers of(P[] values, HeroPowers.PowerAction<P> action) {
        OwnPowers own = new OwnPowers();
        for (P power : values) own.add(power.id(), power::cost, (player, item) -> action.use(player, item, power));
        return own;
    }

    public boolean has(String id) {
        return powers.containsKey(id);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(powers.keySet());
    }

    public int cost(String id) {
        Entry entry = powers.get(id);
        return entry == null ? 0 : Math.max(0, entry.cost().getAsInt());
    }

    public boolean use(ServerPlayer player, ItemStack item, String id) {
        Entry entry = powers.get(id);
        return entry != null && entry.use().use(player, item);
    }

    /** The power of before a function runs ({@code "poder"} in its settings, over the ability's), or null when it runs a preset. */
    public static String power(CharacterData.Function function, CharacterData.Ability ability) {
        if (!PRESET.equals(function.preset())) return null;
        String power = ability.settings(function).text("poder", "");
        return power.isEmpty() ? null : power;
    }
}
