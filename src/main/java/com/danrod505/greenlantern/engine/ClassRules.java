package com.danrod505.greenlantern.engine;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The rules of the classes and power levels, read from {@code data/greenlantern/engine/classes.json}:
 * each stat of a character comes from its class times its level, unless its file says otherwise.
 */
public record ClassRules(List<LevelRule> levels, Legendary legendary, List<ClassRule> classes) {
    public static final String PATH = "data/greenlantern/engine/classes.json";

    /** Extra hearts and damage of a power level (1 Street, 2 Hero, 3 Mighty, 4 Legendary). */
    public record LevelRule(int level, String name, double hearts, double damage) {
        static final Codec<LevelRule> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("nivel").forGetter(LevelRule::level),
                Codec.STRING.fieldOf("nome").forGetter(LevelRule::name),
                Codec.DOUBLE.fieldOf("coracoes").forGetter(LevelRule::hearts),
                Codec.DOUBLE.fieldOf("dano").forGetter(LevelRule::damage)
        ).apply(i, LevelRule::new));
    }

    /** What level 4 always has: more defense and speed, and a bigger energy bar. */
    public record Legendary(int defense, int speed, double energy) {
        static final Codec<Legendary> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("defesa").forGetter(Legendary::defense),
                Codec.INT.fieldOf("velocidade").forGetter(Legendary::speed),
                Codec.DOUBLE.fieldOf("energia").forGetter(Legendary::energy)
        ).apply(i, Legendary::new));
    }

    /**
     * How the class energy fills: {@code recharge} per second, times {@code multiplier} while the
     * condition holds, plus {@code perHit} for each blow that lands.
     */
    public record EnergyRule(int capacity, double recharge, RechargeCondition condition, double multiplier, int perHit) {
        static final Codec<EnergyRule> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("capacidade").forGetter(EnergyRule::capacity),
                Codec.DOUBLE.fieldOf("recarga").forGetter(EnergyRule::recharge),
                RechargeCondition.CODEC.fieldOf("condicao").forGetter(EnergyRule::condition),
                Codec.DOUBLE.fieldOf("multiplicador").forGetter(EnergyRule::multiplier),
                Codec.INT.fieldOf("ao_acertar").forGetter(EnergyRule::perHit)
        ).apply(i, EnergyRule::new));
    }

    /**
     * The stat weights of a class: hearts and damage are these times the level's extra, defense,
     * speed (percent) and knockback resistance are the class's own.
     */
    public record ClassRule(CharacterClass id, double life, double damage, int defense, int speed, double firmness, EnergyRule energy, Passive passive) {
        static final Codec<ClassRule> CODEC = RecordCodecBuilder.create(i -> i.group(
                CharacterClass.CODEC.fieldOf("id").forGetter(ClassRule::id),
                Codec.DOUBLE.fieldOf("vida").forGetter(ClassRule::life),
                Codec.DOUBLE.fieldOf("dano").forGetter(ClassRule::damage),
                Codec.INT.fieldOf("defesa").forGetter(ClassRule::defense),
                Codec.INT.fieldOf("velocidade").forGetter(ClassRule::speed),
                Codec.DOUBLE.fieldOf("firmeza").forGetter(ClassRule::firmness),
                EnergyRule.CODEC.fieldOf("energia").forGetter(ClassRule::energy),
                Passive.CODEC.fieldOf("passiva").forGetter(ClassRule::passive)
        ).apply(i, ClassRule::new));
    }

    public static final Codec<ClassRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            LevelRule.CODEC.listOf().fieldOf("niveis").forGetter(ClassRules::levels),
            Legendary.CODEC.fieldOf("lendario").forGetter(ClassRules::legendary),
            ClassRule.CODEC.listOf().fieldOf("classes").forGetter(ClassRules::classes)
    ).apply(i, ClassRules::new));

    private static ClassRules instance;

    public static synchronized ClassRules get() {
        if (instance == null) {
            JsonElement json = DataFiles.readJson(PATH);
            instance = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(error -> new IllegalStateException(PATH + ": " + error));
            instance.validate();
        }
        return instance;
    }

    private void validate() {
        Map<CharacterClass, ClassRule> seen = new EnumMap<>(CharacterClass.class);
        for (ClassRule rule : classes) {
            if (seen.put(rule.id(), rule) != null) throw new IllegalStateException(PATH + ": class " + rule.id().id() + " twice");
        }
        for (CharacterClass c : CharacterClass.values()) {
            if (!seen.containsKey(c)) throw new IllegalStateException(PATH + ": class " + c.id() + " is missing");
        }
        for (int level = 1; level <= 4; level++) level(level);
    }

    public ClassRule rule(CharacterClass characterClass) {
        for (ClassRule rule : classes) {
            if (rule.id() == characterClass) return rule;
        }
        throw new IllegalStateException("No rule for class " + characterClass.id());
    }

    public LevelRule level(int level) {
        for (LevelRule rule : levels) {
            if (rule.level() == level) return rule;
        }
        throw new IllegalStateException(PATH + ": level " + level + " is missing");
    }
}
