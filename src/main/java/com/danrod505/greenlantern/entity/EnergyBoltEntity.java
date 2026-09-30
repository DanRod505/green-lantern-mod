package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hard-light projectile. The "heavy" variant is the Energy Blast (big, slow, splash damage); the
 * light variant is fired by the minigun construct.
 */
public class EnergyBoltEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> DATA_HEAVY = SynchedEntityData.defineId(EnergyBoltEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_LIFE = 80;

    private float damage = 4.0F;

    public EnergyBoltEntity(EntityType<? extends EnergyBoltEntity> type, Level level) {
        super(type, level);
    }

    public static EnergyBoltEntity create(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity, float damage, boolean heavy) {
        EnergyBoltEntity bolt = new EnergyBoltEntity(ModEntities.ENERGY_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.setPos(pos);
        bolt.setDeltaMovement(velocity);
        bolt.damage = damage;
        bolt.entityData.set(DATA_HEAVY, heavy);
        // Face the travel direction immediately so the first rendered frame is correct.
        bolt.updateRotation();
        bolt.yRotO = bolt.getYRot();
        bolt.xRotO = bolt.getXRot();
        return bolt;
    }

    public boolean isHeavy() {
        return entityData.get(DATA_HEAVY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEAVY, false);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFE) {
            discard();
            return;
        }
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
            if (isRemoved()) return;
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();

        if (level().isClientSide()) {
            boolean heavy = isHeavy();
            int count = heavy ? 3 : 1;
            for (int i = 0; i < count; i++) {
                double t = random.nextDouble();
                level().addParticle(heavy ? ModParticles.GLOW.get() : ModParticles.SPARK.get(),
                        getX() - motion.x * t, getY() + getBbHeight() / 2 - motion.y * t, getZ() - motion.z * t,
                        (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02);
            }
        }
        if (isInWater() && !isHeavy()) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof ConstructEntity) && !(target instanceof EnergyBoltEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel serverLevel) {
            Entity target = result.getEntity();
            DamageSource source = ModDamageTypes.hardLight(level(), this, getOwner());
            if (target.hurtServer(serverLevel, source, damage) && target instanceof LivingEntity living) {
                Vec3 push = getDeltaMovement().normalize().scale(isHeavy() ? 0.9 : 0.15);
                living.push(push.x, isHeavy() ? 0.25 : 0.02, push.z);
                living.hurtMarked = true;
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 p = result.getLocation();
            if (isHeavy()) {
                splash(serverLevel, p, result instanceof EntityHitResult ehr ? ehr.getEntity() : null);
                serverLevel.sendParticles(ModParticles.SHOCKWAVE.get(), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ModParticles.SPARK.get(), p.x, p.y, p.z, 24, 0.2, 0.2, 0.2, 0.25);
                serverLevel.sendParticles(ModParticles.GLOW.get(), p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.03);
                serverLevel.playSound(null, p.x, p.y, p.z, ModSounds.BLAST_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
            } else {
                serverLevel.sendParticles(ModParticles.SPARK.get(), p.x, p.y, p.z, 4, 0.05, 0.05, 0.05, 0.12);
            }
            discard();
        }
    }

    /** The heavy blast also damages entities close to the impact point. */
    private void splash(ServerLevel level, Vec3 center, Entity direct) {
        Entity owner = getOwner();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(2.0))) {
            if (living == direct || living == owner || !living.isAlive()) continue;
            double dist = living.position().distanceTo(center);
            if (dist > 2.5) continue;
            living.hurtServer(level, ModDamageTypes.hardLight(level, this, owner), (float) (damage * 0.5 * (1.0 - dist / 2.5)));
            Vec3 push = living.position().subtract(center).normalize().scale(0.5);
            living.push(push.x, 0.2, push.z);
            living.hurtMarked = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", damage);
        output.putBoolean("Heavy", isHeavy());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        damage = input.getFloatOr("Damage", 4.0F);
        entityData.set(DATA_HEAVY, input.getBooleanOr("Heavy", false));
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
