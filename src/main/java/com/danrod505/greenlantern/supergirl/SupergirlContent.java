package com.danrod505.greenlantern.supergirl;

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

/** Everything Supergirl adds to the game, registered by the hero itself (see {@link SupergirlHero#register}). */
public final class SupergirlContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);

    /** Supergirl's Solar Energy stored in the Argo Pendant (the selected power is the shared {@code selected_power}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> ENERGY = COMPONENTS.register("supergirl_solar",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    public static final RegistryObject<SoundEvent> SUIT_UP = sound("supergirl_suit_up");
    public static final RegistryObject<SoundEvent> SUIT_DOWN = sound("supergirl_suit_down");

    /** The Argo Pendant: it calls the suit and holds the Supergirl's Solar Energy. */
    public static final RegistryObject<HeroItem> ITEM = ITEMS.register("argo_pendant", () -> new HeroItem(new Item.Properties()
            .setId(ITEMS.key("argo_pendant"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant(), () -> SupergirlHero.INSTANCE, 0.603F, 0.987F, ChatFormatting.GOLD));

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("supergirl"));

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

    public static final RegistryObject<SuitArmorItem> MASK = suit("supergirl_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> SUIT = suit("supergirl_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> LEGGINGS = suit("supergirl_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> BOOTS = suit("supergirl_boots", ArmorType.BOOTS);

    private SupergirlContent() {}

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
