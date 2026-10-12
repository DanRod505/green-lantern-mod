package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroPower;
import net.minecraft.network.chat.Component;

/**
 * An ability of a character of the engine on the power wheel, built from its data file. The name
 * and description come from {@code power.greenlantern.<id>} in the lang files, or the name in the
 * file when there is none yet. Its icon is drawn from its preset and colour ({@link AbilityIcons}),
 * on the character's own sheet in wheel order.
 */
public record EngineAbility(CharacterData.Ability data, int ordinal, AbilityKind kind) implements HeroPower {
    @Override
    public String id() {
        return data.id();
    }

    @Override
    public int cost() {
        return data.numbers().cost();
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
