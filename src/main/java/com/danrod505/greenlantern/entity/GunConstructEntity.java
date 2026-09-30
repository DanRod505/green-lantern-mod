package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.ring.RingHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The floating hard-light minigun. It hovers next to the owner's ring hand, aims where the owner
 * looks and spins its barrels while firing. Bullets are fired by {@link com.danrod505.greenlantern.construct.impl.MinigunConstruct}.
 */
public class GunConstructEntity extends ConstructEntity {
    /** Render scale of the gun model (also used to place the muzzle). */
    public static final float SCALE = 1.25F;
    private static final EntityDataAccessor<Integer> DATA_SPIN = SynchedEntityData.defineId(GunConstructEntity.class, EntityDataSerializers.INT);

    /** Client-side barrel rotation (degrees) and speed. */
    public float barrelAngle;
    public float barrelAngleO;
    private float barrelSpeed;

    public GunConstructEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPIN, 0);
    }

    /** Number of ticks the owner has been holding the trigger (drives the barrel spin-up). */
    public void setSpinTicks(int ticks) {
        entityData.set(DATA_SPIN, ticks);
    }

    public int getSpinTicks() {
        return entityData.get(DATA_SPIN);
    }

    /** Where bullets leave the barrels. */
    public Vec3 muzzlePosition() {
        return position().add(0, getBbHeight() / 2, 0).add(Vec3.directionFromRotation(getXRot(), getYRot()).scale(1.1 * SCALE));
    }

    @Override
    protected boolean ownerValid() {
        Player owner = getOwner();
        if (!super.ownerValid() || owner == null || !owner.isUsingItem()) return false;
        ItemStack used = owner.getUseItem();
        return RingHelper.isRing(used) && ConstructRegistry.selected(used) == ConstructRegistry.MINIGUN;
    }

    @Override
    public void tick() {
        followOwner();
        super.tick();
        if (level().isClientSide()) {
            float target = Math.min(60.0F, 8.0F + getSpinTicks() * 4.0F);
            barrelSpeed += (target - barrelSpeed) * 0.25F;
            barrelAngleO = barrelAngle;
            barrelAngle += barrelSpeed;
        }
    }

    private void followOwner() {
        Player owner = getOwner();
        if (owner == null) return;
        boolean mainHandRing = RingHelper.isRing(owner.getMainHandItem());
        HumanoidArm arm = mainHandRing ? owner.getMainArm() : owner.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        Vec3 look = owner.getLookAngle();
        // Derived from the yaw so it stays valid when looking straight up or down.
        Vec3 flat = Vec3.directionFromRotation(0, owner.getYRot());
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        Vec3 up = right.cross(look); // "up" of the owner's view, so the gun stays in the same spot on screen
        Vec3 pos = owner.getEyePosition()
                .add(right.scale(0.85 * side))
                .add(look.scale(1.6))
                .add(up.scale(-0.45));
        setPos(pos.x, pos.y - getBbHeight() / 2, pos.z);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
    }
}
