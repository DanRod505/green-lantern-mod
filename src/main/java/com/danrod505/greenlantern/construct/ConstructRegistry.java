package com.danrod505.greenlantern.construct;

import com.danrod505.greenlantern.construct.impl.BubbleConstruct;
import com.danrod505.greenlantern.construct.impl.EnergyBlastConstruct;
import com.danrod505.greenlantern.construct.impl.HammerConstruct;
import com.danrod505.greenlantern.construct.impl.MinigunConstruct;
import com.danrod505.greenlantern.construct.impl.SawConstruct;
import com.danrod505.greenlantern.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Ordered list of all constructs the ring can create. */
public final class ConstructRegistry {
    private static final Map<Identifier, Construct> BY_ID = new LinkedHashMap<>();
    private static final List<Construct> ORDERED = new ArrayList<>();
    private static final List<Construct> VIEW = Collections.unmodifiableList(ORDERED);

    public static final Construct ENERGY_BLAST = register(new EnergyBlastConstruct());
    public static final Construct MINIGUN = register(new MinigunConstruct());
    public static final Construct BUBBLE = register(new BubbleConstruct());
    public static final Construct SAW = register(new SawConstruct());
    public static final Construct HAMMER = register(new HammerConstruct());

    private ConstructRegistry() {}

    /** Forces class loading so all built-in constructs are registered during mod construction. */
    public static void bootstrap() {}

    public static synchronized Construct register(Construct construct) {
        if (BY_ID.putIfAbsent(construct.id(), construct) != null) {
            throw new IllegalStateException("Duplicate construct " + construct.id());
        }
        ORDERED.add(construct);
        return construct;
    }

    public static List<Construct> all() {
        return VIEW;
    }

    public static Construct get(Identifier id) {
        Construct construct = id == null ? null : BY_ID.get(id);
        return construct != null ? construct : ORDERED.getFirst();
    }

    public static Construct selected(ItemStack ring) {
        return get(ring.get(ModDataComponents.SELECTED_CONSTRUCT.get()));
    }

    public static void select(ItemStack ring, Construct construct) {
        ring.set(ModDataComponents.SELECTED_CONSTRUCT.get(), construct.id());
    }

    /** Selects the next (offset 1) or previous (offset -1) construct and returns it. */
    public static Construct cycle(ItemStack ring, int offset) {
        int index = ORDERED.indexOf(selected(ring));
        int size = ORDERED.size();
        Construct next = ORDERED.get(Math.floorMod(index + offset, size));
        select(ring, next);
        return next;
    }
}
