package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The 6 skeletons of the characters (catalog, stage 7). With the full suit on, the player is drawn
 * as the character: the skin on the skeleton's model. The size only changes the look; the collision
 * box stays the player's, so everyone fits through 2-block doors and tunnels.
 *
 * @param slim      thin 3-pixel arms (the Alex model) instead of 4-pixel ones
 * @param height    visual height over the player's
 * @param width     visual width (shoulders and chest) over the player's
 * @param lean      degrees the body leans forward (creatures walk bent over)
 * @param wings     two wing bones on the back, open in flight
 */
public enum Skeleton implements StringRepresentable {
    MALE("masculino", false, 1.0F, 1.0F, 0.0F, false),
    FEMALE("feminino", true, 1.0F, 1.0F, 0.0F, false),
    YOUNG("jovem", true, 0.9F, 0.9F, 0.0F, false),
    BRUTE("brutamontes", false, 1.2F, 1.35F, 0.0F, false),
    CREATURE("criatura", false, 1.1F, 1.15F, 12.0F, false),
    WINGED("alado", false, 1.0F, 1.0F, 0.0F, true);

    public static final Codec<Skeleton> CODEC = StringRepresentable.fromEnum(Skeleton::values);

    private final String id;
    private final boolean slim;
    private final float height;
    private final float width;
    private final float lean;
    private final boolean wings;

    Skeleton(String id, boolean slim, float height, float width, float lean, boolean wings) {
        this.id = id;
        this.slim = slim;
        this.height = height;
        this.width = width;
        this.lean = lean;
        this.wings = wings;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public boolean slim() {
        return slim;
    }

    public float height() {
        return height;
    }

    public float width() {
        return width;
    }

    public float lean() {
        return lean;
    }

    public boolean wings() {
        return wings;
    }

    public Component displayName() {
        return Component.translatable("skeleton.greenlantern." + id);
    }
}
