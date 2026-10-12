package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroPower;
import net.minecraft.network.chat.Component;

/**
 * An ability of a character of the engine on the power wheel, built from its data file. The name
 * and description come from {@code power.greenlantern.<id>} in the lang files, or the name in the
 * file when there is none yet. Its icon is drawn from its preset and colour ({@link AbilityIcons}),
 * on the character's own sheet in wheel order. {@code own} is the power of the hero's own code its
 * default function uses (the first 9 heroes), or null.
 */
public record EngineAbility(CharacterData.Ability data, int ordinal, AbilityKind kind, HeroPower own) implements HeroPower {
    /** An ability that runs on presets only. */
    public EngineAbility(CharacterData.Ability data, int ordinal, AbilityKind kind) {
        this(data, ordinal, kind, null);
    }

    @Override
    public String id() {
        return data.id();
    }

    /**
     * The cost shown on the wheel: that of the hero's own power when the default function is one
     * ({@code own}, the first 9 heroes), else the ability's.
     */
    @Override
    public int cost() {
        return own != null ? own.cost() : data.numbers().cost();
    }

    /** What a function on a preset costs (the ability's {@code custo}); the hero's own powers pay their own. */
    public int presetCost() {
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
