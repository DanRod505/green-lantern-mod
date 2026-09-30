package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base class of every hard-light construct: it belongs to a player (the ring bearer), is never
 * saved to disk and fades away as soon as its owner is gone or no longer suited up.
 */
public abstract class ConstructEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID ownerUuid;
    private @Nullable Player cachedOwner;

    protected ConstructEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_ID, -1);
    }

    public void setOwner(Player owner) {
        this.ownerUuid = owner.getUUID();
        this.cachedOwner = owner;
        this.entityData.set(DATA_OWNER_ID, owner.getId());
    }

    public @Nullable Player getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
        if (level().isClientSide()) {
            Entity entity = level().getEntity(entityData.get(DATA_OWNER_ID));
            cachedOwner = entity instanceof Player player ? player : null;
        } else if (ownerUuid != null) {
            cachedOwner = level().getPlayerByUUID(ownerUuid);
        }
        return cachedOwner;
    }

    public boolean isOwnedBy(Entity entity) {
        return entity instanceof Player player && (player.getUUID().equals(ownerUuid) || player.getId() == entityData.get(DATA_OWNER_ID));
    }

    /** The owner's ring, or EMPTY if the owner is gone or doesn't carry one any more. */
    protected ItemStack ownerRing() {
        Player owner = getOwner();
        return owner == null ? ItemStack.EMPTY : RingHelper.findRing(owner);
    }

    /** Whether the owner can still sustain this construct. Checked on the server every tick. */
    protected boolean ownerValid() {
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level() && RingHelper.isSuited(owner) && !ownerRing().isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > 1 && !ownerValid()) {
            dissipate();
        }
    }

    /** Removes the construct with a burst of green light. */
    public void dissipate() {
        if (level() instanceof ServerLevel serverLevel && !isRemoved()) {
            double w = getBbWidth() * 0.4;
            double h = getBbHeight() * 0.4;
            serverLevel.sendParticles(ModParticles.GLOW.get(), getX(), getY() + getBbHeight() / 2, getZ(), 20, w, h, w, 0.01);
            serverLevel.sendParticles(ModParticles.SPARK.get(), getX(), getY() + getBbHeight() / 2, getZ(), 12, w, h, w, 0.1);
        }
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ownerUuid = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.storeNullable("Owner", UUIDUtil.CODEC, ownerUuid);
    }
}
