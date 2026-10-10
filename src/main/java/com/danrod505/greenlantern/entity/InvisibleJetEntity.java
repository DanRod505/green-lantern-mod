package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Invisible Jet: Wonder Woman's transparent fighter, a sleek shape of glass-like Amazon
 * technology that only shows its edges.
 * <ul>
 *     <li>It flies where she looks: W throttles up, S throttles down until it hovers (it takes off
 *     and lands straight up and down), A/D slide sideways, jump climbs.</li>
 *     <li>Hold sprint: the afterburner, for its top speed.</li>
 *     <li>Left click: the cloak. The jet and its pilot vanish from sight and the monsters lose track of her.</li>
 *     <li>The canopy takes most of the blows meant for the pilot. Sneak climbs out.</li>
 * </ul>
 * Like the Batmobile, the piloted jet is moved by its pilot's client; parked, by the server. It flies
 * off when dismissed, when the armor comes off, or after a few minutes parked.
 */
public class InvisibleJetEntity extends ConstructEntity {
    public static final float WIDTH = 3.2F;
    public static final float HEIGHT = 1.3F;
    public static final float SEAT_HEIGHT = 0.35F;
    private static final int MAX_PARKED_TICKS = 20 * 180;
    /** Fastest the jet turns to follow the pilot's eyes (degrees per tick). */
    private static final float MAX_TURN = 7.0F;

    private static final EntityDataAccessor<Boolean> DATA_BOOSTING = SynchedEntityData.defineId(InvisibleJetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLOAKED = SynchedEntityData.defineId(InvisibleJetEntity.class, EntityDataSerializers.BOOLEAN);

    private static final AttributeModifier CAMERA_DISTANCE = new AttributeModifier(GreenLantern.id("invisible_jet_camera"), 4.0, AttributeModifier.Operation.ADD_VALUE);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    // Controlling client: flight state.
    private double speed;
    private boolean boosting;

    // Server.
    private int parkedTicks;
    private int engineSoundTicks;
    private Vec3 lastServerPos = Vec3.ZERO;
    private double serverSpeed;

    // Client: animation.
    public float bank;
    public float bankO;
    public float thrust;
    public float thrustO;
    public float cloak;
    public float cloakO;

    public InvisibleJetEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static InvisibleJetEntity create(Level level, Player owner) {
        InvisibleJetEntity jet = new InvisibleJetEntity(ModEntities.INVISIBLE_JET.get(), level);
        jet.setOwner(owner);
        jet.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        jet.yRotO = owner.getYRot();
        return jet;
    }

    /** Whether the jet fits where it was placed. */
    public static boolean fits(Level level, InvisibleJetEntity jet) {
        return level.noCollision(jet, jet.getBoundingBox().deflate(0.05));
    }

    /** The owner's jet, or null. */
    public static @Nullable InvisibleJetEntity find(Player owner) {
        List<InvisibleJetEntity> list = owner.level().getEntitiesOfClass(InvisibleJetEntity.class, owner.getBoundingBox().inflate(256),
                j -> j.isOwnedBy(owner) && !j.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOOSTING, false);
        builder.define(DATA_CLOAKED, false);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected boolean ownerValid() {
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level() && WonderWomanHelper.isSuited(owner);
    }

    @Override
    public void dissipate() {
        flyAway();
    }

    /** The jet climbs away into the clouds (it is gone). */
    public void flyAway() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 40, 1.4, 0.4, 1.4, 0.06);
            level.sendParticles(ModParticles.AMAZON_SPARK.get(), getX(), getY() + 0.5, getZ(), 30, 1.4, 0.4, 1.4, 0.05);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.INVISIBLE_JET_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player player) uncloakPilot(player);
        }
        ejectPassengers();
        discard();
    }

    public void playArrival() {
        if (!(level() instanceof ServerLevel level)) return;
        level.playSound(null, getX(), getY(), getZ(), ModSounds.INVISIBLE_JET_SUMMON.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), getX(), getY() + 0.6, getZ(), 40, 1.5, 0.4, 1.5, 0.04);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 24, 1.5, 0.2, 1.5, 0.05);
    }

    public boolean isBoosting() {
        return level().isClientSide() && isLocalInstanceAuthoritative() ? boosting : entityData.get(DATA_BOOSTING);
    }

    public boolean isCloaked() {
        return entityData.get(DATA_CLOAKED);
    }

    /** Speed along the nose, blocks per tick (only meaningful on the pilot's client). */
    public double flyingSpeed() {
        return speed;
    }

    /** Converts a point of the jet (+Z forward, +X to its left) to world coordinates (yaw only). */
    public Vec3 jetPoint(double x, double y, double z) {
        return position().add(new Vec3(x, y, z).yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    // ---- the cloak ------------------------------------------------------------------------------------

    /** Server: the pilot pressed attack: the cloak goes on (or off). */
    public void toggleCloak() {
        if (!(level() instanceof ServerLevel level)) return;
        boolean on = !isCloaked();
        entityData.set(DATA_CLOAKED, on);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.INVISIBLE_JET_CLOAK.get(), SoundSource.PLAYERS, 1.0F, on ? 1.0F : 0.8F);
        level.sendParticles(ModParticles.AMAZON_SPARK.get(), getX(), getY() + 0.6, getZ(), 30, 1.4, 0.3, 1.4, 0.02);
        Player pilot = getFirstPassenger() instanceof Player player ? player : null;
        if (pilot != null) {
            pilot.displayClientMessage(Component.translatable(on ? "message.greenlantern.jet_cloaked" : "message.greenlantern.jet_visible")
                    .withStyle(ChatFormatting.GOLD), true);
            if (on) {
                cloakPilot(pilot);
            } else {
                uncloakPilot(pilot);
            }
        }
    }

    private static void cloakPilot(Player pilot) {
        pilot.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, false));
    }

    private static void uncloakPilot(Player pilot) {
        MobEffectInstance effect = pilot.getEffect(MobEffects.INVISIBILITY);
        if (effect != null && effect.isAmbient() && effect.getDuration() <= 60) pilot.removeEffect(MobEffects.INVISIBILITY);
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
        return new Vec3(0.0, SEAT_HEIGHT, 0.3).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        for (Vec3 offset : new Vec3[] {new Vec3(-2.2, 0, 0), new Vec3(2.2, 0, 0), new Vec3(0, 0, -3.6), new Vec3(0, 0, 3.8)}) {
            Vec3 spot = position().add(offset.yRot(-getYRot() * Mth.DEG_TO_RAD)).add(0, 0.1, 0);
            if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(spot))) return spot;
        }
        return position().add(0, HEIGHT + 0.1, 0);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player player) {
            setCameraDistance(player, true);
            if (isCloaked() && !level().isClientSide()) cloakPilot(player);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player player) {
            setCameraDistance(player, false);
            player.resetFallDistance();
            if (!level().isClientSide()) uncloakPilot(player);
        }
    }

    private static void setCameraDistance(Player player, boolean flying) {
        AttributeInstance camera = player.getAttribute(Attributes.CAMERA_DISTANCE);
        if (camera == null) return;
        if (flying) {
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

    // ---- tick -----------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();

        if (isLocalInstanceAuthoritative()) {
            control();
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision && speed > 0.6) {
                speed *= 0.3;
                SidedHooks.cameraShake.shake(0.4F, 8);
            } else if (horizontalCollision || verticalCollision && getDeltaMovement().y > 0) {
                speed *= 0.7;
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
        LivingEntity pilot = getControllingPassenger();
        Vec3 motion = getDeltaMovement();
        if (pilot == null) {
            // Unmanned: it settles gently to the ground and stays put.
            boosting = false;
            speed *= 0.9;
            double vy = onGround() ? 0.0 : Math.max(motion.y - 0.02, -0.4);
            Vec3 h = Vec3.directionFromRotation(0, getYRot()).scale(speed);
            setDeltaMovement(h.x, vy, h.z);
            setXRot(Mth.approach(getXRot(), 0.0F, 2.0F));
            return;
        }
        float forward = pilot.zza;
        float strafe = pilot.xxa;
        boolean climb = SidedHooks.jumpKeyDown.getAsBoolean();
        double cruise = GLConfig.INVISIBLE_JET_SPEED.get();
        double boostTop = Math.max(cruise, GLConfig.INVISIBLE_JET_BOOST_SPEED.get());
        boosting = forward > 0 && SidedHooks.sprintKeyDown.getAsBoolean();
        double top = boosting ? boostTop : cruise;

        if (forward > 0) {
            speed = speed < top ? Math.min(top, speed + (boosting ? 0.07 : 0.04)) : Math.max(top, speed * 0.98);
        } else if (forward < 0) {
            speed = Math.max(0.0, speed - 0.08);
        } else {
            speed = Math.max(0.0, speed * 0.992 - 0.002);
        }

        // The nose follows her eyes (quickly at low speed, smoothly at high speed).
        float turn = (float) (MAX_TURN * (1.0 - 0.45 * Mth.clamp(speed / boostTop, 0.0, 1.0)));
        float yawDiff = Mth.wrapDegrees(pilot.getYRot() - getYRot());
        float yawStep = Mth.clamp(yawDiff, -turn, turn);
        setYRot(getYRot() + yawStep);
        float pitchTarget = speed > 0.2 ? Mth.clamp(pilot.getXRot(), -60.0F, 60.0F) : 0.0F;
        setXRot(Mth.approach(getXRot(), pitchTarget, turn));
        bank = Mth.approach(bank, Mth.clamp(-yawStep / turn, -1.0F, 1.0F) - strafe * 0.4F, 0.15F);

        Vec3 nose = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 side = Vec3.directionFromRotation(0, getYRot() - 90.0F);
        Vec3 wish = nose.scale(speed).add(side.scale(-strafe * 0.45));
        // Below flying speed it hovers on its lift jets; jump climbs straight up.
        double hover = speed < 0.3 ? (onGround() ? 0.0 : -0.01) : 0.0;
        wish = wish.add(0, climb ? 0.45 : hover, 0);
        double hold = speed > 1.0 ? 0.25 : 0.4;
        setDeltaMovement(motion.lerp(wish, hold));
    }

    // ---- client -------------------------------------------------------------------------------------

    private void clientTick() {
        bankO = bank;
        thrustO = thrust;
        cloakO = cloak;
        if (!isLocalInstanceAuthoritative()) {
            float yawRate = Mth.wrapDegrees(getYRot() - yRotO);
            bank = Mth.approach(bank, Mth.clamp(-yawRate / MAX_TURN, -1.0F, 1.0F), 0.15F);
        }
        double moved = position().subtract(xo, yo, zo).length();
        boolean boost = isBoosting();
        thrust = Mth.approach(thrust, getControllingPassenger() == null ? 0.0F : boost ? 1.0F : (float) Mth.clamp(moved / 1.6, 0.15, 0.7), 0.1F);
        cloak = Mth.approach(cloak, isCloaked() ? 1.0F : 0.0F, 0.06F);
        if (cloak > 0.9F) return;
        // Vapor from the wingtips at speed, a heat shimmer from the engines.
        if (moved > 0.8 && random.nextFloat() < 0.6F) {
            for (int s = -1; s <= 1; s += 2) {
                Vec3 tip = jetPoint(s * 1.55, 0.45, -0.6);
                level().addParticle(ParticleTypes.CLOUD, tip.x, tip.y, tip.z, 0, 0, 0);
            }
        }
        if (thrust > 0.2F) {
            Vec3 back = Vec3.directionFromRotation(getXRot(), getYRot()).scale(-0.3 - thrust * 0.4);
            for (int s = -1; s <= 1; s += 2) {
                Vec3 nozzle = jetPoint(s * 0.35, 0.5, -1.8);
                if (random.nextFloat() < thrust) {
                    level().addParticle(ModParticles.AMAZON_SPARK.get(), nozzle.x, nozzle.y, nozzle.z, back.x, back.y, back.z);
                }
            }
        }
    }

    // ---- server -------------------------------------------------------------------------------------

    private void serverTick(ServerLevel level) {
        Vec3 pos = position();
        serverSpeed = pos.subtract(lastServerPos).length();
        lastServerPos = pos;
        Player owner = getOwner();
        if (!(owner instanceof ServerPlayer pilot) || pilot.getVehicle() != this) {
            entityData.set(DATA_BOOSTING, false);
            if (++parkedTicks > MAX_PARKED_TICKS) {
                if (owner != null) owner.displayClientMessage(Component.translatable("message.greenlantern.jet_left").withStyle(ChatFormatting.GOLD), true);
                flyAway();
            }
            return;
        }
        parkedTicks = 0;
        entityData.set(DATA_BOOSTING, pilot.getLastClientInput().sprint() && pilot.getLastClientInput().forward());
        // The sealed canopy: air and no fire for the pilot.
        pilot.setAirSupply(pilot.getMaxAirSupply());
        if (pilot.isOnFire()) pilot.clearFire();
        if (isCloaked() && tickCount % 20 == 0) {
            cloakPilot(pilot);
            // Out of sight: the monsters chasing her lose track of her.
            for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(32.0), m -> m.getTarget() == pilot)) {
                mob.setTarget(null);
            }
        }
        if (engineSoundTicks-- <= 0 && (serverSpeed > 0.05 || tickCount % 2 == 0)) {
            engineSoundTicks = 30;
            float volume = isCloaked() ? 0.4F : 1.0F;
            level.playSound(null, getX(), getY(), getZ(), ModSounds.INVISIBLE_JET_ENGINE.get(), SoundSource.PLAYERS, volume,
                    0.8F + (float) Math.min(0.6, serverSpeed * 0.2));
        }
    }
}
