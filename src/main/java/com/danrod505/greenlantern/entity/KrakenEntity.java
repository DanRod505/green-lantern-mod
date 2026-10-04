package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanServer;
import com.danrod505.greenlantern.aquaman.SeaCall;
import com.danrod505.greenlantern.aquaman.SeaForce;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The Kraken: a 15 block tall sea monster Aquaman calls from the deep and rides on top of its head.
 * <ul>
 *     <li>On land it walks on its eight arms, slow and heavy: every step shakes the ground. Jump is
 *     a heavy leap that lands with a shockwave.</li>
 *     <li>In deep water it stretches out like a squid and races through the sea where the rider
 *     looks (jump rises, sprint is a burst).</li>
 *     <li>Left click: one of its two long hunting tentacles rises over its head and slams down in
 *     front of it. Hold right click: a powerful jet of water from its siphon, where the rider aims.</li>
 *     <li>It has its own life: monsters, arrows and explosions aimed at its rider hit the Kraken
 *     instead. When its life runs out it dies and needs a while to recover before it can be called
 *     again. In the water its wounds slowly heal.</li>
 *     <li>Sneak to climb off: it stays and fights the monsters around Aquaman. Right click it to
 *     climb back on.</li>
 * </ul>
 * Like the shark, the ridden Kraken is moved by its rider's client; on its own by the server.
 * <p>
 * Body geometry (shared with the renderer): the "head frame" is centered on the head, which is
 * {@link #headHeight} above the feet (and moves forward when swimming); the body pitches around it
 * when swimming up or down.
 */
public class KrakenEntity extends ConstructEntity {
    public static final float LAND_WIDTH = 6.0F;
    public static final float LAND_HEIGHT = 15.0F;
    public static final float SWIM_WIDTH = 8.0F;
    public static final float SWIM_HEIGHT = 8.0F;
    private static final EntityDimensions LAND = EntityDimensions.scalable(LAND_WIDTH, LAND_HEIGHT);
    private static final EntityDimensions SWIM = EntityDimensions.scalable(SWIM_WIDTH, SWIM_HEIGHT);

    /** Height of the head center above the feet, standing on land / swimming. */
    public static final float HEAD_Y_LAND = 7.0F;
    public static final float HEAD_Y_SWIM = 4.0F;
    /** How far the head moves forward of the center when swimming (the arms trail behind). */
    public static final float HEAD_FORWARD_SWIM = 2.5F;
    /** Where Aquaman sits, relative to the head center: on top of the head, in front of the mantle. */
    public static final float SEAT_UP = 1.85F;
    public static final float SEAT_FORWARD = 0.15F;
    /** The siphon the water jet comes out of, relative to the head center. */
    public static final float SIPHON_FORWARD = 3.4F;
    public static final float SIPHON_UP = -1.0F;
    /** Distance covered by one full walk cycle (each group of four arms takes a step). */
    public static final float STRIDE = 6.0F;
    /** Length of the slam animation; the tentacle hits the ground at {@link #SLAM_HIT}. */
    public static final int SLAM_TICKS = 24;
    public static final int SLAM_HIT = 9;
    public static final int DEATH_TICKS = 60;
    public static final int EMERGE_TICKS = 36;

    private static final double GRAVITY = 0.08;
    private static final int SLAM_COOLDOWN = 26;
    private static final int MAX_UNRIDDEN_TICKS = 20 * 180;

    public static final byte EVENT_LANDING = 61;
    public static final byte EVENT_SLAM_IMPACT = 62;

    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_JET = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3fc> DATA_JET_END = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_HURT = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEATH = SynchedEntityData.defineId(KrakenEntity.class, EntityDataSerializers.INT);

    private static final AttributeModifier CAMERA_DISTANCE = new AttributeModifier(GreenLantern.id("kraken_camera"), 9.0, AttributeModifier.Operation.ADD_VALUE);

    /** Owner -> game time when a fallen Kraken can be called again. */
    private static final Map<UUID, Long> RECOVERY = new ConcurrentHashMap<>();

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    // Both sides: blend between the walking (0) and swimming (1) body.
    private float swimBlend;
    private float swimBlendO;

    // Controlling side: movement.
    private double walkSpeed;
    private double swimSpeed;
    private int jumpCooldown;

    // Server.
    private int slamCooldown;
    private int slamPending = -1;
    private boolean slamLeft;
    private boolean jetRequested;
    private int jetTicks;
    private int invulnerable;
    private int unriddenTicks;
    private int hurtSoundCooldown;
    private int deathTicks = -1;
    private @Nullable LivingEntity target;
    private int airTicks;
    private double lastAirSpeedY;
    private double lastServerY = Double.NaN;
    private int roarCooldown = 200;

    // Client: animation.
    public float walkPhase;
    public float walkPhaseO;
    public float walkAmount;
    public float walkAmountO;
    public float swimWave;
    public float swimWaveO;
    public float swimPower;
    public float swimPowerO;
    public float jetBlend;
    public float jetBlendO;
    public float bob;
    public float bobO;
    public float squash;
    public float squashO;
    public int slamStartTick = -1000;
    public boolean slamLeftClient;
    public int hurtTicks;
    public int clientDeathTicks = -1;
    public Vec3 jetEnd = Vec3.ZERO;
    public Vec3 jetEndO = Vec3.ZERO;
    private int lastSlam;
    private int lastHurt;
    private int stepIndex;
    private int pulseIndex;

    public KrakenEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static KrakenEntity create(Level level, Player owner, boolean swimming) {
        KrakenEntity kraken = new KrakenEntity(ModEntities.KRAKEN.get(), level);
        kraken.setOwner(owner);
        kraken.entityData.set(DATA_HEALTH, maxHealth());
        kraken.entityData.set(DATA_SWIMMING, swimming);
        kraken.swimBlend = kraken.swimBlendO = swimming ? 1.0F : 0.0F;
        kraken.refreshDimensions();
        kraken.snapTo(owner.getX(), owner.getY() - (swimming ? 1.0 : 0.0), owner.getZ(), owner.getYRot(), 0.0F);
        return kraken;
    }

    /** The owner's Kraken, or null. */
    public static @Nullable KrakenEntity find(Player owner) {
        List<KrakenEntity> list = owner.level().getEntitiesOfClass(KrakenEntity.class, owner.getBoundingBox().inflate(192),
                k -> k.isOwnedBy(owner) && !k.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    public static float maxHealth() {
        return GLConfig.KRAKEN_HEALTH.get().floatValue();
    }

    /** Seconds before the owner's fallen Kraken can be called again (0 when it can). */
    public static int recoverySeconds(Player owner) {
        Long ready = RECOVERY.get(owner.getUUID());
        if (ready == null) return 0;
        long left = ready - owner.level().getGameTime();
        if (left <= 0) {
            RECOVERY.remove(owner.getUUID());
            return 0;
        }
        return (int) ((left + 19) / 20);
    }

    public static void clearRecovery(Player owner) {
        RECOVERY.remove(owner.getUUID());
    }

    /**
     * Whether a Kraken fits at the player's feet: open space for most of its body (the arms and the
     * tip of the mantle can brush against things).
     */
    public static boolean fits(Level level, Player owner, boolean swimming) {
        EntityDimensions dims = swimming ? SWIM : LAND;
        double hw = dims.width() * 0.5 - 1.5;
        double y = owner.getY() + 1.1 - (swimming ? 1.0 : 0.0);
        AABB box = new AABB(owner.getX() - hw, y, owner.getZ() - hw, owner.getX() + hw, y + dims.height() - 3.5, owner.getZ() + hw);
        return level.noBlockCollision(owner, box);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HEALTH, 100.0F);
        builder.define(DATA_SWIMMING, false);
        builder.define(DATA_SLAM, 0);
        builder.define(DATA_JET, false);
        builder.define(DATA_JET_END, new Vector3f());
        builder.define(DATA_HURT, 0);
        builder.define(DATA_DEATH, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_SWIMMING.equals(accessor)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isSwimmingMode() ? SWIM : LAND;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // ---- state ------------------------------------------------------------------------------------

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        entityData.set(DATA_HEALTH, Mth.clamp(health, 0.0F, maxHealth()));
    }

    public boolean isSwimmingMode() {
        return entityData.get(DATA_SWIMMING);
    }

    public boolean isJetting() {
        return entityData.get(DATA_JET);
    }

    public boolean isDying() {
        return entityData.get(DATA_DEATH) > 0;
    }

    public float swimBlend(float partialTick) {
        return Mth.lerp(partialTick, swimBlendO, swimBlend);
    }

    public static float headHeight(float swim) {
        return Mth.lerp(swim, HEAD_Y_LAND, HEAD_Y_SWIM);
    }

    /** The body's pitch: only when swimming (nose down is positive, like vanilla). */
    public float bodyPitch(float partialTick) {
        return Mth.lerp(partialTick, xRotO, getXRot());
    }

    /**
     * Converts a point of the head frame (forward, up and to the left of the head center, in
     * blocks) to world coordinates.
     */
    public Vec3 bodyPoint(double forward, double up, double left, float partialTick) {
        float swim = swimBlend(partialTick);
        float yaw = getYRot(partialTick);
        float pitch = bodyPitch(partialTick);
        Vec3 flat = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 pos = getPosition(partialTick);
        Vec3 head = pos.add(0, headHeight(swim), 0).add(flat.scale(HEAD_FORWARD_SWIM * swim));
        Vec3 f = Vec3.directionFromRotation(pitch, yaw);
        Vec3 u = Vec3.directionFromRotation(pitch - 90.0F, yaw);
        Vec3 l = u.cross(f);
        return head.add(f.scale(forward)).add(u.scale(up)).add(l.scale(left));
    }

    public Vec3 siphon() {
        return bodyPoint(SIPHON_FORWARD, SIPHON_UP, 0.0, 1.0F);
    }

    // ---- riding -------------------------------------------------------------------------------------

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return !isDying() && getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && isOwnedBy(passenger) && !isDying();
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        Vec3 seat = bodyPoint(SEAT_FORWARD, SEAT_UP - (level().isClientSide() ? bob : 0.0F), 0.0, 1.0F);
        return seat.subtract(position());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (isSwimmingMode()) return bodyPoint(0.0, SEAT_UP + 0.5, 0.0, 1.0F);
        // Slide down an arm to the ground, on whichever side is free.
        for (Vec3 offset : new Vec3[] {new Vec3(-4.5, 0, 0), new Vec3(4.5, 0, 0), new Vec3(0, 0, 5.0), new Vec3(0, 0, -5.0)}) {
            Vec3 spot = position().add(offset.yRot(-getYRot() * Mth.DEG_TO_RAD)).add(0, 0.1, 0);
            if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(spot))) return spot;
        }
        return bodyPoint(SEAT_FORWARD, SEAT_UP + 0.2, 0.0, 1.0F);
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
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player player) setCameraDistance(player, true);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player player) {
            setCameraDistance(player, false);
            player.resetFallDistance();
        }
        jetRequested = false;
    }

    /** Pulls the third person camera back so the whole Kraken fits on screen. */
    private static void setCameraDistance(Player player, boolean riding) {
        AttributeInstance camera = player.getAttribute(Attributes.CAMERA_DISTANCE);
        if (camera == null) return;
        if (riding) {
            camera.addOrUpdateTransientModifier(CAMERA_DISTANCE);
        } else {
            camera.removeModifier(CAMERA_DISTANCE.id());
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!isOwnedBy(player) || isVehicle() || isDying() || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide()) player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return !isDying();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return isSwimmingMode() ? 1.5F : 2.6F;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 224 * 224;
    }

    @Override
    protected boolean ownerValid() {
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level() && AquamanHelper.isSuited(owner);
    }

    @Override
    public void dissipate() {
        retreat();
    }

    /** The Kraken goes back to the deep: it sinks out of sight in a burst of bubbles and ink. */
    public void retreat() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            Vec3 c = bodyPoint(0, 0, 0, 1.0F);
            level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 80, 2.5, 2.5, 2.5, 0.05);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 1.0, getZ(), 80, 3.0, 1.0, 3.0, 0.2);
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 2.0, getZ(), 80, 3.0, 1.0, 3.0, 0.3);
            level.playSound(null, c.x, c.y, c.z, ModSounds.KRAKEN_SUMMON.get(), SoundSource.NEUTRAL, 1.6F, 1.25F);
        }
        ejectPassengers();
        discard();
    }

    // ---- life and death -----------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || isDying() || amount <= 0) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && (isOwnedBy(attacker) || attacker == this)) return false;
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        if (source.is(DamageTypeTags.IS_FIRE)) {
            if (isInWater()) return false;
            amount *= 0.5F;
        }
        if (invulnerable > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        invulnerable = 8;
        setHealth(getHealth() - amount);
        entityData.set(DATA_HURT, entityData.get(DATA_HURT) + 1);
        if (hurtSoundCooldown <= 0) {
            Vec3 c = bodyPoint(0, 0, 0, 1.0F);
            level.playSound(null, c.x, c.y, c.z, ModSounds.KRAKEN_HURT.get(), SoundSource.NEUTRAL, 2.0F, 0.85F + random.nextFloat() * 0.2F);
            hurtSoundCooldown = 14;
        }
        Vec3 where = source.getSourcePosition();
        if (where != null) {
            level.sendParticles(ParticleTypes.SQUID_INK, where.x, where.y, where.z, 6, 0.3, 0.3, 0.3, 0.02);
        }
        if (getHealth() <= 0.0F) startDying(level);
        return true;
    }

    /** Server: something hit the rider. The Kraken takes the blow instead. */
    public void shieldRider(ServerPlayer rider, DamageSource source, float amount) {
        if (level() instanceof ServerLevel level) {
            invulnerable = 0;
            hurtServer(level, source, amount);
        }
    }

    private void startDying(ServerLevel level) {
        if (isDying()) return;
        deathTicks = 0;
        entityData.set(DATA_DEATH, 1);
        entityData.set(DATA_JET, false);
        jetRequested = false;
        Player owner = getOwner();
        if (owner != null) {
            RECOVERY.put(owner.getUUID(), level.getGameTime() + Math.round(GLConfig.KRAKEN_RECOVERY_SECONDS.get() * 20));
            owner.displayClientMessage(Component.translatable("message.greenlantern.kraken_defeated",
                    GLConfig.KRAKEN_RECOVERY_SECONDS.get().intValue()).withStyle(ChatFormatting.DARK_AQUA), true);
        }
        ejectPassengers();
        Vec3 c = bodyPoint(0, 0, 0, 1.0F);
        level.playSound(null, c.x, c.y, c.z, ModSounds.KRAKEN_DEATH.get(), SoundSource.NEUTRAL, 3.0F, 1.0F);
    }

    private void tickDying(ServerLevel level) {
        deathTicks++;
        if (deathTicks % 6 == 0) {
            Vec3 c = bodyPoint(0, 0, 0, 1.0F);
            level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 10, 2.0, 1.5, 2.0, 0.03);
            if (isInWater()) level.sendParticles(ParticleTypes.BUBBLE, c.x, c.y, c.z, 12, 2.0, 1.5, 2.0, 0.05);
        }
        if (deathTicks >= DEATH_TICKS) {
            Vec3 c = bodyPoint(0, -1.0, 0, 1.0F);
            level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 160, 3.0, 2.0, 3.0, 0.08);
            level.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z, 40, 3.0, 1.0, 3.0, 0.05);
            discard();
        }
    }

    // ---- tick -------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();

        swimBlendO = swimBlend;
        swimBlend = Mth.approach(swimBlend, isSwimmingMode() ? 1.0F : 0.0F, 0.04F);

        if (isLocalInstanceAuthoritative()) {
            if (jumpCooldown > 0) jumpCooldown--;
            LivingEntity rider = getControllingPassenger();
            if (isDying()) {
                Vec3 m = getDeltaMovement();
                double vy = isInWater() ? -0.04 : (onGround() ? 0.0 : Math.max(m.y - GRAVITY, -1.5));
                setDeltaMovement(m.x * 0.8, vy, m.z * 0.8);
            } else if (rider != null) {
                drive(rider.zza, rider.xxa, SidedHooks.sprintKeyDown.getAsBoolean(), SidedHooks.jumpKeyDown.getAsBoolean(),
                        rider.getYRot(), rider.getXRot());
            } else if (!level().isClientSide()) {
                guard((ServerLevel) level());
            } else {
                drive(0, 0, false, false, getYRot(), 0);
            }
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        resetFallDistance();

        if (level().isClientSide()) {
            clientTick();
        } else {
            serverTick((ServerLevel) level());
        }
    }

    /** Water height over the feet (0 on dry land). */
    private double waterDepth() {
        return getFluidHeight(FluidTags.WATER);
    }

    /**
     * Moves the Kraken (on the controlling side): forward/strafe input, sprint, jump/rise and where
     * to head (yaw and, when swimming, pitch).
     */
    private void drive(float forward, float strafe, boolean boost, boolean rise, float aimYaw, float aimPitch) {
        Vec3 motion = getDeltaMovement();
        if (isSwimmingMode()) {
            double depth = waterDepth();
            float yaw = Mth.approachDegrees(getYRot(), aimYaw, 4.5F);
            float pitchGoal = forward > 0 ? Mth.clamp(aimPitch, -55.0F, 55.0F) : 0.0F;
            float pitch = Mth.approach(getXRot(), pitchGoal, 3.5F);
            setYRot(yaw);
            setXRot(pitch);
            double top = GLConfig.KRAKEN_SWIM_SPEED.get() * (boost ? 1.35 : 1.0);
            if (forward > 0) {
                swimSpeed = Math.min(top, swimSpeed + top / 22.0);
            } else if (forward < 0) {
                swimSpeed = Math.max(-0.2, swimSpeed - 0.06);
            } else {
                swimSpeed *= 0.94;
            }
            walkSpeed = 0;
            Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            if (side.lengthSqr() > 1.0E-4) side = side.normalize();
            Vec3 wish = dir.scale(swimSpeed).add(side.scale(strafe * -0.3)).add(0, rise ? 0.35 : 0.0, 0);
            // It lives in the deep: it can rise to the surface but not out of the water.
            if (depth < 4.5 && wish.y > 0) wish = new Vec3(wish.x, 0.0, wish.z);
            if (depth < 3.0) wish = wish.add(0, -0.08, 0);
            setDeltaMovement(motion.lerp(wish, 0.22));
            return;
        }

        // On land: slow, heavy turns and steps.
        swimSpeed = 0;
        boolean moving = forward != 0 || strafe != 0;
        float yaw = Mth.approachDegrees(getYRot(), aimYaw, moving ? 2.2F : 1.4F);
        setYRot(yaw);
        setXRot(Mth.approach(getXRot(), 0.0F, 3.0F));
        double top = GLConfig.KRAKEN_WALK_SPEED.get() * (boost && forward > 0 ? 1.35 : 1.0);
        double want = forward > 0 ? top : (forward < 0 ? -top * 0.45 : 0.0);
        double accel = want > walkSpeed ? top / 30.0 : top / 18.0;
        walkSpeed = walkSpeed < want ? Math.min(want, walkSpeed + accel) : Math.max(want, walkSpeed - accel);
        Vec3 wish = new Vec3(strafe * top * 0.4, 0, walkSpeed).yRot(-yaw * Mth.DEG_TO_RAD);
        boolean wet = isInWater();
        double vy;
        if (onGround()) {
            vy = 0.0;
            if (rise && jumpCooldown == 0) {
                // A heavy leap.
                vy = 0.72;
                jumpCooldown = 30;
            }
        } else {
            vy = Math.max(motion.y - (wet ? GRAVITY * 0.6 : GRAVITY), -2.0);
        }
        double drag = wet ? 0.85 : 1.0;
        setDeltaMovement(wish.x * drag, vy, wish.z * drag);
    }

    /** On its own (server): fight the monsters around Aquaman, or stay close to him. */
    private void guard(ServerLevel level) {
        Player owner = getOwner();
        if (owner == null) {
            drive(0, 0, false, false, getYRot(), 0);
            return;
        }
        if (tickCount % 10 == 0 || (target != null && !target.isAlive())) target = findTarget(level, owner);
        float forward = 0;
        float yaw = getYRot();
        float pitch = 0;
        Vec3 goal = null;
        double stopAt = 0;
        if (target != null) {
            goal = target.position().add(0, target.getBbHeight() * 0.5, 0);
            stopAt = 8.5;
        } else if (distanceToSqr(owner) > 16 * 16) {
            goal = owner.position();
            stopAt = 10.0;
        }
        if (goal != null) {
            Vec3 from = bodyPoint(0, 0, 0, 1.0F);
            Vec3 to = goal.subtract(from);
            yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
            pitch = (float) (-Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
            double dist = isSwimmingMode() ? to.length() : to.horizontalDistance();
            if (dist > stopAt) forward = 1;
            if (target != null && dist < 11.0 && Math.abs(Mth.wrapDegrees(yaw - getYRot())) < 30.0F) tentacleSlam();
        }
        drive(forward, 0, false, false, yaw, pitch);
    }

    private @Nullable LivingEntity findTarget(ServerLevel level, Player owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker.distanceToSqr(owner) < 24 * 24 && !SeaCall.isCalledBy(attacker, owner)) {
            return attacker;
        }
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(18),
                e -> e instanceof Enemy && e.isAlive() && !e.isInvisible());
        enemies.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        return enemies.isEmpty() ? null : enemies.getFirst();
    }

    // ---- server -------------------------------------------------------------------------------------

    private void serverTick(ServerLevel level) {
        if (invulnerable > 0) invulnerable--;
        if (hurtSoundCooldown > 0) hurtSoundCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (isDying()) {
            tickDying(level);
            return;
        }

        // Walking or swimming, decided by how deep the water is.
        double depth = waterDepth();
        boolean swimming = isSwimmingMode();
        if (!swimming && depth >= 5.5) {
            entityData.set(DATA_SWIMMING, true);
            setDeltaMovement(getDeltaMovement().multiply(1, 0.2, 1));
        } else if (swimming && depth < 2.0) {
            entityData.set(DATA_SWIMMING, false);
            setXRot(0.0F);
        }

        // Its wounds heal in the water.
        if (isInWater() && tickCount % 20 == 0 && getHealth() < maxHealth()) {
            setHealth(getHealth() + GLConfig.KRAKEN_REGEN_IN_WATER.get().floatValue());
        }

        Player owner = getOwner();
        unriddenTicks = isVehicle() ? 0 : unriddenTicks + 1;
        if (unriddenTicks > MAX_UNRIDDEN_TICKS) {
            if (owner != null) owner.displayClientMessage(Component.translatable("message.greenlantern.kraken_left").withStyle(ChatFormatting.DARK_AQUA), true);
            retreat();
            return;
        }
        if (getFirstPassenger() instanceof ServerPlayer rider) {
            rider.setAirSupply(rider.getMaxAirSupply());
            if (rider.isOnFire() && isInWater()) rider.clearFire();
        }

        if (--roarCooldown <= 0) {
            roarCooldown = 300 + random.nextInt(400);
            Vec3 c = bodyPoint(0, 0, 0, 1.0F);
            level.playSound(null, c.x, c.y, c.z, ModSounds.KRAKEN_ROAR.get(), SoundSource.NEUTRAL, 2.5F, 0.9F + random.nextFloat() * 0.2F);
        }

        trackLanding(level);
        if (GLConfig.KRAKEN_TRAMPLES_LEAVES.get() && tickCount % 2 == 0 && owner != null && !isSwimmingMode()) trampleLeaves(level, owner);
        shoveCreatures();
        if (slamPending >= 0 && --slamPending < 0) slamImpact(level);
        tickJet(level);
    }

    private void trackLanding(ServerLevel level) {
        double y = getY();
        double speedY = Double.isNaN(lastServerY) ? 0 : y - lastServerY;
        lastServerY = y;
        if (isSwimmingMode()) {
            airTicks = 0;
            return;
        }
        if (!onGround()) {
            airTicks++;
            lastAirSpeedY = speedY;
            return;
        }
        if (airTicks > 4 && lastAirSpeedY < -0.4) {
            float strength = (float) Mth.clamp(0.5 + (-lastAirSpeedY - 0.4), 0.5, 1.0);
            groundPound(level, position(), 6.0 + 3.0 * strength, GLConfig.KRAKEN_TENTACLE_DAMAGE.get().floatValue() * 0.6F * strength, strength);
            level.broadcastEntityEvent(this, EVENT_LANDING);
        }
        airTicks = 0;
        lastAirSpeedY = 0;
    }

    /** Bursts through leaves (trees don't stop a giant). */
    private void trampleLeaves(ServerLevel level, Player owner) {
        AABB box = getBoundingBox().inflate(0.5, 0.0, 0.5);
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY + 0.5, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES) || !level.mayInteract(owner, pos)) continue;
            level.destroyBlock(pos, true, owner);
            if (++broken >= 24) return;
        }
    }

    /** Creatures under its body get shoved aside. */
    private void shoveCreatures() {
        AABB under = getBoundingBox().setMaxY(getY() + (isSwimmingMode() ? SWIM_HEIGHT : 5.0));
        Player owner = getOwner();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, under, e -> e.isAlive() && e != owner && !hasPassenger(e))) {
            Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 0.3, 0.05, away.z * 0.3);
            e.hurtMarked = true;
        }
    }

    // ---- attacks ------------------------------------------------------------------------------------

    /** Server: starts the tentacle slam if it is ready. Returns whether it started. */
    public boolean tentacleSlam() {
        if (!(level() instanceof ServerLevel level) || isDying() || slamCooldown > 0 || slamPending >= 0) return false;
        slamCooldown = SLAM_COOLDOWN;
        slamPending = SLAM_HIT;
        slamLeft = !slamLeft;
        // Low bit: which tentacle; the counter itself restarts the animation on the clients.
        int next = (entityData.get(DATA_SLAM) & ~1) + 2 + (slamLeft ? 1 : 0);
        entityData.set(DATA_SLAM, next);
        Vec3 c = bodyPoint(0, 1.0, 0, 1.0F);
        level.playSound(null, c.x, c.y, c.z, ModSounds.KRAKEN_SLAM.get(), SoundSource.NEUTRAL, 2.2F, 0.9F + random.nextFloat() * 0.15F);
        return true;
    }

    /** Where the hunting tentacle comes down. */
    public Vec3 slamPoint() {
        boolean left = level().isClientSide() ? slamLeftClient : slamLeft;
        Vec3 p = bodyPoint(9.5, -5.2, left ? 1.2 : -1.2, 1.0F);
        if (isSwimmingMode()) return p;
        // Find the ground under the swing.
        BlockHitResult ground = level().clip(new ClipContext(p.add(0, 3.0, 0), p.add(0, -6.0, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return ground.getType() == HitResult.Type.MISS ? p.add(0, -3.0, 0) : ground.getLocation();
    }

    private void slamImpact(ServerLevel level) {
        Vec3 at = slamPoint();
        groundPound(level, at, 4.5, GLConfig.KRAKEN_TENTACLE_DAMAGE.get().floatValue(), 1.0F);
        level.broadcastEntityEvent(this, EVENT_SLAM_IMPACT);
        if (isInWater()) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 40, 1.5, 1.0, 1.5, 0.2);
            level.sendParticles(ParticleTypes.BUBBLE, at.x, at.y, at.z, 30, 1.5, 1.0, 1.5, 0.2);
        }
    }

    /** Hurts and throws back everything around the point (tentacle slam, landing). */
    private void groundPound(ServerLevel level, Vec3 center, double radius, float damage, float strength) {
        Player owner = getOwner();
        DamageSource source = ModDamageTypes.kraken(level, this, owner);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, radius * 0.75, radius),
                e -> e.isAlive() && e != owner && !hasPassenger(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && (owner == null || !SeaCall.isCalledBy(e, owner)))) {
            double dist = e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(center);
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.5 * dist / radius);
            if (damage > 0) e.hurtServer(level, source, damage * falloff);
            Vec3 away = new Vec3(e.getX() - center.x, 0, e.getZ() - center.z);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : away.normalize();
            e.push(away.x * 1.3 * falloff * strength, 0.55 * falloff * strength + 0.15, away.z * 1.3 * falloff * strength);
            e.hurtMarked = true;
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.KRAKEN_STEP.get(), SoundSource.NEUTRAL, 2.5F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 2, 1.0, 0.3, 1.0, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.8, center.z, 6, radius * 0.4, 0.3, radius * 0.4, 0);
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.5, center.z, 60, radius * 0.4, 0.4, radius * 0.4, 0.4);
        for (int i = 0; i < 32; i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double r = 0.5 + random.nextDouble() * radius;
            double x = center.x + Math.cos(a) * r;
            double z = center.z + Math.sin(a) * r;
            BlockState state = level.getBlockState(BlockPos.containing(x, center.y - 0.5, z));
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 4, 0.2, 0.1, 0.2, 0.3);
            }
        }
    }

    /** Server: the rider pressed or released the water jet. */
    public void setJetFiring(boolean firing) {
        if (isDying()) firing = false;
        if (firing && !jetRequested && level() instanceof ServerLevel level) {
            Vec3 at = siphon();
            level.playSound(null, at.x, at.y, at.z, ModSounds.KRAKEN_JET.get(), SoundSource.NEUTRAL, 2.2F, 1.0F);
            jetTicks = 0;
        }
        jetRequested = firing;
    }

    /** Where the rider aims the jet: from the siphon towards the rider's crosshair. */
    private Vec3 jetDirection(Player rider) {
        Vec3 eye = rider.getEyePosition();
        Vec3 look = rider.getLookAngle();
        double range = GLConfig.KRAKEN_JET_RANGE.get();
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult block = level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        Vec3 aim = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        Vec3 dir = aim.subtract(siphon());
        return dir.lengthSqr() < 1.0E-4 ? look : dir.normalize();
    }

    private void tickJet(ServerLevel level) {
        Player rider = getFirstPassenger() instanceof Player p ? p : null;
        boolean firing = jetRequested && rider != null;
        if (firing && tickCount % 20 == 0 && !rider.isCreative()) {
            ItemStack emblem = AquamanHelper.findEmblem(rider);
            int cost = GLConfig.KRAKEN_JET_COST_PER_SECOND.get();
            if (cost > 0 && (emblem.isEmpty() || !SeaForce.tryConsume(emblem, cost))) {
                if (rider instanceof ServerPlayer sp) AquamanServer.notifyNoSeaForce(sp);
                jetRequested = false;
                firing = false;
            }
        }
        entityData.set(DATA_JET, firing);
        if (!firing) return;
        jetTicks++;
        Vec3 from = siphon();
        Vec3 dir = jetDirection(rider);
        double range = GLConfig.KRAKEN_JET_RANGE.get();
        Vec3 far = from.add(dir.scale(range));
        BlockHitResult block = level.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        entityData.set(DATA_JET_END, new Vector3f((float) end.x, (float) end.y, (float) end.z));

        // Everything along the stream is battered and pushed away.
        if (jetTicks % 4 == 1) {
            DamageSource source = ModDamageTypes.kraken(level, this, rider);
            AABB sweep = new AABB(from, end).inflate(1.4);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep,
                    e -> e.isAlive() && e != rider && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && !SeaCall.isCalledBy(e, rider))) {
                Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
                double t = Mth.clamp(c.subtract(from).dot(dir), 0.0, end.distanceTo(from));
                if (c.distanceTo(from.add(dir.scale(t))) > 1.2 + e.getBbWidth() * 0.5) continue;
                e.invulnerableTime = 0;
                e.hurtServer(level, source, GLConfig.KRAKEN_JET_DAMAGE.get().floatValue());
                e.push(dir.x * 1.0, dir.y * 0.6 + 0.25, dir.z * 1.0);
                e.hurtMarked = true;
                if (e.isOnFire()) e.clearFire();
            }
        }
        if (jetTicks % 2 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 24, 0.8, 0.6, 0.8, 0.5);
            level.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 3, 0.5, 0.4, 0.5, 0.05);
        }
        // The jet puts out fires where it lands.
        BlockPos hit = BlockPos.containing(end);
        for (BlockPos pos : BlockPos.betweenClosed(hit.offset(-1, -1, -1), hit.offset(1, 1, 1))) {
            if (level.getBlockState(pos).is(BlockTags.FIRE)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        if (jetTicks % 28 == 0) {
            level.playSound(null, from.x, from.y, from.z, ModSounds.KRAKEN_JET.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        }
    }

    // ---- client -------------------------------------------------------------------------------------

    private void clientTick() {
        walkPhaseO = walkPhase;
        walkAmountO = walkAmount;
        swimWaveO = swimWave;
        swimPowerO = swimPower;
        jetBlendO = jetBlend;
        bobO = bob;
        squashO = squash;
        jetEndO = jetEnd;

        double dx = getX() - xo;
        double dy = getY() - yo;
        double dz = getZ() - zo;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean swimming = isSwimmingMode();

        // Land: the arms walk.
        walkAmount = Mth.approach(walkAmount, swimming || isDying() ? 0.0F : (float) Math.min(1.0, flat / 0.12), 0.06F);
        if (!swimming && flat > 0.004) {
            float yawRad = getYRot() * Mth.DEG_TO_RAD;
            double along = -dx * Mth.sin(yawRad) + dz * Mth.cos(yawRad);
            walkPhase += (float) (Math.signum(along == 0 ? 1 : along) * Math.max(flat, 0.02) * Mth.TWO_PI / STRIDE);
        } else if (walkAmount < 0.05F) {
            float settle = Math.round(walkPhase / Mth.PI) * Mth.PI;
            walkPhase = Mth.approach(walkPhase, settle, 0.05F);
        }
        float u = Mth.positiveModulo(walkPhase / Mth.PI, 1.0F);
        // Heavy: the body slumps right after each step, then rises as the other arms push.
        bob = walkAmount * 0.45F * Mth.sin(u * Mth.PI) * (1.0F - u) * 1.6F;

        // Water: the arms ripple and the mantle pumps, faster with speed.
        float power = swimming ? (float) Mth.clamp(speed / GLConfig.KRAKEN_SWIM_SPEED.get(), 0.0, 1.0) : 0.0F;
        swimPower = Mth.approach(swimPower, power, 0.05F);
        swimWave += 0.09F + swimPower * 0.35F;
        jetBlend = Mth.approach(jetBlend, isJetting() ? 1.0F : 0.0F, 0.15F);
        squash *= 0.85F;

        Vector3fc end = entityData.get(DATA_JET_END);
        jetEnd = new Vec3(end.x(), end.y(), end.z());
        if (jetEndO.equals(Vec3.ZERO)) jetEndO = jetEnd;

        int slam = entityData.get(DATA_SLAM);
        if (slam != lastSlam) {
            lastSlam = slam;
            slamStartTick = tickCount;
            slamLeftClient = (slam & 1) == 1;
        }
        int hurt = entityData.get(DATA_HURT);
        if (hurt != lastHurt) {
            lastHurt = hurt;
            hurtTicks = 10;
        } else if (hurtTicks > 0) {
            hurtTicks--;
        }
        if (isDying()) {
            if (clientDeathTicks < 0) clientDeathTicks = 0;
            clientDeathTicks++;
        }

        // A heavy step each time a group of arms comes down.
        int step = Mth.floor(walkPhase / Mth.PI);
        if (step != stepIndex) {
            stepIndex = step;
            if (!swimming && walkAmount > 0.25F && onGroundish()) footstep(Math.floorMod(step, 2));
        }
        // A push of the mantle at every swim stroke.
        int pulse = Mth.floor(swimWave / Mth.TWO_PI);
        if (pulse != pulseIndex) {
            pulseIndex = pulse;
            if (swimming && swimPower > 0.3F) swimStroke();
        }
        if (tickCount < EMERGE_TICKS) emergeParticles();
        if (isJetting()) jetParticles();
    }

    private boolean onGroundish() {
        return onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.3, 0));
    }

    /** World position (at the feet's height) of the tip of arm {@code i} standing on land. */
    public Vec3 armTip(int i) {
        double heading = (i + 0.5) * Mth.TWO_PI / 8.0;
        // Model space: arms spread towards (sin, cos), -Z forward; tips are ~5 blocks out.
        Vec3 local = new Vec3(-Math.sin(heading) * 5.0, 0, Math.cos(heading) * 5.0);
        return position().add(local.yRot((180.0F - getYRot()) * Mth.DEG_TO_RAD));
    }

    private void footstep(int group) {
        squash = 1.0F;
        Vec3 center = position();
        level().playLocalSound(center.x, center.y, center.z, ModSounds.KRAKEN_STEP.get(), SoundSource.NEUTRAL,
                2.0F, 0.75F + random.nextFloat() * 0.15F, false);
        for (int i = group; i < 8; i += 2) {
            Vec3 tip = armTip(i);
            BlockState below = level().getBlockState(BlockPos.containing(tip.x, tip.y - 0.5, tip.z));
            if (below.getRenderShape() != RenderShape.INVISIBLE) {
                for (int k = 0; k < 6; k++) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), tip.x + (random.nextDouble() - 0.5) * 1.2,
                            tip.y + 0.1, tip.z + (random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 0.3, 0.15, (random.nextDouble() - 0.5) * 0.3);
                }
            }
            level().addParticle(isInWater() ? ParticleTypes.SPLASH : ParticleTypes.POOF, tip.x, tip.y + 0.2, tip.z, 0, 0.05, 0);
        }
        shakeNearby(center, 0.22F, 32.0, 8);
    }

    private void swimStroke() {
        Vec3 back = bodyPoint(-6.0, 1.0, 0, 1.0F);
        for (int i = 0; i < 14; i++) {
            level().addParticle(ParticleTypes.BUBBLE, back.x + random.nextGaussian() * 1.2, back.y + random.nextGaussian() * 1.0,
                    back.z + random.nextGaussian() * 1.2, 0, 0.05, 0);
        }
        level().playLocalSound(back.x, back.y, back.z, ModSounds.KRAKEN_SWIM.get(), SoundSource.NEUTRAL, 1.4F,
                0.8F + swimPower * 0.3F + random.nextFloat() * 0.1F, false);
    }

    private void emergeParticles() {
        for (int i = 0; i < 6; i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double r = random.nextDouble() * 4.0;
            level().addParticle(isInWater() ? ParticleTypes.BUBBLE_COLUMN_UP : ParticleTypes.SPLASH,
                    getX() + Math.cos(a) * r, getY() + random.nextDouble() * 2.0, getZ() + Math.sin(a) * r, 0, 0.3, 0);
        }
    }

    private void jetParticles() {
        Vec3 from = siphon();
        Vec3 to = jetEnd;
        Vec3 d = to.subtract(from);
        for (int i = 0; i < 4; i++) {
            Vec3 p = from.add(d.scale(random.nextDouble()));
            level().addParticle(isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.FALLING_WATER,
                    p.x + random.nextGaussian() * 0.3, p.y + random.nextGaussian() * 0.3, p.z + random.nextGaussian() * 0.3, 0, 0, 0);
        }
        level().addParticle(ParticleTypes.SPLASH, from.x, from.y, from.z, d.x * 0.02, 0.05, d.z * 0.02);
        if (tickCount % 3 == 0) shakeNearby(from, 0.05F, 12.0, 4);
    }

    private void shakeNearby(Vec3 where, float strength, double range, int ticks) {
        for (Player player : level().players()) {
            if (!player.isLocalPlayer()) continue;
            double dist = player.position().distanceTo(where);
            if (player.getVehicle() == this) dist = 0;
            if (dist < range) SidedHooks.cameraShake.shake((float) (strength * (1.0 - dist / range)), ticks);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_LANDING) {
            squash = 1.0F;
            shakeNearby(position(), 0.9F, 48.0, 18);
        } else if (id == EVENT_SLAM_IMPACT) {
            shakeNearby(slamPoint(), 0.6F, 36.0, 12);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ---- client animation helpers -------------------------------------------------------------------

    public Vec3 jetEnd(float partialTick) {
        return jetEndO.lerp(jetEnd, partialTick);
    }

    /** Ticks since the last slam started (large when none is playing). */
    public float slamTime(float partialTick) {
        return tickCount - slamStartTick + partialTick;
    }
}
