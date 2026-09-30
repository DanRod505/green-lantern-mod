package com.danrod505.greenlantern.construct;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A hard-light construct that can be created with the Power Ring.
 * <p>
 * To add a new construct: extend this class, register it in {@link ConstructRegistry#bootstrap()}
 * and add its icon to {@code textures/gui/constructs.png} plus its name to the lang files.
 */
public abstract class Construct {
    private final Identifier id;
    private final int iconIndex;

    protected Construct(Identifier id, int iconIndex) {
        this.id = id;
        this.iconIndex = iconIndex;
    }

    public final Identifier id() {
        return id;
    }

    /** Index of the 16x16 icon inside {@code textures/gui/constructs.png}. */
    public final int iconIndex() {
        return iconIndex;
    }

    public Component name() {
        return Component.translatable("construct." + id.getNamespace() + "." + id.getPath());
    }

    public Component description() {
        return Component.translatable("construct." + id.getNamespace() + "." + id.getPath() + ".desc");
    }

    /** How the ring behaves when right-clicked with this construct selected. */
    public abstract UseMode useMode();

    /** Energy required to activate this construct. */
    public abstract int activationCost();

    /** Cooldown applied to the ring after activation (ticks). */
    public int cooldown() {
        return 0;
    }

    /**
     * Server side activation. Energy has already been checked (but not consumed): implementations
     * consume what they need and return whether the construct was actually created.
     */
    public abstract boolean activate(ServerPlayer player, ItemStack ring);

    /** Server side tick while the use key is held ({@link UseMode#HOLD} only). */
    public void holdTick(ServerPlayer player, ItemStack ring, int ticksHeld) {}

    /** Server side, called when the use key is released ({@link UseMode#HOLD} only). */
    public void release(ServerPlayer player, ItemStack ring, int ticksHeld) {}

    public enum UseMode {
        /** Fires once per click. */
        INSTANT,
        /** Keeps working while the use key is held. */
        HOLD,
        /** Click to summon, click again to dismiss. */
        TOGGLE
    }
}
