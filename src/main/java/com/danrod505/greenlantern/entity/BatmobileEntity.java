package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.batman.BatCharge;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.batman.BatmanServer;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
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
import org.jspecify.annotations.Nullable;

/**
 * The Batmobile: Batman's armored car, long, low and black, with a jet engine in the tail and
 * missile launchers on the hood.
 * <ul>
 *     <li>W accelerates, S brakes (then reverses), A/D steer (tighter at low speed).</li>
 *     <li>Hold sprint: the jet booster fires and the car races to its top speed (paid for with belt charge).</li>
 *     <li>Jump: a short hop over an obstacle. Left click: a pair of homing missiles. Sneak climbs out.</li>
 *     <li>At speed it rams through monsters; the armor takes most of the hits meant for the driver.</li>
 * </ul>
 * Like the mecha, the driven car is moved by its driver's client; parked, it is moved by the server.
 * It drives off when dismissed, when the batsuit comes off, or after a few minutes parked.
 */
public class BatmobileEntity extends ConstructEntity {
    public static final float WIDTH = 2.4F;
    public static final float HEIGHT = 1.4F;
    /** Height of the driver's seat inside the cockpit. */
    public static final float SEAT_HEIGHT = 0.3F;
    private static final double GRAVITY = 0.08;
    private static final int MAX_PARKED_TICKS = 20 * 180;
    private static final int MISSILE_COOLDOWN = 30;

    private static final EntityDataAccessor<Boolean> DATA_BOOSTING = SynchedEntityData.defineId(BatmobileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOOST_OK = SynchedEntityData.defineId(BatmobileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SALVO = SynchedEntityData.defineId(BatmobileEntity.class, EntityDataSerializers.INT);

    private static final AttributeModifier CAMERA_DISTANCE = new AttributeModifier(GreenLantern.id("batmobile_camera"), 3.0, AttributeModifier.Operation.ADD_VALUE);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    // Controlling client: driving state.
    private double speed;
    private boolean boosting;
    private int jumpCooldown;
    private float steerInput;

    // Server.
    private int parkedTicks;
    private int missileCooldown;
    private int missilesQueued;
    private @Nullable Entity missileTarget;
    private Vec3 missileAim = Vec3.ZERO;
    private @Nullable Vec3 lastServerPos;
    private double serverSpeed;
    private int boostSoundTicks;

    // Client: animation.
    public float wheelSpin;
    public float wheelSpinO;
    public float steer;
    public float steerO;
    public float flame;
    public float flameO;
    public float recoil;
    public float recoilO;
    private int lastSalvo;

    public BatmobileEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static BatmobileEntity create(Level level, Player owner) {
        BatmobileEntity car = new BatmobileEntity(ModEntities.BATMOBILE.get(), level);
        car.setOwner(owner);
        car.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        car.yRotO = owner.getYRot();
        return car;
    }

    /** Whether the car fits where it was placed. */
    public static boolean fits(Level level, BatmobileEntity car) {
        return level.noCollision(car, car.getBoundingBox().deflate(0.05));
    }

    /** The owner's Batmobile, or null. */
    public static @Nullable BatmobileEntity find(Player owner) {
        List<BatmobileEntity> list = owner.level().getEntitiesOfClass(BatmobileEntity.class, owner.getBoundingBox().inflate(256),
                c -> c.isOwnedBy(owner) && !c.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOOSTING, false);
        builder.define(DATA_BOOST_OK, true);
        builder.define(DATA_SALVO, 0);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected boolean ownerValid() {
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level() && BatmanHelper.isSuited(owner);
    }

    @Override
    public void dissipate() {
        driveAway();
    }

    /** The car roars off into the night (it is gone). */
    public void driveAway() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 40, 1.2, 0.4, 1.2, 0.05);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.BATMOBILE_BOOST.get(), SoundSource.PLAYERS, 1.0F, 1.1F);
        }
        ejectPassengers();
        discard();
    }

    public void playEngineStart() {
        level().playSound(null, getX(), getY(), getZ(), ModSounds.BATMOBILE_START.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
    }

    /** Whether the jet booster burns (for every client: the driver's input as the server sees it). */
    public boolean isBoosting() {
        return level().isClientSide() && isLocalInstanceAuthoritative() ? boosting : entityData.get(DATA_BOOSTING);
    }

    /** Speed along the heading, blocks per tick (only meaningful on the driver's client). */
    public double drivingSpeed() {
        return speed;
    }

    public Vec3 heading() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    /** Converts a point of the car (+Z forward, +X to its left) to world coordinates. */
    public Vec3 carPoint(double x, double y, double z) {
        return position().add(new Vec3(x, y, z).yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    // ---- riding ---------------------------------------------------------------------------------------

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
        return new Vec3(0.0, SEAT_HEIGHT, -0.15).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        for (Vec3 offset : new Vec3[] {new Vec3(-1.9, 0, 0), new Vec3(1.9, 0, 0), new Vec3(0, 0, -3.4), new Vec3(0, 0, 3.6)}) {
            Vec3 spot = position().add(offset.yRot(-getYRot() * Mth.DEG_TO_RAD)).add(0, 0.1, 0);
            if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(spot))) return spot;
        }
        return position().add(0, HEIGHT + 0.1, 0);
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
    }

    private static void setCameraDistance(Player player, boolean driving) {
        AttributeInstance camera = player.getAttribute(Attributes.CAMERA_DISTANCE);
        if (camera == null) return;
        if (driving) {
            camera.addOrUpdateTransientModifier(CAMERA_DISTANCE);
        } else {
            camera.removeModifier(CAMERA_DISTANCE.id());
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!isOwnedBy(player) || isVehicle() || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide()) player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public float maxUpStep() {
        return 1.1F;
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
        return distance < 160 * 160;
    }

    // ---- tick -----------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();

        if (isLocalInstanceAuthoritative()) {
            control();
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision && Math.abs(speed) > 0.35) {
                // Crashed into a wall: most of the speed is gone.
                speed *= 0.3;
                SidedHooks.cameraShake.shake(0.35F, 8);
            } else if (horizontalCollision) {
                speed *= 0.6;
            }
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
        boolean ground = onGround();
        if (jumpCooldown > 0) jumpCooldown--;
        double vy = ground && motion.y <= 0 ? -0.02 : Math.max(motion.y - GRAVITY, -2.5);
        if (driver == null) {
            boosting = false;
            speed *= ground ? 0.85 : 0.99;
            Vec3 h = heading().scale(speed);
            setDeltaMovement(h.x, vy, h.z);
            return;
        }
        float forward = driver.zza;
        float strafe = driver.xxa;
        boolean jump = SidedHooks.jumpKeyDown.getAsBoolean();
        double base = GLConfig.BATMOBILE_SPEED.get();
        double boostTop = Math.max(base, GLConfig.BATMOBILE_BOOST_SPEED.get());
        boosting = forward > 0 && SidedHooks.sprintKeyDown.getAsBoolean() && entityData.get(DATA_BOOST_OK);
        double top = boosting ? boostTop : base;
        double grip = ground ? 1.0 : 0.15;

        if (forward > 0) {
            if (speed < top) {
                double accel = (boosting ? 0.065 : 0.028) * (1.0 - 0.5 * Math.max(0, speed) / top);
                speed = Math.min(top, speed + accel * grip);
            } else {
                speed = Math.max(top, speed * 0.985);
            }
        } else if (forward < 0) {
            speed = speed > 0.05 ? speed - 0.07 * grip : Math.max(-0.35, speed - 0.025 * grip);
        } else {
            speed *= ground ? 0.975 : 0.995;
            if (Math.abs(speed) < 0.005) speed = 0;
        }
        if (isInWater()) speed *= 0.85;

        // Steering: tight at low speed, steadier at high speed, reversed when backing up.
        steerInput = Mth.approach(steerInput, -strafe, 0.25F);
        double moving = Mth.clamp(Math.abs(speed) / 0.2, 0.0, 1.0);
        double rate = 5.5 * moving * (1.0 - 0.45 * Mth.clamp(Math.abs(speed) / boostTop, 0.0, 1.0)) * Math.signum(speed) * (ground ? 1.0 : 0.3);
        float yaw = getYRot() + (float) (steerInput * rate);
        setYRot(yaw);

        if (jump && ground && jumpCooldown == 0) {
            vy = 0.55;
            jumpCooldown = 20;
        }
        Vec3 wish = heading().scale(speed);
        // Grip on the ground (a little drift in tight turns at speed), momentum in the air.
        double hold = ground ? Mth.clamp(0.75 - Math.abs(speed) * 0.2, 0.35, 0.75) : 0.04;
        Vec3 h = new Vec3(motion.x, 0, motion.z).lerp(wish, hold);
        if (isInWater()) vy = Math.max(vy, -0.1);
        setDeltaMovement(h.x, vy, h.z);
    }

    // ---- client -------------------------------------------------------------------------------------

    private void clientTick() {
        wheelSpinO = wheelSpin;
        steerO = steer;
        flameO = flame;
        recoilO = recoil;
        double dx = getX() - xo;
        double dz = getZ() - zo;
        Vec3 f = heading();
        double along = dx * f.x + dz * f.z;
        wheelSpin += (float) (along / 0.42);
        float steerTarget;
        if (isLocalInstanceAuthoritative()) {
            steerTarget = steerInput;
        } else {
            steerTarget = Mth.clamp(Mth.wrapDegrees(getYRot() - yRotO) / 4.0F, -1.0F, 1.0F);
        }
        steer = Mth.approach(steer, steerTarget, 0.2F);
        boolean boost = isBoosting();
        flame = Mth.approach(flame, boost ? 1.0F : 0.0F, boost ? 0.25F : 0.1F);
        recoil *= 0.8F;
        int salvo = entityData.get(DATA_SALVO);
        if (salvo != lastSalvo) {
            lastSalvo = salvo;
            recoil = 1.0F;
        }

        double moved = Math.sqrt(dx * dx + dz * dz);
        Vec3 nozzle = carPoint(0, 0.6, -2.8);
        if (flame > 0.05F) {
            Vec3 back = f.scale(-0.4 - flame * 0.5);
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.FLAME, nozzle.x + random.nextGaussian() * 0.08, nozzle.y + random.nextGaussian() * 0.08,
                        nozzle.z + random.nextGaussian() * 0.08, back.x + dx, back.y, back.z + dz);
            }
            if (random.nextFloat() < flame) {
                level().addParticle(ParticleTypes.SMALL_FLAME, nozzle.x, nozzle.y, nozzle.z, back.x * 1.5 + dx, 0.01, back.z * 1.5 + dz);
            }
        } else if (tickCount % 4 == 0 && getControllingPassenger() != null) {
            level().addParticle(ParticleTypes.SMOKE, nozzle.x, nozzle.y, nozzle.z, -f.x * 0.05, 0.02, -f.z * 0.05);
        }
        // Dust and gravel thrown up by the rear wheels at speed.
        if (moved > 0.5 && onGroundish()) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 wheel = carPoint(side * 1.1, 0.05, -1.6);
                BlockState below = level().getBlockState(BlockPos.containing(wheel.x, wheel.y - 0.3, wheel.z));
                if (below.getRenderShape() != RenderShape.INVISIBLE && random.nextFloat() < moved) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), wheel.x, wheel.y + 0.1, wheel.z,
                            -f.x * 0.3 + random.nextGaussian() * 0.05, 0.15, -f.z * 0.3 + random.nextGaussian() * 0.05);
                }
            }
        }
    }

    private boolean onGroundish() {
        return onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.3, 0));
    }

    // ---- server -------------------------------------------------------------------------------------

    private void serverTick(ServerLevel level) {
        Vec3 pos = position();
        serverSpeed = lastServerPos == null ? 0 : pos.subtract(lastServerPos).multiply(1, 0, 1).length();
        lastServerPos = pos;
        if (missileCooldown > 0) missileCooldown--;

        Player owner = getOwner();
        if (!(owner instanceof ServerPlayer driver) || driver.getVehicle() != this) {
            entityData.set(DATA_BOOSTING, false);
            if (++parkedTicks > MAX_PARKED_TICKS) {
                if (owner != null) owner.displayClientMessage(Component.translatable("message.greenlantern.batmobile_left").withStyle(ChatFormatting.YELLOW), true);
                driveAway();
            }
            return;
        }
        parkedTicks = 0;
        ItemStack belt = BatmanHelper.findBelt(driver);
        boolean creative = driver.isCreative();
        int cost = GLConfig.BATMOBILE_BOOST_COST_PER_SECOND.get();
        boolean canBoost = creative || BatCharge.has(belt, Math.max(1, cost));
        entityData.set(DATA_BOOST_OK, canBoost);
        boolean boostingNow = canBoost && driver.getLastClientInput().sprint() && driver.getLastClientInput().forward();
        entityData.set(DATA_BOOSTING, boostingNow);
        if (boostingNow) {
            if (tickCount % 20 == 0 && !creative && !BatCharge.tryConsume(belt, cost)) {
                BatmanServer.notifyNoCharge(driver);
            }
            if (boostSoundTicks-- <= 0) {
                boostSoundTicks = 30;
                level.playSound(null, getX(), getY(), getZ(), ModSounds.BATMOBILE_BOOST.get(), SoundSource.PLAYERS, 1.3F, 0.95F + random.nextFloat() * 0.1F);
            }
        } else {
            boostSoundTicks = 0;
        }

        // The armored cockpit: air and no fire for the driver.
        driver.setAirSupply(driver.getMaxAirSupply());
        if (driver.isOnFire()) driver.clearFire();
        ram(level, driver);
        tickMissiles(level, driver);
    }

    /** Monsters in the way get run over (harder the faster it goes). */
    private void ram(ServerLevel level, ServerPlayer driver) {
        if (serverSpeed < 0.35) return;
        Vec3 f = heading();
        AABB front = getBoundingBox().move(f.scale(1.6)).inflate(0.2, 0.3, 0.2);
        double top = Math.max(GLConfig.BATMOBILE_SPEED.get(), 0.1);
        float damage = (float) (GLConfig.BATMOBILE_RAM_DAMAGE.get() * Math.min(1.5, serverSpeed / top));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, front, e -> e.isAlive() && e != driver && !hasPassenger(e)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            if (target.hurtServer(level, level.damageSources().playerAttack(driver), damage)) {
                Vec3 side = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
                side = side.lengthSqr() < 1.0E-4 ? f : side.normalize();
                Vec3 push = f.scale(serverSpeed * 1.2).add(side.scale(0.5));
                target.push(push.x, 0.4 + serverSpeed * 0.2, push.z);
                target.hurtMarked = true;
                level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.BATARANG_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
            }
        }
    }

    // ---- missiles -----------------------------------------------------------------------------------

    /** Server: the driver pulled the trigger: a pair of homing missiles, if the launchers are loaded. */
    public boolean fireMissiles() {
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer driver)) return false;
        if (missileCooldown > 0 || missilesQueued > 0) return false;
        if (!driver.isCreative() && !BatCharge.tryConsume(BatmanHelper.findBelt(driver), GLConfig.BATMOBILE_MISSILE_COST.get())) {
            BatmanServer.notifyNoCharge(driver);
            return false;
        }
        HitResult hit = aim(level, driver, 96.0, 2.0);
        missileAim = hit.getLocation();
        missileTarget = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : nearestEnemyAhead(level, driver);
        missilesQueued = 2;
        missileCooldown = MISSILE_COOLDOWN;
        entityData.set(DATA_SALVO, entityData.get(DATA_SALVO) + 1);
        return true;
    }

    private HitResult aim(ServerLevel level, Player driver, double range, double assist) {
        Vec3 eye = driver.getEyePosition();
        Vec3 end = eye.add(driver.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, driver));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, this, eye, end, new AABB(eye, end).inflate(1.0 + assist),
                e -> e.isAlive() && e != driver && e != this && !e.isSpectator() && e.isPickable() && !(e instanceof ConstructEntity), (float) assist);
        return entity != null ? entity : block;
    }

    private @Nullable LivingEntity nearestEnemyAhead(ServerLevel level, Player driver) {
        Vec3 eye = driver.getEyePosition();
        Vec3 look = driver.getLookAngle();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(48.0),
                e -> e instanceof Enemy && e.isAlive() && driver.hasLineOfSight(e))) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            double dot = to.scale(1.0 / dist).dot(look);
            if (dot < 0.8) continue;
            double score = dist * (2.0 - dot);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private void tickMissiles(ServerLevel level, ServerPlayer driver) {
        if (missilesQueued <= 0 || tickCount % 3 != 0) return;
        int side = missilesQueued % 2 == 0 ? 1 : -1;
        Vec3 pod = carPoint(side * 0.62, 1.05, 1.9);
        Vec3 launch = heading().scale(0.9 + serverSpeed).add(0, 0.12, 0);
        level.addFreshEntity(BatmobileMissileEntity.create(level, driver, pod, launch, missileTarget, missileAim));
        level.playSound(null, pod.x, pod.y, pod.z, ModSounds.BATMOBILE_MISSILE.get(), SoundSource.PLAYERS, 1.2F, 0.9F + random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.CLOUD, pod.x, pod.y, pod.z, 4, 0.1, 0.1, 0.1, 0.03);
        missilesQueued--;
    }
}
