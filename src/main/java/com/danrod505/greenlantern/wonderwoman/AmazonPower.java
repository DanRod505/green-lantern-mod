package com.danrod505.greenlantern.wonderwoman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Powers of Wonder Woman, picked on the power wheel (hold the wheel key) and used with the power key
 * or right click on the Tiara of Themyscira.
 * <p>
 * To add a power: add a constant here (its icon goes in {@code textures/gui/wonder_woman_powers.png}
 * at the same index), its name / description to the lang files and its behaviour to
 * {@link WonderWomanServer#usePower}.
 */
public enum AmazonPower {
    /** The Lasso of Truth catches a creature: it is bound, can't run and won't fight while held. */
    LASSO_CAPTURE("lasso_capture"),
    /** The lasso yanks a creature to you; thrown at a block, it pulls you there. */
    LASSO_PULL("lasso_pull"),
    /** The lasso whirls around you, sweeping everything away (or swings the creature caught in it and hurls it). */
    LASSO_SPIN("lasso_spin"),
    /** Bracelets of Submission raised: blows are blocked and projectiles bounce back where they came from. */
    BRACELET_GUARD("bracelet_guard"),
    /** The bracelets clash together: a shockwave that throws everything around (stronger after blocking hits). */
    BRACELET_SHOCKWAVE("bracelet_shockwave"),
    /** The Amazon sword in your hand and the shield on your arm (use again to put them away). */
    SWORD_AND_SHIELD("sword_and_shield"),
    /** The shield flies, bounces from enemy to enemy and comes back to your arm. */
    SHIELD_THROW("shield_throw"),
    /** The Invisible Jet lands next to you and you climb aboard. */
    INVISIBLE_JET("invisible_jet");

    private static final AmazonPower[] VALUES = values();

    private final String id;

    AmazonPower(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public int iconIndex() {
        return ordinal();
    }

    public Component displayName() {
        return Component.translatable("power.greenlantern." + id);
    }

    public Component description() {
        return Component.translatable("power.greenlantern." + id + ".desc");
    }

    /** Divine power needed to use the power (per second for the bracelet guard). */
    public int cost() {
        return switch (this) {
            case LASSO_CAPTURE -> GLConfig.LASSO_CAPTURE_COST.get();
            case LASSO_PULL -> GLConfig.LASSO_PULL_COST.get();
            case LASSO_SPIN -> GLConfig.LASSO_SPIN_COST.get();
            case BRACELET_GUARD -> GLConfig.BRACELET_GUARD_COST_PER_SECOND.get();
            case BRACELET_SHOCKWAVE -> GLConfig.BRACELET_SHOCKWAVE_COST.get();
            case SWORD_AND_SHIELD -> GLConfig.SWORD_AND_SHIELD_COST.get();
            case SHIELD_THROW -> GLConfig.SHIELD_THROW_COST.get();
            case INVISIBLE_JET -> GLConfig.INVISIBLE_JET_COST.get();
        };
    }

    public static AmazonPower byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, VALUES.length - 1)];
    }

    public static int count() {
        return VALUES.length;
    }

    public static AmazonPower selected(ItemStack tiara) {
        Integer index = tiara.get(ModDataComponents.SELECTED_POWER.get());
        return byIndex(index == null ? 0 : index);
    }

    public static void select(ItemStack tiara, AmazonPower power) {
        tiara.set(ModDataComponents.SELECTED_POWER.get(), power.ordinal());
    }

    public static AmazonPower cycle(ItemStack tiara, int offset) {
        AmazonPower next = VALUES[Math.floorMod(selected(tiara).ordinal() + offset, VALUES.length)];
        select(tiara, next);
        return next;
    }
}
