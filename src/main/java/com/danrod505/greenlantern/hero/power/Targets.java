package com.danrod505.greenlantern.hero.power;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Who a hero's power hits and where the hero is aiming. Every hero uses the same rules, so a power
 * never hurts players, armor stands or anyone's tamed animal (a companion like Krypto included).
 */
public final class Targets {
    private Targets() {
    }

    /** Whatever a power may hit: any living thing except the hero, players, armor stands and tamed animals. */
    public static boolean hittable(Player hero, LivingEntity entity) {
        if (entity == hero || !entity.isAlive() || entity.isSpectator() || entity instanceof Player || entity instanceof ArmorStand) return false;
        return !(entity instanceof OwnableEntity owned && owned.getOwnerReference() != null);
    }

    /** Hittable and hostile: monsters, and any mob that is after the hero. */
    public static boolean enemy(Player hero, LivingEntity entity) {
        return hittable(hero, entity) && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == hero);
    }

    /** Living things within {@code radius} of {@code center} that pass {@code filter}, nearest first. */
    public static List<LivingEntity> inRadius(ServerLevel level, Vec3 center, double radius, Predicate<LivingEntity> filter) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                        e -> filter.test(e) && middle(e).distanceToSqr(center) <= radius * radius).stream()
                .sorted(Comparator.comparingDouble(e -> middle(e).distanceToSqr(center))).toList();
    }

    /**
     * Living things in a cone: within {@code range} of {@code origin} and at most {@code halfAngle}
     * degrees away from {@code direction}, nearest first.
     */
    public static List<LivingEntity> inCone(ServerLevel level, Vec3 origin, Vec3 direction, double range, double halfAngle,
                                            Predicate<LivingEntity> filter) {
        Vec3 dir = direction.normalize();
        double cos = Math.cos(Math.toRadians(halfAngle));
        return inRadius(level, origin, range, e -> {
            if (!filter.test(e)) return false;
            Vec3 to = middle(e).subtract(origin);
            return to.lengthSqr() < 1.0E-4 || to.normalize().dot(dir) >= cos;
        });
    }

    /** The first creature or block in the hero's line of sight, up to {@code range} blocks. */
    public static HitResult aim(ServerLevel level, Player hero, double range) {
        Vec3 eye = hero.getEyePosition();
        Vec3 end = eye.add(hero.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, hero));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, hero, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e.isAlive() && e != hero && !e.isSpectator() && e.isPickable() && !hero.isPassengerOfSameVehicle(e), 0.3F);
        return entity != null ? entity : block;
    }

    /** Where the entity looks, flattened to the ground (straight ahead when it looks up or down). */
    public static Vec3 horizontal(Entity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** The middle of an entity's body. */
    public static Vec3 middle(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }
}
