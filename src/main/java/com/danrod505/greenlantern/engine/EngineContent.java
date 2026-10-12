package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.preset.Transformations;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * What the engine adds to the game: the data components every character's item uses (class
 * energy, cooldowns and Supreme charge, skin), the Heroes' Wardrobe, and the item and suit pieces of
 * each character of the engine (registered by {@link EngineHero} from its file).
 */
public final class EngineContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, GreenLantern.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);

    /** The class energy of a character of the engine, on its item. */
    public static final RegistryObject<DataComponentType<RingEnergy>> ENERGY = COMPONENTS.register("class_energy",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    /** The cooldowns of the abilities, on the character's item. */
    public static final RegistryObject<DataComponentType<AbilityState>> ABILITIES = COMPONENTS.register("ability_state",
            () -> DataComponentType.<AbilityState>builder().persistent(AbilityState.CODEC).networkSynchronized(AbilityState.STREAM_CODEC).build());

    /** The chosen skin and the unlocked ones, on the character's item. */
    public static final RegistryObject<DataComponentType<SkinState>> SKIN = COMPONENTS.register("skin",
            () -> DataComponentType.<SkinState>builder().persistent(SkinState.CODEC).networkSynchronized(SkinState.STREAM_CODEC).build());

    /** The supreme ability is ready again (its cooldown ended). */
    public static final RegistryObject<SoundEvent> SUPREME_READY = sound("engine_supreme_ready");
    /** The accent of a supreme ability, over the sound of its class. */
    public static final RegistryObject<SoundEvent> SUPREME = sound("engine_supreme");
    public static final RegistryObject<SoundEvent> NO_ENERGY = sound("engine_no_energy");
    public static final RegistryObject<SoundEvent> SKIN_CHANGE = sound("engine_skin_change");
    /** The sound set of each class of ability (3 variations each, see tools/generate_engine_audio.py). */
    public static final java.util.Map<AbilityKind, RegistryObject<SoundEvent>> CLASS_SOUNDS = classSounds();
    /** The sound of each of the 15 transformations, by id. */
    public static final java.util.Map<String, RegistryObject<SoundEvent>> TRANSFORM_SOUNDS = transformSounds();

    /** The Heroes' Wardrobe: with a character's item, using it changes to the next unlocked skin. */
    public static final RegistryObject<Block> WARDROBE = BLOCKS.register("heroes_wardrobe", () -> new WardrobeBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("heroes_wardrobe"))
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()));
    public static final RegistryObject<Item> WARDROBE_ITEM = ITEMS.register("heroes_wardrobe", () -> new BlockItem(WARDROBE.get(),
            new Item.Properties().setId(ITEMS.key("heroes_wardrobe")).useBlockDescriptionPrefix()));

    private EngineContent() {}

    public static void register(BusGroup modBus) {
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    private static java.util.Map<AbilityKind, RegistryObject<SoundEvent>> classSounds() {
        java.util.Map<AbilityKind, RegistryObject<SoundEvent>> map = new java.util.EnumMap<>(AbilityKind.class);
        for (AbilityKind kind : AbilityKind.values()) map.put(kind, sound("engine_class_" + kind.id()));
        return map;
    }

    private static java.util.Map<String, RegistryObject<SoundEvent>> transformSounds() {
        java.util.Map<String, RegistryObject<SoundEvent>> map = new java.util.LinkedHashMap<>();
        for (String id : Transformations.IDS) map.put(id, sound("engine_transform_" + id));
        return map;
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GreenLantern.id(name)));
    }

    /** The item of a character of the engine ({@code <id>}), drawn with {@code models/item/<id>}. */
    static RegistryObject<CharacterItem> characterItem(CharacterData data, java.util.function.Supplier<EngineHero> hero) {
        String name = data.id();
        return ITEMS.register(name, () -> new CharacterItem(new Item.Properties()
                .setId(ITEMS.key(name))
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant(), hero));
    }

    /**
     * A suit piece of a character of the engine ({@code <id>_cabeca}...). Nothing draws them on the
     * body: with the full suit on, the character's skin is the look. Their item files point to the
     * shared {@code item/skin_suit_<slot>} models.
     */
    static RegistryObject<SuitArmorItem> suitPiece(CharacterData data, ArmorMaterial material, ArmorType type, String slot) {
        String name = data.id() + "_" + slot;
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(material, type)
                .setId(ITEMS.key(name))) {
            @Override
            public Component getName(ItemStack stack) {
                // "<name>'s suit: head" unless the language file names this piece.
                String key = getDescriptionId();
                return Language.getInstance().has(key) ? Component.translatable(key)
                        : Component.translatableWithFallback("item.greenlantern.engine_suit_" + slot, "%s: " + slot, data.name());
            }
        });
    }
}
