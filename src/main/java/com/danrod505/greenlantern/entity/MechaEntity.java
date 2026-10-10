package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The giant hard-light mecha: a 10 block tall armored suit. The ring bearer sits in the cockpit in
 * its chest, fully protected (the mecha absorbs every hit with ring energy).
 * <ul>
 *     <li>W/S walk, A/D sidestep, the legs slowly turn towards where the pilot looks (the torso turns first).</li>
 *     <li>Sprint makes it run on the ground and fires the afterburner in the air.</li>
 *     <li>Hold jump: the back thrusters spool up and lift it off. In the air W flies where the pilot
 *     looks (look down to descend) and releasing jump makes it hover, slowly sinking.</li>
 *     <li>Hold right click: twin lasers from the arm cannons. Left click: a salvo of homing missiles
 *     from the shoulder pods.</li>
 *     <li>Hard landings send out a shockwave. Sneak to climb out.</li>
 * </ul>
 * Movement is simulated by the controlling client and synced to the server (like the drill), the
 * weapons, energy and protection run on the server.
 */
public class MechaEntity extends ConstructEntity {
    public static final float WIDTH = 3.6F;
    public static final float HEIGHT = 10.0F;
    /** Where the pilot sits, inside the chest. */
    public static final float SEAT_HEIGHT = 5.8F;
    /** Height of the hip joints (top of the legs) and of the waist joint the torso turns around. */
    public static final float HIP_HEIGHT = 4.4F;
    public static final float WAIST_HEIGHT = 5.0F;
    /** Shoulder joints, relative to the waist. */
    public static final float SHOULDER_X = 1.75F;
    public static final float SHOULDER_Y = 2.45F;
    /** Length of a straight arm, from the shoulder joint to the cannon at the fist. */
    public static final float ARM_LENGTH = 3.95F;
    /** How far the torso turns before the legs have to follow. */
    public static final float MAX_TWIST = 70.0F;
    /** Distance covered by one full walk cycle (two steps). */
    public static final float STRIDE = 6.0F;

    private static final double GRAVITY = 0.1;
    private static final double TERMINAL_SPEED = 2.5;
    private static final double GROUND_FRICTION = 0.9;
    private static final double FLIGHT_DRAG = 0.95;
    /** Lift of fully spooled thrusters (more than gravity: the excess is the climb acceleration). */
    private static final double LIFT = 0.16;
    private static final double CLIMB = 0.045;
    private static final double MAX_CLIMB = 0.7;
    private static final double HOVER_SINK = 0.004;
    private static final int MISSILE_COOLDOWN = 50;
    private static final int MISSILE_INTERVAL = 3;

    public static final byte EVENT_LANDING = 60;

    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(MechaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LASER = SynchedEntityData.defineId(MechaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3fc> DATA_LASER_END = SynchedEntityData.defineId(MechaEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_SALVO = SynchedEntityData.defineId(MechaEntity.class, EntityDataSerializers.INT);

    private static final AttributeModifier CAMERA_DISTANCE = new AttributeModifier(GreenLantern.id("mecha_camera"), 8.0, AttributeModifier.Operation.ADD_VALUE);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    // Controlling client: flight state.
    private float spool;
    private boolean flying;

    // Server: weapons and landing.
    private boolean laserRequested;
    private int missilesQueued;
    private int missileCooldown;
    private @Nullable Entity missileTarget;
    private Vec3 missileAim = Vec3.ZERO;
    private double lastServerY = Double.NaN;
    private int airTicks;
    private double lastAirSpeedY;
    private int laserTicks;

    // Client: animation.
    public float walkPhase;
    public float walkPhaseO;
    public float walkAmount;
    public float walkAmountO;
    public float flyPose;
    public float flyPoseO;
    public float thrust;
    public float thrustO;
    public float aimBlend;
    public float aimBlendO;
    public float squash;
    public float squashO;
    public float recoil;
    public float recoilO;
    public Vec3 laserEnd = Vec3.ZERO;
    public Vec3 laserEndO = Vec3.ZERO;
    private int lastSalvo;
    private int stepIndex;

    public MechaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static MechaEntity create(Level level, Player owner) {
        MechaEntity mecha = new MechaEntity(ModEntities.MECHA.get(), level);
        mecha.setOwner(owner);
        return mecha;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLYING, false);
        builder.define(DATA_LASER, false);
        builder.define(DATA_LASER_END, new Vector3f());
        builder.define(DATA_SALVO, 0);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // ---- State --------------------------------------------------------------------------------------

    public boolean isFlying() {
        return level().isClientSide() && isLocalInstanceAuthoritative() ? flying : entityData.get(DATA_FLYING);
    }

    public boolean isFiringLaser() {
        return entityData.get(DATA_LASER);
    }

    /** Pilot's aim yaw, which the torso follows. */
    public float aimYaw(float partialTick) {
        Player pilot = getOwner();
        return pilot != null && pilot.getVehicle() == this ? pilot.getViewYRot(partialTick) : getYRot(partialTick);
    }

    /** Torso twist relative to the legs, limited to {@link #MAX_TWIST}. */
    public float twist(float partialTick) {
        return Mth.clamp(Mth.wrapDegrees(aimYaw(partialTick) - getYRot(partialTick)), -MAX_TWIST, MAX_TWIST);
    }

    /** Converts a point of the torso (relative to the waist, +Z forward, +X left) to world coordinates. */
    public Vec3 torsoPoint(double x, double y, double z) {
        float yaw = getYRot() + twist(1.0F);
        return position().add(new Vec3(x, WAIST_HEIGHT + y, z).yRot(-yaw * Mth.DEG_TO_RAD));
    }

    // ---- Riding -------------------------------------------------------------------------------------

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && isOwnedBy(passenger);
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, SEAT_HEIGHT, 0.05).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Climb out next to a foot, on whichever side is free.
        for (Vec3 offset : new Vec3[] {new Vec3(-2.6, 0, 0), new Vec3(2.6, 0, 0), new Vec3(0, 0, -2.8), new Vec3(0, 0, 2.8)}) {
            Vec3 spot = position().add(offset.yRot(-getYRot() * Mth.DEG_TO_RAD)).add(0, 0.1, 0);
            if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(spot))) return spot;
        }
        return position().add(0, HEIGHT + 0.1, 0);
    }

    @Override
    public float maxUpStep() {
        return 2.1F;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    @Override
    protected boolean ownerValid() {
        // The mecha only exists while its owner pilots it.
        Player owner = getOwner();
        return super.ownerValid() && owner != null && owner.getVehicle() == this;
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
        if (!level().isClientSide() && getPassengers().isEmpty()) {
            dissipate();
        }
    }

    /** Pulls the third person camera back so the whole mecha fits on screen. */
    private static void setCameraDistance(Player player, boolean piloting) {
        AttributeInstance camera = player.getAttribute(Attributes.CAMERA_DISTANCE);
        if (camera == null) return;
        if (piloting) {
            camera.addOrUpdateTransientModifier(CAMERA_DISTANCE);
        } else {
            camera.removeModifier(CAMERA_DISTANCE.id());
        }
    }

    // ---- Tick -----------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();

        if (isLocalInstanceAuthoritative()) {
            control();
            move(MoverType.SELF, getDeltaMovement());
            if (onGround()) flying = false;
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

    private void control() {
        LivingEntity driver = getControllingPassenger();
        Vec3 motion = getDeltaMovement();
        if (driver == null) {
            spool = 0;
            setDeltaMovement(motion.x * 0.8, onGround() ? 0 : Math.max(motion.y - GRAVITY, -TERMINAL_SPEED), motion.z * 0.8);
            return;
        }
        boolean jump = SidedHooks.jumpKeyDown.getAsBoolean();
        boolean boost = SidedHooks.sprintKeyDown.getAsBoolean();
        float forward = driver.zza;
        float strafe = driver.xxa;

        // Thrusters take a moment to spool up (and to wind down).
        spool = jump ? Math.min(1.0F, spool + 0.05F) : Math.max(0.0F, spool - 0.04F);
        if (!onGround() && spool > 0.05F) flying = true;

        // Heavy turning: the torso turns first, the legs follow when moving or when it twists too far.
        float aim = driver.getYRot();
        float diff = Mth.wrapDegrees(aim - getYRot());
        boolean moving = forward != 0 || strafe != 0;
        if (flying || moving || Math.abs(diff) > MAX_TWIST - 10.0F) {
            float rate = flying ? 3.5F : (moving ? 2.4F : 1.6F);
            float yaw = Mth.approachDegrees(getYRot(), aim, rate);
            setYRot(yaw);
            yRotO = yaw;
        }
        float yaw = getYRot();

        double vx;
        double vy;
        double vz;
        if (flying) {
            // W flies along the aim (look down to descend), jump climbs, otherwise it hovers and slowly sinks.
            Vec3 look = Vec3.directionFromRotation(driver.getXRot(), aim);
            Vec3 wish = look.scale(forward > 0 ? forward : forward * 0.4)
                    .add(new Vec3(strafe * 0.5, 0, 0).yRot(-aim * Mth.DEG_TO_RAD));
            double top = GLConfig.MECHA_FLIGHT_SPEED.get() * (boost && forward > 0 ? 1.7 : 1.0);
            double accel = top * (1.0 - FLIGHT_DRAG);
            vx = motion.x * FLIGHT_DRAG + wish.x * accel;
            vz = motion.z * FLIGHT_DRAG + wish.z * accel;
            vy = motion.y * FLIGHT_DRAG + wish.y * accel + (jump ? CLIMB * spool : -HOVER_SINK);
            vy = Math.min(vy, MAX_CLIMB);
        } else {
            boolean ground = onGround();
            Vec3 wish = new Vec3(strafe * 0.45, 0, forward > 0 ? forward : forward * 0.55).yRot(-yaw * Mth.DEG_TO_RAD);
            if (wish.lengthSqr() > 1.0) wish = wish.normalize();
            double top = GLConfig.MECHA_WALK_SPEED.get() * (boost && forward > 0 ? 1.6 : 1.0);
            double friction = isInWater() ? 0.8 : ground ? GROUND_FRICTION : 0.98;
            double accel = top * (1.0 - GROUND_FRICTION) * (ground ? 1.0 : 0.25);
            vx = motion.x * friction + wish.x * accel;
            vz = motion.z * friction + wish.z * accel;
            vy = Math.max(motion.y - GRAVITY, -TERMINAL_SPEED) + LIFT * spool;
            if (ground && vy < 0) vy = 0;
        }
        if (isInWater()) vy = Math.max(vy, -0.3);
        setDeltaMovement(vx, vy, vz);
    }

    // ---- Client -------------------------------------------------------------------------------------

    private void clientTick() {
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        double dx = getX() - xo;
        double dz = getZ() - zo;
        double along = -dx * Mth.sin(yawRad) + dz * Mth.cos(yawRad);
        double speed = Math.sqrt(dx * dx + dz * dz);
        boolean fly = isFlying();

        walkPhaseO = walkPhase;
        walkAmountO = walkAmount;
        flyPoseO = flyPose;
        thrustO = thrust;
        aimBlendO = aimBlend;
        squashO = squash;
        recoilO = recoil;
        laserEndO = laserEnd;

        walkAmount = Mth.approach(walkAmount, fly ? 0.0F : (float) Math.min(1.0, speed / 0.18), 0.08F);
        if (!fly && speed > 0.005) {
            walkPhase += (float) (Math.signum(along == 0 ? 1 : along) * speed * Mth.TWO_PI / STRIDE);
        } else if (walkAmount < 0.05F) {
            // Settle into the standing pose.
            float target = Math.round(walkPhase / Mth.PI) * Mth.PI;
            walkPhase = Mth.approach(walkPhase, target, 0.1F);
        }
        flyPose = Mth.approach(flyPose, fly ? 1.0F : 0.0F, 0.06F);
        float thrustTarget = fly ? (isLocalInstanceAuthoritative() ? 0.45F + 0.55F * spool : 0.8F) : spool * 0.6F;
        thrust = Mth.approach(thrust, thrustTarget, 0.1F);
        aimBlend = Mth.approach(aimBlend, isFiringLaser() ? 1.0F : 0.0F, 0.15F);
        squash = squash * 0.85F;
        recoil = recoil * 0.8F;
        Vector3fc end = entityData.get(DATA_LASER_END);
        laserEnd = new Vec3(end.x(), end.y(), end.z());
        if (laserEndO.equals(Vec3.ZERO)) laserEndO = laserEnd;

        int salvo = entityData.get(DATA_SALVO);
        if (salvo != lastSalvo) {
            lastSalvo = salvo;
            recoil = 1.0F;
        }

        // A heavy footstep each time a foot comes down.
        int step = Mth.floor(walkPhase / Mth.PI);
        if (step != stepIndex) {
            stepIndex = step;
            if (!fly && onGroundish() && walkAmount > 0.2F) footstep(Math.floorMod(step, 2) == 0 ? 1 : -1);
        }

        if (thrust > 0.05F) thrusterParticles();
        if (isFiringLaser() && tickCount % 2 == 0) {
            level().addParticle(ModParticles.SPARK.get(), laserEnd.x, laserEnd.y, laserEnd.z,
                    (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.4);
            level().addParticle(ModParticles.GLOW.get(), laserEnd.x, laserEnd.y, laserEnd.z, 0, 0.03, 0);
        }
        if (tickCount < 30) {
            // Materializing: light runs up the frame.
            double h = HEIGHT * tickCount / 30.0;
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                level().addParticle(ModParticles.GLOW.get(), getX() + Math.cos(a) * 1.6, getY() + h, getZ() + Math.sin(a) * 1.6, 0, 0.02, 0);
            }
        }
    }

    /** Remote clients don't know onGround reliably; a quick look under the feet does. */
    private boolean onGroundish() {
        return onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.3, 0));
    }

    private void footstep(int side) {
        Vec3 foot = position().add(new Vec3(side * 0.8, 0, 0.3).yRot(-getYRot() * Mth.DEG_TO_RAD));
        level().playLocalSound(foot.x, foot.y, foot.z, ModSounds.MECHA_STEP.get(), SoundSource.PLAYERS,
                1.4F, 0.85F + random.nextFloat() * 0.2F, false);
        BlockState below = level().getBlockState(BlockPos.containing(foot.x, foot.y - 0.5, foot.z));
        if (below.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < 14; i++) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), foot.x + (random.nextDouble() - 0.5) * 1.4,
                        foot.y + 0.1, foot.z + (random.nextDouble() - 0.5) * 1.6, (random.nextDouble() - 0.5) * 0.3, 0.15, (random.nextDouble() - 0.5) * 0.3);
            }
        }
        level().addParticle(ParticleTypes.POOF, foot.x, foot.y + 0.1, foot.z, 0, 0.02, 0);
        shakeNearby(foot, 0.16F, 18.0, 6);
    }

    private void shakeNearby(Vec3 where, float strength, double range, int ticks) {
        for (Player player : level().players()) {
            if (!player.isLocalPlayer()) continue;
            double dist = player.position().distanceTo(where);
            if (player.getVehicle() == this) dist = 0;
            if (dist < range) SidedHooks.cameraShake.shake((float) (strength * (1.0 - dist / range)), ticks);
        }
    }

    private void thrusterParticles() {
        for (int side = -1; side <= 1; side += 2) {
            Vec3 nozzle = torsoPoint(side * 0.6, 0.55, -1.45);
            Vec3 back = torsoPoint(side * 0.6, -0.6, -2.2).subtract(nozzle).normalize();
            double s = 0.25 + thrust * 0.45;
            level().addParticle(ModParticles.GLOW.get(), nozzle.x, nozzle.y, nozzle.z,
                    back.x * s + (random.nextDouble() - 0.5) * 0.08, back.y * s, back.z * s + (random.nextDouble() - 0.5) * 0.08);
            if (random.nextFloat() < thrust) {
                level().addParticle(ModParticles.SPARK.get(), nozzle.x, nozzle.y, nozzle.z,
                        back.x * s * 1.5 + (random.nextDouble() - 0.5) * 0.2, back.y * s * 1.5, back.z * s * 1.5 + (random.nextDouble() - 0.5) * 0.2);
            }
        }
        // Downwash kicking up dust close to the ground.
        if (thrust > 0.3F && tickCount % 2 == 0 && !level().noCollision(this, getBoundingBox().move(0, -4.0, 0))) {
            for (int i = 0; i < 4; i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * 2.0, getY() + 0.2, getZ() + Math.sin(a) * 2.0,
                        Math.cos(a) * 0.3, 0.02, Math.sin(a) * 0.3);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_LANDING) {
            squash = 1.0F;
            shakeNearby(position(), 0.9F, 40.0, 16);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ---- Server -------------------------------------------------------------------------------------

    private void serverTick(ServerLevel level) {
        Player owner = getOwner();
        if (!(owner instanceof ServerPlayer pilot)) return;
        boolean creative = pilot.isCreative();
        ItemStack ring = ownerRing();

        // Flight state (for every client) from the server's view of the movement.
        boolean airborne = !onGround();
        if (!airborne) {
            entityData.set(DATA_FLYING, false);
        } else if (pilot.getLastClientInput().jump()) {
            entityData.set(DATA_FLYING, true);
        }
        boolean flyingNow = entityData.get(DATA_FLYING);

        if (tickCount % 20 == 0 && !creative) {
            int cost = GLConfig.MECHA_COST_PER_SECOND.get() + (flyingNow ? GLConfig.MECHA_FLIGHT_COST_PER_SECOND.get() : 0)
                    + (laserRequested ? GLConfig.MECHA_LASER_COST_PER_SECOND.get() : 0);
            if (cost > 0 && !RingEnergy.tryConsume(ring, cost)) {
                PowerRingItem.notifyNoEnergy(pilot);
                dissipate();
                return;
            }
        }
        if (flyingNow && tickCount % 20 == 3) {
            level.playSound(null, getX(), getY() + 6, getZ(), ModSounds.MECHA_THRUSTER.get(), SoundSource.PLAYERS, 1.2F, 0.9F + random.nextFloat() * 0.1F);
        }

        // The cockpit keeps its pilot alive: air, no fire.
        pilot.setAirSupply(pilot.getMaxAirSupply());
        if (pilot.isOnFire()) pilot.clearFire();

        trackLanding(level, pilot);
        if (GLConfig.MECHA_TRAMPLES_LEAVES.get() && tickCount % 2 == 0) trampleLeaves(level, pilot);
        shoveCreatures(level, pilot);
        tickLaser(level, pilot);
        tickMissiles(level, pilot);
    }

    private void trackLanding(ServerLevel level, ServerPlayer pilot) {
        double y = getY();
        double speedY = Double.isNaN(lastServerY) ? 0 : y - lastServerY;
        lastServerY = y;
        if (!onGround()) {
            airTicks++;
            lastAirSpeedY = speedY;
            return;
        }
        if (airTicks > 3 && lastAirSpeedY < -0.5) {
            landingShockwave(level, pilot, (float) Mth.clamp(0.4 + (-lastAirSpeedY - 0.5) / 1.5, 0.4, 1.0));
        }
        airTicks = 0;
        lastAirSpeedY = 0;
    }

    private void landingShockwave(ServerLevel level, ServerPlayer pilot, float strength) {
        Vec3 center = position();
        double radius = 4.0 + 4.0 * strength;
        float damage = GLConfig.MECHA_LANDING_DAMAGE.get().floatValue() * strength;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 3.0, radius))) {
            if (target == pilot || !target.isAlive()) continue;
            double dist = Math.sqrt(target.distanceToSqr(center.x, target.getY(), center.z));
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.6 * dist / radius);
            if (damage > 0) target.hurtServer(level, ModDamageTypes.hardLight(level, this, pilot), damage * falloff);
            Vec3 away = new Vec3(target.getX() - center.x, 0, target.getZ() - center.z);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(1.4 * falloff);
            target.push(away.x, 0.6 * falloff + 0.2, away.z);
            target.hurtMarked = true;
        }
        level.broadcastEntityEvent(this, EVENT_LANDING);
        level.playSound(null, center.x, center.y, center.z, ModSounds.MECHA_LAND.get(), SoundSource.PLAYERS, 2.0F, 0.9F + random.nextFloat() * 0.1F);
        level.sendParticles(ModParticles.SHOCKWAVE.get(), center.x, center.y + 0.1, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 2, 1.0, 0.2, 1.0, 0);
        level.sendParticles(ModParticles.SPARK.get(), center.x, center.y + 0.3, center.z, 60, radius * 0.35, 0.2, radius * 0.35, 0.3);
        for (int i = 0; i < 36; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = 1.5 + random.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockState state = level.getBlockState(BlockPos.containing(x, center.y - 0.5, z));
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 4, 0.2, 0.1, 0.2, 0.3);
            }
        }
    }

    /** Bursts through leaves (trees don't stop a giant). */
    private void trampleLeaves(ServerLevel level, ServerPlayer pilot) {
        AABB box = getBoundingBox().inflate(0.4, 0.0, 0.4);
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY + 0.5, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES) || !level.mayInteract(pilot, pos)) continue;
            level.destroyBlock(pos, true, pilot);
            if (++broken >= 24) return;
        }
    }

    /** Creatures under its feet get shoved aside. */
    private void shoveCreatures(ServerLevel level, ServerPlayer pilot) {
        AABB legs = getBoundingBox().setMaxY(getY() + HIP_HEIGHT);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, legs, e -> e.isAlive() && e != pilot && !hasPassenger(e))) {
            Vec3 away = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            target.push(away.x * 0.25, 0.05, away.z * 0.25);
            target.hurtMarked = true;
        }
    }

    // ---- Weapons ------------------------------------------------------------------------------------

    /** Server: the pilot pressed or released the laser trigger. */
    public void setLaserFiring(boolean firing) {
        laserRequested = firing;
        if (firing && level() instanceof ServerLevel level) {
            Vec3 at = torsoPoint(0, 1.5, 0);
            level.playSound(null, at.x, at.y, at.z, ModSounds.MECHA_LASER.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
            laserTicks = 0;
        }
    }

    /** Where the pilot's aim meets a block or a creature, and what it hit. */
    private HitResult aim(ServerLevel level, Player pilot, double range, double assist) {
        Vec3 eye = pilot.getEyePosition();
        Vec3 end = eye.add(pilot.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pilot));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, this, eye, end, new AABB(eye, end).inflate(1.0 + assist),
                e -> e.isAlive() && e != pilot && !e.isSpectator() && e.isPickable() && !(e instanceof ConstructEntity), (float) assist);
        return entity != null ? entity : block;
    }

    private void tickLaser(ServerLevel level, ServerPlayer pilot) {
        entityData.set(DATA_LASER, laserRequested);
        if (!laserRequested) return;
        laserTicks++;
        HitResult hit = aim(level, pilot, GLConfig.MECHA_LASER_RANGE.get(), 0.3);
        Vec3 end = hit.getLocation();
        entityData.set(DATA_LASER_END, new Vector3f((float) end.x, (float) end.y, (float) end.z));

        if (laserTicks % 5 == 1 && hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            target.invulnerableTime = 0;
            if (target.hurtServer(level, ModDamageTypes.hardLight(level, this, pilot), GLConfig.MECHA_LASER_DAMAGE.get().floatValue())) {
                target.igniteForSeconds(3.0F);
                Vec3 push = end.subtract(pilot.getEyePosition()).normalize().scale(0.15);
                target.push(push.x, 0.02, push.z);
                target.hurtMarked = true;
            }
        }
        if (laserTicks % 3 == 0) {
            level.sendParticles(ModParticles.SPARK.get(), end.x, end.y, end.z, 3, 0.15, 0.15, 0.15, 0.2);
        }
        if (laserTicks % 20 == 0) {
            Vec3 at = torsoPoint(0, 1.5, 0);
            level.playSound(null, at.x, at.y, at.z, ModSounds.MECHA_LASER.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
        }
    }

    /** Server: launches a salvo of homing missiles if the pods are reloaded. Returns whether it fired. */
    public boolean fireMissiles() {
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer pilot)) return false;
        if (missileCooldown > 0 || missilesQueued > 0) return false;
        if (!pilot.isCreative() && !RingEnergy.tryConsume(ownerRing(), GLConfig.MECHA_MISSILE_COST.get())) {
            PowerRingItem.notifyNoEnergy(pilot);
            return false;
        }
        HitResult hit = aim(level, pilot, 96.0, 2.0);
        missileAim = hit.getLocation();
        missileTarget = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : nearestEnemyInSight(level, pilot);
        missilesQueued = GLConfig.MECHA_MISSILES_PER_SALVO.get();
        missileCooldown = MISSILE_COOLDOWN;
        entityData.set(DATA_SALVO, entityData.get(DATA_SALVO) + 1);
        return true;
    }

    private @Nullable LivingEntity nearestEnemyInSight(ServerLevel level, Player pilot) {
        Vec3 eye = pilot.getEyePosition();
        Vec3 look = pilot.getLookAngle();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(48.0),
                e -> e instanceof Enemy && e.isAlive() && pilot.hasLineOfSight(e))) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            double dot = to.scale(1.0 / dist).dot(look);
            if (dot < 0.85) continue;
            double score = dist * (2.0 - dot);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private void tickMissiles(ServerLevel level, ServerPlayer pilot) {
        if (missileCooldown > 0) missileCooldown--;
        if (missilesQueued <= 0 || tickCount % MISSILE_INTERVAL != 0) return;
        int side = missilesQueued % 2 == 0 ? 1 : -1;
        int row = (missilesQueued / 2) % 3;
        Vec3 pod = torsoPoint(side * SHOULDER_X, SHOULDER_Y + 1.15, 0.25 - row * 0.35);
        float yaw = getYRot() + twist(1.0F);
        Vec3 launch = new Vec3(side * 0.18, 0.6, 0.3).yRot(-yaw * Mth.DEG_TO_RAD)
                .add((random.nextDouble() - 0.5) * 0.1, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.1);
        level.addFreshEntity(MechaMissileEntity.create(level, pilot, pod, launch, missileTarget, missileAim));
        level.playSound(null, pod.x, pod.y, pod.z, ModSounds.MECHA_MISSILE.get(), SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.CLOUD, pod.x, pod.y, pod.z, 3, 0.1, 0.1, 0.1, 0.03);
        missilesQueued--;
    }

    /** Server: the pilot was hit. The mecha takes the hit instead, paid for with ring energy. */
    public void absorbHit(ServerPlayer pilot, DamageSource source, float amount) {
        if (!pilot.isCreative()) {
            int cost = Mth.ceil(amount * GLConfig.MECHA_DAMAGE_ENERGY_COST.get());
            if (cost > 0) RingEnergy.tryConsume(ownerRing(), Math.min(cost, RingEnergy.get(ownerRing()).stored()));
        }
        if (level() instanceof ServerLevel level && tickCount % 4 == 0) {
            Vec3 where = source.getSourcePosition();
            Vec3 at = where != null ? torsoPoint(0, 1.5, 0).lerp(where, 0.25) : torsoPoint(0, 1.5, 0.9);
            level.sendParticles(ModParticles.SPARK.get(), at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, at.x, at.y, at.z, ModSounds.BUBBLE_HIT.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }
}
