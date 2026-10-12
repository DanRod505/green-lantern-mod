package com.danrod505.greenlantern.construct;

import com.danrod505.greenlantern.hero.HeroPower;
import net.minecraft.network.chat.Component;

/**
 * A construct of the Power Ring as one of the Lantern's own powers (stage 3): the functions of the
 * Lantern's wheel with the preset {@code poder} name it by the path of its id ({@code "hammer"}), and
 * its icon is the one in {@code textures/gui/constructs.png}.
 */
public record ConstructPower(Construct construct, int ordinal) implements HeroPower {
    @Override
    public String id() {
        return construct.id().getPath();
    }

    @Override
    public int cost() {
        return construct.activationCost();
    }

    @Override
    public int iconIndex() {
        return construct.iconIndex();
    }

    @Override
    public Component displayName() {
        return construct.name();
    }

    @Override
    public Component description() {
        return construct.description();
    }
}
