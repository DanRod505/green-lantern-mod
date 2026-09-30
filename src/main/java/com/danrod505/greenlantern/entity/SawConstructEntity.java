package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The giant hard-light saw: a vehicle the ring bearer stands on and steers with the movement keys
 * (W/S accelerate, A/D strafe, mouse steers, jump hops, sneak dismounts). The spinning blade in
 * front shreds creatures and, optionally, cuts through plants and trees.
 * <p>
 * Like boats, movement is simulated by the controlling client and synced to the server.
 */
public class SawConstructEntity extends ConstructEntity {
    public static final float BLADE_RADIUS = 1.35F;
    public static final double BLADE_OFFSET = 1.75;
    /** Height of the (horizontal) blade above the bottom of the saw. */
    public static final float BLADE_HEIGHT = 0.8F;
    private static final double MAX_SPEED = 0.85;
    private static final double ACCELERATION = 0.06;
    private static final double HOVER_HEIGHT = 0.35;

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private int jumpCooldown;

    /** Client-side blade rotation. */
    public float bladeAngle;
    public float bladeAngleO;

    public SawConstructEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // ---- Riding ---------------------------------------------------------------------------------

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
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height(), -0.2).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-1.5);
        return position().add(back.x, 0.5, back.z);
    }

    @Override
    public float maxUpStep() {
        return 1.1F;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }


    // ---- Tick -------------------------------------------------------------------------------------

    @Override
    protected boolean ownerValid() {
        // The saw only exists while its owner rides it.
        Player owner = getOwner();
        return super.ownerValid() && owner != null && owner.getVehicle() == this;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();

        if (isLocalInstanceAuthoritative()) {
            control();
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }

        if (level().isClientSide()) {
            clientEffects();
        } else {
            serverTick((ServerLevel) level());
        }
    }

    private void control() {
        LivingEntity driver = getControllingPassenger();
        Vec3 motion = getDeltaMovement();
        double vy = motion.y;

        if (driver != null) {
            // Smoothly turn towards where the driver looks.
            float targetYaw = driver.getYRot();
            float yaw = Mth.approachDegrees(getYRot(), targetYaw, 12.0F);
            setYRot(yaw);
            yRotO = yaw;

            float forward = driver.zza;
            float strafe = driver.xxa * 0.6F;
            Vec3 wish = new Vec3(strafe, 0, forward);
            if (wish.lengthSqr() > 1.0) wish = wish.normalize();
            Vec3 dir = wish.yRot(-yaw * Mth.DEG_TO_RAD).scale(ACCELERATION);
            double vx = motion.x * 0.91 + dir.x;
            double vz = motion.z * 0.91 + dir.z;
            double speed = Math.sqrt(vx * vx + vz * vz);
            if (speed > MAX_SPEED) {
                vx *= MAX_SPEED / speed;
                vz *= MAX_SPEED / speed;
            }
            motion = new Vec3(vx, vy, vz);
        } else {
            motion = new Vec3(motion.x * 0.8, vy, motion.z * 0.8);
        }

        // Hover over the ground.
        double ground = groundDistance();
        if (jumpCooldown > 0) jumpCooldown--;
        if (ground < HOVER_HEIGHT + 0.05) {
            vy = Math.max(vy, (HOVER_HEIGHT - ground) * 0.5);
            if (driver != null && jumpCooldown == 0 && SidedHooks.jumpKeyDown.getAsBoolean()) {
                vy = 0.62;
                jumpCooldown = 12;
            }
        } else if (ground < HOVER_HEIGHT + 0.25 && vy <= 0) {
            vy *= 0.5;
        } else {
            vy -= 0.06;
        }
        // The saw hovers, so vanilla step-up never triggers: hop over low obstacles instead.
        if (horizontalCollision && driver != null && ground < 1.0 && motion.horizontalDistanceSqr() > 1.0E-4) {
            vy = Math.max(vy, 0.42);
        }
        vy *= 0.98;
        if (isInWater()) vy = Math.max(vy, 0.04);
        setDeltaMovement(motion.x, vy, motion.z);
    }

    /** Distance from the bottom of the saw to the ground below (capped at 3 blocks). */
    private double groundDistance() {
        AABB box = getBoundingBox();
        for (double d = 0; d <= 3.0; d += 0.125) {
            if (!level().noCollision(this, box.move(0, -d - 0.01, 0))) {
                return d;
            }
        }
        return 3.0;
    }

    private void clientEffects() {
        double speed = getDeltaMovement().horizontalDistance();
        Player owner = getOwner();
        if (owner != null && owner.isLocalPlayer()) {
            speed = getKnownSpeed().horizontalDistance();
        }
        bladeAngleO = bladeAngle;
        bladeAngle += 38.0F + (float) speed * 40.0F;
        if (tickCount % 2 == 0) {
            // Sparks flying off the rim of the blade.
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 center = bladeCenter();
            double rx = Math.cos(a) * BLADE_RADIUS;
            double rz = Math.sin(a) * BLADE_RADIUS;
            level().addParticle(ModParticles.SPARK.get(), center.x + rx, center.y, center.z + rz,
                    -rz * 0.15, 0.05 + random.nextDouble() * 0.1, rx * 0.15);
        }
        if (tickCount % 4 == 0) {
            level().addParticle(ModParticles.GLOW.get(), getX() + (random.nextDouble() - 0.5) * 1.4, getY(), getZ() + (random.nextDouble() - 0.5) * 1.4, 0, -0.02, 0);
        }
    }

    /** Center of the spinning blade, in front of the platform. */
    public Vec3 bladeCenter() {
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        return position().add(forward.scale(BLADE_OFFSET)).add(0, BLADE_HEIGHT, 0);
    }

    private void serverTick(ServerLevel level) {
        Player owner = getOwner();
        ItemStack ring = ownerRing();
        int cost = GLConfig.SAW_COST_PER_SECOND.get();
        if (tickCount % 20 == 0 && cost > 0 && !(owner != null && owner.isCreative())) {
            if (!RingEnergy.tryConsume(ring, cost)) {
                if (owner instanceof ServerPlayer sp) PowerRingItem.notifyNoEnergy(sp);
                dissipate();
                return;
            }
        }
        if (tickCount % 20 == 1) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.SAW_LOOP.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        Vec3 center = bladeCenter();
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        // The blade sweeps a flat disc; creatures and plants from the ground up to just above it are hit.
        AABB bladeBox = new AABB(center.x - BLADE_RADIUS, getY() - 0.3, center.z - BLADE_RADIUS,
                center.x + BLADE_RADIUS, center.y + 0.9, center.z + BLADE_RADIUS);

        boolean cut = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bladeBox, e -> e.isAlive() && e != owner && !hasPassenger(e))) {
            if (target.hurtServer(level, ModDamageTypes.hardLight(level, this, owner), GLConfig.SAW_DAMAGE.get().floatValue())) {
                Vec3 side = target.position().subtract(center).normalize().add(forward).scale(0.45);
                target.push(side.x, 0.25, side.z);
                target.hurtMarked = true;
                cut = true;
            }
        }

        if (GLConfig.SAW_CUTS_PLANTS.get() && owner != null && tickCount % 2 == 0) {
            BlockPos min = BlockPos.containing(bladeBox.minX, bladeBox.minY + 0.35, bladeBox.minZ);
            BlockPos max = BlockPos.containing(bladeBox.maxX, bladeBox.maxY, bladeBox.maxZ);
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockState state = level.getBlockState(pos);
                if (isCuttable(state) && level.mayInteract(owner, pos)) {
                    level.destroyBlock(pos, true, owner);
                    cut = true;
                }
            }
        }

        if (cut && tickCount % 4 == 0) {
            level.playSound(null, center.x, center.y, center.z, ModSounds.SAW_CUT.get(), SoundSource.PLAYERS, 0.9F, 0.9F + random.nextFloat() * 0.25F);
            level.sendParticles(ModParticles.SPARK.get(), center.x, center.y, center.z, 10, 0.4, 0.4, 0.4, 0.25);
        }
    }

    private static boolean isCuttable(BlockState state) {
        if (state.isAir()) return false;
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.BAMBOO_BLOCKS) || state.is(BlockTags.CROPS) || state.is(BlockTags.WOOL)
                || state.is(BlockTags.CAVE_VINES) || state.is(BlockTags.SWORD_EFFICIENT);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && getPassengers().isEmpty()) {
            dissipate();
        }
    }
}
