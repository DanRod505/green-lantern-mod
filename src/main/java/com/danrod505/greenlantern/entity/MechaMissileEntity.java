package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hard-light missile fired by the mecha's shoulder pods. It pops out of the pod, ignites and then
 * homes in on its target (or the point the pilot aimed at), exploding on impact.
 */
public class MechaMissileEntity extends Projectile {
    private static final int IGNITION = 6;
    private static final int MAX_LIFE = 140;
    private static final double SPEED = 1.5;

    private @Nullable Entity target;
    private @Nullable Vec3 aimPoint;

    public MechaMissileEntity(EntityType<? extends MechaMissileEntity> type, Level level) {
        super(type, level);
    }

    public static MechaMissileEntity create(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity, @Nullable Entity target, @Nullable Vec3 aimPoint) {
        MechaMissileEntity missile = new MechaMissileEntity(ModEntities.MECHA_MISSILE.get(), level);
        missile.setOwner(owner);
        missile.setPos(pos);
        missile.setDeltaMovement(velocity);
        missile.target = target;
        missile.aimPoint = aimPoint;
        missile.updateRotation();
        missile.yRotO = missile.getYRot();
        missile.xRotO = missile.getXRot();
        return missile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    public boolean ignited() {
        return tickCount > IGNITION;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFE) {
            explode(position());
            return;
        }
        Vec3 motion = getDeltaMovement();
        if (!ignited()) {
            // Ejected from the pod: coasts and slows down before the motor lights.
            motion = motion.scale(0.9);
        } else if (!level().isClientSide()) {
            Vec3 goal = target != null && target.isAlive() ? target.getBoundingBox().getCenter() : aimPoint;
            if (goal == null || (target == null && goal.distanceToSqr(position()) < 1.0)) {
                goal = position().add(motion.normalize().scale(10.0));
                aimPoint = null;
            }
            Vec3 desired = goal.subtract(position()).normalize().scale(SPEED);
            double steer = tickCount < IGNITION + 12 ? 0.18 : 0.3;
            motion = motion.add(desired.subtract(motion).scale(steer));
        }
        setDeltaMovement(motion);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
            if (isRemoved()) return;
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();

        if (level().isClientSide() && ignited()) {
            Vec3 back = motion.lengthSqr() > 1.0E-4 ? motion.normalize().scale(-0.4) : Vec3.ZERO;
            level().addParticle(ModParticles.GLOW.get(), getX() + back.x, getY() + back.y, getZ() + back.z, 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, getX() + back.x * 2, getY() + back.y * 2, getZ() + back.z * 2, 0, 0.01, 0);
            if (random.nextBoolean()) {
                level().addParticle(ModParticles.SPARK.get(), getX() + back.x, getY() + back.y, getZ() + back.z,
                        back.x * 0.3 + (random.nextDouble() - 0.5) * 0.1, back.y * 0.3, back.z * 0.3 + (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return super.canHitEntity(entity) && entity != owner && !(entity instanceof ConstructEntity) && !(entity instanceof MechaMissileEntity)
                && !(owner != null && entity.isPassengerOfSameVehicle(owner));
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) explode(result.getLocation());
    }

    private void explode(Vec3 at) {
        if (level() instanceof ServerLevel level) {
            Level.ExplosionInteraction interaction = GLConfig.MECHA_MISSILES_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
            level.explode(this, at.x, at.y, at.z, GLConfig.MECHA_MISSILE_POWER.get().floatValue(), false, interaction);
            level.sendParticles(ModParticles.SPARK.get(), at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.3);
            level.sendParticles(ModParticles.GLOW.get(), at.x, at.y, at.z, 8, 0.4, 0.4, 0.4, 0.05);
        }
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
