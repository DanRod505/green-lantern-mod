package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** A bonus the suit gives while it is worn (strength, speed, reach...), as a transient attribute modifier. */
public record SuitModifier(Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
    /** A flat bonus ({@code ADD_VALUE}); {@code id} is a path in the mod's namespace, e.g. {@code "batman_strength"}. */
    public static SuitModifier add(Holder<Attribute> attribute, String id, double amount) {
        return new SuitModifier(attribute, GreenLantern.id(id), amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** A bonus proportional to the base value ({@code ADD_MULTIPLIED_BASE}). */
    public static SuitModifier multiply(Holder<Attribute> attribute, String id, double amount) {
        return new SuitModifier(attribute, GreenLantern.id(id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** Adds the modifier (if missing) or removes it. */
    public void apply(ServerPlayer player, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (on) {
            if (!instance.hasModifier(id)) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
            }
        } else {
            instance.removeModifier(id);
        }
    }
}
