package com.danrod505.greenlantern.supergirl;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.HeroItem;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Everything Supergirl adds to the game, registered by the hero itself (see {@link SupergirlHero#register}). */
public final class SupergirlContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GreenLantern.MODID);

    /** Supergirl's Solar Energy stored in the Argo Pendant (the selected power is the shared {@code selected_power}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> ENERGY = COMPONENTS.register("supergirl_solar",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Whether Super Hearing is on, kept on the pendant so the client knows when to draw the sound waves. */
    public static final RegistryObject<DataComponentType<Boolean>> HEARING = COMPONENTS.register("supergirl_hearing",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** Krypto while he is away: name, health, whether he already met her and when he comes back. */
    public static final RegistryObject<DataComponentType<KryptoData>> KRYPTO_DATA = COMPONENTS.register("supergirl_krypto",
            () -> DataComponentType.<KryptoData>builder().persistent(KryptoData.CODEC).networkSynchronized(KryptoData.STREAM_CODEC).build());

    /** Krypto, the Super-Dog: Supergirl's companion. */
    public static final RegistryObject<EntityType<KryptoEntity>> KRYPTO = ENTITIES.register("krypto",
            () -> EntityType.Builder.<KryptoEntity>of(KryptoEntity::new, MobCategory.CREATURE)
                    .noLootTable().sized(0.6F, 0.85F).eyeHeight(0.68F)
                    .clientTrackingRange(10).updateInterval(2)
                    .build(ENTITIES.key("krypto")));

    public static final RegistryObject<SoundEvent> SUIT_UP = sound("supergirl_suit_up");
    public static final RegistryObject<SoundEvent> SUIT_DOWN = sound("supergirl_suit_down");
    public static final RegistryObject<SoundEvent> HEAT_BOLT = sound("supergirl_heat_bolt");
    public static final RegistryObject<SoundEvent> HEAT_HIT = sound("supergirl_heat_hit");
    public static final RegistryObject<SoundEvent> METEOR_DASH = sound("supergirl_meteor_dash");
    public static final RegistryObject<SoundEvent> THUNDER_CLAP = sound("supergirl_thunder_clap");
    public static final RegistryObject<SoundEvent> FROST_BREATH = sound("supergirl_frost_breath");
    public static final RegistryObject<SoundEvent> FROST_MELT = sound("supergirl_frost_melt");
    public static final RegistryObject<SoundEvent> HEARING_ON = sound("supergirl_hearing_on");
    public static final RegistryObject<SoundEvent> HEARING_OFF = sound("supergirl_hearing_off");
    public static final RegistryObject<SoundEvent> HEARING_PULSE = sound("supergirl_hearing_pulse");
    public static final RegistryObject<SoundEvent> THROW_GRAB = sound("supergirl_throw_grab");
    public static final RegistryObject<SoundEvent> THROW_HURL = sound("supergirl_throw_hurl");
    public static final RegistryObject<SoundEvent> SOLAR_FLARE = sound("supergirl_solar_flare");
    public static final RegistryObject<SoundEvent> ROLL = sound("supergirl_roll");
    public static final RegistryObject<SoundEvent> SOLAR_FULL = sound("supergirl_solar_full");
    public static final RegistryObject<SoundEvent> KRYPTO_BARK = sound("krypto_bark");
    public static final RegistryObject<SoundEvent> KRYPTO_HURT = sound("krypto_hurt");
    public static final RegistryObject<SoundEvent> KRYPTO_WHINE = sound("krypto_whine");
    public static final RegistryObject<SoundEvent> KRYPTO_FLY = sound("krypto_fly");
    public static final RegistryObject<SoundEvent> KRYPTO_WHISTLE = sound("krypto_whistle");
    public static final RegistryObject<SoundEvent> THEME_BASE = sound("supergirl_theme_base");
    public static final RegistryObject<SoundEvent> THEME_PEAK = sound("supergirl_theme_peak");

    /** The Argo Pendant: it calls the suit and holds the Supergirl's Solar Energy. */
    public static final RegistryObject<HeroItem> ITEM = ITEMS.register("argo_pendant", () -> new HeroItem(new Item.Properties()
            .setId(ITEMS.key("argo_pendant"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant(), () -> SupergirlHero.INSTANCE, 0.603F, 0.987F, ChatFormatting.GOLD));

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("supergirl"));

    /** The suit: as tough as diamond (Kryptonian skin shrugs off part of every blow on top, see {@link SupergirlHero#damageTakenMultiplier}). */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            1000,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 8),
            1,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            2.0F,
            0.0F,
            ItemTags.REPAIRS_DIAMOND_ARMOR,
            ASSET);

    public static final RegistryObject<SuitArmorItem> MASK = suit("supergirl_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> SUIT = suit("supergirl_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> LEGGINGS = suit("supergirl_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> BOOTS = suit("supergirl_boots", ArmorType.BOOTS);

    private SupergirlContent() {}

    static void register(BusGroup modBus) {
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GreenLantern.id(name)));
    }

    private static RegistryObject<SuitArmorItem> suit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(MATERIAL, type).setId(ITEMS.key(name))));
    }
}
