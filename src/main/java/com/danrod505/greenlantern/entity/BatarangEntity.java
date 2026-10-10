package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A batarang in flight. It spins out fast and straight, strikes the first creature in its way
 * (and stuns it for a moment) or glances off a wall, then curves back to Batman's belt.
 */
public class BatarangEntity extends Projectile {
    public static final double THROW_SPEED = 2.0;
    private static final int MAX_FLIGHT_TICKS = 18;

    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(BatarangEntity.class, EntityDataSerializers.BOOLEAN);

    private int flightTicks;
    private double returnSpeed;

    public BatarangEntity(EntityType<? extends BatarangEntity> type, Level level) {
        super(type, level);
    }

    public static BatarangEntity create(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity) {
        BatarangEntity batarang = new BatarangEntity(ModEntities.BATARANG.get(), level);
        batarang.setOwner(owner);
        batarang.setPos(pos);
        batarang.setDeltaMovement(velocity);
        batarang.updateRotation();
        batarang.yRotO = batarang.getYRot();
        batarang.xRotO = batarang.getXRot();
        return batarang;
    }

    /** The owner's batarangs in flight. */
    public static List<BatarangEntity> findAll(Player owner) {
        return owner.level().getEntitiesOfClass(BatarangEntity.class, owner.getBoundingBox().inflate(128),
                b -> b.getOwner() == owner && !b.isRemoved());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RETURNING, false);
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    private void startReturn() {
        entityData.set(DATA_RETURNING, true);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwner();
        if (!level().isClientSide()) {
            boolean ownerOk = owner instanceof ServerPlayer player && player.isAlive() && player.level() == level() && BatmanHelper.isSuited(player);
            if (!ownerOk || tickCount > 200) {
                discard();
                return;
            }
        }
        if (isReturning()) {
            tickReturn(owner);
        } else {
            tickFlight();
        }
        if (level().isClientSide() && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private void tickFlight() {
        flightTicks++;
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
                hitTargetOrDeflectSelf(hit);
                if (isRemoved() || isReturning()) return;
            }
            if (flightTicks > MAX_FLIGHT_TICKS) startReturn();
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    private void tickReturn(@Nullable Entity owner) {
        if (owner == null) return;
        Vec3 target = owner.position().add(0, owner.getBbHeight() * 0.55, 0);
        Vec3 to = target.subtract(position());
        double dist = to.length();
        returnSpeed = Math.min(2.6, Math.max(returnSpeed, 0.5) + 0.15);
        // Curves back: the old heading blends into the way home.
        Vec3 wish = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(returnSpeed, dist) / dist);
        Vec3 motion = getDeltaMovement().lerp(wish, dist < 4.0 ? 1.0 : 0.35);
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        if (!level().isClientSide() && dist < 1.3) {
            level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.BATARANG_HIT.get(), SoundSource.PLAYERS, 0.5F, 1.6F);
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !(target instanceof ConstructEntity) && !(target instanceof Projectile)
                && !(target instanceof BatDefenderEntity) && !(target instanceof Player p && p.isSpectator());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)) return;
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = level.damageSources().thrown(this, owner == null ? this : owner);
        if (target.hurtServer(level, source, GLConfig.BATARANG_DAMAGE.get().floatValue()) && target instanceof LivingEntity living) {
            // Stunned for a moment.
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3), owner);
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize();
            living.push(push.x * 0.4, 0.15, push.z * 0.4);
            living.hurtMarked = true;
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.BATARANG_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
        setDeltaMovement(getDeltaMovement().scale(-0.3));
        startReturn();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 at = result.getLocation();
        setPos(at.subtract(getDeltaMovement().normalize().scale(0.2)));
        // Glances off the wall.
        Vec3 n = result.getDirection().getUnitVec3();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.subtract(n.scale(2 * v.dot(n))).scale(0.4));
        level.playSound(null, at.x, at.y, at.z, ModSounds.BATARANG_HIT.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.2);
        startReturn();
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
