package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * The loop of the Lasso of Truth, thrown by Wonder Woman; the golden rope runs back to her hand.
 * <ul>
 *     <li><b>Capture</b>: the loop closes around the first creature it meets. Bound by the lasso it
 *     can't run off (the rope keeps it close) and, compelled by the truth, it can't fight.</li>
 *     <li><b>Pull</b>: a creature is yanked to her; thrown at a wall, the lasso pulls her there.</li>
 * </ul>
 * Whirling a caught creature around is done by {@code WonderWomanServer} (lasso spin).
 */
public class LassoEntity extends Projectile {
    public static final int MODE_CAPTURE = 0;
    public static final int MODE_PULL = 1;

    public static final int STATE_FLYING = 0;
    public static final int STATE_BOUND = 1;
    public static final int STATE_RETURNING = 2;
    public static final int STATE_ANCHORED = 3;

    public static final double THROW_SPEED = 2.0;
    /** How far a caught creature may stray from her before the rope pulls it back. */
    public static final double LEASH = 5.0;

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(LassoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(LassoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(LassoEntity.class, EntityDataSerializers.INT);

    /** Creatures bound by a lasso right now (server): they can't attack. */
    private static final Map<UUID, UUID> BOUND = new ConcurrentHashMap<>();

    private int stateTicks;
    private int boundTicks;
    private @Nullable LivingEntity target;

    public LassoEntity(EntityType<? extends LassoEntity> type, Level level) {
        super(type, level);
    }

    public static LassoEntity create(Level level, Player owner, int mode, Vec3 pos, Vec3 velocity) {
        LassoEntity lasso = new LassoEntity(ModEntities.LASSO.get(), level);
        lasso.setOwner(owner);
        lasso.entityData.set(DATA_MODE, mode);
        lasso.setPos(pos);
        lasso.setDeltaMovement(velocity);
        lasso.updateRotation();
        lasso.yRotO = lasso.getYRot();
        lasso.xRotO = lasso.getXRot();
        return lasso;
    }

    /** The owner's lasso (thrown, or holding a creature), or null. */
    public static @Nullable LassoEntity find(Player owner) {
        List<LassoEntity> list = owner.level().getEntitiesOfClass(LassoEntity.class, owner.getBoundingBox().inflate(96),
                l -> l.getOwner() == owner && !l.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    /** Whether the creature is bound by a Lasso of Truth (it can't attack). */
    public static boolean isBound(Entity entity) {
        return BOUND.containsKey(entity.getUUID());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, MODE_CAPTURE);
        builder.define(DATA_STATE, STATE_FLYING);
        builder.define(DATA_TARGET, -1);
    }

    public int mode() {
        return entityData.get(DATA_MODE);
    }

    public int state() {
        return entityData.get(DATA_STATE);
    }

    private void setState(int state) {
        entityData.set(DATA_STATE, state);
        stateTicks = 0;
    }

    /** The creature held by the loop (both sides), or null. */
    public @Nullable LivingEntity boundTarget() {
        if (state() != STATE_BOUND) return null;
        if (!level().isClientSide()) return target;
        Entity entity = level().getEntity(entityData.get(DATA_TARGET));
        return entity instanceof LivingEntity living ? living : null;
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
    public boolean isPickable() {
        return false;
    }

    // ---- tick -----------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        stateTicks++;
        Entity owner = getOwner();
        if (!level().isClientSide()) {
            boolean ownerOk = owner instanceof ServerPlayer player && player.isAlive() && player.level() == level() && WonderWomanHelper.isSuited(player);
            if (!ownerOk) {
                discard();
                return;
            }
        }
        switch (state()) {
            case STATE_FLYING -> tickFlight(owner);
            case STATE_BOUND -> tickBound(owner);
            case STATE_ANCHORED -> {
                setDeltaMovement(Vec3.ZERO);
                if (!level().isClientSide() && stateTicks > 8) setState(STATE_RETURNING);
            }
            default -> tickReturn(owner);
        }
    }

    private void tickFlight(@Nullable Entity owner) {
        Vec3 motion = getDeltaMovement().add(0, -0.02, 0);
        setDeltaMovement(motion);
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                if (isRemoved() || state() != STATE_FLYING) return;
            }
            if (owner == null || distanceTo(owner) > GLConfig.LASSO_RANGE.get() || stateTicks > 40) setState(STATE_RETURNING);
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        if (level().isClientSide() && random.nextFloat() < 0.6F) {
            level().addParticle(ModParticles.AMAZON_SPARK.get(), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private void tickReturn(@Nullable Entity owner) {
        if (owner == null) return;
        Vec3 hand = owner.getEyePosition().add(0, -0.5, 0);
        Vec3 to = hand.subtract(position());
        double dist = to.length();
        double speed = Math.min(3.0, 0.8 + stateTicks * 0.2);
        Vec3 motion = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(speed, dist) / dist);
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (!level().isClientSide() && (dist < 1.5 || stateTicks > 60)) discard();
    }

    private void tickBound(@Nullable Entity owner) {
        if (level().isClientSide()) {
            LivingEntity held = boundTarget();
            if (held != null) {
                setPos(held.getX(), held.getY() + held.getBbHeight() * 0.5, held.getZ());
                if (random.nextFloat() < 0.35F) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double r = held.getBbWidth() * 0.6 + 0.15;
                    level().addParticle(ModParticles.AMAZON_SPARK.get(), held.getX() + Math.cos(a) * r, held.getY() + held.getBbHeight() * 0.55,
                            held.getZ() + Math.sin(a) * r, 0, 0.02, 0);
                }
            }
            return;
        }
        LivingEntity held = target;
        if (held == null || !held.isAlive() || held.level() != level() || owner == null || owner.distanceTo(held) > GLConfig.LASSO_RANGE.get() + 8
                || ++boundTicks > Mth.ceil(GLConfig.LASSO_CAPTURE_SECONDS.get() * 20.0)) {
            release();
            return;
        }
        setPos(held.getX(), held.getY() + held.getBbHeight() * 0.5, held.getZ());
        setDeltaMovement(Vec3.ZERO);
        // Bound: it can barely move and won't fight; a creature that strays is pulled back on the rope.
        held.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 4, true, false));
        held.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15, 2, true, false));
        if (held instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        Vec3 toOwner = owner.position().subtract(held.position());
        double dist = toOwner.length();
        if (dist > LEASH && held != getOwnerVehicle(owner)) {
            Vec3 pull = toOwner.scale(Math.min(0.6, (dist - LEASH) * 0.15) / dist);
            held.setDeltaMovement(held.getDeltaMovement().scale(0.5).add(pull.x, Math.max(0.0, pull.y) + 0.02, pull.z));
            held.hurtMarked = true;
        }
    }

    private static @Nullable Entity getOwnerVehicle(Entity owner) {
        return owner.getVehicle();
    }

    /** The loop opens: the creature is free and the lasso flies back. */
    public void release() {
        if (target != null) BOUND.remove(target.getUUID());
        target = null;
        entityData.set(DATA_TARGET, -1);
        boundTicks = 0;
        if (!isRemoved()) setState(STATE_RETURNING);
    }

    /** Lets go of the creature (it was hurled by the spin) and flies back. */
    public @Nullable LivingEntity letGo() {
        LivingEntity held = target;
        release();
        return held;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (target != null) BOUND.remove(target.getUUID());
        super.remove(reason);
    }

    // ---- hits -----------------------------------------------------------------------------------------

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity instanceof LivingEntity && !(entity instanceof ConstructEntity)
                && !(entity instanceof Player p && (p.isSpectator() || p.isCreative())) && !entity.isPassengerOfSameVehicle(getOwner() == null ? this : getOwner());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity living)) return;
        Entity owner = getOwner();
        if (mode() == MODE_CAPTURE) {
            target = living;
            BOUND.put(living.getUUID(), owner == null ? getUUID() : owner.getUUID());
            entityData.set(DATA_TARGET, living.getId());
            setState(STATE_BOUND);
            boundTicks = 0;
            if (living instanceof Mob mob) mob.setTarget(null);
            level.playSound(null, living.getX(), living.getY(), living.getZ(), ModSounds.LASSO_CAPTURE.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 24,
                    living.getBbWidth() * 0.5, living.getBbHeight() * 0.4, living.getBbWidth() * 0.5, 0.05);
        } else {
            // Yanked to her feet.
            if (owner != null) {
                Vec3 to = owner.position().subtract(living.position());
                double dist = to.length();
                if (dist > 1.0E-3) {
                    Vec3 pull = to.scale(Math.min(2.2, 0.35 + dist * 0.16) / dist);
                    living.setDeltaMovement(pull.x, 0.35 + Math.max(0.0, pull.y * 0.5), pull.z);
                    living.hurtMarked = true;
                }
            }
            living.hurtServer(level, level.damageSources().thrown(this, owner == null ? this : owner), 2.0F);
            level.playSound(null, living.getX(), living.getY(), living.getZ(), ModSounds.LASSO_PULL.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 14, 0.3, 0.3, 0.3, 0.1);
            setState(STATE_RETURNING);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 at = result.getLocation();
        setPos(at);
        setDeltaMovement(Vec3.ZERO);
        Entity owner = getOwner();
        if (mode() == MODE_PULL && owner instanceof ServerPlayer player && !player.isPassenger()) {
            // The loop catches on the wall: she is pulled there.
            Vec3 to = at.subtract(player.position());
            double dist = to.length();
            if (dist > 1.5) {
                Vec3 pull = to.scale(Math.min(2.4, 0.5 + dist * 0.12) / dist);
                player.setDeltaMovement(pull.x, pull.y + 0.35, pull.z);
                player.hurtMarked = true;
                player.resetFallDistance();
            }
            level.playSound(null, at.x, at.y, at.z, ModSounds.LASSO_PULL.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.1);
            setState(STATE_ANCHORED);
            return;
        }
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.05);
        setState(STATE_RETURNING);
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
