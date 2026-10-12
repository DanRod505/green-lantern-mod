package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Every character of the mod, read from {@code data/greenlantern/characters/*.json} the first time
 * anything asks (while the mod loads). The first 9 heroes are there marked {@code "legado"}; every
 * other file is a character of the engine, built by {@link EngineHero} with no code of its own.
 */
public final class Characters {
    public static final String FOLDER = "data/greenlantern/characters";
    /** The look of every engine suit piece: nothing, the skin draws the character. */
    public static final ResourceKey<EquipmentAsset> SKIN_SUIT = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("skin_suit"));

    private static Map<String, CharacterData> characters;

    private Characters() {}

    private static synchronized Map<String, CharacterData> load() {
        if (characters == null) {
            ClassRules.get();
            Map<String, CharacterData> map = new LinkedHashMap<>();
            for (String path : DataFiles.listJson(FOLDER)) {
                CharacterData data = CharacterData.CODEC.parse(JsonOps.INSTANCE, DataFiles.readJson(path))
                        .getOrThrow(error -> new IllegalStateException(path + ": " + error));
                String expected = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                List<String> problems = check(data, expected);
                if (!problems.isEmpty()) throw new IllegalStateException(path + ": " + String.join("; ", problems));
                if (map.put(data.id(), data) != null) throw new IllegalStateException(path + ": id " + data.id() + " twice");
            }
            characters = Collections.unmodifiableMap(map);
            GreenLantern.LOGGER.info("Loaded {} characters ({} of the engine)", map.size(), map.values().stream().filter(c -> !c.legacy()).count());
        }
        return characters;
    }

    /**
     * What is wrong with a character file (empty when it is right): the id must be the file name and
     * a character of the engine has its 6 abilities in wheel order (3 main, 2 secondary, the Supreme),
     * each with a kind, and at least one skin.
     */
    public static List<String> check(CharacterData data, String fileName) {
        List<String> problems = new ArrayList<>();
        if (!data.id().equals(fileName)) problems.add("id " + data.id() + " is not the file name " + fileName);
        if (!data.id().matches("[a-z0-9_]+")) problems.add("id must be lowercase letters, digits and _");
        if (data.legacy()) {
            if (data.suit().isEmpty()) problems.add("a legacy hero keeps its suit in the file (traje)");
            return problems;
        }
        List<CharacterData.Ability> abilities = data.abilities();
        if (abilities.size() != 6) {
            problems.add("needs 6 abilities, has " + abilities.size());
        } else {
            AbilitySlot[] order = {AbilitySlot.MAIN, AbilitySlot.MAIN, AbilitySlot.MAIN, AbilitySlot.SECONDARY, AbilitySlot.SECONDARY, AbilitySlot.SUPREME};
            for (int i = 0; i < 6; i++) {
                CharacterData.Ability ability = abilities.get(i);
                if (ability.slot().orElse(null) != order[i]) problems.add("ability " + (i + 1) + " must be " + order[i].getSerializedName());
                if (ability.kind().isEmpty()) problems.add("ability " + ability.id() + " has no classe");
                if (i == 5 && ability.kind().orElse(null) != AbilityKind.SUPREME) problems.add("the last ability is the Supreme (classe suprema)");
            }
        }
        if (data.skins().isEmpty()) problems.add("needs at least one skin");
        if (!com.danrod505.greenlantern.engine.preset.Transformations.exists(data.transformation())) {
            problems.add("unknown transformacao " + data.transformation() + " (one of " + com.danrod505.greenlantern.engine.preset.Transformations.IDS + " or padrao)");
        }
        return problems;
    }

    public static Map<String, CharacterData> all() {
        return load();
    }

    public static Optional<CharacterData> find(String id) {
        return Optional.ofNullable(load().get(id));
    }

    /** The file of a character that must exist (the 9 heroes ask for theirs while the mod loads). */
    public static CharacterData get(String id) {
        CharacterData data = load().get(id);
        if (data == null) throw new IllegalStateException("No character file " + FOLDER + "/" + id + ".json");
        return data;
    }

    /** The characters of the engine (not the first 9 heroes), in file name order. */
    public static List<CharacterData> engine() {
        return load().values().stream().filter(c -> !c.legacy()).toList();
    }

    // ---- what the suit gives -----------------------------------------------------------------------

    /** The bonuses of the suit of {@code id}, as the suit applies them. */
    public static List<SuitModifier> suitModifiers(String id) {
        CharacterData data = get(id);
        return data.suit().map(suit -> suit.bonuses().stream().map(Characters::modifier).toList()).orElseGet(() -> StatRules.bonuses(data));
    }

    private static SuitModifier modifier(CharacterData.Bonus bonus) {
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.get(bonus.attribute())
                .orElseThrow(() -> new IllegalStateException("Unknown attribute " + bonus.attribute()));
        return new SuitModifier(attribute, GreenLantern.id(bonus.id()), bonus.value(),
                bonus.multiply() ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE);
    }

    /** The armor material of the suit pieces of {@code id}, drawn with {@code look} when the file doesn't name one. */
    public static ArmorMaterial armorMaterial(String id, ResourceKey<EquipmentAsset> look) {
        CharacterData data = get(id);
        CharacterData.Suit suit = data.suit().orElseGet(() -> StatRules.suit(data));
        CharacterData.Defense defense = suit.defense();
        ResourceKey<EquipmentAsset> asset = suit.look().map(l -> ResourceKey.create(EquipmentAssets.ROOT_ID, l)).orElse(look);
        Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.get(suit.equipSound())
                .orElseThrow(() -> new IllegalStateException(id + ": unknown sound " + suit.equipSound()));
        TagKey<Item> repair = TagKey.create(Registries.ITEM, suit.repairTag());
        return new ArmorMaterial(1000,
                Map.of(ArmorType.HELMET, defense.head(), ArmorType.CHESTPLATE, defense.chest(), ArmorType.LEGGINGS, defense.legs(),
                        ArmorType.BOOTS, defense.feet(), ArmorType.BODY, defense.chest()),
                1, sound, (float) suit.toughness(), (float) suit.knockbackResistance(), repair, asset);
    }
}
