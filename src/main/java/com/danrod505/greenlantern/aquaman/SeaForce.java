package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Power of the Seas stored in the Atlantean Emblem. It refills quickly while Aquaman is in water
 * (slowly on land) and pays for his powers: the trident, the shark and the call of the sea.
 */
public final class SeaForce {
    private SeaForce() {}

    public static RingEnergy get(ItemStack emblem) {
        RingEnergy value = emblem.get(ModDataComponents.SEA_FORCE.get());
        return value != null ? value : new RingEnergy(0, capacity());
    }

    public static int capacity() {
        return GLConfig.SEA_FORCE_CAPACITY.get();
    }

    public static void set(ItemStack emblem, int stored) {
        int capacity = capacity();
        emblem.set(ModDataComponents.SEA_FORCE.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds Power of the Seas and returns how much was actually stored. */
    public static int add(ItemStack emblem, int amount) {
        RingEnergy current = get(emblem);
        int next = Mth.clamp(current.stored() + amount, 0, capacity());
        set(emblem, next);
        return next - current.stored();
    }

    public static boolean has(ItemStack emblem, int amount) {
        return get(emblem).stored() >= amount;
    }

    /** Consumes Power of the Seas if (and only if) there is enough of it. */
    public static boolean tryConsume(ItemStack emblem, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(emblem);
        if (current.stored() < amount) return false;
        set(emblem, current.stored() - amount);
        return true;
    }
}
