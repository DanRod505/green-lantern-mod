package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The giant manta ray of Atlantis: a wingspan of eight blocks, slow to turn and graceful. Ridden fast
 * up through the surface it bursts out of the sea and glides over the water on its wings for a while
 * before diving back in.
 */
public class MantaRayEntity extends AtlanteanMountEntity {
    public static final float WIDTH = 3.2F;
    public static final float HEIGHT = 0.8F;
    public static final int VARIANTS = 3;
    private int leapCooldown;

    public MantaRayEntity(EntityType<? extends MantaRayEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int variants() {
        return VARIANTS;
    }

    @Override
    protected double cruiseSpeed() {
        return 0.75;
    }

    @Override
    protected double topSpeed() {
        return 1.7;
    }

    @Override
    protected double secondsToTop() {
        return 3.0;
    }

    @Override
    protected float turnRate() {
        return 5.0F;
    }

    @Override
    protected double wanderSpeed() {
        return 0.1;
    }

    @Override
    protected Vec3 seat() {
        return new Vec3(0.0, 0.5, 0.05);
    }

    @Override
    protected void riddenSwimTick(Player rider, boolean boostKey, boolean riseKey) {
        if (leapCooldown > 0) leapCooldown--;
        Vec3 motion = getDeltaMovement();
        // Racing up through the surface: burst out of the water with a beat of the wings.
        if (!isUnderWater() && motion.y > 0.2 && speed > 0.9 && leapCooldown == 0) {
            leapCooldown = 30;
            setDeltaMovement(motion.x * 1.1, Math.min(1.1, 0.55 + motion.y), motion.z * 1.1);
            level().playLocalSound(getX(), getY(), getZ(), ModSounds.MANTA_FLAP.get(), SoundSource.NEUTRAL, 1.2F, 0.9F, false);
            for (int i = 0; i < 30; i++) {
                level().addParticle(ParticleTypes.SPLASH, getX() + random.nextGaussian() * 1.5, getY() + 0.5, getZ() + random.nextGaussian() * 1.5,
                        0, 0.3, 0);
            }
        }
    }

    /** Out of the water: the wings hold it up, so it glides down slowly while it keeps its speed. */
    @Override
    protected Vec3 riddenInAir(Player rider, Vec3 motion) {
        if (leapCooldown > 0) leapCooldown--;
        boolean gliding = rider.zza > 0 && !onGround();
        double fall = gliding ? Math.max(motion.y - 0.035, -0.22) : Math.max(motion.y - 0.08, -2.0);
        Vec3 look = Vec3.directionFromRotation(0.0F, getYRot());
        double horizontal = motion.horizontalDistance();
        if (gliding) {
            // Steer the glide with the rider's view.
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), rider.getYRot(), 3.0F));
            horizontal = Math.max(0.4, horizontal * 0.995);
            return new Vec3(look.x * horizontal, fall, look.z * horizontal);
        }
        double drag = onGround() ? 0.6 : 0.98;
        return new Vec3(motion.x * drag, fall, motion.z * drag);
    }

    @Override
    protected void clientExtras() {
        if (!isInWater() && isVehicle() && random.nextInt(2) == 0) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 tip = position().add(facing().yRot(side * 1.4F).scale(3.6)).add(0, 0.3, 0);
                level().addParticle(ParticleTypes.FALLING_WATER, tip.x, tip.y, tip.z, 0, 0, 0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MANTA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MANTA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MANTA_HURT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.4F;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }
}
