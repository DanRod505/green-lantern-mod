package com.danrod505.greenlantern.hero.power;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A charge in a straight line over a few ticks that stops at walls and rams each creature in the
 * way once (Supergirl's Meteor Dash is one). Keep one per player in the hero's server state:
 * {@link #start} when the power is used, {@link #tick} every server tick while {@link #active}.
 */
public final class Dash {
    private Vec3 step = Vec3.ZERO;
    private int ticks;
    private final Set<Integer> hit = new HashSet<>();

    /**
     * Starts a dash of {@code distance} blocks over {@code ticks} ticks along {@code direction}. On the
     * ground a dash that points down goes level instead, so the hero never digs into the floor.
     */
    public void start(ServerPlayer player, Vec3 direction, double distance, int ticks) {
        Vec3 dir = player.onGround() && direction.y < 0 ? Targets.horizontal(player) : direction.normalize();
        this.ticks = Math.max(1, ticks);
        this.step = dir.scale(distance / this.ticks);
        hit.clear();
    }

    public boolean active() {
        return ticks > 0;
    }

    public Vec3 direction() {
        return step.lengthSqr() < 1.0E-8 ? Vec3.ZERO : step.normalize();
    }

    public void stop() {
        ticks = 0;
    }

    /**
     * Moves the player one tick's worth (as far as they fit) and calls {@code onHit} once for each
     * creature in the swept path that passes {@link Targets#hittable}. Returns whether the dash goes on:
     * false once it hits a wall or runs out of ticks.
     */
    public boolean tick(ServerPlayer player, ServerLevel level, BiConsumer<LivingEntity, Vec3> onHit) {
        if (ticks <= 0) return false;
        ticks--;
        Vec3 from = player.position();
        Vec3 to = from;
        int steps = Math.max(1, (int) Math.ceil(step.length() / 0.4));
        Vec3 small = step.scale(1.0 / steps);
        boolean blocked = false;
        for (int i = 0; i < steps; i++) {
            Vec3 next = to.add(small);
            if (!level.noCollision(player, player.getBoundingBox().move(next.subtract(from)))) {
                blocked = true;
                break;
            }
            to = next;
        }
        if (to.distanceToSqr(from) > 1.0E-4) {
            player.teleportTo(to.x, to.y, to.z);
            player.resetFallDistance();
        }
        AABB swept = player.getBoundingBox().expandTowards(from.subtract(to)).inflate(0.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, swept, e -> Targets.hittable(player, e) && !hit.contains(e.getId()))) {
            hit.add(target.getId());
            onHit.accept(target, direction());
        }
        if (blocked) ticks = 0;
        return ticks > 0;
    }
}
