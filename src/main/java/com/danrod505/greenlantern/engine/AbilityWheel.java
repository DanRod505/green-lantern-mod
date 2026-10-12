package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.PowerSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * The wheel of the new model (stage 21) for any hero: the 5 to 8 abilities of its file, each with its
 * functions chosen by the press, the aim and the place ({@link AbilityFunctions}), run by
 * {@link EngineServer#useAbility}. The characters of the engine build theirs here, and so do the first
 * 9 heroes since the adaptation (stage 3, lot 1): their functions either run a preset or, with the
 * preset {@value #OWN}, one of the hero's own powers ({@code "ajustes": {"poder": "heat_vision"}}),
 * which pays its own energy and cooldown as before.
 * <p>
 * The traits of a hero of before run here too, except the ones its own code already does (marked
 * {@code "ajustes": {"legado": true}} in the file: the Lantern's flight, the Flash's running...).
 */
public final class AbilityWheel {
    /** The preset of a function that uses one of the hero's own powers. */
    public static final String OWN = "poder";

    private AbilityWheel() {}

    /** The wheel of {@code hero}, from the abilities of {@code data}. */
    public static HeroPowers<EngineAbility> build(HeroDefinition hero, CharacterData data, HeroEnergy energy, ChatFormatting color, float pitchBase,
                                                  float pitchStep, WheelStyle style) {
        List<CharacterData.Ability> list = data.abilities();
        EngineAbility[] abilities = new EngineAbility[list.size()];
        for (int i = 0; i < abilities.length; i++) {
            CharacterData.Ability ability = list.get(i);
            AbilityKind kind = ability.kind().orElseThrow(() -> new IllegalStateException(data.id() + ": ability " + ability.id() + " has no classe"));
            for (CharacterData.Function function : ability.allFunctions()) {
                if (!function.preset().equals(OWN)) continue;
                String power = ownPower(ability, function);
                if (!hero.hasOwnPower(power)) {
                    throw new IllegalStateException(data.id() + ": ability " + ability.id() + " uses the power " + power + ", which " + hero.id() + " doesn't have");
                }
            }
            abilities[i] = new EngineAbility(ability, i, kind, ownPowerOf(hero, ability, ability.defaultFunction()));
        }
        return new HeroPowers<>(new PowerSet<>(abilities), energy, color, pitchBase, pitchStep, EngineServer::useAbility, style);
    }

    /** The wheel of a hero of before: its own colours and texts, the icons drawn for the new abilities. */
    public static WheelStyle style(WheelStyle own, CharacterData data) {
        return new WheelStyle(own.theme(), GreenLantern.id("engine_icons/" + data.id()), AbilityIcons.SIZE * AbilityIcons.cells(data), own.costKey(),
                own.energyKey(), own.background(), own.nameColor(), own.costColor(), own.textColor(), own.dimIcon(), own.numberColor(), own.hintColor());
    }

    /** The id of the hero's own power a function names ({@code "poder"} in its settings), empty when it names none. */
    public static String ownPower(CharacterData.Ability ability, CharacterData.Function function) {
        return ability.settings(function).text("poder", "");
    }

    /** The hero's own power a function uses, or null when it runs a preset (or a power without an icon of its own). */
    public static HeroPower ownPowerOf(HeroDefinition hero, CharacterData.Ability ability, CharacterData.Function function) {
        if (!function.preset().equals(OWN)) return null;
        String id = ownPower(ability, function);
        for (HeroPower power : hero.ownPowers()) {
            if (power.id().equals(id)) return power;
        }
        return null;
    }

    // ---- the heroes of before ----------------------------------------------------------------------

    /** The suit came off: what the presets left running stops and the traits go away. */
    public static void suitDown(ServerPlayer player) {
        PresetRuntime.stopLasting(player);
        Traits.suitDown(player);
    }

    /** Every tick of a hero of before with the wheel of the new model: its traits, with the suit on. */
    public static void tick(ServerPlayer player, HeroDefinition hero) {
        if (hero instanceof EngineHero || !hero.abilityWheel() || !hero.isSuited(player)) return;
        Traits.tick(player, hero);
    }

    /** Damage multiplier of the traits of a hero of before (invulneravel), 1 when they don't take it away. */
    public static float damageTaken(ServerPlayer player, HeroDefinition hero, DamageSource source) {
        if (hero instanceof EngineHero || !hero.abilityWheel() || !hero.isSuited(player)) return 1.0F;
        return Traits.immune(hero.data(), source) ? 0.0F : 1.0F;
    }

    /** Whether the traits of a hero of before take away the fall damage. */
    public static boolean noFall(Player player, HeroDefinition hero) {
        return !(hero instanceof EngineHero) && hero.abilityWheel() && hero.isSuited(player) && Traits.noFall(hero.data());
    }
}
