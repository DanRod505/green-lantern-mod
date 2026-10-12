package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.PresetContext;
import com.danrod505.greenlantern.engine.Presets;
import com.danrod505.greenlantern.engine.Presets.Outcome;
import com.danrod505.greenlantern.hero.power.Dash;
import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Evasion (evasão) presets: the dodge, invisibility, the smoke bomb, shrinking and the escape in
 * another shape. Hidden players are lost by the creatures that chased them (see {@link Guards#hide}).
 * Numbers: {@code alcance}, {@code raio}, {@code duracao}; the rest are settings.
 */
public final class EvasionPresets {
    private EvasionPresets() {}

    public static void register() {
        Presets.registerFight("esquiva", EvasionPresets::dodge);
        Presets.registerFight("invisivel", EvasionPresets::invisible);
        Presets.registerFight("fumaca", EvasionPresets::smoke);
        Presets.registerFight("encolher", EvasionPresets::shrink);
        Presets.registerFight("fuga_de_forma", EvasionPresets::shapeEscape);
    }

    // ---- esquiva -----------------------------------------------------------------------------------

    /**
     * Esquiva: a quick step {@code alcance} blocks (4) to the side, untouchable for {@code janela}
     * seconds (0.5). Settings: {@code lado} (auto: away from the nearest creature, toward the side with
     * room; esquerda, direita or tras).
     */
    static Outcome dodge(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double distance = c.reach(4.0);
        Vec3 direction = side(c, distance);
        Guards.Guard untouchable = new Guards.Guard(1.0, 180.0, false, 1.0);
        Guards.add(player, untouchable);
        int window = Math.max(1, (int) Math.round(c.settings().number("janela", 0.5) * 20.0));
        Dash dash = new Dash();
        dash.start(player, direction, distance, 4);
        ParticleOptions particle = PresetFx.particle(c.element(), c.lightColor());
        Vec3 start = player.position();
        level.sendParticles(ParticleTypes.CLOUD, start.x, start.y + 0.9, start.z, 8, 0.2, 0.5, 0.2, 0.02);
        Fx.play(level, player, SoundEvents.BREEZE_JUMP, 0.8F, 1.6F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            Vec3 before = player.position();
            if (dash.active()) dash.tick(player, level, (target, dir) -> {});
            Fx.line(level, before.add(0, 0.9, 0), player.position().add(0, 0.9, 0), particle, 3.0);
            return age < window || dash.active();
        }, () -> Guards.remove(player, untouchable)));
        return Outcome.DONE;
    }

    /** Which way the dodge goes. */
    private static Vec3 side(PresetContext c, double distance) {
        ServerPlayer player = c.player();
        Vec3 forward = Targets.horizontal(player);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return switch (c.settings().text("lado", "auto")) {
            case "direita" -> right;
            case "esquerda" -> right.scale(-1);
            case "tras" -> forward.scale(-1);
            default -> {
                double rightRoom = room(player, right, distance);
                double leftRoom = room(player, right.scale(-1), distance);
                List<LivingEntity> near = Targets.inRadius(c.level(), player.position(), 8.0, c::hittable);
                if (!near.isEmpty()) {
                    double toRight = near.getFirst().position().subtract(player.position()).dot(right);
                    if (toRight > 0 && leftRoom >= 1.0) yield right.scale(-1);
                    if (toRight <= 0 && rightRoom >= 1.0) yield right;
                }
                yield leftRoom > rightRoom ? right.scale(-1) : right;
            }
        };
    }

    /** How far the player fits going that way, up to {@code distance}. */
    private static double room(ServerPlayer player, Vec3 dir, double distance) {
        double free = 0;
        for (double d = 0.5; d <= distance; d += 0.5) {
            if (!player.level().noCollision(player, player.getBoundingBox().move(dir.scale(d)))) break;
            free = d;
        }
        return free;
    }

    // ---- invisivel ---------------------------------------------------------------------------------

    /**
     * Invisível: the character vanishes for {@code duracao} seconds (15): players stop seeing them and
     * creatures lose them. With {@code quebra_ao_atacar} (true) it ends on hurting something; the key
     * also ends it.
     */
    static Outcome invisible(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int ticks = c.durationTicks(15.0);
        String key = PresetRuntime.key(c);
        boolean breaks = c.settings().flag("quebra_ao_atacar", true);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks + 20, 0, false, false, true));
        Guards.hide(player, key, breaks ? () -> PresetRuntime.stop(player, key) : null);
        puff(level, player, c);
        Fx.play(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (age % 10 == 0) Guards.loseTrack(player, 16.0);
            return age < ticks;
        }, () -> {
            Guards.unhide(player, key);
            player.removeEffect(MobEffects.INVISIBILITY);
            puff(level, player, c);
            Fx.play(level, player, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.8F, 1.2F);
        }));
    }

    private static void puff(ServerLevel level, ServerPlayer player, PresetContext c) {
        Vec3 mid = player.position().add(0, 1.0, 0);
        level.sendParticles(ParticleTypes.POOF, mid.x, mid.y, mid.z, 20, 0.3, 0.6, 0.3, 0.02);
        PresetFx.burst(level, mid, c.element(), c.lightColor(), 12, 0.4);
    }

    // ---- fumaca ------------------------------------------------------------------------------------

    /**
     * Fumaça: a smoke bomb where the character stands, {@code raio} (4) wide, for {@code duracao}
     * seconds (6): creatures in it are blinded and lose their target, and whoever stands in it can't
     * be picked out from outside.
     */
    static Outcome smoke(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        double radius = c.radius(4.0);
        int ticks = c.durationTicks(6.0);
        Vec3 center = player.position().add(0, 0.5, 0);
        String name = PresetRuntime.key(c) + "_fumaca";
        boolean[] inside = {false};
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        Fx.play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.8F);
        Fx.play(level, player, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.6F);
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            double r = radius * Math.min(1.0, age / 6.0);
            level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + r * 0.3, center.z, 12, r * 0.5, r * 0.35, r * 0.5, 0.005);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + r * 0.3, center.z, 10, r * 0.5, r * 0.4, r * 0.5, 0.01);
            if (age % 10 == 1) {
                for (LivingEntity target : Targets.inRadius(level, center, radius, e -> e != player && !(e instanceof ServerPlayer))) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
                    if (target instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.getNavigation().stop();
                    }
                }
            }
            boolean in = player.position().distanceTo(center) <= radius;
            if (in && !inside[0]) Guards.hide(player, name, null);
            if (!in && inside[0]) Guards.unhide(player, name);
            inside[0] = in;
            return age < ticks;
        }, () -> Guards.unhide(player, name)));
        return Outcome.DONE;
    }

    // ---- encolher ----------------------------------------------------------------------------------

    /**
     * Encolher: the character shrinks to {@code tamanho} (0.25 of their size) for {@code duracao}
     * seconds (30), small enough for holes; the key brings them back.
     */
    static Outcome shrink(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        int ticks = c.durationTicks(30.0);
        double size = Mth.clamp(c.settings().number("tamanho", 0.25), 0.07, 1.0);
        Identifier scale = PresetBody.add(c, Attributes.SCALE, "size", size - 1.0);
        puff(level, player, c);
        Fx.play(level, player, SoundEvents.PUFFER_FISH_BLOW_OUT, 1.0F, 1.6F);
        return PresetRuntime.lasting(c, PresetRuntime.Task.ending(age -> {
            if (age % 8 == 0) PresetFx.burst(level, player.position().add(0, 0.1, 0), c.element(), c.lightColor(), 1, 0.1);
            return age < ticks;
        }, () -> {
            PresetBody.remove(player, Attributes.SCALE, scale);
            puff(level, player, c);
            Fx.play(level, player, SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 0.8F);
        }));
    }

    // ---- fuga_de_forma -----------------------------------------------------------------------------

    /**
     * Fuga de forma: the character turns into something else to get away for {@code duracao}
     * seconds (5), faster and lost by the creatures; hurting something ends it. Settings:
     * {@code forma} (sombra: unseen, in a trail of shadow; animal: small and quick; poca: a puddle
     * on the ground that takes half the damage), {@code velocidade} (extra speed, 0.6).
     */
    static Outcome shapeEscape(PresetContext c) {
        ServerPlayer player = c.player();
        ServerLevel level = c.level();
        CharacterData.Settings s = c.settings();
        int ticks = c.durationTicks(5.0);
        String shape = s.text("forma", "sombra");
        String name = PresetRuntime.key(c) + "_forma";
        boolean[] over = {false};
        Identifier speed = PresetBody.times(c, Attributes.MOVEMENT_SPEED, "shape_speed", Math.max(0.0, s.number("velocidade", 0.6)));
        Identifier scale = switch (shape) {
            case "animal" -> PresetBody.add(c, Attributes.SCALE, "shape_size", -0.5);
            case "poca" -> PresetBody.add(c, Attributes.SCALE, "shape_size", -0.8);
            default -> null;
        };
        Guards.Guard puddle = shape.equals("poca") ? new Guards.Guard(0.5, 180.0, false, 1.0) : null;
        if (puddle != null) Guards.add(player, puddle);
        if (shape.equals("sombra")) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks + 10, 0, false, false, true));
        Guards.hide(player, name, () -> over[0] = true);
        Vec3 mid = player.position().add(0, 0.8, 0);
        switch (shape) {
            case "animal" -> {
                level.sendParticles(ParticleTypes.POOF, mid.x, mid.y, mid.z, 25, 0.4, 0.5, 0.4, 0.05);
                Fx.play(level, player, SoundEvents.FOX_SCREECH, 0.8F, 1.4F);
            }
            case "poca" -> {
                level.sendParticles(ParticleTypes.SPLASH, mid.x, player.getY() + 0.1, mid.z, 40, 0.6, 0.1, 0.6, 0.2);
                Fx.play(level, player, SoundEvents.SLIME_SQUISH, 1.0F, 0.8F);
            }
            default -> {
                level.sendParticles(ParticleTypes.SQUID_INK, mid.x, mid.y, mid.z, 30, 0.4, 0.6, 0.4, 0.02);
                Fx.play(level, player, SoundEvents.WARDEN_SONIC_CHARGE, 0.5F, 2.0F);
            }
        }
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            Vec3 at = player.position();
            switch (shape) {
                case "animal" -> {
                    if (age % 3 == 0) level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.2, at.z, 1, 0.1, 0.1, 0.1, 0.0);
                }
                case "poca" -> level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y + 0.1, at.z, 3, 0.4, 0.0, 0.4, 0.0);
                default -> level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.4, at.z, 2, 0.2, 0.3, 0.2, 0.0);
            }
            return !over[0] && age < ticks;
        }, () -> {
            Guards.unhide(player, name);
            PresetBody.remove(player, Attributes.MOVEMENT_SPEED, speed);
            if (scale != null) PresetBody.remove(player, Attributes.SCALE, scale);
            if (puddle != null) Guards.remove(player, puddle);
            if (shape.equals("sombra")) player.removeEffect(MobEffects.INVISIBILITY);
            puff(level, player, c);
        }));
        return Outcome.DONE;
    }
}
