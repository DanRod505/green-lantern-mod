package com.danrod505.greenlantern.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * A character as a data file ({@code data/greenlantern/characters/<id>.json}): class, power level,
 * stats, item, skeleton, skins, transformation, theme, weaknesses and the abilities. The engine
 * reads the file and builds the character from it (see {@link Characters}); nobody writes code for
 * one character.
 * <p>
 * The first 9 heroes are files too, marked {@code "legado": true}: their powers and energy are still
 * their own code, and the file holds what the engine needs from them (the suit, its bonuses and the
 * order of the wheel), with the same numbers as before.
 *
 * @param id             short id, e.g. {@code "superman"} (also the file name)
 * @param name           display name in Portuguese, used when the lang files have no {@code character.greenlantern.<id>}
 * @param characterClass the class: energy, stat rule and passive
 * @param level          power level, 1 (Street) to 4 (Legendary)
 * @param skeleton       the model the skin is drawn on
 * @param transformation the suit up animation (one of the 15 of the catalog)
 * @param theme          the music theme (one of the 15 of the catalog)
 * @param legacy         one of the 9 heroes that already existed: their own code drives them
 * @param source         where the energy comes from (Colossus: sol, tempestade, calor) or the element (Elementalist)
 * @param stats          the stats; what is left out comes from the class rule
 * @param suit           the suit pieces and their bonuses (worked out from the stats when left out)
 * @param abilities      the abilities in wheel order (6 for a new character: 3 main, 2 secondary, the Supreme)
 * @param skins          the skins, the first one is the default
 * @param weaknesses     what hurts the character (kryptonite, magic, fire...)
 * @param colors         colours of the HUD and the wheel
 */
public record CharacterData(String id, String name, CharacterClass characterClass, int level, Skeleton skeleton, String transformation,
                            String theme, boolean legacy, Optional<String> source, Stats stats, Optional<Suit> suit, List<Ability> abilities,
                            List<Skin> skins, List<Weakness> weaknesses, Colors colors) {

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
     * An ability: the preset that runs it in a fight and the one that runs it in the world (crouch
     * plus the power key), with its numbers. The first heroes only list the ids of their powers.
     */
    public record Ability(String id, Optional<String> name, Optional<AbilityKind> kind, Optional<AbilitySlot> slot, String preset,
                          Optional<String> worldPreset, Numbers numbers) {
        static final Codec<Ability> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Ability::id),
                Codec.STRING.optionalFieldOf("nome").forGetter(Ability::name),
                AbilityKind.CODEC.optionalFieldOf("classe").forGetter(Ability::kind),
                AbilitySlot.CODEC.optionalFieldOf("tipo").forGetter(Ability::slot),
                Codec.STRING.optionalFieldOf("preset", "legado").forGetter(Ability::preset),
                Codec.STRING.optionalFieldOf("uso_no_mundo").forGetter(Ability::worldPreset),
                Numbers.CODEC.optionalFieldOf("numeros", Numbers.NONE).forGetter(Ability::numbers)
        ).apply(i, Ability::new));
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
            Colors.CODEC.optionalFieldOf("cores", Colors.DEFAULT).forGetter(CharacterData::colors)
    ).apply(i, CharacterData::new));

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

    /** The default skin (the first one), or a skin named after the character. */
    public Skin defaultSkin() {
        return skins.isEmpty() ? new Skin(id, name, "padrao") : skins.getFirst();
    }
}
