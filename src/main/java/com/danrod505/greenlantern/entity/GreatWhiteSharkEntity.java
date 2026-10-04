package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.SeaCall;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Aquaman's great white shark, called from the deep with the emblem. Aquaman rides it on its back:
 * <ul>
 *     <li>Underwater it swims where the rider looks: W speeds up, S slows down, A/D drift to the
 *     sides, jump rises and sprint is a burst of speed. At speed it can leap out of the water.</li>
 *     <li>Left click (attack) while riding: the shark bites whatever is in front of its jaws.</li>
 *     <li>Sneak to climb off: then it stays close to Aquaman and hunts the monsters around him (and
 *     whoever hurt him). Right click it to climb back on.</li>
 * </ul>
 * Like the mecha, the ridden shark is moved by its rider's client; on its own it is moved by the
 * server. It returns to the sea when dismissed, when the armor is taken off, after a while out of
 * the water or after a couple of minutes without a rider.
 */
public class GreatWhiteSharkEntity extends ConstructEntity {
    public static final float WIDTH = 1.5F;
    public static final float HEIGHT = 1.25F;
    /** How far in front of its center the shark's jaws are (the body is about 4.5 blocks long). */
    public static final double MOUTH = 2.3;
    private static final int MAX_DRY_TICKS = 100;
    private static final int MAX_UNRIDDEN_TICKS = 20 * 120;

    private static final EntityDataAccessor<Integer> DATA_BITE = SynchedEntityData.defineId(GreatWhiteSharkEntity.class, EntityDataSerializers.INT);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    // Server: hunting.
    private @Nullable LivingEntity target;
    private int biteCooldown;
    private int dryTicks;
    private int unriddenTicks;
    private double aiSpeed;

    // Controlling client: speed.
    private double speed;

    // Client: animation.
    public float tailPhase;
    public float tailPhaseO;
    public float swimAmount;
    public float swimAmountO;
    public float jaw;
    public float jawO;
    private int lastBite;

    public GreatWhiteSharkEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static GreatWhiteSharkEntity create(Level level, Player owner) {
        GreatWhiteSharkEntity shark = new GreatWhiteSharkEntity(ModEntities.GREAT_WHITE_SHARK.get(), level);
        shark.setOwner(owner);
        shark.snapTo(owner.getX(), owner.getY() - 0.3, owner.getZ(), owner.getYRot(), 0.0F);
        return shark;
    }

    /** The owner's shark, or null. */
    public static @Nullable GreatWhiteSharkEntity find(Player owner) {
        List<GreatWhiteSharkEntity> list = owner.level().getEntitiesOfClass(GreatWhiteSharkEntity.class, owner.getBoundingBox().inflate(160),
                s -> s.isOwnedBy(owner) && !s.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BITE, 0);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected boolean ownerValid() {
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level() && AquamanHelper.isSuited(owner);
    }

    @Override
    public void dissipate() {
        swimAway();
    }

    /** The shark dives back into the deep (it is gone). */
    public void swimAway() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 0.5, getZ(), 40, 1.0, 0.4, 1.0, 0.2);
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.8, getZ(), 30, 1.0, 0.4, 1.0, 0.3);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.SHARK_SUMMON.get(), SoundSource.NEUTRAL, 0.8F, 1.3F);
        }
        ejectPassengers();
        discard();
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
        return new Vec3(0.0, HEIGHT * 0.78, -0.25).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!isOwnedBy(player) || isVehicle() || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide()) player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    /** Direction the shark faces (yaw and swim pitch). */
    public Vec3 facing() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    public Vec3 mouth() {
        return position().add(0, HEIGHT * 0.45, 0).add(facing().scale(MOUTH));
    }

    // ---- tick -------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();

        if (isLocalInstanceAuthoritative()) {
            if (getControllingPassenger() != null) {
                ridden(getControllingPassenger());
            } else if (!level().isClientSide()) {
                hunt((ServerLevel) level());
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

    /** Swimming with a rider: follow the rider's view (runs on the rider's client). */
    private void ridden(LivingEntity rider) {
        Vec3 motion = getDeltaMovement();
        if (!isInWater()) {
            flop(motion);
            speed *= 0.9;
            return;
        }
        float forward = rider.zza;
        float strafe = rider.xxa;
        boolean boost = SidedHooks.sprintKeyDown.getAsBoolean();
        boolean rise = SidedHooks.jumpKeyDown.getAsBoolean();

        float yaw = Mth.approachDegrees(getYRot(), rider.getYRot(), 9.0F);
        float pitch = Mth.approach(getXRot(), Mth.clamp(rider.getXRot(), -70.0F, 70.0F), 6.0F);
        setYRot(yaw);
        setXRot(pitch);
        double top = GLConfig.SHARK_SPEED.get() * (boost ? 1.3 : 1.0);
        if (forward > 0) {
            speed = Math.min(top, speed + top / 25.0);
        } else if (forward < 0) {
            speed = Math.max(-0.25, speed - 0.08);
        } else {
            speed *= 0.94;
        }
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        if (side.lengthSqr() > 1.0E-4) side = side.normalize();
        Vec3 wish = dir.scale(speed).add(side.scale(strafe * -0.35)).add(0, rise ? 0.3 : 0.0, 0);
        setDeltaMovement(motion.lerp(wish, 0.35));
    }

    /** On its own: hunt the monsters around Aquaman, or keep him company (server). */
    private void hunt(ServerLevel level) {
        Vec3 motion = getDeltaMovement();
        if (!isInWater()) {
            flop(motion);
            return;
        }
        Player owner = getOwner();
        if (owner == null) return;
        if (tickCount % 10 == 0 || (target != null && !target.isAlive())) target = findTarget(level, owner);

        Vec3 goal;
        double want;
        if (target != null) {
            goal = target.position().add(0, target.getBbHeight() * 0.5, 0);
            want = 1.3;
        } else {
            // Circle around Aquaman, a few blocks away.
            double a = tickCount * 0.04;
            goal = owner.position().add(Math.cos(a) * 5.0, 0.5, Math.sin(a) * 5.0);
            want = distanceToSqr(owner) > 12 * 12 ? 1.4 : 0.45;
        }
        Vec3 to = goal.subtract(position().add(0, HEIGHT * 0.5, 0));
        if (to.lengthSqr() > 1.0E-3) {
            float yawGoal = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
            float pitchGoal = (float) (-Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yawGoal, 7.0F));
            setXRot(Mth.approach(getXRot(), Mth.clamp(pitchGoal, -60.0F, 60.0F), 5.0F));
        }
        aiSpeed = Mth.lerp(0.08, aiSpeed, want);
        setDeltaMovement(motion.lerp(facing().scale(aiSpeed), 0.3));

        if (target != null && biteCooldown == 0 && target.getBoundingBox().inflate(0.8).contains(mouth())) {
            bite();
        } else if (target != null && biteCooldown == 0 && target.distanceToSqr(mouth()) < 2.2 * 2.2) {
            bite();
        }
    }

    /** Out of the water: no swimming, just falling and thrashing about. */
    private void flop(Vec3 motion) {
        double vy = onGround() ? (tickCount % 12 == 0 ? 0.35 : 0.0) : Math.max(motion.y - 0.08, -2.0);
        setDeltaMovement(motion.x * 0.85, vy, motion.z * 0.85);
        setXRot(Mth.approach(getXRot(), 0.0F, 4.0F));
    }

    private @Nullable LivingEntity findTarget(ServerLevel level, Player owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker.distanceToSqr(owner) < 24 * 24 && !SeaCall.isCalledBy(attacker, owner)) {
            return attacker;
        }
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16),
                e -> e instanceof Enemy && e.isAlive() && !e.isInvisible());
        enemies.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        return enemies.isEmpty() ? null : enemies.getFirst();
    }

    private void serverTick(ServerLevel level) {
        if (biteCooldown > 0) biteCooldown--;
        dryTicks = isInWater() ? 0 : dryTicks + 1;
        unriddenTicks = isVehicle() ? 0 : unriddenTicks + 1;
        if (dryTicks > MAX_DRY_TICKS || unriddenTicks > MAX_UNRIDDEN_TICKS) {
            Player owner = getOwner();
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("message.greenlantern.shark_left").withStyle(ChatFormatting.AQUA), true);
            }
            swimAway();
        }
    }

    // ---- biting -----------------------------------------------------------------------------------------

    /** Snaps the jaws: hurts the closest creature in front of them. Returns whether something was bitten. */
    public boolean bite() {
        if (!(level() instanceof ServerLevel level) || biteCooldown > 0) return false;
        biteCooldown = 12;
        entityData.set(DATA_BITE, entityData.get(DATA_BITE) + 1);
        Vec3 mouth = mouth();
        Player owner = getOwner();
        List<LivingEntity> prey = level.getEntitiesOfClass(LivingEntity.class, new AABB(mouth, mouth).inflate(1.7),
                e -> e.isAlive() && e != owner && !hasPassenger(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && (owner == null || !SeaCall.isCalledBy(e, owner)));
        prey.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mouth)));
        level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.SHARK_BITE.get(), SoundSource.NEUTRAL, 1.2F, 0.9F + random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.BUBBLE_POP, mouth.x, mouth.y, mouth.z, 12, 0.3, 0.3, 0.3, 0.1);
        if (prey.isEmpty()) return false;
        LivingEntity victim = prey.getFirst();
        DamageSource source = ModDamageTypes.sharkBite(level, this, owner);
        if (victim.hurtServer(level, source, GLConfig.SHARK_BITE_DAMAGE.get().floatValue())) {
            // Shaken like a rag doll.
            Vec3 push = facing();
            victim.push(push.x * 0.6 + (random.nextDouble() - 0.5) * 0.4, 0.3, push.z * 0.6 + (random.nextDouble() - 0.5) * 0.4);
            victim.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 14, 0.3, 0.3, 0.3, 0.3);
        }
        return true;
    }

    // ---- client -----------------------------------------------------------------------------------------

    private void clientTick() {
        tailPhaseO = tailPhase;
        swimAmountO = swimAmount;
        jawO = jaw;
        double moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
        boolean water = isInWater();
        float target = water ? (float) Mth.clamp(0.25 + moved / 1.2, 0.25, 1.0) : 1.0F;
        swimAmount = Mth.approach(swimAmount, target, 0.05F);
        tailPhase += water ? (float) (0.18 + moved * 0.45) : 0.6F;
        int bite = entityData.get(DATA_BITE);
        if (bite != lastBite) {
            lastBite = bite;
            jaw = 1.0F;
        } else {
            jaw = Math.max(0.0F, jaw - 0.12F);
        }
        if (water && moved > 0.3) {
            Vec3 back = position().add(0, HEIGHT * 0.5, 0).subtract(facing().scale(2.4));
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.BUBBLE, back.x + random.nextGaussian() * 0.3, back.y + random.nextGaussian() * 0.3,
                        back.z + random.nextGaussian() * 0.3, 0, 0.05, 0);
            }
        }
    }

    public float jaw(float partialTick) {
        return Mth.lerp(partialTick, jawO, jaw);
    }
}
