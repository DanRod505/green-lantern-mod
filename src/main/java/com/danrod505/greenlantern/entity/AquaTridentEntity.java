package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The trident of Atlantis in flight. It flies fast and straight (water doesn't slow it down), hits
 * hard, and then always flies back to Aquaman's hand, like it knows its king.
 */
public class AquaTridentEntity extends Projectile {
    public static final double THROW_SPEED = 2.6;
    private static final int MAX_FLIGHT_TICKS = 30;
    private static final int STUCK_TICKS = 6;

    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(AquaTridentEntity.class, EntityDataSerializers.BOOLEAN);

    private ItemStack stack = ItemStack.EMPTY;
    private int flightTicks;
    private int stuckTicks;
    private double returnSpeed;
    private boolean hitSomething;

    public AquaTridentEntity(EntityType<? extends AquaTridentEntity> type, Level level) {
        super(type, level);
    }

    public static AquaTridentEntity create(Level level, LivingEntity owner, ItemStack stack, Vec3 pos, Vec3 velocity) {
        AquaTridentEntity trident = new AquaTridentEntity(ModEntities.AQUA_TRIDENT.get(), level);
        trident.setOwner(owner);
        trident.stack = stack;
        trident.setPos(pos);
        trident.setDeltaMovement(velocity);
        trident.updateRotation();
        trident.yRotO = trident.getYRot();
        trident.xRotO = trident.getXRot();
        return trident;
    }

    /** The owner's trident in flight, or null. */
    public static @Nullable AquaTridentEntity find(Player owner) {
        List<AquaTridentEntity> list = owner.level().getEntitiesOfClass(AquaTridentEntity.class, owner.getBoundingBox().inflate(160),
                t -> t.getOwner() == owner && !t.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RETURNING, false);
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    /** Called back by the trident power: it flies home right now. */
    public void recall() {
        stuckTicks = 0;
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
    public void tick() {
        super.tick();
        Entity owner = getOwner();
        if (!level().isClientSide()) {
            boolean ownerOk = owner instanceof ServerPlayer player && player.isAlive() && player.level() == level() && AquamanHelper.isSuited(player);
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
        if (level().isClientSide()) trailParticles();
    }

    private void tickFlight() {
        if (stuckTicks > 0) {
            setDeltaMovement(Vec3.ZERO);
            if (--stuckTicks == 0 && !level().isClientSide()) entityData.set(DATA_RETURNING, true);
            return;
        }
        flightTicks++;
        Vec3 motion = getDeltaMovement();
        if (!isInWater()) motion = motion.add(0, -0.03, 0);
        setDeltaMovement(motion);
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
                hitTargetOrDeflectSelf(hit);
                if (isRemoved() || stuckTicks > 0 || isReturning()) return;
            }
            if (flightTicks > MAX_FLIGHT_TICKS) entityData.set(DATA_RETURNING, true);
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    private void tickReturn(@Nullable Entity owner) {
        if (owner == null) return;
        Vec3 target = owner.getEyePosition().add(0, -0.4, 0);
        Vec3 to = target.subtract(position());
        double dist = to.length();
        returnSpeed = Math.min(3.5, Math.max(returnSpeed, 0.4) + 0.12);
        Vec3 motion = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(returnSpeed, dist) / dist);
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        if (!level().isClientSide() && dist < 1.4 && owner instanceof ServerPlayer player) {
            giveBack(player);
        }
    }

    /** Back in Aquaman's hand (or in his backpack if the hand is busy). */
    private void giveBack(ServerPlayer player) {
        if (AquamanHelper.tridentSlot(player) < 0) {
            ItemStack trident = stack.isEmpty() ? new ItemStack(ModItems.AQUAMAN_TRIDENT.get()) : stack;
            Inventory inventory = player.getInventory();
            int selected = inventory.getSelectedSlot();
            if (inventory.getItem(selected).isEmpty()) {
                inventory.setItem(selected, trident);
            } else if (!inventory.add(trident)) {
                player.displayClientMessage(Component.translatable("message.greenlantern.trident_no_room").withStyle(ChatFormatting.AQUA), true);
            }
        }
        ServerLevel level = (ServerLevel) level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRIDENT_RETURN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 16, 0.3, 0.3, 0.3, 0.2);
        discard();
    }

    private void vanish() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY(), getZ(), 16, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 16, 0.3, 0.3, 0.3, 0.2);
        }
        discard();
    }

    private void trailParticles() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 0.01) return;
        for (int i = 0; i < 3; i++) {
            double t = random.nextDouble();
            double x = getX() - motion.x * t;
            double y = getY() - motion.y * t;
            double z = getZ() - motion.z * t;
            if (isInWater()) {
                level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0, 0.02, 0);
            } else {
                level().addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0, 0);
            }
        }
        level().addParticle(ParticleTypes.DOLPHIN, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof ConstructEntity) && !(target instanceof AquaTridentEntity)
                && !(target instanceof Projectile);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)) return;
        Entity target = result.getEntity();
        Entity owner = getOwner();
        float damage = GLConfig.TRIDENT_THROW_DAMAGE.get().floatValue();
        // Like Impaling: deadlier against anything in the water or the rain.
        if (target.isInWaterOrRain()) damage *= 1.25F;
        DamageSource source = level.damageSources().trident(this, owner == null ? this : owner);
        if (target.hurtServer(level, source, damage) && target instanceof LivingEntity living) {
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize();
            living.push(push.x * 0.8, 0.25, push.z * 0.8);
            living.hurtMarked = true;
        }
        hitSomething = true;
        level.playSound(null, getX(), getY(), getZ(), ModSounds.TRIDENT_HIT.get(), SoundSource.PLAYERS, 1.2F, 0.95F + random.nextFloat() * 0.1F);
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 30, 0.4, 0.4, 0.4, 0.4);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
        setDeltaMovement(getDeltaMovement().scale(-0.1));
        entityData.set(DATA_RETURNING, true);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 at = result.getLocation().subtract(getDeltaMovement().normalize().scale(0.3));
        setPos(at);
        stuckTicks = STUCK_TICKS;
        setDeltaMovement(Vec3.ZERO);
        level.playSound(null, at.x, at.y, at.z, ModSounds.TRIDENT_HIT.get(), SoundSource.PLAYERS, 0.7F, 1.3F);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.2);
    }

    public boolean hasHitSomething() {
        return hitSomething;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
