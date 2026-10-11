package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The 10 classes of character (catalog, stage 2). The class decides the character's energy (how it
 * fills and what it is called), the rule its stats come from and its passive; the numbers live in
 * {@code data/greenlantern/engine/classes.json} (see {@link ClassRules}).
 */
public enum CharacterClass implements StringRepresentable {
    RING_BEARER("anel"),
    COLOSSUS("colosso"),
    SPEEDSTER("velocista"),
    VIGILANTE("vigilante"),
    MARKSMAN("atirador"),
    MYSTIC("mistico"),
    WARRIOR("guerreiro"),
    INVENTOR("inventor"),
    ELEMENTALIST("elemental"),
    SHAPESHIFTER("metamorfo");

    public static final Codec<CharacterClass> CODEC = StringRepresentable.fromEnum(CharacterClass::values);

    private final String id;

    CharacterClass(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** The class name ("Colosso"). */
    public Component displayName() {
        return Component.translatable("class.greenlantern." + id);
    }

    /** The name of the class energy ("Reserva de Poder"). */
    public Component energyName() {
        return Component.translatable("energy.greenlantern." + id);
    }

    public ClassRules.ClassRule rule() {
        return ClassRules.get().rule(this);
    }
}
