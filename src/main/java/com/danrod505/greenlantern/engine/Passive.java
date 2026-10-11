package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The passive of each class (catalog, stage 12), always on with the full suit. Only the characters
 * of the engine have one: the first 9 heroes keep exactly what they had (see {@link EngineServer}).
 */
public enum Passive implements StringRepresentable {
    /** Ring bearer: the first 2 hearts of damage of each fight come out of the energy instead of the health. */
    LIGHT_SHIELD("escudo_de_luz"),
    /** Colossus: no fall damage and explosions don't push. */
    UNSHAKEABLE("inabalavel"),
    /** Speedster: steps up a block without jumping and takes no fall damage while running. */
    QUICK_STEP("passo_rapido"),
    /** Vigilante: sees in the dark. */
    NIGHT_EYES("olhos_da_noite"),
    /** Marksman: arrows and shots don't drop with distance (used by the ballistic preset). */
    STEADY_AIM("mira_firme"),
    /** Mystic: invisible creatures nearby glow. */
    SEE_HIDDEN("ver_o_oculto"),
    /** Warrior: blocking with a shield gives back some Fury. */
    FURY_BLOCK("bloqueio_da_furia"),
    /** Inventor: sees the health of the creature aimed at. */
    SEE_HEALTH("ver_a_vida"),
    /** Elementalist: immune to their own element. */
    ELEMENT_IMMUNE("imune_ao_elemento"),
    /** Shapeshifter: food fills more and health comes back faster with a full stomach. */
    HEARTY("comida_reforcada");

    public static final Codec<Passive> CODEC = StringRepresentable.fromEnum(Passive::values);

    private final String id;

    Passive(String id) {
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
