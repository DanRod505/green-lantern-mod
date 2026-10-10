package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanServer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Wonder Woman's shield, thrown: it spins flat through the air, ricochets from enemy to enemy and
 * then flies back to her arm, wherever she is.
 */
public class AmazonShieldEntity extends Projectile {
    public static final double THROW_SPEED = 2.2;
    private static final int MAX_FLIGHT_TICKS = 30;
    private static final double BOUNCE_RANGE = 12.0;

    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(AmazonShieldEntity.class, EntityDataSerializers.BOOLEAN);

    private int flightTicks;
    private int bounces;
    private double returnSpeed;
    private final Set<Integer> hit = new HashSet<>();
    private @Nullable LivingEntity homing;

    /** Client: spin angle of the shield, degrees. */
    public float spin;
    public float spinO;

    public AmazonShieldEntity(EntityType<? extends AmazonShieldEntity> type, Level level) {
        super(type, level);
    }

    public static AmazonShieldEntity create(Level level, Player owner, Vec3 pos, Vec3 velocity) {
        AmazonShieldEntity shield = new AmazonShieldEntity(ModEntities.AMAZON_SHIELD.get(), level);
        shield.setOwner(owner);
        shield.setPos(pos);
        shield.setDeltaMovement(velocity);
        shield.updateRotation();
        shield.yRotO = shield.getYRot();
        shield.xRotO = shield.getXRot();
        return shield;
    }

    /** The owner's shield in flight, or null. */
    public static @Nullable AmazonShieldEntity find(Player owner) {
        List<AmazonShieldEntity> list = owner.level().getEntitiesOfClass(AmazonShieldEntity.class, owner.getBoundingBox().inflate(160),
                s -> s.getOwner() == owner && !s.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RETURNING, false);
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    /** Called back: it flies home right now. */
    public void recall() {
        homing = null;
        returnSpeed = Math.max(returnSpeed, 1.0);
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
    public boolean isOnFire() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwner();
        if (!level().isClientSide()) {
            boolean ownerOk = owner instanceof ServerPlayer player && player.isAlive() && player.level() == level() && WonderWomanHelper.isSuited(player);
            if (!ownerOk) {
                vanish();
                return;
            }
        }
        if (isReturning()) {
            tickReturn(owner);
        } else {
            tickFlight();
        }
        if (level().isClientSide()) {
            spinO = spin;
            spin += 42.0F;
            if (random.nextFloat() < 0.5F) level().addParticle(ModParticles.AMAZON_SPARK.get(), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private void tickFlight() {
        flightTicks++;
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide()) {
            if (homing != null) {
                if (!homing.isAlive() || homing.isRemoved()) {
                    homing = null;
                } else {
                    // Curving toward the next enemy.
                    Vec3 to = homing.getBoundingBox().getCenter().subtract(position());
                    motion = motion.lerp(to.normalize().scale(THROW_SPEED), 0.5);
                    setDeltaMovement(motion);
                }
            }
            HitResult result = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (result.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, result)) {
                onHit(result);
                if (isRemoved() || isReturning()) return;
                motion = getDeltaMovement();
            }
            if (flightTicks > MAX_FLIGHT_TICKS) entityData.set(DATA_RETURNING, true);
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    private void tickReturn(@Nullable Entity owner) {
        if (owner == null) return;
        Vec3 target = owner.getEyePosition().add(0, -0.5, 0);
        Vec3 to = target.subtract(position());
        double dist = to.length();
        returnSpeed = Math.min(3.5, Math.max(returnSpeed, 0.5) + 0.12);
        Vec3 motion = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(returnSpeed, dist) / dist);
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (!level().isClientSide() && dist < 1.4 && owner instanceof ServerPlayer player) giveBack(player);
    }

    /** Back on her arm. */
    private void giveBack(ServerPlayer player) {
        if (WonderWomanHelper.shieldSlot(player) < 0) WonderWomanServer.giveShield(player);
        ServerLevel level = (ServerLevel) level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHIELD_RETURN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.08);
        discard();
    }

    private void vanish() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), getX(), getY(), getZ(), 16, 0.3, 0.3, 0.3, 0.1);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target instanceof LivingEntity && !hit.contains(target.getId()) && !(target instanceof ConstructEntity)
                && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))
                && !target.isPassengerOfSameVehicle(getOwner() == null ? this : getOwner());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target)) return;
        Entity owner = getOwner();
        hit.add(target.getId());
        target.invulnerableTime = 0;
        if (target.hurtServer(level, ModDamageTypes.amazon(level, this, owner == null ? this : owner), GLConfig.SHIELD_THROW_DAMAGE.get().floatValue())) {
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize();
            target.push(push.x * 0.9, 0.3, push.z * 0.9);
            target.hurtMarked = true;
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SHIELD_HIT.get(), SoundSource.PLAYERS, 1.2F, 0.95F + random.nextFloat() * 0.15F);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.15);
        // Ricochet to the next enemy around, if there is one left to hit.
        bounces++;
        LivingEntity next = bounces < GLConfig.SHIELD_BOUNCES.get() ? nextTarget(level, owner) : null;
        if (next == null) {
            setDeltaMovement(getDeltaMovement().scale(-0.2));
            entityData.set(DATA_RETURNING, true);
            return;
        }
        homing = next;
        flightTicks = 0;
        setDeltaMovement(next.getBoundingBox().getCenter().subtract(position()).normalize().scale(THROW_SPEED));
    }

    private @Nullable LivingEntity nextTarget(ServerLevel level, @Nullable Entity owner) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BOUNCE_RANGE),
                e -> e.isAlive() && e != owner && canHitEntity(e)
                        && (e instanceof Enemy || (e instanceof Mob mob && owner != null && mob.getTarget() == owner)))) {
            if (!candidate.hasLineOfSight(this)) continue;
            double dist = candidate.distanceToSqr(this);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 at = result.getLocation();
        level.playSound(null, at.x, at.y, at.z, ModSounds.SHIELD_HIT.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.2);
        setPos(at.subtract(getDeltaMovement().normalize().scale(0.3)));
        recall();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }
}
