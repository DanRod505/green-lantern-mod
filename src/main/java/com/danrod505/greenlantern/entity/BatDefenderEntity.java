package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.registry.ModEntities;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One bat of the swarm Batman calls out of the dark. The bats whirl around him, dive at the
 * monsters close by (and at whoever hurt him), biting, blinding and slowing them down, and throw
 * themselves in front of arrows and fireballs aimed at him. When their time is up (or the gadget is
 * used again) they scatter into the night.
 */
public class BatDefenderEntity extends ConstructEntity {
    private static final int MAX_PER_TARGET = 4;
    private static final int SCATTER_TICKS = 30;

    private static final EntityDataAccessor<Boolean> DATA_SCATTER = SynchedEntityData.defineId(BatDefenderEntity.class, EntityDataSerializers.BOOLEAN);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    private int life = 400;
    private int index;
    private int scatterTicks;
    private int attackCooldown;
    private @Nullable LivingEntity target;

    public BatDefenderEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static BatDefenderEntity create(Level level, Player owner, Vec3 at, int life, int index) {
        BatDefenderEntity bat = new BatDefenderEntity(ModEntities.BAT_DEFENDER.get(), level);
        bat.setOwner(owner);
        bat.life = life;
        bat.index = index;
        bat.attackCooldown = 10 + index % 7;
        bat.snapTo(at.x, at.y, at.z, owner.getRandom().nextFloat() * 360.0F, 0.0F);
        return bat;
    }

    /** The owner's bats that are still defending him (not scattering). */
    public static List<BatDefenderEntity> findAll(Player owner) {
        return owner.level().getEntitiesOfClass(BatDefenderEntity.class, owner.getBoundingBox().inflate(96),
                b -> b.isOwnedBy(owner) && !b.isRemoved() && !b.isScattering());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCATTER, false);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    public boolean isScattering() {
        return entityData.get(DATA_SCATTER);
    }

    /** Flies off into the night (and is gone). */
    public void scatter() {
        if (isScattering()) return;
        entityData.set(DATA_SCATTER, true);
        scatterTicks = 0;
        target = null;
        Vec3 away = new Vec3(random.nextGaussian(), 0, random.nextGaussian());
        setDeltaMovement(away.normalize().scale(0.4).add(0, 0.5, 0));
    }

    @Override
    protected boolean ownerValid() {
        Player owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level() && BatmanHelper.isSuited(owner);
    }

    @Override
    public void dissipate() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.25, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    // ---- tick -------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        interpolation.interpolate();
        if (level().isClientSide()) {
            if (tickCount % 3 == 0 && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.01, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (isScattering()) {
            setDeltaMovement(getDeltaMovement().add(0, 0.04, 0).scale(1.02));
            move(MoverType.SELF, getDeltaMovement());
            face(getDeltaMovement());
            if (++scatterTicks > SCATTER_TICKS) dissipate();
            return;
        }
        Player owner = getOwner();
        if (owner == null) return;
        if (--life <= 0) {
            scatter();
            return;
        }
        if (attackCooldown > 0) attackCooldown--;
        if ((tickCount + index) % 10 == 0 || (target != null && (!target.isAlive() || target.distanceToSqr(owner) > 20 * 20))) {
            target = findTarget(level, owner);
        }

        Vec3 motion = getDeltaMovement();
        Vec3 goal;
        double maxSpeed;
        if (target != null && attackCooldown == 0) {
            goal = target.position().add(0, target.getBbHeight() * 0.6, 0);
            maxSpeed = 0.9;
        } else {
            // Whirl around Batman, each bat on its own ring.
            double phase = tickCount * (0.14 + (index % 3) * 0.02) + index * Mth.TWO_PI / Math.max(1, GLConfig.BAT_SWARM_COUNT.get());
            double r = 1.4 + (index % 3) * 0.7 + Math.sin(tickCount * 0.05 + index) * 0.3;
            double h = 0.7 + (index % 4) * 0.45 + Math.sin(tickCount * 0.21 + index * 1.7) * 0.35;
            if (index % 2 == 1) phase = -phase;
            goal = owner.position().add(Math.cos(phase) * r, h, Math.sin(phase) * r);
            maxSpeed = owner.getDeltaMovement().length() + 0.7;
        }
        Vec3 steer = goal.subtract(position()).scale(0.14);
        motion = motion.scale(0.78).add(steer);
        if (motion.length() > maxSpeed) motion = motion.normalize().scale(maxSpeed);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        face(motion);

        if (target != null && attackCooldown == 0 && target.getBoundingBox().inflate(0.4).contains(position())) {
            attack(level, owner, target);
        } else if (target != null && attackCooldown == 0 && distanceToSqr(target.getBoundingBox().getCenter()) < 1.0) {
            attack(level, owner, target);
        }
        if (random.nextInt(160) == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 0.35F, 0.9F + random.nextFloat() * 0.3F);
        }
    }

    private void face(Vec3 motion) {
        if (motion.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(yaw);
        }
    }

    private @Nullable LivingEntity findTarget(ServerLevel level, Player owner) {
        List<BatDefenderEntity> swarm = findAll(owner);
        LivingEntity attacker = owner.getLastHurtByMob();
        if (valid(attacker, owner) && count(swarm, attacker) < MAX_PER_TARGET + 2) return attacker;
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(10),
                e -> e instanceof Enemy && valid(e, owner));
        enemies.sort(Comparator.comparingDouble(e -> e.distanceToSqr(owner)));
        for (LivingEntity enemy : enemies) {
            if (enemy == target || count(swarm, enemy) < MAX_PER_TARGET) return enemy;
        }
        return null;
    }

    private static boolean valid(@Nullable LivingEntity e, Player owner) {
        return e != null && e != owner && e.isAlive() && !e.isInvisible() && e.distanceToSqr(owner) < 16 * 16
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    private static int count(List<BatDefenderEntity> swarm, LivingEntity prey) {
        int n = 0;
        for (BatDefenderEntity bat : swarm) if (bat.target == prey) n++;
        return n;
    }

    /** Bites, blinds and confuses the prey, then flutters back to the swarm. */
    private void attack(ServerLevel level, Player owner, LivingEntity prey) {
        attackCooldown = 22 + random.nextInt(10);
        float damage = GLConfig.BAT_SWARM_DAMAGE.get().floatValue();
        prey.invulnerableTime = 0;
        if (prey.hurtServer(level, level.damageSources().indirectMagic(this, owner), damage)) {
            prey.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), owner);
            prey.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), owner);
            Vec3 away = prey.position().subtract(owner.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.25);
            prey.push(away.x, 0.08, away.z);
            prey.hurtMarked = true;
        }
        // A swarm in the face makes a monster forget who it was after.
        if (prey instanceof Mob mob && mob.getTarget() == owner && random.nextInt(3) == 0) mob.setTarget(null);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BAT_HURT, SoundSource.PLAYERS, 0.5F, 1.2F + random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 4, 0.15, 0.15, 0.15, 0.02);
        setDeltaMovement(getDeltaMovement().scale(-0.8).add(0, 0.3, 0));
    }

    /** Throws itself in front of a projectile flying at Batman. */
    public void intercept(Projectile projectile) {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 at = projectile.position();
        setPos(at.x, at.y, at.z);
        projectile.discard();
        attackCooldown = 15;
        setDeltaMovement(new Vec3(random.nextGaussian() * 0.2, 0.3, random.nextGaussian() * 0.2));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 0.7F, 1.3F);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.03);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.2);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
