package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.block.PowerBatteryBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GreenLantern.MODID);

    public static final RegistryObject<BlockEntityType<PowerBatteryBlockEntity>> POWER_BATTERY = BLOCK_ENTITIES.register("power_battery",
            () -> new BlockEntityType<>(PowerBatteryBlockEntity::new, Set.of(ModBlocks.POWER_BATTERY.get())));

    private ModBlockEntities() {}
}
