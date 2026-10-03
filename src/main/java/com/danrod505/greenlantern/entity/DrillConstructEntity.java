package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The giant hard-light drill: a digging vehicle. The rider sits in a cockpit with a huge spinning
 * drill bit in front that points where they look. W drives towards the bit (down into the ground,
 * up through ceilings or straight ahead), S reverses, A/D strafe, jump hops and sneak dismounts.
 * <p>
 * While driving forward the bit bores a tunnel big enough for the drill and its rider, mining
 * stone, dirt and every ore (drops go straight to the rider's inventory) and shredding creatures.
 * Against terrain the drill "grips" the rock, so it can hang in a vertical shaft; in open air it falls.
 * <p>
 * Like the saw (and boats), movement is simulated by the controlling client and synced to the
 * server, which does the mining based on the rider's input.
 */
public class DrillConstructEntity extends ConstructEntity {
    /** Pivot of the drill arm, relative to the bottom center of the cockpit (forward = +Z). */
    public static final float PIVOT_HEIGHT = 0.55F;
    public static final float PIVOT_FORWARD = 0.7F;
    /** Distance from the pivot to the base of the bit, and the length of the bit. */
    public static final float BIT_START = 0.25F;
    public static final float BIT_LENGTH = 2.1F;
    public static final float BIT_RADIUS = 0.8F;
    /** Pitch below/above which the drill bores diagonally instead of driving level. */
    private static final float LEVEL_DEAD_ZONE = 25.0F;
    private static final double MAX_SPEED = 0.5;
    private static final double ACCELERATION = 0.07;
    private static final double DIG_RADIUS = 1.45;

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    /** Server side: mining progress (in hardness units) of the blocks the bit is grinding. */
    private final Long2FloatOpenHashMap digProgress = new Long2FloatOpenHashMap();
    private int jumpCooldown;
    private boolean diggingThisTick;

    /** Client-side bit rotation. */
    public float bitAngle;
    public float bitAngleO;

    public DrillConstructEntity(EntityType<?> type, Level level) {
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
        return true;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, 0.35, -0.25).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-1.4);
        return position().add(back.x, 0.3, back.z);
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

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- Geometry -------------------------------------------------------------------------------

    /** Pitch the bit actually bores at: level driving inside the dead zone, otherwise the rider's pitch. */
    public static float effectivePitch(float pitch) {
        return Math.abs(pitch) < LEVEL_DEAD_ZONE ? 0.0F : pitch;
    }

    /** Direction the bit points to (and the drill bores towards). */
    public Vec3 boreDirection() {
        return Vec3.directionFromRotation(effectivePitch(getXRot()), getYRot());
    }

    /** Point the drill arm rotates around. */
    public Vec3 pivot() {
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        return position().add(forward.scale(PIVOT_FORWARD)).add(0, PIVOT_HEIGHT, 0);
    }

    /** Middle of the bit, where blocks and creatures are ground. */
    public Vec3 bitCenter() {
        return pivot().add(boreDirection().scale(BIT_START + BIT_LENGTH * 0.55));
    }

    // ---- Tick -------------------------------------------------------------------------------------

    @Override
    protected boolean ownerValid() {
        // The drill only exists while its owner rides it.
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
        resetFallDistance();

        if (level().isClientSide()) {
            clientEffects();
        } else {
            serverTick((ServerLevel) level());
        }
    }

    /** Whether the drill touches terrain on any side (it grips the rock and doesn't fall). */
    private boolean grippingTerrain() {
        return !level().noCollision(this, getBoundingBox().inflate(0.2, 0.15, 0.2));
    }

    private void control() {
        LivingEntity driver = getControllingPassenger();
        Vec3 motion = getDeltaMovement();
        boolean grip = grippingTerrain();
        if (jumpCooldown > 0) jumpCooldown--;

        if (driver == null) {
            setDeltaMovement(motion.x * 0.7, onGround() ? 0 : motion.y - 0.08, motion.z * 0.7);
            return;
        }

        // Turn the drill and aim the bit where the driver looks.
        float yaw = Mth.approachDegrees(getYRot(), driver.getYRot(), 10.0F);
        setYRot(yaw);
        yRotO = yaw;
        float pitch = Mth.approach(getXRot(), Mth.clamp(driver.getXRot(), -80.0F, 85.0F), 8.0F);
        setXRot(pitch);
        xRotO = pitch;

        float forward = driver.zza;
        float strafe = driver.xxa * 0.5F;
        Vec3 wish;
        if (forward > 0) {
            Vec3 dir = boreDirection();
            // In open air the drill can only bore downwards (gravity) or level.
            if (!grip && dir.y > 0) dir = new Vec3(dir.x, 0, dir.z).normalize();
            wish = dir.scale(forward);
        } else {
            wish = new Vec3(0, 0, forward * 0.6).yRot(-yaw * Mth.DEG_TO_RAD);
        }
        wish = wish.add(new Vec3(strafe, 0, 0).yRot(-yaw * Mth.DEG_TO_RAD));
        if (wish.lengthSqr() > 1.0) wish = wish.normalize();

        boolean boringVertically = forward > 0 && Math.abs(wish.y) > 1.0E-3;
        double vx = motion.x * 0.8 + wish.x * ACCELERATION;
        double vz = motion.z * 0.8 + wish.z * ACCELERATION;
        double vy;
        if (boringVertically && (grip || wish.y < 0)) {
            vy = motion.y * 0.8 + wish.y * ACCELERATION;
        } else if (grip && !onGround()) {
            // Hang on to the walls of a shaft.
            vy = 0;
        } else {
            vy = motion.y - 0.08;
            if (onGround() && jumpCooldown == 0 && SidedHooks.jumpKeyDown.getAsBoolean()) {
                vy = 0.5;
                jumpCooldown = 12;
            }
        }
        // Hop over low obstacles while driving level (the bit grinds through anything taller).
        if (horizontalCollision && onGround() && !boringVertically && motion.horizontalDistanceSqr() > 1.0E-4) {
            vy = Math.max(vy, 0.42);
        }
        vy *= 0.98;
        if (isInWater()) vy = Math.max(vy, 0.02);

        Vec3 v = new Vec3(vx, vy, vz);
        double speed = v.length();
        if (speed > MAX_SPEED && (boringVertically || vy > -MAX_SPEED)) v = v.scale(MAX_SPEED / speed);
        setDeltaMovement(v);
    }

    private void clientEffects() {
        Player owner = getOwner();
        boolean pushing = owner != null && owner.zza > 0;
        bitAngleO = bitAngle;
        bitAngle += pushing ? 55.0F : 30.0F;
        if (tickCount % 2 == 0) {
            Vec3 center = bitCenter();
            level().addParticle(ModParticles.SPARK.get(), center.x + (random.nextDouble() - 0.5) * 1.2, center.y + (random.nextDouble() - 0.5) * 1.2,
                    center.z + (random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.3);
        }
        if (tickCount % 4 == 0) {
            level().addParticle(ModParticles.GLOW.get(), getX() + (random.nextDouble() - 0.5) * 1.3, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 1.3, 0, -0.02, 0);
        }
    }

    private void serverTick(ServerLevel level) {
        Player owner = getOwner();
        ItemStack ring = ownerRing();
        int cost = GLConfig.DRILL_COST_PER_SECOND.get();
        if (tickCount % 20 == 0 && cost > 0 && !(owner != null && owner.isCreative())) {
            if (!RingEnergy.tryConsume(ring, cost)) {
                if (owner instanceof ServerPlayer sp) PowerRingItem.notifyNoEnergy(sp);
                dissipate();
                return;
            }
        }
        if (tickCount % 20 == 1) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.DRILL_LOOP.get(), SoundSource.PLAYERS, 0.7F, diggingThisTick ? 0.85F : 1.0F);
        }

        Vec3 center = bitCenter();
        Vec3 dir = boreDirection();
        boolean hit = false;
        AABB bitBox = AABB.ofSize(center, 2.4, 2.4, 2.4);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bitBox, e -> e.isAlive() && e != owner && !hasPassenger(e))) {
            if (target.hurtServer(level, ModDamageTypes.hardLight(level, this, owner), GLConfig.DRILL_DAMAGE.get().floatValue())) {
                target.push(dir.x * 0.5, 0.2, dir.z * 0.5);
                target.hurtMarked = true;
                hit = true;
            }
        }

        diggingThisTick = false;
        boolean pushing = owner instanceof ServerPlayer sp && sp.getLastClientInput().forward();
        if (GLConfig.DRILL_DIGS_BLOCKS.get() && owner instanceof ServerPlayer driver && pushing) {
            diggingThisTick = dig(level, driver);
        } else {
            digProgress.clear();
        }

        if ((hit || diggingThisTick) && tickCount % 4 == 0) {
            level.playSound(null, center.x, center.y, center.z, ModSounds.DRILL_GRIND.get(), SoundSource.PLAYERS, 0.9F, 0.85F + random.nextFloat() * 0.3F);
        }
    }

    /** Grinds the blocks in front of the bit and along the path of the drill. Returns whether any block was hit. */
    private boolean dig(ServerLevel level, ServerPlayer driver) {
        Vec3 bore = boreDirection();
        boolean level0 = Math.abs(bore.y) < 1.0E-3;
        int floorY = Mth.floor(getY() + 0.01);
        LongSet targets = new LongOpenHashSet();

        // Room for the drill and its rider one step ahead along the bore direction.
        AABB body = getBoundingBox().minmax(driver.getBoundingBox()).move(bore.scale(0.8)).deflate(0.02);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY, body.minZ), BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
            targets.add(pos.asLong());
        }
        // The (round) hole bored by the bit.
        Vec3 tip = pivot().add(bore.scale(BIT_START + BIT_LENGTH * 0.7));
        BlockPos min = BlockPos.containing(tip.x - DIG_RADIUS, tip.y - DIG_RADIUS, tip.z - DIG_RADIUS);
        BlockPos max = BlockPos.containing(tip.x + DIG_RADIUS, tip.y + DIG_RADIUS, tip.z + DIG_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            // Driving level keeps a flat floor instead of digging a trench.
            if (level0 && pos.getY() < floorY) continue;
            if (pos.getCenter().distanceToSqr(tip) <= DIG_RADIUS * DIG_RADIUS) targets.add(pos.asLong());
        }

        float speed = GLConfig.DRILL_SPEED.get().floatValue();
        double maxHardness = GLConfig.DRILL_MAX_HARDNESS.get();
        boolean collect = GLConfig.DRILL_COLLECTS_DROPS.get();
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        boolean any = false;
        int broken = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Forget blocks the bit moved away from.
        digProgress.keySet().removeIf((long key) -> !targets.contains(key));
        for (long key : targets) {
            pos.set(key);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty()) continue;
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0 || hardness > maxHardness || !level.mayInteract(driver, pos)) continue;
            any = true;
            float progress = digProgress.get(key) + speed;
            if (progress < hardness) {
                digProgress.put(key, progress);
                continue;
            }
            digProgress.remove(key);
            BlockPos immutable = pos.immutable();
            mineBlock(level, driver, immutable, state, tool, collect);
            if (++broken % 3 == 1) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), immutable.getX() + 0.5, immutable.getY() + 0.5, immutable.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
            }
        }
        if (any && tickCount % 3 == 0) {
            level.sendParticles(ModParticles.SPARK.get(), tip.x, tip.y, tip.z, 6, 0.5, 0.5, 0.5, 0.25);
        }
        return any;
    }

    private static void mineBlock(ServerLevel level, ServerPlayer driver, BlockPos pos, BlockState state, ItemStack tool, boolean collect) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, driver, tool);
        if (!level.destroyBlock(pos, false, driver)) return;
        for (ItemStack drop : drops) {
            if (collect) driver.getInventory().add(drop);
            if (!drop.isEmpty()) Block.popResource(level, collect ? driver.blockPosition() : pos, drop);
        }
        // Experience from ores.
        state.spawnAfterBreak(level, pos, tool, true);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && getPassengers().isEmpty()) {
            dissipate();
        }
    }
}
