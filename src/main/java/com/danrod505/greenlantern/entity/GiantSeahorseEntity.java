package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.SidedHooks;
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
 * The giant seahorse of Atlantis, the steed of the royal riders: nimble, quick to turn, and every
 * one is different (eight colors and three patterns: plain, spotted and striped). Starting a sprint
 * it kicks with its tail in a swirl of bubbles; on land it hops along on its curled tail.
 */
public class GiantSeahorseEntity extends AtlanteanMountEntity {
    public static final float WIDTH = 1.8F;
    public static final float HEIGHT = 4.6F;
    public static final int COLORS = 8;
    public static final int PATTERNS = 3;
    private boolean kicked;

    public GiantSeahorseEntity(EntityType<? extends GiantSeahorseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int variants() {
        return COLORS * PATTERNS;
    }

    public int color() {
        return variant() / PATTERNS;
    }

    public int pattern() {
        return variant() % PATTERNS;
    }

    @Override
    protected double cruiseSpeed() {
        return 0.6;
    }

    @Override
    protected double topSpeed() {
        return 1.35;
    }

    @Override
    protected double secondsToTop() {
        return 1.8;
    }

    @Override
    protected float turnRate() {
        return 10.0F;
    }

    @Override
    protected double wanderSpeed() {
        return 0.08;
    }

    @Override
    protected Vec3 seat() {
        return new Vec3(0.0, 3.3, -0.34);
    }

    @Override
    protected void riddenSwimTick(Player rider, boolean boostKey, boolean riseKey) {
        // The first beat of a sprint: a kick of the tail.
        if (boostKey && rider.zza > 0 && !kicked) {
            kicked = true;
            setDeltaMovement(getDeltaMovement().add(facing().scale(0.5)));
            level().playLocalSound(getX(), getY(), getZ(), ModSounds.SEAHORSE_DASH.get(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
            for (int i = 0; i < 24; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.BUBBLE, getX() + Math.cos(a) * 0.6, getY() + random.nextDouble() * 1.2, getZ() + Math.sin(a) * 0.6,
                        Math.cos(a) * 0.15, 0.05, Math.sin(a) * 0.15);
            }
        } else if (!boostKey && boost() <= 0.0) {
            kicked = false;
        }
    }

    /** On land: hop forward on the tail (jump), otherwise fall. */
    @Override
    protected Vec3 riddenInAir(Player rider, Vec3 motion) {
        if (onGround() && (SidedHooks.jumpKeyDown.getAsBoolean() || rider.zza > 0)) {
            Vec3 look = Vec3.directionFromRotation(0.0F, getYRot());
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), rider.getYRot(), 12.0F));
            return new Vec3(look.x * 0.35, 0.55, look.z * 0.35);
        }
        double drag = onGround() ? 0.5 : 0.98;
        return new Vec3(motion.x * drag, Math.max(motion.y - 0.08, -2.0), motion.z * drag);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SEAHORSE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SEAHORSE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SEAHORSE_HURT.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }
}
