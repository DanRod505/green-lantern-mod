package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/**
 * A character as a data file ({@code data/greenlantern/characters/<id>.json}): class, power level,
 * stats, item, skeleton, skins, transformation, theme, weaknesses and the abilities. The engine
 * reads the file and builds the character from it (see {@link Characters}); nobody writes code for
 * one character.
 * <p>
 * The first 9 heroes are files too, marked {@code "legado": true} (see {@link LegacyHero}): their
 * wheel, functions and traits come from the file like any character's, and a function with the
 * preset {@code poder_do_heroi} runs a power of their own code, with its own energy, cooldown and
 * checks as before.
 *
 * @param id             short id, e.g. {@code "superman"} (also the file name)
 * @param name           display name in Portuguese, used when the lang files have no {@code character.greenlantern.<id>}
 * @param characterClass the class: energy, stat rule and passive
 * @param level          power level, 1 (Street) to 4 (Legendary)
 * @param skeleton       the model the skin is drawn on
 * @param transformation the suit up animation (one of the 15 of the catalog)
 * @param theme          the music theme (one of the 15 of the catalog)
 * @param legacy         one of the 9 heroes that already existed: their own code has their powers
 * @param source         where the energy comes from (Colossus: sol, tempestade, calor) or the element (Elementalist)
 * @param stats          the stats; what is left out comes from the class rule
 * @param suit           the suit pieces and their bonuses (worked out from the stats when left out)
 * @param abilities      the abilities in wheel order (5 to 8 for a new character: the fighting ones, the world ones, the supreme last)
 * @param skins          the skins, the first one is the default
 * @param weaknesses     what hurts the character (kryptonite, magic, fire...)
 * @param colors         colours of the HUD and the wheel
 * @param visual         the settings of the transformation, the trail and the aura
 * @param traits         the character's traits (0 to 4), off the wheel: flight, super speed, swimming...
 */
public record CharacterData(String id, String name, CharacterClass characterClass, int level, Skeleton skeleton, String transformation,
                            String theme, boolean legacy, Optional<String> source, Stats stats, Optional<Suit> suit, List<Ability> abilities,
                            List<Skin> skins, List<Weakness> weaknesses, Colors colors, Visual visual, List<Trait> traits) {

    /** Stats of the catalog (stage 12); empty ones come from the class rule. */
    public record Stats(Optional<Double> hearts, Optional<Double> damage, Optional<Integer> defense, Optional<Integer> speed,
                        Optional<Double> firmness, Optional<Double> flight, Optional<Integer> energy) {
        public static final Stats NONE = new Stats(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        static final Codec<Stats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("coracoes").forGetter(Stats::hearts),
                Codec.DOUBLE.optionalFieldOf("dano").forGetter(Stats::damage),
                Codec.INT.optionalFieldOf("defesa").forGetter(Stats::defense),
                Codec.INT.optionalFieldOf("velocidade").forGetter(Stats::speed),
                Codec.DOUBLE.optionalFieldOf("firmeza").forGetter(Stats::firmness),
                Codec.DOUBLE.optionalFieldOf("voo").forGetter(Stats::flight),
                Codec.INT.optionalFieldOf("energia").forGetter(Stats::energy)
        ).apply(i, Stats::new));
    }

    /** A bonus of the suit while it is worn, exactly like {@code hero.SuitModifier}. */
    public record Bonus(Identifier attribute, String id, double value, String operation) {
        static final Codec<Bonus> CODEC = RecordCodecBuilder.<Bonus>create(i -> i.group(
                Identifier.CODEC.fieldOf("atributo").forGetter(Bonus::attribute),
                Codec.STRING.fieldOf("id").forGetter(Bonus::id),
                Codec.DOUBLE.fieldOf("valor").forGetter(Bonus::value),
                Codec.STRING.optionalFieldOf("operacao", "somar").forGetter(Bonus::operation)
        ).apply(i, Bonus::new)).validate(b -> b.operation.equals("somar") || b.operation.equals("multiplicar")
                ? DataResult.success(b) : DataResult.error(() -> "operacao must be somar or multiplicar: " + b.operation));

        public boolean multiply() {
            return operation.equals("multiplicar");
        }
    }

    /** Armor points of each suit piece. */
    public record Defense(int head, int chest, int legs, int feet) {
        static final Codec<Defense> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("cabeca").forGetter(Defense::head),
                Codec.INT.fieldOf("peito").forGetter(Defense::chest),
                Codec.INT.fieldOf("pernas").forGetter(Defense::legs),
                Codec.INT.fieldOf("pes").forGetter(Defense::feet)
        ).apply(i, Defense::new));

        public int total() {
            return head + chest + legs + feet;
        }
    }

    /**
     * The suit pieces: their armor, toughness, knockback resistance, equip sound, the repair tag the
     * armor material asks for, the equipment asset that draws them, and the bonuses while worn.
     */
    public record Suit(Optional<Identifier> look, Defense defense, double toughness, double knockbackResistance, Identifier equipSound,
                       Identifier repairTag, List<Bonus> bonuses) {
        static final Codec<Suit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("visual").forGetter(Suit::look),
                Defense.CODEC.fieldOf("defesa").forGetter(Suit::defense),
                Codec.DOUBLE.optionalFieldOf("resistencia", 2.0).forGetter(Suit::toughness),
                Codec.DOUBLE.optionalFieldOf("resistencia_a_empurrao", 0.0).forGetter(Suit::knockbackResistance),
                Identifier.CODEC.optionalFieldOf("som", Identifier.withDefaultNamespace("item.armor.equip_generic")).forGetter(Suit::equipSound),
                Identifier.CODEC.optionalFieldOf("conserto", Identifier.withDefaultNamespace("repairs_diamond_armor")).forGetter(Suit::repairTag),
                Bonus.CODEC.listOf().optionalFieldOf("atributos", List.of()).forGetter(Suit::bonuses)
        ).apply(i, Suit::new));
    }

    /** The numbers of an ability (stage 15): damage, cost, cooldown (seconds), reach, radius and duration (seconds). */
    public record Numbers(double damage, int cost, double cooldown, double reach, double radius, double duration) {
        public static final Numbers NONE = new Numbers(0, 0, 0, 0, 0, 0);
        static final Codec<Numbers> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("dano", 0.0).forGetter(Numbers::damage),
                Codec.INT.optionalFieldOf("custo", 0).forGetter(Numbers::cost),
                Codec.DOUBLE.optionalFieldOf("recarga", 0.0).forGetter(Numbers::cooldown),
                Codec.DOUBLE.optionalFieldOf("alcance", 0.0).forGetter(Numbers::reach),
                Codec.DOUBLE.optionalFieldOf("raio", 0.0).forGetter(Numbers::radius),
                Codec.DOUBLE.optionalFieldOf("duracao", 0.0).forGetter(Numbers::duration)
        ).apply(i, Numbers::new));
    }

    /**
     * An ability on the wheel: its main class ({@code "classe"}), whether it is the character's
     * supreme one ({@code "suprema"}: stronger or more impressive, with a much bigger cost and
     * cooldown), the preset of its default function with its numbers and settings ({@code "ajustes"}:
     * the push of a punch, the number of shots of a fan...), and up to 2 more functions
     * ({@code "funcoes"}) that go off instead when their condition holds (see
     * {@link AbilityFunctions}). The first heroes only list the ids of their powers.
     */
    public record Ability(String id, Optional<String> name, Optional<AbilityKind> kind, boolean supreme, String preset, Numbers numbers,
                          Settings settings, List<Function> functions) {
        static final Codec<Ability> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Ability::id),
                Codec.STRING.optionalFieldOf("nome").forGetter(Ability::name),
                AbilityKind.CODEC.optionalFieldOf("classe").forGetter(Ability::kind),
                Codec.BOOL.optionalFieldOf("suprema", false).forGetter(Ability::supreme),
                Codec.STRING.optionalFieldOf("preset", "legado").forGetter(Ability::preset),
                Numbers.CODEC.optionalFieldOf("numeros", Numbers.NONE).forGetter(Ability::numbers),
                Settings.CODEC.optionalFieldOf("ajustes", Settings.NONE).forGetter(Ability::settings),
                Function.CODEC.listOf().optionalFieldOf("funcoes", List.of()).forGetter(Ability::functions)
        ).apply(i, Ability::new));

        /** An ability with only its default function. */
        public Ability(String id, Optional<String> name, Optional<AbilityKind> kind, boolean supreme, String preset, Numbers numbers, Settings settings) {
            this(id, name, kind, supreme, preset, numbers, settings, List.of());
        }

        /** The default function (index 0): the ability's own preset, numbers and settings, with no condition. */
        public Function defaultFunction() {
            return new Function(preset, When.ALWAYS, Numbers.NONE, Settings.NONE);
        }

        /** Every function in order: the default one first, then those of {@code "funcoes"}. */
        public List<Function> allFunctions() {
            List<Function> all = new java.util.ArrayList<>(functions.size() + 1);
            all.add(defaultFunction());
            all.addAll(functions);
            return all;
        }

        /** The numbers of a function: its own where it gives them (above 0), else the ability's. */
        public Numbers numbers(Function function) {
            Numbers own = function.numbers();
            return new Numbers(own.damage() > 0 ? own.damage() : numbers.damage(), numbers.cost(), numbers.cooldown(),
                    own.reach() > 0 ? own.reach() : numbers.reach(), own.radius() > 0 ? own.radius() : numbers.radius(),
                    own.duration() > 0 ? own.duration() : numbers.duration());
        }

        /** The settings of a function: the ability's, with the function's own on top. */
        public Settings settings(Function function) {
            if (function.settings().values().isEmpty()) return settings;
            Map<String, JsonElement> merged = new java.util.LinkedHashMap<>(settings.values());
            merged.putAll(function.settings().values());
            return new Settings(merged);
        }
    }

    /**
     * When a function goes off ({@code "quando"} in the file): how the key is pressed
     * ({@code "aperto"}: toque, segurar or soltar after holding), what the crosshair is on
     * ({@code "mira"}: mob, bloco, vazio, a kind of block like minerio or agua, a block id or a
     * {@code #tag}) and where the player is ({@code "lugar"}: chao, ar, agua or correndo). What is
     * left out always holds.
     */
    public record When(Optional<String> press, Optional<String> aim, Optional<String> place) {
        public static final When ALWAYS = new When(Optional.empty(), Optional.empty(), Optional.empty());
        static final Codec<When> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("aperto").forGetter(When::press),
                Codec.STRING.optionalFieldOf("mira").forGetter(When::aim),
                Codec.STRING.optionalFieldOf("lugar").forGetter(When::place)
        ).apply(i, When::new));

        public boolean always() {
            return press.isEmpty() && aim.isEmpty() && place.isEmpty();
        }
    }

    /**
     * One more function of an ability: the preset it runs, when ({@code "quando"}), and its own
     * numbers and settings on top of the ability's. It may be of another class than the ability, even
     * of the other group: the heat vision is a shot at a mob and melts the block it aims at.
     */
    public record Function(String preset, When when, Numbers numbers, Settings settings) {
        static final Codec<Function> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("preset").forGetter(Function::preset),
                When.CODEC.optionalFieldOf("quando", When.ALWAYS).forGetter(Function::when),
                Numbers.CODEC.optionalFieldOf("numeros", Numbers.NONE).forGetter(Function::numbers),
                Settings.CODEC.optionalFieldOf("ajustes", Settings.NONE).forGetter(Function::settings)
        ).apply(i, Function::new));
    }

    /**
     * A trait of the character ({@code "passivas"} in the file): something natural to it that is not
     * on the wheel and turns on by itself, through Minecraft's own action or the moment (flight on a
     * double jump, super speed when sprinting, swimming in water...). See {@link Traits}.
     */
    public record Trait(String type, Settings settings) {
        static final Codec<Trait> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("tipo").forGetter(Trait::type),
                Settings.CODEC.optionalFieldOf("ajustes", Settings.NONE).forGetter(Trait::settings)
        ).apply(i, Trait::new));
    }

    /**
     * The settings of a preset ({@code "ajustes"} in the file): numbers, words and yes/no by name.
     * Each preset says which ones it reads and their defaults; a setting it doesn't know is ignored.
     */
    public record Settings(Map<String, JsonElement> values) {
        public static final Settings NONE = new Settings(Map.of());
        static final Codec<Settings> CODEC = Codec.unboundedMap(Codec.STRING, ExtraCodecs.JSON).xmap(Settings::new, Settings::values);

        public double number(String key, double fallback) {
            JsonElement value = values.get(key);
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsDouble() : fallback;
        }

        public int whole(String key, int fallback) {
            return (int) Math.round(number(key, fallback));
        }

        public String text(String key, String fallback) {
            JsonElement value = values.get(key);
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : fallback;
        }

        public boolean flag(String key, boolean fallback) {
            JsonElement value = values.get(key);
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
        }

        /** A colour ({@code "#FFAA00"}), or {@code fallback} when it is missing or not a colour. */
        public int color(String key, int fallback) {
            String text = text(key, "");
            if (text.isEmpty()) return fallback;
            try {
                return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        public boolean has(String key) {
            return values.containsKey(key);
        }

        /** A list of words ({@code ["queda", "fogo"]} or {@code "queda, fogo"}), or empty when the setting is missing. */
        public List<String> words(String key) {
            JsonElement value = values.get(key);
            List<String> list = new java.util.ArrayList<>();
            if (value == null) return list;
            if (value.isJsonArray()) {
                for (JsonElement e : value.getAsJsonArray()) {
                    if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) list.add(e.getAsString().trim());
                }
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                for (String word : value.getAsString().split(",")) {
                    if (!word.isBlank()) list.add(word.trim());
                }
            }
            return list;
        }

        /** These settings, with {@code defaults} for the ones they leave out. */
        public Settings or(Map<String, JsonElement> defaults) {
            Map<String, JsonElement> merged = new java.util.LinkedHashMap<>(defaults);
            merged.putAll(values);
            return new Settings(merged);
        }

        /** A list of numbers ({@code [4, 4, 7]}), or empty when the setting is missing. */
        public List<Double> numbers(String key) {
            JsonElement value = values.get(key);
            if (value == null || !value.isJsonArray()) return List.of();
            List<Double> list = new java.util.ArrayList<>();
            for (JsonElement e : value.getAsJsonArray()) {
                if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) list.add(e.getAsDouble());
            }
            return list;
        }
    }

    /** A skin: its texture is {@code textures/entity/skin/<id>.png}, with {@code _brilho} (glows) and {@code _asas} (wings) beside it. */
    public record Skin(String id, String name, String unlock) {
        static final Codec<Skin> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Skin::id),
                Codec.STRING.fieldOf("nome").forGetter(Skin::name),
                Codec.STRING.optionalFieldOf("liberada_por", "padrao").forGetter(Skin::unlock)
        ).apply(i, Skin::new));

        public Identifier texture() {
            return Identifier.fromNamespaceAndPath("greenlantern", "textures/entity/skin/" + id + ".png");
        }

        public Identifier glow() {
            return Identifier.fromNamespaceAndPath("greenlantern", "textures/entity/skin/" + id + "_brilho.png");
        }

        public Identifier wings() {
            return Identifier.fromNamespaceAndPath("greenlantern", "textures/entity/skin/" + id + "_asas.png");
        }
    }

    /**
     * A weakness: {@code bloco_perto} (a block within {@code reach} blocks weakens the character and
     * drains the energy) or {@code dano} (damage of the given types is multiplied).
     */
    public record Weakness(String type, Optional<Identifier> block, double reach, List<Identifier> damageTypes, double multiplier,
                           Optional<String> description) {
        static final Codec<Weakness> CODEC = RecordCodecBuilder.<Weakness>create(i -> i.group(
                Codec.STRING.fieldOf("tipo").forGetter(Weakness::type),
                Identifier.CODEC.optionalFieldOf("bloco").forGetter(Weakness::block),
                Codec.DOUBLE.optionalFieldOf("alcance", 6.0).forGetter(Weakness::reach),
                Identifier.CODEC.listOf().optionalFieldOf("danos", List.of()).forGetter(Weakness::damageTypes),
                Codec.DOUBLE.optionalFieldOf("multiplicador", 2.0).forGetter(Weakness::multiplier),
                Codec.STRING.optionalFieldOf("descricao").forGetter(Weakness::description)
        ).apply(i, Weakness::new)).validate(w -> switch (w.type) {
            case "bloco_perto" -> w.block.isPresent() ? DataResult.success(w) : DataResult.error(() -> "bloco_perto needs a bloco");
            case "dano" -> !w.damageTypes.isEmpty() ? DataResult.success(w) : DataResult.error(() -> "dano needs danos");
            case "descricao" -> DataResult.success(w);
            default -> DataResult.error(() -> "unknown weakness tipo " + w.type);
        });
    }

    /** Colours (RGB) of the HUD and the wheel: the main one, the second one and the light one. */
    public record Colors(int main, int second, int light) {
        public static final Colors DEFAULT = new Colors(0x3C6EB4, 0xF2B705, 0xFFFFFF);
        private static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success(Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16) & 0xFFFFFF);
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "not a colour: " + s);
            }
        }, c -> String.format("#%06X", c));
        static final Codec<Colors> CODEC = RecordCodecBuilder.create(i -> i.group(
                HEX.fieldOf("principal").forGetter(Colors::main),
                HEX.fieldOf("secundaria").forGetter(Colors::second),
                HEX.optionalFieldOf("brilho", 0xFFFFFF).forGetter(Colors::light)
        ).apply(i, Colors::new));
    }

    /**
     * How the character looks beyond its skin ({@code "visual"} in the file): the settings of its
     * transformation (colour, sound, duration, kind...), of the trail it leaves when it flies or runs
     * and of the aura around its body (colour, width, glow). Each is empty when left out: the
     * transformation then uses the character's colours, and there is no trail or aura.
     */
    public record Visual(Settings transformation, Settings trail, Settings aura) {
        public static final Visual NONE = new Visual(Settings.NONE, Settings.NONE, Settings.NONE);
        static final Codec<Visual> CODEC = RecordCodecBuilder.create(i -> i.group(
                Settings.CODEC.optionalFieldOf("transformacao", Settings.NONE).forGetter(Visual::transformation),
                Settings.CODEC.optionalFieldOf("rastro", Settings.NONE).forGetter(Visual::trail),
                Settings.CODEC.optionalFieldOf("aura", Settings.NONE).forGetter(Visual::aura)
        ).apply(i, Visual::new));
    }

    public static final Codec<CharacterData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterData::id),
            Codec.STRING.fieldOf("nome").forGetter(CharacterData::name),
            CharacterClass.CODEC.fieldOf("classe").forGetter(CharacterData::characterClass),
            Codec.intRange(1, 4).fieldOf("nivel").forGetter(CharacterData::level),
            Skeleton.CODEC.fieldOf("esqueleto").forGetter(CharacterData::skeleton),
            Codec.STRING.fieldOf("transformacao").forGetter(CharacterData::transformation),
            Codec.STRING.fieldOf("trilha").forGetter(CharacterData::theme),
            Codec.BOOL.optionalFieldOf("legado", false).forGetter(CharacterData::legacy),
            Codec.STRING.optionalFieldOf("fonte").forGetter(CharacterData::source),
            Stats.CODEC.optionalFieldOf("stats", Stats.NONE).forGetter(CharacterData::stats),
            Suit.CODEC.optionalFieldOf("traje").forGetter(CharacterData::suit),
            Ability.CODEC.listOf().optionalFieldOf("habilidades", List.of()).forGetter(CharacterData::abilities),
            Skin.CODEC.listOf().optionalFieldOf("skins", List.of()).forGetter(CharacterData::skins),
            Weakness.CODEC.listOf().optionalFieldOf("fraquezas", List.of()).forGetter(CharacterData::weaknesses),
            Colors.CODEC.optionalFieldOf("cores", Colors.DEFAULT).forGetter(CharacterData::colors),
            Extra.CODEC.forGetter(d -> new Extra(d.visual, d.traits))
    ).apply(i, (id, name, characterClass, level, skeleton, transformation, theme, legacy, source, stats, suit, abilities, skins, weaknesses, colors, extra) ->
            new CharacterData(id, name, characterClass, level, skeleton, transformation, theme, legacy, source, stats, suit, abilities, skins, weaknesses,
                    colors, extra.visual(), extra.traits())));

    /** The last fields of the file, in a group of their own (a codec takes at most 16 at once). */
    private record Extra(Visual visual, List<Trait> traits) {
        static final com.mojang.serialization.MapCodec<Extra> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Visual.CODEC.optionalFieldOf("visual", Visual.NONE).forGetter(Extra::visual),
                Trait.CODEC.listOf().optionalFieldOf("passivas", List.of()).forGetter(Extra::traits)
        ).apply(i, Extra::new));
    }

    // ---- stats worked out from the class rule ------------------------------------------------------

    public ClassRules.ClassRule classRule() {
        return characterClass.rule();
    }

    public Passive passive() {
        return classRule().passive();
    }

    private boolean legendary() {
        return level == 4;
    }

    /** Total hearts with the full suit (10 is a normal player). */
    public double hearts() {
        return stats.hearts().orElseGet(() -> 10.0 + classRule().life() * ClassRules.get().level(level).hearts());
    }

    /** Extra melee damage. */
    public double damage() {
        return stats.damage().orElseGet(() -> classRule().damage() * ClassRules.get().level(level).damage());
    }

    /** Armor points of the full suit. */
    public int defense() {
        return stats.defense().orElseGet(() -> legendary() ? ClassRules.get().legendary().defense() : classRule().defense());
    }

    /** Walking speed bonus, in percent. */
    public int speed() {
        return stats.speed().orElseGet(() -> legendary() ? ClassRules.get().legendary().speed() : classRule().speed());
    }

    /** Knockback resistance, 0 to 1. */
    public double firmness() {
        return stats.firmness().orElseGet(() -> legendary() && classRule().firmness() > 0 ? 1.0 : classRule().firmness());
    }

    /** Top flying speed (blocks per tick), 0 when the character doesn't fly. */
    public double flight() {
        return stats.flight().orElse(0.0);
    }

    /** Size of the energy bar. */
    public int energy() {
        return stats.energy().orElseGet(() -> {
            int capacity = classRule().energy().capacity();
            return legendary() ? (int) Math.round(capacity * ClassRules.get().legendary().energy()) : capacity;
        });
    }

    /** The trait of this type ({@code "voo"}, {@code "aquatico"}...), when the character has it. */
    public Optional<Trait> trait(String type) {
        return traits.stream().filter(t -> t.type().equals(type)).findFirst();
    }

    /** The default skin (the first one), or a skin named after the character. */
    public Skin defaultSkin() {
        return skins.isEmpty() ? new Skin(id, name, "padrao") : skins.getFirst();
    }
}
