package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroPower;
import java.util.function.IntSupplier;
import net.minecraft.network.chat.Component;

/**
 * An ability of a character of the engine on the power wheel, built from its data file. The name
 * and description come from {@code power.greenlantern.<id>} in the lang files, or the name in the
 * file when there is none yet. Its icon is drawn from its preset and colour ({@link AbilityIcons}),
 * on the character's own sheet in wheel order. For a hero of before whose ability goes off by
 * default as one of its powers of before ({@link OwnPowers}), {@code ownCost} is that power's cost.
 */
public record EngineAbility(CharacterData.Ability data, int ordinal, AbilityKind kind, IntSupplier ownCost) implements HeroPower {
    public EngineAbility(CharacterData.Ability data, int ordinal, AbilityKind kind) {
        this(data, ordinal, kind, null);
    }

    @Override
    public String id() {
        return data.id();
    }

    @Override
    public int cost() {
        return ownCost != null ? ownCost.getAsInt() : data.numbers().cost();
    }

    @Override
    public int iconIndex() {
        return ordinal;
    }

    /** Cooldown in ticks (every ability has one, even a short one). */
    public int cooldownTicks() {
        return Math.max(10, (int) Math.round(data.numbers().cooldown() * 20.0));
    }

    /** The character's supreme ability: stronger, with a gold border on the wheel and the HUD. */
    public boolean supreme() {
        return data.supreme();
    }

    @Override
    public Component displayName() {
        return Component.translatableWithFallback("power.greenlantern." + id(), data.name().orElse(id()));
    }

    @Override
    public Component description() {
        return Component.translatableWithFallback("power.greenlantern." + id() + ".desc", "");
    }
}
