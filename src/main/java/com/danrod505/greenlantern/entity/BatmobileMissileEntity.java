package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModEntities;
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
 * Missile of the Batmobile's launchers. It leaves the tube, lights its motor and homes in on its
 * target (or the point the driver aimed at), exploding on impact.
 */
public class BatmobileMissileEntity extends Projectile {
    private static final int IGNITION = 3;
    private static final int MAX_LIFE = 120;
    private static final double SPEED = 1.8;

    private @Nullable Entity target;
    private @Nullable Vec3 aimPoint;

    public BatmobileMissileEntity(EntityType<? extends BatmobileMissileEntity> type, Level level) {
        super(type, level);
    }

    public static BatmobileMissileEntity create(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity, @Nullable Entity target, @Nullable Vec3 aimPoint) {
        BatmobileMissileEntity missile = new BatmobileMissileEntity(ModEntities.BATMOBILE_MISSILE.get(), level);
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
        if (ignited() && !level().isClientSide()) {
            Vec3 goal = target != null && target.isAlive() ? target.getBoundingBox().getCenter() : aimPoint;
            if (goal == null || (target == null && goal.distanceToSqr(position()) < 1.0)) {
                goal = position().add(motion.normalize().scale(10.0));
                aimPoint = null;
            }
            Vec3 desired = goal.subtract(position()).normalize().scale(SPEED);
            double steer = tickCount < IGNITION + 8 ? 0.15 : 0.28;
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
            Vec3 back = motion.lengthSqr() > 1.0E-4 ? motion.normalize().scale(-0.35) : Vec3.ZERO;
            level().addParticle(ParticleTypes.FLAME, getX() + back.x, getY() + back.y, getZ() + back.z, back.x * 0.1, back.y * 0.1, back.z * 0.1);
            level().addParticle(ParticleTypes.SMOKE, getX() + back.x * 2, getY() + back.y * 2, getZ() + back.z * 2, 0, 0.01, 0);
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + back.x * 3, getY() + back.y * 3, getZ() + back.z * 3, 0, 0.02, 0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return super.canHitEntity(entity) && entity != owner && !(entity instanceof ConstructEntity) && !(entity instanceof BatmobileMissileEntity)
                && !(owner != null && entity.isPassengerOfSameVehicle(owner));
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) explode(result.getLocation());
    }

    private void explode(Vec3 at) {
        if (level() instanceof ServerLevel level) {
            Level.ExplosionInteraction interaction = GLConfig.BATMOBILE_MISSILES_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
            level.explode(this, at.x, at.y, at.z, GLConfig.BATMOBILE_MISSILE_POWER.get().floatValue(), false, interaction);
            level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, 0.5, 0.5, 0.5, 0.05);
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
