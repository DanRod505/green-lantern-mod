package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Charge of the Utility Belt's power cells. It refills steadily while the batsuit is worn (faster
 * in the dark: the night belongs to Batman) and pays for the gadgets and the Batmobile's boost.
 */
public final class BatCharge {
    private BatCharge() {}

    public static RingEnergy get(ItemStack belt) {
        RingEnergy value = belt.get(ModDataComponents.BAT_CHARGE.get());
        return value != null ? value : new RingEnergy(0, capacity());
    }

    public static int capacity() {
        return GLConfig.BAT_CHARGE_CAPACITY.get();
    }

    public static void set(ItemStack belt, int stored) {
        int capacity = capacity();
        belt.set(ModDataComponents.BAT_CHARGE.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds charge and returns how much was actually stored. */
    public static int add(ItemStack belt, int amount) {
        RingEnergy current = get(belt);
        int next = Mth.clamp(current.stored() + amount, 0, capacity());
        set(belt, next);
        return next - current.stored();
    }

    public static boolean has(ItemStack belt, int amount) {
        return get(belt).stored() >= amount;
    }

    /** Consumes charge if (and only if) there is enough of it. */
    public static boolean tryConsume(ItemStack belt, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(belt);
        if (current.stored() < amount) return false;
        set(belt, current.stored() - amount);
        return true;
    }
}
