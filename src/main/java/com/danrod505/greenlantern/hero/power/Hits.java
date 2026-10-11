package com.danrod505.greenlantern.hero.power;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** What powers do to the creatures they hit (usually the ones {@link Targets} picked). */
public final class Hits {
    private Hits() {
    }

    /** Damage that lands even if the target was hit a moment ago (powers often hit several times in a row). */
    public static boolean strike(ServerLevel level, LivingEntity target, DamageSource source, float amount) {
        target.invulnerableTime = 0;
        return target.hurtServer(level, source, amount);
    }

    /**
     * Pushes the target away from {@code from} along the ground, {@code strength} blocks per tick,
     * and up by {@code lift}.
     */
    public static void knockAway(Entity target, Vec3 from, double strength, double lift) {
        Vec3 away = target.position().subtract(from);
        away = new Vec3(away.x, 0, away.z);
        away = away.lengthSqr() < 1.0E-4 ? Targets.horizontal(target).scale(-1) : away.normalize();
        target.push(away.x * strength, lift, away.z * strength);
        target.hurtMarked = true;
    }

    /** Pushes the target along {@code direction}, {@code strength} blocks per tick, and up by {@code lift}. */
    public static void fling(Entity target, Vec3 direction, double strength, double lift) {
        Vec3 dir = direction.normalize();
        target.push(dir.x * strength, dir.y * strength + lift, dir.z * strength);
        target.hurtMarked = true;
    }

    /**
     * The mob forgets its target, stops walking and is slowed to a crawl and weakened for
     * {@code ticks}. It recovers on its own when the effects end.
     */
    public static void stun(Mob mob, Entity by, int ticks) {
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, true), by);
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, true), by);
    }
}
