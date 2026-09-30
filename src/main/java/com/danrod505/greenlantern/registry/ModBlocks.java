package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.block.PowerBatteryBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, GreenLantern.MODID);

    public static final RegistryObject<PowerBatteryBlock> POWER_BATTERY = BLOCKS.register("power_battery", () -> new PowerBatteryBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("power_battery"))
            .mapColor(MapColor.EMERALD)
            .strength(3.5F, 1200.0F)
            .sound(SoundType.LANTERN)
            .lightLevel(state -> 15)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}
}
