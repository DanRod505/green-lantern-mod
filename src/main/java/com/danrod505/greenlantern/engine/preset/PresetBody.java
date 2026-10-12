package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.PresetContext;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;

/**
 * What the presets change in the player's body while they last (speed, size, gravity, step): each
 * change is a modifier named after the ability, never saved, taken off when the preset ends.
 */
public final class PresetBody {
    private PresetBody() {}

    /** The id of one of the ability's modifiers ({@code what}: speed, size...). */
    public static Identifier id(PresetContext c, String what) {
        return GreenLantern.id(("preset_" + c.ability().data().id() + "_" + what).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"));
    }

    /** Adds {@code amount} to the attribute ({@code ADD_VALUE}). */
    public static Identifier add(PresetContext c, Holder<Attribute> attribute, String what, double amount) {
        return put(c, attribute, what, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Multiplies the attribute by {@code 1 + amount} ({@code ADD_MULTIPLIED_TOTAL}). */
    public static Identifier times(PresetContext c, Holder<Attribute> attribute, String what, double amount) {
        return put(c, attribute, what, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static Identifier put(PresetContext c, Holder<Attribute> attribute, String what, double amount, AttributeModifier.Operation operation) {
        Identifier id = id(c, what);
        AttributeInstance instance = c.player().getAttribute(attribute);
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        return id;
    }

    public static void remove(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    /** Sets the player's motion (the client follows it). */
    public static void push(ServerPlayer player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
    }
}
