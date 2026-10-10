package com.danrod505.greenlantern.hero;

import com.danrod505.greenlantern.registry.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The powers of one hero, in wheel order, and which one is selected on the hero's item (kept in the
 * {@code selected_power} data component).
 */
public final class PowerSet<P extends Enum<P> & HeroPower> {
    private final P[] values;

    public PowerSet(P[] values) {
        this.values = values.clone();
    }

    public P byIndex(int index) {
        return values[Mth.clamp(index, 0, values.length - 1)];
    }

    public int count() {
        return values.length;
    }

    public P selected(ItemStack item) {
        Integer index = item.get(ModDataComponents.SELECTED_POWER.get());
        return byIndex(index == null ? 0 : index);
    }

    public void select(ItemStack item, P power) {
        item.set(ModDataComponents.SELECTED_POWER.get(), power.ordinal());
    }

    public P cycle(ItemStack item, int offset) {
        P next = values[Math.floorMod(selected(item).ordinal() + offset, values.length)];
        select(item, next);
        return next;
    }
}
