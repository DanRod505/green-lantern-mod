package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.GreenLantern;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
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
 * A piece of the hard-light Green Lantern uniform. Pieces are created by the ring, can't be taken
 * off (Curse of Binding) and disappear as soon as they leave an armor slot.
 */
public class SuitArmorItem extends Item {
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("green_lantern"));

    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            1000,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 8),
            1, // enchantability must be positive; the uniform can't reach an enchanting table anyway
            SoundEvents.ARMOR_EQUIP_GENERIC,
            2.0F,
            0.1F,
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            ASSET);

    private final ArmorType type;

    public SuitArmorItem(ArmorType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public static Properties properties(ArmorType type) {
        return new Properties()
                .humanoidArmor(MATERIAL, type)
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
