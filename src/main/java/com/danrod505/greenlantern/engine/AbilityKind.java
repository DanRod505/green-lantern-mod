package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The 10 classes of ability (catalog, stage 3). Each character has 6 abilities: 3 main ones, 2
 * secondary ones and the Supreme, always in that order on the wheel. The kind also picks the
 * ability's icon on the shared icon sheet until the character has its own.
 */
public enum AbilityKind implements StringRepresentable {
    STRIKE("golpe"),
    SHOT("disparo"),
    BLAST("explosao"),
    MOVEMENT("movimento"),
    DEFENSE("defesa"),
    CONTROL("controle"),
    SUMMON("invocacao"),
    EVASION("evasao"),
    EMPOWER("fortalecimento"),
    SUPREME("suprema");

    public static final Codec<AbilityKind> CODEC = StringRepresentable.fromEnum(AbilityKind::values);

    private final String id;

    AbilityKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
