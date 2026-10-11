package com.danrod505.greenlantern.shazam;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Everything Shazam adds to the game, registered by the hero itself (see {@link ShazamHero#register}). */
public final class ShazamContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);

    /** Shazam's Magic stored in the Wizard's Bolt (the selected power is the shared {@code selected_power}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> ENERGY = COMPONENTS.register("shazam_magic",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    public static final RegistryObject<SoundEvent> SUIT_UP = sound("shazam_suit_up");
    public static final RegistryObject<SoundEvent> SUIT_DOWN = sound("shazam_suit_down");
    public static final RegistryObject<SoundEvent> ZEUS_LIGHTNING = sound("shazam_zeus_lightning");
    public static final RegistryObject<SoundEvent> LIGHTNING_HIT = sound("shazam_lightning_hit");
    public static final RegistryObject<SoundEvent> THUNDER_CALL = sound("shazam_thunder_call");
    public static final RegistryObject<SoundEvent> MERCURY_DASH = sound("shazam_mercury_dash");
    public static final RegistryObject<SoundEvent> HERCULES_SLAM = sound("shazam_hercules_slam");
    public static final RegistryObject<SoundEvent> ACHILLES_ROAR = sound("shazam_achilles_roar");
    public static final RegistryObject<SoundEvent> ATLAS_ON = sound("shazam_atlas_on");
    public static final RegistryObject<SoundEvent> ATLAS_OFF = sound("shazam_atlas_off");
    public static final RegistryObject<SoundEvent> SOLOMON_ON = sound("shazam_solomon_on");
    public static final RegistryObject<SoundEvent> SOLOMON_OFF = sound("shazam_solomon_off");
    public static final RegistryObject<SoundEvent> MAGIC_FULL = sound("shazam_magic_full");
    public static final RegistryObject<SoundEvent> THEME_BASE = sound("shazam_theme_base");
    public static final RegistryObject<SoundEvent> THEME_PEAK = sound("shazam_theme_peak");

    /** Whether Stamina of Atlas is on (kept on the Wizard's Bolt, so the client sees it too). */
    public static final RegistryObject<DataComponentType<Boolean>> ATLAS = COMPONENTS.register("shazam_atlas",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** Whether Wisdom of Solomon is on (kept on the Wizard's Bolt, so the client shows the health of whoever he aims at). */
    public static final RegistryObject<DataComponentType<Boolean>> SOLOMON = COMPONENTS.register("shazam_solomon",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** The Wizard's Bolt: it calls the suit with a lightning strike and holds the Shazam's Magic. */
    public static final RegistryObject<HeroItem> ITEM = ITEMS.register("wizard_bolt", () -> new WizardBoltItem(new Item.Properties()
            .setId(ITEMS.key("wizard_bolt"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant(), () -> ShazamHero.INSTANCE, 0.000F, 0.125F, ChatFormatting.GOLD));

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("shazam"));

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

    public static final RegistryObject<SuitArmorItem> MASK = suit("shazam_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> SUIT = suit("shazam_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> LEGGINGS = suit("shazam_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> BOOTS = suit("shazam_boots", ArmorType.BOOTS);

    private ShazamContent() {}

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
