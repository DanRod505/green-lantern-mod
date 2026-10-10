package com.danrod505.greenlantern.wonderwoman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The gift of the gods, kept in the Tiara of Themyscira: it pays for Wonder Woman's powers. It
 * comes back slowly on its own, and faster in battle (every blow of the Amazon sword feeds it).
 */
public final class DivinePower {
    private DivinePower() {}

    public static RingEnergy get(ItemStack tiara) {
        RingEnergy value = tiara.get(ModDataComponents.DIVINE_POWER.get());
        return value != null ? value : new RingEnergy(0, capacity());
    }

    public static int capacity() {
        return GLConfig.DIVINE_CAPACITY.get();
    }

    public static void set(ItemStack tiara, int stored) {
        int capacity = capacity();
        tiara.set(ModDataComponents.DIVINE_POWER.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds power and returns how much was actually stored. */
    public static int add(ItemStack tiara, int amount) {
        RingEnergy current = get(tiara);
        int next = Mth.clamp(current.stored() + amount, 0, capacity());
        set(tiara, next);
        return next - current.stored();
    }

    public static boolean has(ItemStack tiara, int amount) {
        return get(tiara).stored() >= amount;
    }

    /** Consumes power if (and only if) there is enough of it. */
    public static boolean tryConsume(ItemStack tiara, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(tiara);
        if (current.stored() < amount) return false;
        set(tiara, current.stored() - amount);
        return true;
    }

    /** Takes up to {@code amount}, even if there is less (continuous powers drain the last drops). */
    public static void drain(ItemStack tiara, int amount) {
        if (amount <= 0) return;
        set(tiara, get(tiara).stored() - amount);
    }
}
