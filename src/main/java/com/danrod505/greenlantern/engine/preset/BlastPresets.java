package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Hits;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Blast (explosão) presets, an area: wave around, explosion at a point, cone, pulse and rain.
 * Numbers: {@code dano}, {@code alcance}, {@code raio}, {@code duracao}; the rest are settings,
 * {@code elemento} among them.
 */
public final class BlastPresets {
    /** An explosion that only shows and breaks: the preset deals the damage and the push itself. */
    private static final ExplosionDamageCalculator HARMLESS = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.0F;
        }
    };

    private BlastPresets() {}

    public static void register() {
        Presets.registerFight("onda_em_volta", BlastPresets::wave);
        Presets.registerFight("explosao_no_ponto", BlastPresets::pointBlast);
        Presets.registerFight("cone", BlastPresets::cone);
        Presets.registerFight("pulso", BlastPresets::pulse);
        Presets.registerFight("chuva", BlastPresets::rain);
    }

    /**
     * Onda em volta: a ring that grows from the character and pushes and hurts everyone around.
     * {@code raio} is its size (5). Settings: {@code empurrao} (1.4).
     */
    static Outcome wave(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(5.0);
        double push = c.settings().number("empurrao", 1.4);
        Vec3 center = player.position().add(0, 0.9, 0);
        for (LivingEntity target : Targets.inRadius(level, center, radius + 0.5, c::hittable)) {
            double near = 1.0 - 0.5 * Targets.middle(target).distanceTo(center) / (radius + 0.5);
            c.hit(target, c.damage());
            Hits.knockAway(target, center, push * near, 0.35);
        }
        Fx.play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.4F);
        Fx.play(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 0.8F);
        PresetFx.shake(player, 0.3F, 8);
        ParticleOptions particle = PresetFx.particle(c);
        PresetRuntime.start(player, age -> {
            double r = radius * age / 6.0;
            Fx.ring(level, center.add(0, -0.5, 0), 0.1, ParticleTypes.CLOUD, 4);
            for (int i = 0; i < 36; i++) {
                double a = Mth.TWO_PI * i / 36;
                level.sendParticles(particle, center.x + Math.cos(a) * r, center.y - 0.4, center.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
            }
            return age < 6;
        });
        return Outcome.DONE;
    }

    /**
     * Explosão no ponto: an explosion where the player aims. {@code alcance} (32), {@code raio} (3).
     * Settings: {@code quebra_blocos} (false), {@code fogo} (sets fire, false). The damage falls off
     * to half at the edge.
     */
    static Outcome pointBlast(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        double reach = c.reach(32.0);
        double radius = c.radius(3.0);
        HitResult aim = c.aim(reach);
        Vec3 at = aim != null ? PresetContext.point(aim) : player.getEyePosition().add(player.getLookAngle().scale(reach));
        boolean breaks = s.flag("quebra_blocos", false);
        level.explode(player, null, HARMLESS, at, (float) radius, s.flag("fogo", false),
                breaks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        for (LivingEntity target : Targets.inRadius(level, at, radius + 0.5, c::hittable)) {
            double near = 1.0 - 0.5 * Targets.middle(target).distanceTo(at) / (radius + 0.5);
            c.hit(target, (float) (c.damage() * near));
            Hits.knockAway(target, at, 0.8 * near, 0.4);
        }
        PresetFx.burst(c, at, 30, radius * 0.4);
        if (player.position().distanceTo(at) < radius * 3) PresetFx.shake(player, 0.4F, 10);
        return Outcome.DONE;
    }

    /**
     * Cone: a breath or a jet in front (ice, fire, gas, sound...) that lasts {@code duracao} seconds
     * (1) and does {@code dano} in all to whoever stays in it, plus the element's effect.
     * {@code alcance} (6). Settings: {@code angulo} (the whole opening in degrees, 50).
     */
    static Outcome cone(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double reach = c.reach(6.0);
        double half = Math.max(5.0, c.settings().number("angulo", 50.0) / 2.0);
        int ticks = c.durationTicks(1.0);
        int hits = Math.max(1, ticks / 5);
        float perHit = c.damage() / hits;
        ParticleOptions particle = PresetFx.particle(c);
        Fx.play(level, player, switch (c.element()) {
            case "fogo" -> SoundEvents.BLAZE_SHOOT;
            case "gelo" -> SoundEvents.POWDER_SNOW_STEP;
            case "som" -> SoundEvents.WARDEN_SONIC_CHARGE;
            default -> SoundEvents.BREEZE_SHOOT;
        }, 1.0F, 1.0F);
        PresetRuntime.start(player, age -> {
            Vec3 from = c.muzzle();
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < 10; i++) {
                double yaw = (level.random.nextDouble() * 2 - 1) * Math.toRadians(half);
                double pitch = (level.random.nextDouble() * 2 - 1) * Math.toRadians(half) * 0.6;
                Vec3 dir = look.yRot((float) yaw).xRot((float) pitch);
                level.sendParticles(particle, from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, reach * 0.06);
            }
            if (age % 5 == 0) {
                for (LivingEntity target : Targets.inCone(level, player.getEyePosition(), look, reach, half, c::hittable)) {
                    c.hit(target, perHit);
                    if (c.element().equals("som") || c.element().equals("agua")) Hits.knockAway(target, player.position(), 0.5, 0.1);
                }
            }
            if (age % 8 == 0) Fx.play(level, player, SoundEvents.BREEZE_IDLE_GROUND, 0.4F, 1.4F);
            return age < ticks;
        });
        return Outcome.DONE;
    }

    /**
     * Pulso: a wave that stuns: creatures stop, forget their target and are slowed for
     * {@code duracao} seconds (3), machines (golems) included. {@code raio} (6). The element is
     * {@code raio} (an EMP) unless the ability names another ({@code som} for a scream).
     */
    static Outcome pulse(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(6.0);
        int stun = c.durationTicks(3.0);
        String element = c.settings().text("elemento", "raio");
        Vec3 center = player.position().add(0, 1.0, 0);
        for (LivingEntity target : Targets.inRadius(level, center, radius + 0.5, c::hittable)) {
            if (c.damage() > 0) Hits.strike(level, target, c.source(), c.damage());
            if (target instanceof Mob mob) Hits.stun(mob, player, stun);
            level.sendParticles(PresetFx.particle(element, c.color()), target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                    6, 0.2, 0.2, 0.2, 0.05);
        }
        Fx.play(level, player, element.equals("som") ? SoundEvents.WARDEN_SONIC_BOOM : SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.4F);
        Fx.play(level, player, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.7F, 1.6F);
        ParticleOptions particle = PresetFx.particle(element, c.color());
        PresetRuntime.start(player, age -> {
            double r = radius * age / 5.0;
            for (int i = 0; i < 40; i++) {
                double a = Mth.TWO_PI * i / 40;
                level.sendParticles(particle, center.x + Math.cos(a) * r, center.y, center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            return age < 5;
        });
        return Outcome.DONE;
    }

    /**
     * Chuva: many blows fall on an area for {@code duracao} seconds (3): arrows, meteors, hail,
     * lightning, by the element. {@code raio} is the area (4) around the aimed point ({@code alcance},
     * 24); {@code dano} is each blow's. Settings: {@code golpes_por_segundo} (6).
     */
    static Outcome rain(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        HitResult aim = c.aim(c.reach(24.0));
        Vec3 center = aim != null ? (aim.getType() == HitResult.Type.ENTITY ? ((net.minecraft.world.phys.EntityHitResult) aim).getEntity().position()
                : aim.getLocation()) : player.position().add(Targets.horizontal(player).scale(6));
        double radius = c.radius(4.0);
        int ticks = c.durationTicks(3.0);
        int every = Math.max(1, (int) Math.round(20.0 / Math.max(1.0, c.settings().number("golpes_por_segundo", 6.0))));
        ParticleOptions particle = PresetFx.particle(c);
        ParticleOptions streak = c.element().equals(PresetFx.ENERGY) ? ParticleTypes.END_ROD : particle;
        Fx.play(level, player, SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.0F, 0.8F);
        PresetRuntime.start(player, age -> {
            if (age % 4 == 0) Fx.ring(level, center.add(0, 0.1, 0), radius * 0.15, particle, 16);
            if (age % every == 0) {
                double a = level.random.nextDouble() * Mth.TWO_PI;
                double r = Math.sqrt(level.random.nextDouble()) * radius;
                Vec3 ground = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Vec3 at = new Vec3(ground.x, Fx.groundBelow(level, ground.add(0, 3, 0)), ground.z);
                Fx.line(level, at.add(1.5, 10, 0), at, streak, 1.5);
                PresetFx.burst(c, at.add(0, 0.2, 0), 6, 0.25);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.2, at.z, 4, 0.2, 0.1, 0.2, 0.2);
                level.playSound(null, at.x, at.y, at.z, PresetFx.impactSound(c.element()), SoundSource.PLAYERS, 0.5F, 1.4F);
                Set<Integer> hit = new HashSet<>();
                for (LivingEntity target : Targets.inRadius(level, at.add(0, 0.8, 0), 1.6, c::hittable)) {
                    if (hit.add(target.getId())) c.hit(target, c.damage());
                }
            }
            return age < ticks;
        });
        return Outcome.DONE;
    }
}
