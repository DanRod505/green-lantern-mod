package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The hook of the grapnel gun. It flies out trailing its cable; when it bites into a block the cable
 * reels Batman in (the pull itself runs on his client, see {@code client.batman.GrappleController}),
 * and when it catches a creature it yanks it towards him. It lets go when he arrives, when he jumps
 * or sneaks, or when the gadget is used again.
 */
public class GrappleHookEntity extends Projectile {
    public static final double SPEED = 3.2;
    /** Batman lets go this close to the hook (he has arrived). */
    public static final double ARRIVED = 1.8;
    private static final int MAX_ATTACHED_TICKS = 120;

    private static final EntityDataAccessor<Boolean> DATA_ATTACHED = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);

    private int attachedTicks;

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
        super(type, level);
    }

    public static GrappleHookEntity create(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity) {
        GrappleHookEntity hook = new GrappleHookEntity(ModEntities.GRAPPLE_HOOK.get(), level);
        hook.setOwner(owner);
        hook.setPos(pos);
        hook.setDeltaMovement(velocity);
        hook.updateRotation();
        hook.yRotO = hook.getYRot();
        hook.xRotO = hook.getXRot();
        return hook;
    }

    /** The owner's hook (flying or attached), or null. */
    public static @Nullable GrappleHookEntity find(Player owner) {
        List<GrappleHookEntity> list = owner.level().getEntitiesOfClass(GrappleHookEntity.class, owner.getBoundingBox().inflate(160),
                h -> h.getOwner() == owner && !h.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ATTACHED, false);
    }

    /** Whether the hook has bitten into a block (the cable is reeling in). */
    public boolean isAttached() {
        return entityData.get(DATA_ATTACHED);
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    /** Lets go (the gadget used again, a jump or a sneak): the cable winds back into the gun. */
    public void release() {
        if (level() instanceof ServerLevel level && getOwner() != null) {
            Entity owner = getOwner();
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.GRAPPLE_REEL.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwner();
        if (!level().isClientSide()) {
            if (!(owner instanceof ServerPlayer player) || !player.isAlive() || player.level() != level() || !BatmanHelper.isSuited(player)
                    || player.isPassenger()) {
                discard();
                return;
            }
            double dist = player.position().distanceTo(position());
            if (dist > GLConfig.GRAPPLE_RANGE.get() + 8.0) {
                release();
                return;
            }
            if (isAttached()) {
                attachedTicks++;
                boolean arrived = player.position().add(0, 1.0, 0).distanceTo(position()) < ARRIVED + 0.6;
                if (arrived || attachedTicks > MAX_ATTACHED_TICKS || (attachedTicks > 4 && player.isShiftKeyDown())) {
                    release();
                    return;
                }
            }
        }
        if (!isAttached()) tickFlight(owner);
    }

    private void tickFlight(@Nullable Entity owner) {
        Vec3 motion = getDeltaMovement().add(0, -0.02, 0);
        setDeltaMovement(motion);
        if (!level().isClientSide()) {
            if (owner != null && owner.position().distanceTo(position()) > GLConfig.GRAPPLE_RANGE.get()) {
                // Out of cable: it falls short.
                release();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
                hitTargetOrDeflectSelf(hit);
                if (isRemoved() || isAttached()) return;
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && target instanceof LivingEntity && !(target instanceof Player p && p.isSpectator());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target) || getOwner() == null) return;
        // Yanked off its feet, towards Batman.
        Entity owner = getOwner();
        Vec3 to = owner.position().subtract(target.position());
        double dist = to.length();
        Vec3 pull = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(1.6, 0.25 + dist * 0.09) / dist);
        target.hurtServer(level, level.damageSources().thrown(this, owner), 2.0F);
        target.setDeltaMovement(pull.x, 0.45 + Math.min(0.3, dist * 0.02), pull.z);
        target.hurtMarked = true;
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.GRAPPLE_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.GRAPPLE_REEL.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 at = result.getLocation();
        setPos(at.subtract(getDeltaMovement().normalize().scale(0.1)));
        setDeltaMovement(Vec3.ZERO);
        entityData.set(DATA_ATTACHED, true);
        attachedTicks = 0;
        level.playSound(null, at.x, at.y, at.z, ModSounds.GRAPPLE_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        Entity owner = getOwner();
        if (owner != null) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.GRAPPLE_REEL.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        }
        BlockState state = level.getBlockState(result.getBlockPos());
        if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 12, 0.1, 0.1, 0.1, 0.1);
        }
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.05, 0.05, 0.05, 0.2);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
