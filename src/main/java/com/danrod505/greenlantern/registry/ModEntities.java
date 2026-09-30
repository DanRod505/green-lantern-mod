package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.danrod505.greenlantern.entity.GunConstructEntity;
import com.danrod505.greenlantern.entity.HammerConstructEntity;
import com.danrod505.greenlantern.entity.SawConstructEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GreenLantern.MODID);

    public static final RegistryObject<EntityType<EnergyBoltEntity>> ENERGY_BOLT = ENTITIES.register("energy_bolt",
            () -> EntityType.Builder.<EnergyBoltEntity>of(EnergyBoltEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).fireImmune()
                    .build(ENTITIES.key("energy_bolt")));

    public static final RegistryObject<EntityType<GunConstructEntity>> GUN_CONSTRUCT = ENTITIES.register("gun_construct",
            () -> EntityType.Builder.<GunConstructEntity>of(GunConstructEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(2).fireImmune().noSummon()
                    .build(ENTITIES.key("gun_construct")));

    public static final RegistryObject<EntityType<BubbleConstructEntity>> BUBBLE_CONSTRUCT = ENTITIES.register("bubble_construct",
            () -> EntityType.Builder.<BubbleConstructEntity>of(BubbleConstructEntity::new, MobCategory.MISC)
                    .noLootTable().sized(4.4F, 4.4F).clientTrackingRange(8).updateInterval(2).fireImmune().noSummon()
                    .build(ENTITIES.key("bubble_construct")));

    public static final RegistryObject<EntityType<SawConstructEntity>> SAW_CONSTRUCT = ENTITIES.register("saw_construct",
            () -> EntityType.Builder.<SawConstructEntity>of(SawConstructEntity::new, MobCategory.MISC)
                    .noLootTable().sized(1.6F, 1.0F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("saw_construct")));

    public static final RegistryObject<EntityType<HammerConstructEntity>> HAMMER_CONSTRUCT = ENTITIES.register("hammer_construct",
            () -> EntityType.Builder.<HammerConstructEntity>of(HammerConstructEntity::new, MobCategory.MISC)
                    .noLootTable().sized(2.0F, 2.0F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("hammer_construct")));

    private ModEntities() {}
}
