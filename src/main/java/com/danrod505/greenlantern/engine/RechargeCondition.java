package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * When the energy of a class fills faster (catalog, stage 5): the recharge of the class is
 * multiplied while the condition holds (see {@link EngineServer#rechargePerSecond}).
 */
public enum RechargeCondition implements StringRepresentable {
    /** Never comes back on its own (the ring is charged at a Power Battery). */
    NEVER("nunca"),
    /** Always the same. */
    ALWAYS("sempre"),
    /** The character's source ({@code fonte}): sun in the open sky, a thunderstorm or the heat of lava and the Nether. */
    SOURCE("fonte"),
    /** Running. */
    RUNNING("correndo"),
    /** In the dark or crouching. */
    DARK("escuro"),
    /** Only after 2 seconds without using an ability, and then fast. */
    PAUSE("pausa"),
    /** At night (more with a full moon). */
    NIGHT("noite"),
    /** Near powered redstone. */
    REDSTONE("redstone"),
    /** Near the character's element ({@code fonte}: fire, water, ice, plants, lightning, wind or sun). */
    ELEMENT("elemento"),
    /** With a full stomach (and nothing at all while hungry). */
    FULL("fome_cheia");

    public static final Codec<RechargeCondition> CODEC = StringRepresentable.fromEnum(RechargeCondition::values);

    private final String id;

    RechargeCondition(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
