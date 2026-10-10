package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.entity.DrillConstructEntity;
import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.danrod505.greenlantern.entity.FlightTrailEntity;
import com.danrod505.greenlantern.entity.GunConstructEntity;
import com.danrod505.greenlantern.entity.HammerConstructEntity;
import com.danrod505.greenlantern.entity.LanternCorpsmanEntity;
import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.entity.MechaMissileEntity;
import com.danrod505.greenlantern.entity.OaGuardianEntity;
import com.danrod505.greenlantern.entity.OaPortalEntity;
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
                    .noLootTable().sized(1.6F, 0.6F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("saw_construct")));

    public static final RegistryObject<EntityType<HammerConstructEntity>> HAMMER_CONSTRUCT = ENTITIES.register("hammer_construct",
            () -> EntityType.Builder.<HammerConstructEntity>of(HammerConstructEntity::new, MobCategory.MISC)
                    .noLootTable().sized(2.0F, 2.0F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("hammer_construct")));

    public static final RegistryObject<EntityType<DrillConstructEntity>> DRILL_CONSTRUCT = ENTITIES.register("drill_construct",
            () -> EntityType.Builder.<DrillConstructEntity>of(DrillConstructEntity::new, MobCategory.MISC)
                    .noLootTable().sized(1.5F, 1.0F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("drill_construct")));

    public static final RegistryObject<EntityType<MechaEntity>> MECHA = ENTITIES.register("mecha",
            () -> EntityType.Builder.<MechaEntity>of(MechaEntity::new, MobCategory.MISC)
                    .noLootTable().sized(MechaEntity.WIDTH, MechaEntity.HEIGHT).clientTrackingRange(16).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("mecha")));

    public static final RegistryObject<EntityType<MechaMissileEntity>> MECHA_MISSILE = ENTITIES.register("mecha_missile",
            () -> EntityType.Builder.<MechaMissileEntity>of(MechaMissileEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.4F, 0.4F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("mecha_missile")));

    public static final RegistryObject<EntityType<OaPortalEntity>> OA_PORTAL = ENTITIES.register("oa_portal",
            () -> EntityType.Builder.<OaPortalEntity>of(OaPortalEntity::new, MobCategory.MISC)
                    .noLootTable().sized(3.0F, 4.0F).clientTrackingRange(10).updateInterval(20).fireImmune().noSummon()
                    .build(ENTITIES.key("oa_portal")));

    public static final RegistryObject<EntityType<OaGuardianEntity>> OA_GUARDIAN = ENTITIES.register("oa_guardian",
            () -> EntityType.Builder.<OaGuardianEntity>of(OaGuardianEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.6F, 1.5F).clientTrackingRange(10).fireImmune()
                    .build(ENTITIES.key("oa_guardian")));

    public static final RegistryObject<EntityType<LanternCorpsmanEntity>> LANTERN_CORPSMAN = ENTITIES.register("lantern_corpsman",
            () -> EntityType.Builder.<LanternCorpsmanEntity>of(LanternCorpsmanEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.6F, 1.95F).clientTrackingRange(12).updateInterval(2).fireImmune()
                    .build(ENTITIES.key("lantern_corpsman")));

    /** The Flash's vortex: pulls, lifts and batters everything around its center. */
    public static final RegistryObject<EntityType<com.danrod505.greenlantern.entity.SpeedTornadoEntity>> SPEED_TORNADO = ENTITIES.register("speed_tornado",
            () -> EntityType.Builder.<com.danrod505.greenlantern.entity.SpeedTornadoEntity>of(com.danrod505.greenlantern.entity.SpeedTornadoEntity::new, MobCategory.MISC)
                    .noLootTable().sized(6.0F, 11.0F).clientTrackingRange(10).updateInterval(2).fireImmune().noSummon()
                    .build(ENTITIES.key("speed_tornado")));

    /** Bolt of Speed Force lightning thrown by the Flash. */
    public static final RegistryObject<EntityType<com.danrod505.greenlantern.entity.SpeedLightningEntity>> SPEED_LIGHTNING = ENTITIES.register("speed_lightning",
            () -> EntityType.Builder.<com.danrod505.greenlantern.entity.SpeedLightningEntity>of(com.danrod505.greenlantern.entity.SpeedLightningEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("speed_lightning")));

    /** The trident of Atlantis in flight (it always comes back). */
    public static final RegistryObject<EntityType<com.danrod505.greenlantern.entity.AquaTridentEntity>> AQUA_TRIDENT = ENTITIES.register("aquaman_trident",
            () -> EntityType.Builder.<com.danrod505.greenlantern.entity.AquaTridentEntity>of(com.danrod505.greenlantern.entity.AquaTridentEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon()
                    .build(ENTITIES.key("aquaman_trident")));

    /** Aquaman's great white shark: a fast mount underwater that bites his enemies. */
    public static final RegistryObject<EntityType<com.danrod505.greenlantern.entity.GreatWhiteSharkEntity>> GREAT_WHITE_SHARK = ENTITIES.register("great_white_shark",
            () -> EntityType.Builder.<com.danrod505.greenlantern.entity.GreatWhiteSharkEntity>of(com.danrod505.greenlantern.entity.GreatWhiteSharkEntity::new, MobCategory.MISC)
                    .noLootTable().sized(com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.WIDTH, com.danrod505.greenlantern.entity.GreatWhiteSharkEntity.HEIGHT)
                    .clientTrackingRange(10).updateInterval(1).noSummon()
                    .build(ENTITIES.key("great_white_shark")));

    /** Client-only trail renderer holder (never spawned on the server). */
    public static final RegistryObject<EntityType<FlightTrailEntity>> FLIGHT_TRAIL = ENTITIES.register("flight_trail",
            () -> EntityType.Builder.<FlightTrailEntity>of(FlightTrailEntity::new, MobCategory.MISC)
                    .noLootTable().sized(0.1F, 0.1F).clientTrackingRange(0).fireImmune().noSummon().noSave()
                    .build(ENTITIES.key("flight_trail")));

    private ModEntities() {}
}
