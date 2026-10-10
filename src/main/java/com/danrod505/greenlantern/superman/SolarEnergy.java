package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Solar energy of Superman, kept in the Kryptonian Crystal. His cells soak up the light of the
 * (yellow) sun under the open sky, and it pays for flight and every power.
 */
public final class SolarEnergy {
    private SolarEnergy() {}

    public static RingEnergy get(ItemStack crystal) {
        RingEnergy value = crystal.get(ModDataComponents.SOLAR_ENERGY.get());
        return value != null ? value : new RingEnergy(0, capacity());
    }

    public static int capacity() {
        return GLConfig.SOLAR_CAPACITY.get();
    }

    public static void set(ItemStack crystal, int stored) {
        int capacity = capacity();
        crystal.set(ModDataComponents.SOLAR_ENERGY.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds energy and returns how much was actually stored. */
    public static int add(ItemStack crystal, int amount) {
        RingEnergy current = get(crystal);
        int next = Mth.clamp(current.stored() + amount, 0, capacity());
        set(crystal, next);
        return next - current.stored();
    }

    public static boolean has(ItemStack crystal, int amount) {
        return get(crystal).stored() >= amount;
    }

    /** Consumes energy if (and only if) there is enough of it. */
    public static boolean tryConsume(ItemStack crystal, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(crystal);
        if (current.stored() < amount) return false;
        set(crystal, current.stored() - amount);
        return true;
    }

    /** Takes up to {@code amount}, even if there is less (continuous powers drain the last drops). */
    public static void drain(ItemStack crystal, int amount) {
        if (amount <= 0) return;
        set(crystal, get(crystal).stored() - amount);
    }
}
