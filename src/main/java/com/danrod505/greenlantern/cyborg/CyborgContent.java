package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.HeroItem;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
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

    public static final RegistryObject<SoundEvent> SUIT_UP = sound("cyborg_suit_up");
    public static final RegistryObject<SoundEvent> SUIT_DOWN = sound("cyborg_suit_down");

    /** The Mother Box: it calls the suit and holds the Cyborg Battery. */
    public static final RegistryObject<HeroItem> ITEM = ITEMS.register("mother_box", () -> new HeroItem(new Item.Properties()
            .setId(ITEMS.key("mother_box"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant(), () -> CyborgHero.INSTANCE, 0.604F, 0.000F, ChatFormatting.GOLD));

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("cyborg"));

    /** The suit: as tough as diamond. Tune it to the hero. */
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
