package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.hero.power.Fx;
import com.danrod505.greenlantern.hero.power.Targets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * What the defense and evasion presets put on a player while they last: guards that cut the damage
 * (all around or only from the front), turn projectiles back or make the player untouchable, and
 * the hiding that makes creatures lose the player. Each preset adds its own and takes it away when
 * it ends; nothing here touches a player without one.
 */
public final class Guards {
    /**
     * A guard: {@code reduction} of the damage it takes away (1 is untouchable), {@code halfAngle} in
     * degrees from where the player looks (180 is all around), whether it turns projectiles back and
     * the chance of it. A negative reduction makes the hits hurt more (the fury).
     */
    public record Guard(double reduction, double halfAngle, boolean reflects, double chance) {
        boolean covers(ServerPlayer player, Vec3 from) {
            if (halfAngle >= 180.0 || from == null) return true;
            Vec3 to = from.subtract(player.getEyePosition());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (flat.lengthSqr() < 1.0E-4) return true;
            double cos = flat.normalize().dot(Targets.horizontal(player));
            return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos)))) <= halfAngle;
        }
    }

    private static final Map<UUID, List<Guard>> GUARDS = new ConcurrentHashMap<>();
    /** Players that creatures can't find, and whether attacking ends it (by the preset's name). */
    private static final Map<UUID, Map<String, Runnable>> HIDDEN = new ConcurrentHashMap<>();

    private Guards() {}

    static void register() {
        LivingAttackEvent.BUS.addListener(Guards::onAttack);
        LivingHurtEvent.BUS.addListener(Guards::onHurt);
        LivingChangeTargetEvent.BUS.addListener(Guards::onTarget);
    }

    public static void add(ServerPlayer player, Guard guard) {
        GUARDS.computeIfAbsent(player.getUUID(), id -> new CopyOnWriteArrayList<>()).add(guard);
    }

    public static void remove(ServerPlayer player, Guard guard) {
        List<Guard> list = GUARDS.get(player.getUUID());
        if (list == null) return;
        list.remove(guard);
        if (list.isEmpty()) GUARDS.remove(player.getUUID());
    }

    /** The guards on the player now (for the tests). */
    public static List<Guard> of(ServerPlayer player) {
        return List.copyOf(GUARDS.getOrDefault(player.getUUID(), List.of()));
    }

    /**
     * Hides the player from creatures under {@code name}: those that chase them stop, and new ones
     * don't pick them. {@code onAttack} runs when the player hurts something (null: attacking
     * doesn't break it).
     */
    public static void hide(ServerPlayer player, String name, Runnable onAttack) {
        HIDDEN.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>()).put(name, onAttack != null ? onAttack : () -> {});
        loseTrack(player, 24.0);
    }

    public static void unhide(ServerPlayer player, String name) {
        Map<String, Runnable> map = HIDDEN.get(player.getUUID());
        if (map == null) return;
        map.remove(name);
        if (map.isEmpty()) HIDDEN.remove(player.getUUID());
    }

    public static boolean hidden(Entity entity) {
        return entity instanceof ServerPlayer && HIDDEN.containsKey(entity.getUUID());
    }

    /** Creatures within {@code radius} that chase the player forget them. */
    public static void loseTrack(ServerPlayer player, double radius) {
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), m -> m.getTarget() == player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static Vec3 from(DamageSource source) {
        if (source.getDirectEntity() != null) return source.getDirectEntity().position();
        return source.getSourcePosition();
    }

    /** An untouchable guard (a dodge) takes no hit at all. */
    private static boolean onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        List<Guard> list = GUARDS.get(player.getUUID());
        if (list == null || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        Vec3 from = from(event.getSource());
        for (Guard guard : list) {
            if (guard.reduction() >= 1.0 && guard.covers(player, from)) return true;
        }
        return false;
    }

    private static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            Map<String, Runnable> hidden = HIDDEN.get(attacker.getUUID());
            if (hidden != null) List.copyOf(hidden.values()).forEach(Runnable::run);
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<Guard> list = GUARDS.get(player.getUUID());
        if (list == null || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        Vec3 from = from(event.getSource());
        float amount = event.getAmount();
        boolean blocked = false;
        for (Guard guard : list) {
            if (guard.reduction() != 0 && guard.covers(player, from)) {
                amount *= (float) Math.max(0.0, 1.0 - guard.reduction());
                blocked |= guard.reduction() > 0 && guard.halfAngle() < 180.0;
            }
        }
        event.setAmount(amount);
        if (blocked) {
            ServerLevel level = player.level();
            Vec3 at = player.getEyePosition().add(Targets.horizontal(player).scale(0.7)).add(0, -0.4, 0);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.2, 0.3, 0.2, 0.3);
            Fx.play(level, player, SoundEvents.SHIELD_BLOCK.value(), 1.0F, 1.0F);
        }
    }

    /**
     * A guard that turns projectiles back, checked every tick while it lasts: the projectiles about
     * to reach the player from where it covers go back to whoever shot them, now the player's. Each
     * projectile gets one roll of the chance ({@code seen} keeps those already rolled).
     */
    public static void deflect(ServerPlayer player, Guard guard, Set<Integer> seen) {
        if (!guard.reflects()) return;
        Vec3 middle = Targets.middle(player);
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(6.0),
                p -> p.getOwner() != player && !seen.contains(p.getId()))) {
            Vec3 motion = projectile.getDeltaMovement();
            // Caught one or two ticks before it would land, however fast it flies.
            if (motion.lengthSqr() < 0.04 || motion.dot(middle.subtract(projectile.position())) <= 0) continue;
            if (projectile.position().distanceTo(middle) > 1.2 + 1.8 * motion.length()) continue;
            if (!guard.covers(player, projectile.position())) continue;
            seen.add(projectile.getId());
            if (guard.chance() < 1.0 && player.getRandom().nextDouble() >= guard.chance()) continue;
            reflect(player, projectile);
        }
    }

    private static void reflect(ServerPlayer player, Projectile projectile) {
        Entity shooter = projectile.getOwner();
        double speed = Math.max(0.6, projectile.getDeltaMovement().length());
        Vec3 back = shooter != null && shooter != player && shooter.level() == player.level()
                ? Targets.middle(shooter).subtract(projectile.position()).normalize()
                : projectile.getDeltaMovement().scale(-1).normalize();
        projectile.setDeltaMovement(back.scale(speed));
        projectile.setOwner(player);
        projectile.needsSync = true;
        projectile.hurtMarked = true;
        ServerLevel level = player.level();
        Vec3 at = projectile.position();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.2);
        Fx.play(level, player, SoundEvents.ANVIL_LAND, 0.5F, 1.8F);
    }

    /** Creatures can't pick a hidden player; one already beside them still can. */
    private static boolean onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        return target != null && hidden(target) && event.getEntity().distanceToSqr(target) > 2.0 * 2.0;
    }
}
