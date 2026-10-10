package com.danrod505.greenlantern.registry;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.FlashRingItem;
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

    // ---- The Flash ------------------------------------------------------------------------------

    /** The Flash ring: the suit lives compressed inside it, and it stores the Speed Force. */
    public static final RegistryObject<FlashRingItem> FLASH_RING = ITEMS.register("flash_ring", () -> new FlashRingItem(new Item.Properties()
            .setId(ITEMS.key("flash_ring"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()));

    public static final RegistryObject<SuitArmorItem> FLASH_MASK = flashSuit("flash_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> FLASH_SUIT = flashSuit("flash_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> FLASH_LEGGINGS = flashSuit("flash_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> FLASH_BOOTS = flashSuit("flash_boots", ArmorType.BOOTS);

    // ---- Aquaman --------------------------------------------------------------------------------

    /** The Atlantean Emblem: the armor of Atlantis lives inside it, and it stores the Power of the Seas. */
    public static final RegistryObject<com.danrod505.greenlantern.item.AquamanEmblemItem> AQUAMAN_EMBLEM = ITEMS.register("aquaman_emblem",
            () -> new com.danrod505.greenlantern.item.AquamanEmblemItem(new Item.Properties()
                    .setId(ITEMS.key("aquaman_emblem"))
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant()));

    /** The trident of Atlantis, summoned by the emblem (never crafted). */
    public static final RegistryObject<com.danrod505.greenlantern.item.AquaTridentItem> AQUAMAN_TRIDENT = ITEMS.register("aquaman_trident",
            () -> new com.danrod505.greenlantern.item.AquaTridentItem(com.danrod505.greenlantern.item.AquaTridentItem.properties()
                    .setId(ITEMS.key("aquaman_trident"))));

    public static final RegistryObject<SuitArmorItem> AQUAMAN_SUIT = aquamanSuit("aquaman_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> AQUAMAN_LEGGINGS = aquamanSuit("aquaman_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> AQUAMAN_BOOTS = aquamanSuit("aquaman_boots", ArmorType.BOOTS);

    // ---- Atlantis -------------------------------------------------------------------------------

    /** Opens the whirlpool portal to Atlantis (and back), like Aquaman's power. */
    public static final RegistryObject<com.danrod505.greenlantern.item.AtlantisGateItem> ATLANTIS_GATE = ITEMS.register("atlantis_gate",
            () -> new com.danrod505.greenlantern.item.AtlantisGateItem(new Item.Properties()
                    .setId(ITEMS.key("atlantis_gate"))
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    /** Lets anyone breathe underwater from anywhere in the inventory (works with every hero suit). */
    public static final RegistryObject<com.danrod505.greenlantern.item.AtlanteanRespiratorItem> ATLANTEAN_RESPIRATOR = ITEMS.register("atlantean_respirator",
            () -> new com.danrod505.greenlantern.item.AtlanteanRespiratorItem(new Item.Properties()
                    .setId(ITEMS.key("atlantean_respirator"))
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    // Eggs of the sea creatures of Atlantis (they hatch in the water, or on the block clicked).
    public static final RegistryObject<com.danrod505.greenlantern.item.AtlanteanEggItem> MANTA_RAY_EGG = egg("manta_ray_spawn_egg", ModEntities.MANTA_RAY);
    public static final RegistryObject<com.danrod505.greenlantern.item.AtlanteanEggItem> GIANT_SEAHORSE_EGG = egg("giant_seahorse_spawn_egg", ModEntities.GIANT_SEAHORSE);
    public static final RegistryObject<com.danrod505.greenlantern.item.AtlanteanEggItem> ATLANTEAN_DOLPHIN_EGG = egg("atlantean_dolphin_spawn_egg", ModEntities.ATLANTEAN_DOLPHIN);

    private static RegistryObject<com.danrod505.greenlantern.item.AtlanteanEggItem> egg(String name,
            java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends com.danrod505.greenlantern.entity.AtlanteanMountEntity>> type) {
        return ITEMS.register(name, () -> new com.danrod505.greenlantern.item.AtlanteanEggItem(type, new Item.Properties()
                .setId(ITEMS.key(name))
                .rarity(Rarity.UNCOMMON)));
    }

    // Eggs of the Trench (creative mode, or for building your own nest of horrors).
    public static final RegistryObject<com.danrod505.greenlantern.item.TrenchEggItem> TRENCH_CREATURE_EGG = trenchEgg("trench_creature_spawn_egg", false);
    public static final RegistryObject<com.danrod505.greenlantern.item.TrenchEggItem> TRENCH_BRUTE_EGG = trenchEgg("trench_brute_spawn_egg", true);

    private static RegistryObject<com.danrod505.greenlantern.item.TrenchEggItem> trenchEgg(String name, boolean brute) {
        return ITEMS.register(name, () -> new com.danrod505.greenlantern.item.TrenchEggItem(brute, new Item.Properties()
                .setId(ITEMS.key(name))));
    }

    // ---- Batman ---------------------------------------------------------------------------------

    /** The Utility Belt: the batsuit is folded inside it, and its power cells run the gadgets. */
    public static final RegistryObject<com.danrod505.greenlantern.item.UtilityBeltItem> UTILITY_BELT = ITEMS.register("utility_belt",
            () -> new com.danrod505.greenlantern.item.UtilityBeltItem(new Item.Properties()
                    .setId(ITEMS.key("utility_belt"))
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant()));

    public static final RegistryObject<SuitArmorItem> BATMAN_COWL = batmanSuit("batman_cowl", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> BATMAN_SUIT = batmanSuit("batman_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> BATMAN_LEGGINGS = batmanSuit("batman_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> BATMAN_BOOTS = batmanSuit("batman_boots", ArmorType.BOOTS);

    private static RegistryObject<SuitArmorItem> batmanSuit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(SuitArmorItem.BATMAN_MATERIAL, type).setId(ITEMS.key(name))));
    }

    // ---- Superman -------------------------------------------------------------------------------

    /** The Kryptonian Crystal: Superman's suit is kept in it, and it stores the sunlight his cells absorb. */
    public static final RegistryObject<com.danrod505.greenlantern.item.KryptonianCrystalItem> KRYPTONIAN_CRYSTAL = ITEMS.register("kryptonian_crystal",
            () -> new com.danrod505.greenlantern.item.KryptonianCrystalItem(new Item.Properties()
                    .setId(ITEMS.key("kryptonian_crystal"))
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant()));

    public static final RegistryObject<SuitArmorItem> SUPERMAN_HAIR = supermanSuit("superman_hair", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> SUPERMAN_SUIT = supermanSuit("superman_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> SUPERMAN_LEGGINGS = supermanSuit("superman_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> SUPERMAN_BOOTS = supermanSuit("superman_boots", ArmorType.BOOTS);

    private static RegistryObject<SuitArmorItem> supermanSuit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(SuitArmorItem.SUPERMAN_MATERIAL, type).setId(ITEMS.key(name))));
    }

    private static RegistryObject<SuitArmorItem> aquamanSuit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(SuitArmorItem.AQUAMAN_MATERIAL, type).setId(ITEMS.key(name))));
    }

    private static RegistryObject<SuitArmorItem> suit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(type).setId(ITEMS.key(name))));
    }

    private static RegistryObject<SuitArmorItem> flashSuit(String name, ArmorType type) {
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(SuitArmorItem.FLASH_MATERIAL, type).setId(ITEMS.key(name))));
    }

    private ModItems() {}
}
