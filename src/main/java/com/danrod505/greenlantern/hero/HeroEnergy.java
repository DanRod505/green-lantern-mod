package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * A hero's own resource (Speed Force, Power of the Seas, the belt's charge, solar energy, divine
 * power), stored on the hero's item in its own data component. The capacity comes from the config
 * and is stored with the value, so the client draws the bar right even if the server's config differs.
 */
public final class HeroEnergy {
    private final Supplier<DataComponentType<RingEnergy>> component;
    private final Supplier<Integer> capacity;

    public HeroEnergy(Supplier<DataComponentType<RingEnergy>> component, Supplier<Integer> capacity) {
        this.component = component;
        this.capacity = capacity;
    }

    public RingEnergy get(ItemStack item) {
        RingEnergy value = item.get(component.get());
        return value != null ? value : new RingEnergy(0, capacity());
    }

    public int capacity() {
        return capacity.get();
    }

    public void set(ItemStack item, int stored) {
        int capacity = capacity();
        item.set(component.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds energy and returns how much was actually stored. */
    public int add(ItemStack item, int amount) {
        RingEnergy current = get(item);
        int next = Mth.clamp(current.stored() + amount, 0, capacity());
        set(item, next);
        return next - current.stored();
    }

    public boolean has(ItemStack item, int amount) {
        return get(item).stored() >= amount;
    }

    /** Consumes energy if (and only if) there is enough of it. */
    public boolean tryConsume(ItemStack item, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(item);
        if (current.stored() < amount) return false;
        set(item, current.stored() - amount);
        return true;
    }

    /** Takes up to {@code amount}, even if there is less (continuous powers drain the last drops). */
    public void drain(ItemStack item, int amount) {
        if (amount <= 0) return;
        set(item, get(item).stored() - amount);
    }
}
