package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.PowerSet;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A hero whose wheel is in the new model of abilities: 5 to 8 abilities from its file, each with its
 * functions by context (how the key is pressed, the aim, where the player is), the supreme one last,
 * and the traits off the wheel. Every character of the engine ({@link EngineHero}) is one; since the
 * adaptation of the 9 heroes (stage 3, lot 1) the first 9 heroes are too. They keep their own item,
 * energy, suit, flight and HUD, and a function of an ability may run one of their powers of before
 * ({@link OwnPowers}, preset {@value OwnPowers#PRESET}) with the energy and recharge it always had.
 */
public abstract class AbilityHero extends HeroDefinition {

    protected AbilityHero(String id, Predicate<ItemStack> activator, SuitSet suit) {
        super(id, activator, suit);
    }

    /** The hero's file in {@code data/greenlantern/characters}. */
    public abstract CharacterData data();

    /** The energy that pays for the abilities. */
    public abstract HeroEnergy energy();

    @Override
    public abstract HeroPowers<EngineAbility> powers();

    /** The powers of before that the functions of this hero's abilities may run, or {@link OwnPowers#NONE}. */
    public OwnPowers ownPowers() {
        return OwnPowers.NONE;
    }

    /** The index on the wheel of the ability whose default function is the power of before {@code power}, or -1. */
    public final int abilityOf(String power) {
        HeroPowers<EngineAbility> powers = powers();
        for (int i = 0; i < powers.set().count(); i++) {
            if (power.equals(OwnPowers.power(powers.set().byIndex(i).data().defaultFunction(), powers.set().byIndex(i).data()))) return i;
        }
        return -1;
    }

    /**
     * Builds the wheel of a hero of before from its file: the abilities in order, used with a tap by
     * the wheel, right click and the power key, with the hero's own colours and its icons drawn by
     * the engine ({@link AbilityIcons}). An ability whose default function is a power of before shows
     * that power's cost.
     */
    protected static HeroPowers<EngineAbility> wheel(CharacterData data, OwnPowers own, HeroEnergy energy, ChatFormatting color,
                                                     float pitchBase, float pitchStep, WheelStyle style) {
        List<CharacterData.Ability> list = data.abilities();
        EngineAbility[] abilities = new EngineAbility[list.size()];
        for (int i = 0; i < abilities.length; i++) {
            CharacterData.Ability ability = list.get(i);
            String power = OwnPowers.power(ability.defaultFunction(), ability);
            abilities[i] = new EngineAbility(ability, i, ability.kind().orElseThrow(), power == null ? null : () -> own.cost(power));
        }
        WheelStyle drawn = new WheelStyle(style.theme(), GreenLantern.id("engine_icons/" + data.id()), AbilityIcons.SIZE * AbilityIcons.cells(data),
                style.costKey(), style.energyKey(), style.background(), style.nameColor(), style.costColor(), style.textColor(), style.dimIcon(),
                style.numberColor(), style.hintColor());
        return new HeroPowers<>(new PowerSet<>(abilities), energy, color, pitchBase, pitchStep, EngineServer::useAbility, drawn);
    }

    // ---- the engine around a hero of before (an EngineHero does all this itself) -------------------

    /** Every tick, after the hero's own tick: the traits the engine gives this hero. */
    public void engineTick(ServerPlayer player) {
        if (isSuited(player)) Traits.tick(player, this);
    }

    /** The suit came off: the lasting abilities and the traits end. */
    public void engineSuitDown(ServerPlayer player) {
        com.danrod505.greenlantern.engine.preset.PresetRuntime.stopLasting(player);
        Traits.suitDown(player);
    }

    @Override
    protected final void afterSuitRemoved(ServerPlayer player) {
        engineSuitDown(player);
    }

    /** Damage multiplier of the traits (invulneravel) while suited. */
    public float traitDamage(ServerPlayer player, DamageSource source) {
        return isSuited(player) && Traits.immune(data(), source) ? 0.0F : 1.0F;
    }

    /** The hero's wheel of before, whose icon sheet still has the hero's emblem in its last cell (null for a character of the engine). */
    public WheelStyle ownWheel() {
        return null;
    }

    /** Whether a power of before of this hero is running now, for the HUD (both sides). */
    public boolean ownPowerRunning(Player player, String power) {
        return false;
    }
}
