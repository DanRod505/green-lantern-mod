package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Speed Force stored in the Flash ring. It refills on its own and much faster while running, and
 * pays for the speedster powers (tornado, phasing, lightning).
 */
public final class SpeedForce {
    private SpeedForce() {}

    public static RingEnergy get(ItemStack ring) {
        RingEnergy value = ring.get(ModDataComponents.SPEED_FORCE.get());
        return value != null ? value : new RingEnergy(0, capacity());
    }

    public static int capacity() {
        return GLConfig.SPEED_FORCE_CAPACITY.get();
    }

    public static void set(ItemStack ring, int stored) {
        int capacity = capacity();
        ring.set(ModDataComponents.SPEED_FORCE.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds Speed Force and returns how much was actually stored. */
    public static int add(ItemStack ring, int amount) {
        RingEnergy current = get(ring);
        int next = Mth.clamp(current.stored() + amount, 0, capacity());
        set(ring, next);
        return next - current.stored();
    }

    public static boolean has(ItemStack ring, int amount) {
        return get(ring).stored() >= amount;
    }

    /** Consumes Speed Force if (and only if) there is enough of it. */
    public static boolean tryConsume(ItemStack ring, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(ring);
        if (current.stored() < amount) return false;
        set(ring, current.stored() - amount);
        return true;
    }
}
