package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Protective hard-light bubble around its owner: absorbs most incoming damage (see
 * {@link com.danrod505.greenlantern.ring.CommonEvents}), pushes creatures away and deflects projectiles.
 */
public class BubbleConstructEntity extends ConstructEntity {
    public static final float RADIUS = 2.2F;
    private static final byte EVENT_HIT = 60;

    /** Client-side flash timer when the bubble absorbs a hit. */
    public int hitFlash;

    public BubbleConstructEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Finds the active bubble of a player, if any. */
    public static BubbleConstructEntity find(Player player) {
        List<BubbleConstructEntity> list = player.level().getEntitiesOfClass(BubbleConstructEntity.class,
                player.getBoundingBox().inflate(RADIUS + 2), bubble -> bubble.isOwnedBy(player) && bubble.isAlive());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    public void tick() {
        Player owner = getOwner();
        if (owner != null) {
            setPos(owner.getX(), owner.getY() + owner.getBbHeight() / 2 - getBbHeight() / 2, owner.getZ());
        }
        super.tick();
        if (isRemoved()) return;

        if (level().isClientSide()) {
            if (hitFlash > 0) hitFlash--;
            if (tickCount % 3 == 0) {
                Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(RADIUS);
                Vec3 c = position().add(0, getBbHeight() / 2, 0);
                level().addParticle(ModParticles.GLOW.get(), c.x + dir.x, c.y + dir.y, c.z + dir.z, 0, 0, 0);
            }
            return;
        }

        ServerLevel level = (ServerLevel) level();
        ItemStack ring = ownerRing();
        int cost = GLConfig.BUBBLE_COST_PER_SECOND.get();
        if (tickCount % 20 == 0 && cost > 0 && !(owner != null && owner.isCreative())) {
            if (!RingEnergy.tryConsume(ring, cost)) {
                if (owner instanceof ServerPlayer sp) PowerRingItem.notifyNoEnergy(sp);
                dissipate();
                return;
            }
        }

        Vec3 center = position().add(0, getBbHeight() / 2, 0);
        AABB area = new AABB(center, center).inflate(RADIUS + 0.5);
        for (Entity entity : level.getEntities(this, area)) {
            if (entity == owner || entity instanceof ConstructEntity || (owner != null && entity.isPassengerOfSameVehicle(owner))) continue;
            Vec3 offset = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(center);
            double dist = offset.length();
            if (dist > RADIUS + 0.4) continue;
            Vec3 normal = dist < 1.0E-4 ? new Vec3(1, 0, 0) : offset.scale(1.0 / dist);

            if (entity instanceof Projectile projectile) {
                if (owner != null && projectile.getOwner() == owner) continue;
                Vec3 motion = projectile.getDeltaMovement();
                if (motion.dot(normal) < 0) {
                    // Reflect the projectile on the bubble surface.
                    Vec3 reflected = motion.subtract(normal.scale(2 * motion.dot(normal))).scale(0.8);
                    projectile.setDeltaMovement(reflected);
                    projectile.hurtMarked = true;
                    onAbsorbHit(entity.position());
                }
            } else if (entity instanceof LivingEntity living && !(living instanceof Player p && p.isSpectator())) {
                double strength = 0.35 + 0.25 * (1.0 - Math.min(1.0, dist / RADIUS));
                living.push(normal.x * strength, 0.08, normal.z * strength);
                living.hurtMarked = true;
            }
        }
    }

    /** Visual/sound feedback when the bubble blocks something. */
    public void onAbsorbHit(Vec3 where) {
        if (level() instanceof ServerLevel level) {
            level.broadcastEntityEvent(this, EVENT_HIT);
            level.playSound(null, where.x, where.y, where.z, ModSounds.BUBBLE_HIT.get(), SoundSource.PLAYERS, 0.9F, 0.9F + random.nextFloat() * 0.3F);
            level.sendParticles(ModParticles.SPARK.get(), where.x, where.y, where.z, 8, 0.1, 0.1, 0.1, 0.15);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_HIT) {
            hitFlash = 8;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
