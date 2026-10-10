package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Atlantean dolphins: bigger than their cousins of the open sea, brightly colored, with
 * bioluminescent markings that glow in the dark depths and a trail of light when they race. They
 * are the fastest of the mounts of Atlantis and leap out of the water like Aquaman.
 */
public class AtlanteanDolphinEntity extends AtlanteanMountEntity {
    public static final float WIDTH = 1.1F;
    public static final float HEIGHT = 0.9F;
    public static final int VARIANTS = 6;
    /** Glow color of each variant (the markings and the trail), RGB. */
    public static final int[] GLOW = {0x4FF5FF, 0xC77DFF, 0xFF5FD2, 0xFFD34F, 0x5CFF8A, 0xFF8A4F};
    private int leapCooldown;

    public AtlanteanDolphinEntity(EntityType<? extends AtlanteanDolphinEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int variants() {
        return VARIANTS;
    }

    public int glowColor() {
        return GLOW[variant()];
    }

    @Override
    protected double cruiseSpeed() {
        return 0.85;
    }

    @Override
    protected double topSpeed() {
        return 2.1;
    }

    @Override
    protected double secondsToTop() {
        return 2.2;
    }

    @Override
    protected float turnRate() {
        return 11.0F;
    }

    @Override
    protected double wanderSpeed() {
        return 0.16;
    }

    @Override
    protected Vec3 seat() {
        return new Vec3(0.0, 0.62, 0.05);
    }

    @Override
    protected void riddenSwimTick(Player rider, boolean boostKey, boolean riseKey) {
        if (leapCooldown > 0) leapCooldown--;
        Vec3 motion = getDeltaMovement();
        // Racing up through the surface: a leap high out of the water.
        if (!isUnderWater() && motion.y > 0.25 && speed > 0.9 && leapCooldown == 0) {
            leapCooldown = 25;
            setDeltaMovement(motion.x, Math.min(1.4, motion.y * 1.4 + 0.35), motion.z);
            level().playLocalSound(getX(), getY(), getZ(), ModSounds.ATLANTEAN_DOLPHIN_LEAP.get(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
            for (int i = 0; i < 30; i++) {
                level().addParticle(ParticleTypes.SPLASH, getX() + random.nextGaussian() * 0.6, getY() + 0.6, getZ() + random.nextGaussian() * 0.6,
                        random.nextGaussian() * 0.2, 0.3 + random.nextDouble() * 0.3, random.nextGaussian() * 0.2);
            }
        }
    }

    @Override
    protected Vec3 riddenInAir(Player rider, Vec3 motion) {
        if (leapCooldown > 0) leapCooldown--;
        // Arcing through the air nose first, keeping its speed.
        double drag = onGround() ? 0.6 : 0.99;
        return new Vec3(motion.x * drag, Math.max(motion.y - 0.07, -2.0), motion.z * drag);
    }

    @Override
    protected void clientExtras() {
        double moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
        if (moved > 0.35 || random.nextInt(6) == 0) {
            // A trail of light from the glowing markings (stronger the faster it goes).
            DustParticleOptions dust = new DustParticleOptions(glowColor(), moved > 1.0 ? 1.6F : 1.0F);
            Vec3 back = position().add(0, HEIGHT * 0.5, 0).subtract(facing().scale(moved > 0.35 ? 1.3 : 0.4));
            int count = moved > 1.0 ? 3 : 1;
            for (int i = 0; i < count; i++) {
                level().addParticle(dust, back.x + random.nextGaussian() * 0.2, back.y + random.nextGaussian() * 0.2, back.z + random.nextGaussian() * 0.2,
                        0, 0, 0);
            }
            if (moved > 0.8 && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.GLOW, back.x, back.y, back.z, random.nextGaussian() * 0.05, 0.02, random.nextGaussian() * 0.05);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ATLANTEAN_DOLPHIN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ATLANTEAN_DOLPHIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ATLANTEAN_DOLPHIN_HURT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }
}
