package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Where an ability sits on the wheel: 3 main ones, then 2 secondary ones, then the Supreme. */
public enum AbilitySlot implements StringRepresentable {
    MAIN("principal"),
    SECONDARY("secundaria"),
    SUPREME("suprema");

    public static final Codec<AbilitySlot> CODEC = StringRepresentable.fromEnum(AbilitySlot::values);

    private final String id;

    AbilitySlot(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
