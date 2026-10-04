package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanSuit;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.batman.BatmanSuit;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.FlashSuit;
import com.danrod505.greenlantern.ring.RingHelper;
import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The heroes of the mod. Decides which hero the shared keys (suit up, wheel, power) talk to: the
 * hero whose suit is worn, else the one whose item is in hand, else the first item carried.
 */
public enum Hero {
    NONE,
    LANTERN,
    FLASH,
    AQUAMAN,
    BATMAN;

    /** The hero whose suit the player is wearing, or {@link #NONE}. */
    public static Hero suited(Player player) {
        if (RingHelper.isSuited(player)) return LANTERN;
        if (FlashHelper.isSuited(player)) return FLASH;
        if (AquamanHelper.isSuited(player)) return AQUAMAN;
        if (BatmanHelper.isSuited(player)) return BATMAN;
        return NONE;
    }

    /** Whether this hero picks powers on the power wheel (every hero but the Lantern, who has constructs). */
    public boolean hasPowers() {
        return this == FLASH || this == AQUAMAN || this == BATMAN;
    }

    /** The hero the shared keys control right now (see the class comment), or {@link #NONE}. */
    public static Hero context(Player player) {
        Hero suited = suited(player);
        if (suited != NONE) return suited;
        if (!RingHelper.heldRing(player).isEmpty()) return LANTERN;
        if (!FlashHelper.heldRing(player).isEmpty()) return FLASH;
        if (!AquamanHelper.heldEmblem(player).isEmpty()) return AQUAMAN;
        if (!BatmanHelper.heldBelt(player).isEmpty()) return BATMAN;
        if (!RingHelper.findRing(player).isEmpty()) return LANTERN;
        if (!FlashHelper.findRing(player).isEmpty()) return FLASH;
        if (!AquamanHelper.findEmblem(player).isEmpty()) return AQUAMAN;
        if (!BatmanHelper.findBelt(player).isEmpty()) return BATMAN;
        return NONE;
    }

    /** Only one hero suit at a time: takes off every suit except the given hero's. */
    public static void dismissOthers(ServerPlayer player, Hero keep) {
        if (keep != LANTERN && RingHelper.isSuited(player)) Uniform.dismiss(player, false);
        if (keep != FLASH && FlashHelper.isSuited(player)) FlashSuit.dismiss(player, false);
        if (keep != AQUAMAN && AquamanHelper.isSuited(player)) AquamanSuit.dismiss(player, false);
        if (keep != BATMAN && BatmanHelper.isSuited(player)) BatmanSuit.dismiss(player, false);
    }
}
