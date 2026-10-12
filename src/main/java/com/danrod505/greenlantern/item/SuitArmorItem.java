package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.Characters;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jspecify.annotations.Nullable;

/**
 * A piece of a hero suit (the hard-light Green Lantern uniform, the Flash suit, the armor of Atlantis, the batsuit, Superman's suit or Wonder Woman's armor). Pieces are
 * created by a ring, can't be taken off (Curse of Binding) and disappear as soon as they leave an
 * armor slot. The armor of each suit comes from the hero's file in {@code data/greenlantern/characters}.
 */
public class SuitArmorItem extends Item {
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("green_lantern"));
    public static final ResourceKey<EquipmentAsset> FLASH_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("flash"));
    public static final ResourceKey<EquipmentAsset> AQUAMAN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("aquaman"));
    public static final ResourceKey<EquipmentAsset> BATMAN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("batman"));
    public static final ResourceKey<EquipmentAsset> SUPERMAN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("superman"));
    public static final ResourceKey<EquipmentAsset> WONDER_WOMAN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("wonder_woman"));

    public static final ArmorMaterial MATERIAL = Characters.armorMaterial("lantern", ASSET);

    /** The Flash suit: friction-proof fabric, lighter than hard light. */
    public static final ArmorMaterial FLASH_MATERIAL = Characters.armorMaterial("flash", FLASH_ASSET);

    /** The armor of Atlantis: orichalcum scales, as tough as diamond. */
    public static final ArmorMaterial AQUAMAN_MATERIAL = Characters.armorMaterial("aquaman", AQUAMAN_ASSET);

    /** The batsuit: Kevlar weave over armor plates, as tough as diamond and a little tougher. */
    public static final ArmorMaterial BATMAN_MATERIAL = Characters.armorMaterial("batman", BATMAN_ASSET);

    /** Superman's suit: Kryptonian weave, tougher than netherite (the Man of Steel himself is tougher still). */
    public static final ArmorMaterial SUPERMAN_MATERIAL = Characters.armorMaterial("superman", SUPERMAN_ASSET);

    /** Wonder Woman's armor: forged on Themyscira, as tough as netherite. */
    public static final ArmorMaterial WONDER_WOMAN_MATERIAL = Characters.armorMaterial("wonder_woman", WONDER_WOMAN_ASSET);

    private final ArmorType type;

    public SuitArmorItem(ArmorType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public static Properties properties(ArmorType type) {
        return properties(MATERIAL, type);
    }

    public static Properties properties(ArmorMaterial material, ArmorType type) {
        return new Properties()
                .humanoidArmor(material, type)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false)
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant();
    }

    public ArmorType type() {
        return type;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player && (slot == null || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)) {
            stack.setCount(0);
        }
    }
}
