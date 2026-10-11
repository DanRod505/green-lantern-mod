package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.HeroItem;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Everything Cyborg adds to the game, registered by the hero itself (see {@link CyborgHero#register}). */
public final class CyborgContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);

    /** Cyborg Battery stored in the Mother Box (the selected power is the shared {@code selected_power}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> ENERGY = COMPONENTS.register("cyborg_power",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** Whether Tech Scan is on, kept on the Mother Box so the client knows when to draw the ores. */
    public static final RegistryObject<DataComponentType<Boolean>> SCAN = COMPONENTS.register("cyborg_scan",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** Where the Boom Tube goes (marked by using it while sneaking). */
    public static final RegistryObject<DataComponentType<GlobalPos>> BOOM_MARK = COMPONENTS.register("cyborg_boom_mark",
            () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    public static final RegistryObject<SoundEvent> SUIT_UP = sound("cyborg_suit_up");
    public static final RegistryObject<SoundEvent> SUIT_DOWN = sound("cyborg_suit_down");
    public static final RegistryObject<SoundEvent> SONIC_CANNON = sound("cyborg_sonic_cannon");
    public static final RegistryObject<SoundEvent> MISSILE_LAUNCH = sound("cyborg_missile_launch");
    public static final RegistryObject<SoundEvent> MISSILE_EXPLODE = sound("cyborg_missile_explode");
    public static final RegistryObject<SoundEvent> SCAN_ON = sound("cyborg_scan_on");
    public static final RegistryObject<SoundEvent> SCAN_OFF = sound("cyborg_scan_off");
    public static final RegistryObject<SoundEvent> SCAN_PING = sound("cyborg_scan_ping");
    public static final RegistryObject<SoundEvent> HACK = sound("cyborg_hack");
    public static final RegistryObject<SoundEvent> EMP = sound("cyborg_emp");
    public static final RegistryObject<SoundEvent> REPAIR = sound("cyborg_repair");
    public static final RegistryObject<SoundEvent> BATTERY_FULL = sound("cyborg_battery_full");
    public static final RegistryObject<SoundEvent> BOOM_TUBE = sound("cyborg_boom_tube");
    public static final RegistryObject<SoundEvent> THEME_BASE = sound("cyborg_theme_base");
    public static final RegistryObject<SoundEvent> THEME_PEAK = sound("cyborg_theme_peak");

    /** The Mother Box: it calls the suit and holds the Cyborg Battery. */
    public static final RegistryObject<HeroItem> ITEM = ITEMS.register("mother_box", () -> new HeroItem(new Item.Properties()
            .setId(ITEMS.key("mother_box"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant(), () -> CyborgHero.INSTANCE, 0.604F, 0.000F, ChatFormatting.GOLD));

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("cyborg"));

    /** The suit: as tough as diamond (the armored body takes less damage on top, see {@link CyborgHero#damageTakenMultiplier}). */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            1000,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 8),
            1,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            2.0F,
            0.0F,
            ItemTags.REPAIRS_DIAMOND_ARMOR,
            ASSET);

    public static final RegistryObject<SuitArmorItem> MASK = suit("cyborg_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> SUIT = suit("cyborg_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> LEGGINGS = suit("cyborg_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> BOOTS = suit("cyborg_boots", ArmorType.BOOTS);

    private CyborgContent() {}

    static void register(BusGroup modBus) {
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GreenLantern.id(name)));
    }

    private static RegistryObject<SuitArmorItem> suit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(MATERIAL, type).setId(ITEMS.key(name))));
    }
}
