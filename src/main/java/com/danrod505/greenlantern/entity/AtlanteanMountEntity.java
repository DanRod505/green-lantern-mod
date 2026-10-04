package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A sea creature of Atlantis that anyone can ride (the giant manta ray, the giant seahorse and the
 * Atlantean dolphins). On their own they drift around the spot they live in, always in the water.
 * Right click one to climb on its back:
 * <ul>
 *     <li>It swims where the rider looks: W speeds up gradually, S slows down, A/D drift to the
 *     sides, jump rises and sprint builds up to its top speed.</li>
 *     <li>The bond of Atlantis lets the rider breathe underwater while riding.</li>
 *     <li>Each creature has its own trick out of the water (the manta glides, the seahorse kicks up,
 *     the dolphin leaps).</li>
 * </ul>
 * Like the horses, a ridden mount is moved by its rider's client; on its own by the server.
 */
public abstract class AtlanteanMountEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(AtlanteanMountEntity.class, EntityDataSerializers.INT);

    /** Where it lives: it keeps swimming around this spot. */
    private @Nullable BlockPos home;
    // Server, on its own: where it is swimming to.
    private @Nullable Vec3 waypoint;
    private int waypointTicks;
    private int restTicks;

    // Controlling client: the swim with a rider.
    protected double speed;
    protected double boost;
    private boolean boosting;

    // Client: animation.
    public float swimPhase;
    public float swimPhaseO;
    public float swimAmount;
    public float swimAmountO;

    protected AtlanteanMountEntity(EntityType<? extends AtlanteanMountEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(double health) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ---- per creature ---------------------------------------------------------------------------

    /** How many looks (colors / patterns) the creature has. */
    public abstract int variants();

    /** Cruising speed with a rider (blocks/tick). */
    protected abstract double cruiseSpeed();

    /** Top speed with a rider holding sprint (blocks/tick). */
    protected abstract double topSpeed();

    /** Seconds of sprint to build up from the cruise to the top speed. */
    protected double secondsToTop() {
        return 2.5;
    }

    /** How fast it turns to follow the rider's view (degrees/tick). */
    protected float turnRate() {
        return 8.0F;
    }

    /** Speed of the creature swimming on its own (blocks/tick). */
    protected double wanderSpeed() {
        return 0.12;
    }

    /** Where the rider sits, relative to the creature's position (not rotated). */
    protected abstract Vec3 seat();

    /** Moving out of the water with a rider (gravity, gliding, hopping...). Returns the new motion. */
    protected Vec3 riddenInAir(Player rider, Vec3 motion) {
        return new Vec3(motion.x * 0.98, Math.max(motion.y - 0.08, -2.0), motion.z * 0.98);
    }

    /** Called on the controlling client every tick of a ridden swim, after the motion was set. */
    protected void riddenSwimTick(Player rider, boolean boostKey, boolean riseKey) {}

    /** Called on every client each tick (particles, animation extras). */
    protected void clientExtras() {}

    // ---- state ----------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    public int variant() {
        return Mth.clamp(entityData.get(DATA_VARIANT), 0, variants() - 1);
    }

    public void setVariant(int variant) {
        entityData.set(DATA_VARIANT, Math.floorMod(variant, variants()));
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        setVariant(random.nextInt(variants()));
        if (home == null) home = blockPosition();
        setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
        if (home != null) {
            output.putInt("HomeX", home.getX());
            output.putInt("HomeY", home.getY());
            output.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
        if (input.getInt("HomeX").isPresent()) {
            home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // ---- riding ---------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isVehicle() || player.isSecondaryUseActive() || isBaby()) return super.mobInteract(player, hand);
        if (!level().isClientSide()) {
            if (player.startRiding(this)) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.ATLANTEAN_MOUNT_SADDLE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                waypoint = null;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return seat().yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean canBeRiddenUnderFluidType(net.minecraftforge.fluids.FluidType type, Entity rider) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // The rider's own swings and arrows never hurt the creature carrying them.
        if (source.getEntity() != null && hasPassenger(source.getEntity())) return false;
        return super.hurtServer(level, source, amount);
    }

    /** Direction the creature faces (yaw and swim pitch). */
    public Vec3 facing() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    /** 0-1: how far the ridden sprint has built up (controlling client). */
    public double boost() {
        return boost;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        if (!isLocalInstanceAuthoritative()) return;
        Vec3 motion = getDeltaMovement();
        if (!isInWater()) {
            boost = Math.max(0.0, boost - 0.02);
            setXRot(Mth.approach(getXRot(), Mth.clamp((float) (-motion.y * 40.0), -45.0F, 45.0F), 4.0F));
            setDeltaMovement(riddenInAir(rider, motion));
            return;
        }
        float forward = rider.zza;
        float strafe = rider.xxa;
        boolean boostKey = SidedHooks.sprintKeyDown.getAsBoolean();
        boolean rise = SidedHooks.jumpKeyDown.getAsBoolean();

        float yaw = Mth.approachDegrees(getYRot(), rider.getYRot(), turnRate());
        float pitch = Mth.approach(getXRot(), Mth.clamp(rider.getXRot(), -60.0F, 60.0F), turnRate() * 0.7F);
        setYRot(yaw);
        setXRot(pitch);
        setYBodyRot(yaw);
        setYHeadRot(yaw);

        // Sprint builds up gradually (eased) and eases back down when let go.
        boosting = forward > 0 && (boostKey || boosting);
        double ramp = secondsToTop() * 20.0;
        boost = boosting ? Math.min(1.0, boost + 1.0 / ramp) : Math.max(0.0, boost - 1.5 / ramp);
        double eased = boost * boost * (3.0 - 2.0 * boost);
        double top = Mth.lerp(eased, cruiseSpeed(), topSpeed());
        if (forward > 0) {
            speed = Math.min(top, speed + Math.max(0.02, top / 30.0));
            if (speed > top) speed = Mth.lerp(0.1, speed, top);
        } else if (forward < 0) {
            speed = Math.max(-0.2, speed - 0.06);
        } else {
            speed *= 0.95;
        }
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        if (side.lengthSqr() > 1.0E-4) side = side.normalize();
        Vec3 wish = dir.scale(speed).add(side.scale(strafe * -0.3)).add(0, rise ? 0.28 : 0.0, 0);
        setDeltaMovement(motion.lerp(wish, 0.3));
        riddenSwimTick(rider, boostKey, rise);
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player) {
            move(MoverType.SELF, getDeltaMovement());
            return;
        }
        super.travel(input);
    }

    // ---- tick -----------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        boolean water = isInWater();
        setNoGravity(water);
        if (water || isVehicle()) resetFallDistance();
        if (level() instanceof ServerLevel serverLevel) {
            if (!isVehicle() && !isNoAi()) {
                speed = 0;
                boost = 0;
                boosting = false;
                if (water) {
                    Vec3 velocity = wanderVelocity(serverLevel);
                    setDeltaMovement(velocity);
                    if (velocity.lengthSqr() > 1.0E-4) {
                        float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
                        float pitch = (float) (-Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG);
                        setYRot(Mth.approachDegrees(getYRot(), yaw, 6.0F));
                        setXRot(Mth.approach(getXRot(), Mth.clamp(pitch, -35.0F, 35.0F), 3.0F));
                        setYBodyRot(getYRot());
                        setYHeadRot(getYRot());
                    }
                } else {
                    setXRot(Mth.approach(getXRot(), 0.0F, 4.0F));
                }
            }
            for (Entity passenger : getPassengers()) {
                // The bond of Atlantis: the rider breathes through the creature.
                if (passenger instanceof LivingEntity living) living.setAirSupply(living.getMaxAirSupply());
            }
        }
        super.aiStep();
        if (level().isClientSide()) clientTick();
    }

    /** On its own: drifts from one spot of water to another around its home, resting in between. */
    private Vec3 wanderVelocity(ServerLevel level) {
        if (home == null) home = blockPosition();
        if (restTicks > 0) {
            restTicks--;
            return getDeltaMovement().scale(0.85).add(0, Mth.sin(tickCount * 0.07F) * 0.004, 0);
        }
        if (waypoint == null || waypointTicks-- <= 0 || position().distanceToSqr(waypoint) < 1.5 || horizontalCollision) {
            boolean arrived = waypoint != null && position().distanceToSqr(waypoint) < 4.0;
            waypoint = pickWaypoint(level);
            waypointTicks = 300;
            if (arrived) restTicks = 20 + random.nextInt(80);
            if (waypoint == null) return getDeltaMovement().scale(0.8);
        }
        Vec3 to = waypoint.subtract(position());
        double len = to.length();
        Vec3 want = to.scale(Math.min(wanderSpeed(), len) / Math.max(len, 1.0E-4));
        return getDeltaMovement().lerp(want, 0.08);
    }

    private @Nullable Vec3 pickWaypoint(ServerLevel level) {
        BlockPos center = home != null && home.distSqr(blockPosition()) < 24 * 24 ? home : blockPosition();
        for (int tries = 0; tries < 10; tries++) {
            BlockPos pos = center.offset(random.nextInt(21) - 10, random.nextInt(9) - 4, random.nextInt(21) - 10);
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER)
                    && level.getBlockState(pos.below()).is(Blocks.WATER)) {
                return Vec3.atCenterOf(pos);
            }
        }
        return null;
    }

    private void clientTick() {
        swimPhaseO = swimPhase;
        swimAmountO = swimAmount;
        double moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
        boolean water = isInWater();
        float target = water ? (float) Mth.clamp(0.3 + moved * 1.2, 0.3, 1.0) : 0.15F;
        swimAmount = Mth.approach(swimAmount, target, 0.05F);
        swimPhase += water ? (float) (0.1 + moved * 0.5) : 0.05F;
        if (water && moved > 0.4 && random.nextInt(2) == 0) {
            Vec3 back = position().add(0, getBbHeight() * 0.5, 0).subtract(facing().scale(getBbWidth() * 0.6));
            level().addParticle(ParticleTypes.BUBBLE, back.x + random.nextGaussian() * 0.3, back.y + random.nextGaussian() * 0.2,
                    back.z + random.nextGaussian() * 0.3, 0, 0.05, 0);
        }
        clientExtras();
    }

    public float swimPhase(float partialTick) {
        return Mth.lerp(partialTick, swimPhaseO, swimPhase);
    }

    public float swimAmount(float partialTick) {
        return Mth.lerp(partialTick, swimAmountO, swimAmount);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
