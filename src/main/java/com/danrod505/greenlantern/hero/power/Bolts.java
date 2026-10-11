package com.danrod505.greenlantern.hero.power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shots that fly for a few ticks without being entities (heat bolts, rockets, energy blasts): each
 * moves every tick, stops at the first block or {@link Targets#hittable} creature, and may home in on
 * a target. Keep one per player in the hero's server state, {@link #fire} when the power is used and
 * {@link #tick} every server tick.
 */
public final class Bolts {
    /** What a bolt reached: the creature (null for a block or the end of its range) and where. */
    public record Impact(LivingEntity entity, BlockHitResult block, Vec3 at) {
    }

    private static final class Bolt {
        Vec3 pos;
        Vec3 velocity;
        final int targetId;
        int age;

        Bolt(Vec3 pos, Vec3 velocity, int targetId) {
            this.pos = pos;
            this.velocity = velocity;
            this.targetId = targetId;
        }
    }

    private final List<Bolt> bolts = new ArrayList<>();
    private final int life;
    private final double homing;

    /**
     * @param life   ticks a bolt flies before it ends on its own
     * @param homing how hard a bolt with a target turns towards it each tick (0 flies straight, 0.25 is a rocket)
     */
    public Bolts(int life, double homing) {
        this.life = life;
        this.homing = homing;
    }

    /** A bolt from {@code from} with {@code velocity} blocks per tick, homing on {@code target} when it is not null. */
    public void fire(Vec3 from, Vec3 velocity, Entity target) {
        bolts.add(new Bolt(from, velocity, target == null ? -1 : target.getId()));
    }

    public boolean isEmpty() {
        return bolts.isEmpty();
    }

    public void clear() {
        bolts.clear();
    }

    /**
     * Moves every bolt one tick. {@code trail} is called with each bolt's segment (from, to) for its
     * particles; {@code onImpact} once when a bolt hits something or runs out of range.
     */
    public void tick(ServerPlayer player, ServerLevel level, BiConsumer<Vec3, Vec3> trail, Consumer<Impact> onImpact) {
        Iterator<Bolt> it = bolts.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            bolt.age++;
            Entity target = bolt.targetId < 0 ? null : level.getEntity(bolt.targetId);
            if (homing > 0 && target != null && target.isAlive()) {
                Vec3 want = Targets.middle(target).subtract(bolt.pos).normalize().scale(bolt.velocity.length());
                bolt.velocity = bolt.velocity.lerp(want, homing);
            }
            Vec3 next = bolt.pos.add(bolt.velocity);
            BlockHitResult block = level.clip(new ClipContext(bolt.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, bolt.pos, end, new AABB(bolt.pos, end).inflate(0.6),
                    e -> e instanceof LivingEntity living && Targets.hittable(player, living), 0.3F);
            trail.accept(bolt.pos, end);
            if (entity != null && entity.getEntity() instanceof LivingEntity hit) {
                onImpact.accept(new Impact(hit, null, entity.getLocation()));
                it.remove();
            } else if (block.getType() != HitResult.Type.MISS) {
                onImpact.accept(new Impact(null, block, block.getLocation()));
                it.remove();
            } else if (bolt.age >= life) {
                onImpact.accept(new Impact(null, null, next));
                it.remove();
            } else {
                bolt.pos = next;
            }
        }
    }
}
