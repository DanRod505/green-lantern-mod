package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Energy stored in a Power Ring. The capacity is stored alongside the value so the client can
 * render the energy bar correctly even if the server uses a different configuration.
 */
public record RingEnergy(int stored, int capacity) {
    public static final int DEFAULT_CAPACITY = 1000;
    public static final RingEnergy EMPTY = new RingEnergy(0, DEFAULT_CAPACITY);

    public static final Codec<RingEnergy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("stored").forGetter(RingEnergy::stored),
            Codec.INT.fieldOf("capacity").forGetter(RingEnergy::capacity)
    ).apply(i, RingEnergy::new));

    public static final StreamCodec<ByteBuf, RingEnergy> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RingEnergy::stored,
            ByteBufCodecs.VAR_INT, RingEnergy::capacity,
            RingEnergy::new);

    public float fraction() {
        return capacity <= 0 ? 0.0F : Mth.clamp((float) stored / capacity, 0.0F, 1.0F);
    }

    public boolean isFull() {
        return stored >= capacity;
    }

    // ---- ItemStack helpers ---------------------------------------------------------------------

    public static RingEnergy get(ItemStack ring) {
        return ring.getOrDefault(ModDataComponents.RING_ENERGY.get(), EMPTY);
    }

    public static int configuredCapacity() {
        return GLConfig.MAX_ENERGY.get();
    }

    public static void set(ItemStack ring, int stored) {
        int capacity = configuredCapacity();
        ring.set(ModDataComponents.RING_ENERGY.get(), new RingEnergy(Mth.clamp(stored, 0, capacity), capacity));
    }

    /** Adds energy and returns how much was actually inserted. */
    public static int add(ItemStack ring, int amount) {
        RingEnergy current = get(ring);
        int capacity = configuredCapacity();
        int next = Mth.clamp(current.stored + amount, 0, capacity);
        set(ring, next);
        return next - current.stored;
    }

    /** Consumes energy if (and only if) there is enough of it. */
    public static boolean tryConsume(ItemStack ring, int amount) {
        if (amount <= 0) return true;
        RingEnergy current = get(ring);
        if (current.stored < amount) return false;
        set(ring, current.stored - amount);
        return true;
    }

    public static boolean has(ItemStack ring, int amount) {
        return get(ring).stored >= amount;
    }
}
