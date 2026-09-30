package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.SidedHooks;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The giant hard-light hammer. It materializes raised high, slams down on the target point and
 * releases a shockwave that damages and throws back every creature in the area.
 * <p>
 * The entity sits on the impact point; its yaw is the direction of the swing.
 */
public class HammerConstructEntity extends ConstructEntity {
    public static final int RAISE_TICKS = 10;
    public static final int IMPACT_TICK = 16;
    public static final int LIFETIME = 34;

    public HammerConstructEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Swing angle in degrees: 105 = raised behind the target, 0 = head on the ground. */
    public static float swingAngle(float age) {
        if (age < RAISE_TICKS) {
            float t = age / RAISE_TICKS;
            return 60.0F + 45.0F * Mth.sin(t * Mth.HALF_PI);
        }
        if (age < IMPACT_TICK) {
            float t = (age - RAISE_TICKS) / (float) (IMPACT_TICK - RAISE_TICKS);
            return 105.0F * (1.0F - t * t * t);
        }
        // Small rebound after the impact.
        float t = Math.min(1.0F, (age - IMPACT_TICK) / 6.0F);
        return 6.0F * Mth.sin(t * Mth.PI);
    }

    /** Opacity used by the renderer (fades in and out). */
    public static float alpha(float age) {
        if (age < 5) return age / 5.0F;
        if (age > LIFETIME - 10) return Math.max(0.0F, (LIFETIME - age) / 10.0F);
        return 1.0F;
    }

    @Override
    protected boolean ownerValid() {
        // The hammer finishes its swing even if the owner changes item.
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level();
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        if (tickCount == 1 && level().isClientSide()) {
            for (int i = 0; i < 20; i++) {
                level().addParticle(ModParticles.GLOW.get(), getX() + random.nextGaussian(), getY() + 2 + random.nextGaussian(), getZ() + random.nextGaussian(), 0, 0.02, 0);
            }
        }

        if (tickCount == IMPACT_TICK) {
            if (level() instanceof ServerLevel serverLevel) {
                impact(serverLevel);
            } else {
                Player local = null;
                for (Player player : level().players()) {
                    if (player.isLocalPlayer()) local = player;
                }
                if (local != null) {
                    double dist = local.distanceTo(this);
                    if (dist < 24) SidedHooks.cameraShake.shake((float) (1.0 - dist / 24.0), 14);
                }
            }
        }
        if (!level().isClientSide() && tickCount >= LIFETIME) {
            discard();
        }
    }

    private void impact(ServerLevel level) {
        Player owner = getOwner();
        double radius = GLConfig.HAMMER_RADIUS.get();
        float damage = GLConfig.HAMMER_DAMAGE.get().floatValue();
        Vec3 center = position();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 3.0, radius))) {
            if (target == owner || !target.isAlive()) continue;
            double dist = Math.sqrt(target.distanceToSqr(center.x, target.getY(), center.z));
            if (dist > radius) continue;
            float falloff = (float) (1.0 - 0.6 * (dist / radius));
            target.hurtServer(level, ModDamageTypes.hardLight(level, this, owner), damage * falloff);
            Vec3 away = new Vec3(target.getX() - center.x, 0, target.getZ() - center.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : away.normalize().scale(1.2 * falloff);
            target.push(away.x, 0.55 * falloff + 0.2, away.z);
            target.hurtMarked = true;
        }

        level.playSound(null, center.x, center.y, center.z, ModSounds.HAMMER_IMPACT.get(), SoundSource.PLAYERS, 1.6F, 0.95F + random.nextFloat() * 0.1F);
        level.sendParticles(ModParticles.SHOCKWAVE.get(), center.x, center.y + 0.1, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.SPARK.get(), center.x, center.y + 0.3, center.z, 60, radius * 0.35, 0.2, radius * 0.35, 0.3);

        // Debris of the blocks around the impact point.
        for (int i = 0; i < 28; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = random.nextDouble() * radius;
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            BlockPos pos = BlockPos.containing(x, center.y - 0.5, z);
            BlockState state = level.getBlockState(pos);
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.1, z, 4, 0.2, 0.1, 0.2, 0.3);
            }
        }
    }


    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
