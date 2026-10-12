package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.PowerSet;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One of the 9 heroes that already existed, in the new model of abilities (stage 3, "Adaptação dos 9
 * heróis"): the wheel is the one of the catalog, read from its character file (5 to 8 abilities,
 * the supreme last, each with its functions by how the key is pressed and where the player aims),
 * and the traits of the file are on. The powers of the hero's own code (the mecha, the Kraken, the
 * lasso...) are still the same code: a function with the preset {@value CharacterHero#OWN_POWER}
 * names one with {@code "poder"} and uses it exactly as before, with its own energy, cooldown and
 * checks. The other functions are presets of the library, paid from the hero's energy.
 */
public abstract class LegacyHero extends HeroDefinition implements CharacterHero {
    /** The traits the heroes' own code already gives: their flight, the Flash's run, Aquaman's swim and Batman's cape. */
    public static final Set<String> OWN_TRAITS = Set.of("voo", "supervelocidade", "aquatico", "planar");

    private HeroPowers<EngineAbility> wheel;

    protected LegacyHero(String id, Predicate<ItemStack> activator, SuitSet suit) {
        super(id, activator, suit);
    }

    @Override
    public CharacterData data() {
        return Characters.get(id());
    }

    /**
     * The powers of the hero's own code, as they were on the old wheel: what the preset
     * {@value CharacterHero#OWN_POWER} uses, by id. Null for a hero whose own powers are something
     * else (the Lantern's constructs), which then overrides {@link #ownPower} and {@link #ownCost}.
     */
    public abstract HeroPowers<? extends HeroPower> ownPowers();

    /** How the old wheel looked: the new one keeps its colours and texts, with the icons of the new abilities. */
    public WheelStyle ownWheel() {
        return ownPowers().wheel();
    }

    /** The colour of the line that names the chosen ability. */
    protected ChatFormatting selectColor() {
        return ownPowers() != null ? ownPowers().selectColor() : ChatFormatting.GREEN;
    }

    /** The wheel of the catalog, built from the character file the first time it is asked for. */
    @Override
    public synchronized HeroPowers<EngineAbility> powers() {
        if (wheel == null) {
            List<CharacterData.Ability> list = data().abilities();
            EngineAbility[] abilities = new EngineAbility[list.size()];
            for (int i = 0; i < abilities.length; i++) {
                CharacterData.Ability ability = list.get(i);
                abilities[i] = new EngineAbility(ability, i, ability.kind().orElseThrow(), ownPowerOf(ability).map(this::ownCost).orElse(null));
            }
            WheelStyle own = ownWheel();
            WheelStyle style = new WheelStyle(own.theme(), GreenLantern.id("engine_icons/" + id()), AbilityIcons.SIZE * AbilityIcons.cells(data()),
                    own.costKey(), own.energyKey(), own.background(), own.nameColor(), own.costColor(), own.textColor(), own.dimIcon(), own.numberColor(),
                    own.hintColor());
            float pitchBase = ownPowers() != null ? ownPowers().pitchBase() : 0.8F;
            float pitchStep = ownPowers() != null ? ownPowers().pitchStep() : 0.08F;
            wheel = new HeroPowers<>(new PowerSet<>(abilities), energy(), selectColor(), pitchBase, pitchStep, EngineServer::useAbility, style);
        }
        return wheel;
    }

    /** The power of the hero's own code that a tap of {@code ability} runs (its default function), if it is one. */
    public static Optional<String> ownPowerOf(CharacterData.Ability ability) {
        return ownPowerOf(ability, ability.defaultFunction());
    }

    /** The power of the hero's own code that {@code function} of {@code ability} runs, if it is one. */
    public static Optional<String> ownPowerOf(CharacterData.Ability ability, CharacterData.Function function) {
        if (!function.preset().equals(OWN_POWER)) return Optional.empty();
        String power = ability.settings(function).text("poder", "");
        return power.isEmpty() ? Optional.empty() : Optional.of(power);
    }

    /** The power of the hero's own code with this id, or null. */
    public HeroPower ownPowerById(String power) {
        HeroPowers<? extends HeroPower> own = ownPowers();
        if (own == null) return null;
        for (int i = 0; i < own.set().count(); i++) {
            HeroPower candidate = own.set().byIndex(i);
            if (candidate.id().equals(power)) return candidate;
        }
        return null;
    }

    /** The cell of a power of the hero's own code on its old icon sheet ({@link #ownWheel}), or -1. */
    public int ownIconIndex(String power) {
        HeroPower own = ownPowerById(power);
        return own == null ? -1 : own.iconIndex();
    }

    @Override
    public boolean hasOwnPower(String power) {
        return ownPowerById(power) != null;
    }

    /** The cost of a power of the hero's own code, as its old wheel showed it. */
    protected IntSupplier ownCost(String power) {
        HeroPower own = ownPowerById(power);
        return own == null ? null : own::cost;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean ownPower(ServerPlayer player, ItemStack item, String power) {
        HeroPower own = ownPowerById(power);
        if (own == null) return false;
        HeroPowers powers = ownPowers();
        return powers.action().use(player, item, own);
    }

    /** The index on the new wheel of the ability whose tap runs this power of the hero's own code, or -1. */
    public int wheelIndexOf(String power) {
        HeroPowers<EngineAbility> powers = powers();
        for (int i = 0; i < powers.set().count(); i++) {
            if (ownPowerOf(powers.set().byIndex(i).data()).filter(power::equals).isPresent()) return i;
        }
        return -1;
    }

    /** The index on the new wheel of the ability with a function (the tap first) that runs this power of the hero's own code, or -1. */
    public int abilityIndexOf(String power) {
        int tap = wheelIndexOf(power);
        if (tap >= 0) return tap;
        HeroPowers<EngineAbility> powers = powers();
        for (int i = 0; i < powers.set().count(); i++) {
            CharacterData.Ability ability = powers.set().byIndex(i).data();
            for (CharacterData.Function function : ability.allFunctions()) {
                if (ownPowerOf(ability, function).filter(power::equals).isPresent()) return i;
            }
        }
        return -1;
    }

    /** Chooses on the hero's item the ability that runs this power of the hero's own code (for the tests and the screenshot scripts). */
    public void selectOwn(ItemStack item, String power) {
        int index = abilityIndexOf(power);
        if (index < 0) throw new IllegalArgumentException(id() + ": no ability runs " + power);
        powers().set().select(item, powers().set().byIndex(index));
    }

    /** What the presets and the traits left goes away with the suit. */
    @Override
    protected void onAbilitiesOff(ServerPlayer player) {
        EngineServer.legacySuitDown(player);
    }
}
