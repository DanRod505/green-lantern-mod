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
     * What is wrong with a character file (empty when it is right): the id must be the file name, and
     * a character (of the engine or one of the 9 heroes of before) has 5 to 8 abilities, each with a class, at least 3 for fighting and
     * 2 for the world (by main class) within its class's arsenal, the fighting ones first, then the
     * world ones, and exactly 1 supreme, the last; each ability has up to 2 more functions, each with
     * a condition the game knows; up to 4 known traits; at least one skin; a known transformation.
     */
    public static List<String> check(CharacterData data, String fileName) {
        List<String> problems = new ArrayList<>();
        if (!data.id().equals(fileName)) problems.add("id " + data.id() + " is not the file name " + fileName);
        if (!data.id().matches("[a-z0-9_]+")) problems.add("id must be lowercase letters, digits and _");
        // The 9 heroes of before keep their suit in the file; since stage 3 their wheel follows the same rules.
        if (data.legacy() && data.suit().isEmpty()) problems.add("a legacy hero keeps its suit in the file (traje)");
        List<CharacterData.Ability> abilities = data.abilities();
        ClassRules.Arsenal arsenal = data.classRule().arsenal();
        int fight = 0;
        int world = 0;
        int supreme = 0;
        boolean worldSeen = false;
        for (int i = 0; i < abilities.size(); i++) {
            CharacterData.Ability ability = abilities.get(i);
            String who = "ability " + ability.id() + ": ";
            if (ability.kind().isEmpty()) {
                problems.add(who + "has no classe");
                continue;
            }
            boolean last = i == abilities.size() - 1;
            if (ability.supreme()) {
                supreme++;
                if (!last) problems.add(who + "the supreme ability is the last one on the wheel");
            }
            if (ability.kind().get().fight()) {
                fight++;
                if (worldSeen && !(ability.supreme() && last)) problems.add(who + "the fighting abilities come before the world ones");
            } else {
                world++;
                if (!ability.supreme()) worldSeen = true;
            }
            if (ability.functions().size() > 2) problems.add(who + "has " + (ability.functions().size() + 1) + " functions, at most 3");
            for (CharacterData.Function function : ability.functions()) {
                CharacterData.When when = function.when();
                if (when.always()) problems.add(who + "function " + function.preset() + " needs a condition (quando)");
                when.press().filter(p -> !AbilityFunctions.PRESSES.contains(p)).ifPresent(p -> problems.add(who + "unknown aperto " + p + " (one of " + AbilityFunctions.PRESSES + ")"));
                when.place().filter(p -> !AbilityFunctions.PLACES.contains(p)).ifPresent(p -> problems.add(who + "unknown lugar " + p + " (one of " + AbilityFunctions.PLACES + ")"));
                when.aim().filter(a -> !AbilityFunctions.knownAim(a)).ifPresent(a -> problems.add(who + "unknown mira " + a + " (one of " + AbilityFunctions.AIMS + ", a block or a #tag)"));
            }
        }
        if (!arsenal.wheel().holds(abilities.size()) || abilities.size() < 5 || abilities.size() > 8) {
            problems.add("needs " + arsenal.wheel() + " abilities for the class " + data.characterClass().id() + ", has " + abilities.size());
        }
        if (fight < 3 || !arsenal.fight().holds(fight)) problems.add("needs " + arsenal.fight() + " fighting abilities (at least 3), has " + fight);
        if (world < 2 || !arsenal.world().holds(world)) problems.add("needs " + arsenal.world() + " world abilities (at least 2), has " + world);
        if (supreme != 1) problems.add("needs exactly 1 supreme ability (suprema: true), has " + supreme);
        if (data.traits().size() > Traits.MAX) problems.add("has " + data.traits().size() + " traits (passivas), at most " + Traits.MAX);
        for (CharacterData.Trait trait : data.traits()) {
            if (!Traits.TYPES.contains(trait.type())) problems.add("unknown passiva " + trait.type() + " (one of " + Traits.TYPES + ")");
        }
        if (data.traits().stream().map(CharacterData.Trait::type).distinct().count() != data.traits().size()) problems.add("a passiva is listed twice");
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
