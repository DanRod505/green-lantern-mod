package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.engine.preset.PresetFx;
import com.danrod505.greenlantern.engine.preset.Summons;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One use of an ability by a preset: who uses it, the character, its item and the ability with its
 * numbers (stage 15) and settings. The numbers left at 0 in the file fall back to the preset's own
 * default, so {@code reach(4)} is the ability's {@code alcance} or 4 blocks. {@code chosen} is the
 * function of the ability that goes off: its numbers and settings are on top of the ability's.
 */
public record PresetContext(ServerPlayer player, CharacterHero hero, ItemStack item, EngineAbility ability, AbilityFunctions.Chosen chosen) {
    /** A use of the ability's default function. */
    public PresetContext(ServerPlayer player, CharacterHero hero, ItemStack item, EngineAbility ability) {
        this(player, hero, item, ability, new AbilityFunctions.Chosen(0, ability.data().defaultFunction()));
    }

    public ServerLevel level() {
        return player.level();
    }

    /** The numbers of the function that goes off (its own, else the ability's). */
    public CharacterData.Numbers numbers() {
        return ability.data().numbers(chosen.function());
    }

    /** The settings of the function that goes off (the ability's, with the function's own on top). */
    public CharacterData.Settings settings() {
        return ability.data().settings(chosen.function());
    }

    /** The preset of the function that goes off. */
    public String preset() {
        return chosen.function().preset();
    }

    public float damage() {
        return (float) numbers().damage();
    }

    public double reach(double fallback) {
        return numbers().reach() > 0 ? numbers().reach() : fallback;
    }

    public double radius(double fallback) {
        return numbers().radius() > 0 ? numbers().radius() : fallback;
    }

    /** The duration in ticks ({@code duracao} is in seconds). */
    public int durationTicks(double fallbackSeconds) {
        double seconds = numbers().duration() > 0 ? numbers().duration() : fallbackSeconds;
        return Math.max(1, (int) Math.round(seconds * 20.0));
    }

    /** The element of the ability's look and side effect ({@code "elemento"}), {@code energia} when it has none. */
    public String element() {
        return settings().text("elemento", PresetFx.ENERGY);
    }

    /** The colour of the ability's look: its own ({@code "cor"}), or else the main colour of the character. */
    public int color() {
        return settings().color("cor", hero.data().colors().main());
    }

    /** The size of the ability's particles ({@code "particula_tamanho"}, 1 is normal). */
    public float particleSize() {
        return (float) Math.max(0.2, Math.min(4.0, settings().number("particula_tamanho", 1.0)));
    }

    /** How many particles the ability makes ({@code "particula_quantidade"}, 1 is normal, 0 none). */
    public double particleAmount() {
        return Math.max(0.0, Math.min(5.0, settings().number("particula_quantidade", 1.0)));
    }

    public int lightColor() {
        return hero.data().colors().light();
    }

    /** The damage of the character's abilities: it counts as the player's. */
    public DamageSource source() {
        return player.damageSources().playerAttack(player);
    }

    /** Whether a preset may hit this creature (never players, armor stands, tamed animals or summons). */
    public boolean hittable(LivingEntity entity) {
        return Targets.hittable(player, entity) && !Summons.summoned(entity);
    }

    /** Damage that lands even right after another hit, plus the side effect of the ability's element. */
    public boolean hit(LivingEntity target, float amount) {
        boolean hurt = amount <= 0 || Hits.strike(level(), target, source(), amount);
        PresetFx.elementHit(element(), target, player);
        return hurt;
    }

    /**
     * The creature the player aims at within {@code reach}: the one under the crosshair, or else the
     * nearest within {@code halfAngle} degrees of where they look. Null when there is none.
     */
    public LivingEntity aimedTarget(double reach, double halfAngle) {
        HitResult hit = Targets.aim(level(), player, reach);
        if (hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity living && hittable(living)) return living;
        List<LivingEntity> cone = Targets.inCone(level(), player.getEyePosition(), player.getLookAngle(), reach, halfAngle, this::hittable);
        return cone.isEmpty() ? null : cone.getFirst();
    }

    /** Where the player aims within {@code reach}: a creature, a block, or null when it is just air. */
    public HitResult aim(double reach) {
        HitResult hit = Targets.aim(level(), player, reach);
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    /** The point of an aim: the middle of a creature or the face of a block. */
    public static Vec3 point(HitResult hit) {
        return hit instanceof EntityHitResult e ? Targets.middle(e.getEntity()) : hit.getLocation();
    }

    /** Where the shots of the player start: in front of the chest, a little below the eyes. */
    public Vec3 muzzle() {
        return player.getEyePosition().add(0, -0.25, 0).add(player.getLookAngle().scale(0.6));
    }
}
