package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);

    /** Energy stored in a Power Ring. */
    public static final RegistryObject<DataComponentType<RingEnergy>> RING_ENERGY = COMPONENTS.register("ring_energy",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Id of the construct currently selected on a Power Ring. */
    public static final RegistryObject<DataComponentType<Identifier>> SELECTED_CONSTRUCT = COMPONENTS.register("selected_construct",
            () -> DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

    /** Speed Force stored in the Flash ring (same shape as the ring energy, its own capacity). */
    public static final RegistryObject<DataComponentType<RingEnergy>> SPEED_FORCE = COMPONENTS.register("speed_force",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Index of the speedster power currently selected on the Flash ring. */
    public static final RegistryObject<DataComponentType<Integer>> SELECTED_POWER = COMPONENTS.register("selected_power",
            () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());

    /** Power of the Seas stored in the Atlantean Emblem (selected power: {@link #SELECTED_POWER}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> SEA_FORCE = COMPONENTS.register("sea_force",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Charge of the Utility Belt's power cells (selected gadget: {@link #SELECTED_POWER}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> BAT_CHARGE = COMPONENTS.register("bat_charge",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Air left in an Atlantean Respirator, in ticks. */
    public static final RegistryObject<DataComponentType<Integer>> RESPIRATOR_AIR = COMPONENTS.register("respirator_air",
            () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());

    /** Solar energy stored in Superman's Kryptonian Crystal (selected power: {@link #SELECTED_POWER}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> SOLAR_ENERGY = COMPONENTS.register("solar_energy",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Divine power stored in Wonder Woman's Tiara of Themyscira (selected power: {@link #SELECTED_POWER}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> DIVINE_POWER = COMPONENTS.register("divine_power",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    private ModDataComponents() {}
}
