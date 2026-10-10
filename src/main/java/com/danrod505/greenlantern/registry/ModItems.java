package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.GuideBookItem;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.item.SuitArmorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);

    public static final RegistryObject<PowerRingItem> POWER_RING = ITEMS.register("power_ring", () -> new PowerRingItem(new Item.Properties()
            .setId(ITEMS.key("power_ring"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()
            // Using the ring (minigun) barely slows the player down and allows sprinting.
            .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 0.85F))));
    // Note: custom data components are registered after items, so the ring's energy / construct
    // components are read with defaults (see RingEnergy#get) instead of being set here.

    public static final RegistryObject<BlockItem> POWER_BATTERY = ITEMS.register("power_battery", () -> new BlockItem(ModBlocks.POWER_BATTERY.get(), new Item.Properties()
            .setId(ITEMS.key("power_battery"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()));

    public static final RegistryObject<GuideBookItem> GUIDE_BOOK = ITEMS.register("guide_book", () -> new GuideBookItem(new Item.Properties()
            .setId(ITEMS.key("guide_book"))
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)));

    // The uniform pieces are summoned by the ring; they cannot be crafted and vanish when removed.
    public static final RegistryObject<SuitArmorItem> LANTERN_MASK = suit("lantern_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> LANTERN_SUIT = suit("lantern_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> LANTERN_LEGGINGS = suit("lantern_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> LANTERN_BOOTS = suit("lantern_boots", ArmorType.BOOTS);

    private static RegistryObject<SuitArmorItem> suit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(type).setId(ITEMS.key(name))));
    }

    private ModItems() {}
}
