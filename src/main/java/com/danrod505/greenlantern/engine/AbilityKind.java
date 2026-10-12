package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The 15 classes of ability (the new model of abilities, approved on 2026-10-12): 8 for fighting and
 * 7 for the world. The class of an ability is its main one: it picks the icon, its place on the wheel
 * (the fighting ones first, then the world ones, the supreme last) and its numbers. An ability may
 * also have functions of other classes (see {@link CharacterData.Function}).
 */
public enum AbilityKind implements StringRepresentable {
    STRIKE("golpe", true),
    SHOT("disparo", true),
    BLAST("explosao", true),
    CONTROL("controle", true),
    DEFENSE("defesa", true),
    SUMMON("invocacao", true),
    EVASION("evasao", true),
    EMPOWER("fortalecimento", true),
    MOBILITY("mobilidade", false),
    GATHERING("coleta_e_terreno", false),
    BUILDING("construcao", false),
    TRANSMUTATION("transmutacao", false),
    NATURE("natureza", false),
    DETECTION("deteccao", false),
    LOGISTICS("logistica", false);

    public static final Codec<AbilityKind> CODEC = StringRepresentable.fromEnum(AbilityKind::values);

    private final String id;
    private final boolean fight;

    AbilityKind(String id, boolean fight) {
        this.id = id;
        this.fight = fight;
    }

    public String id() {
        return id;
    }

    /** A fighting class (the other 7 are for the world). */
    public boolean fight() {
        return fight;
    }

    /** The class name ("Coleta e Terreno"). */
    public Component displayName() {
        return Component.translatable("ability_class.greenlantern." + id);
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
