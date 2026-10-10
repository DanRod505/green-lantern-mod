package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bolt of Speed Force lightning thrown by the Flash. It flies straight and fast, hits hard and
 * arcs to the closest creatures around the first one it strikes.
 */
public class SpeedLightningEntity extends Projectile {
    private static final int MAX_LIFE = 40;
    private static final double CHAIN_RANGE = 6.0;

    private float damage = 12.0F;

    public SpeedLightningEntity(EntityType<? extends SpeedLightningEntity> type, Level level) {
        super(type, level);
    }

    public static SpeedLightningEntity create(Level level, LivingEntity owner, Vec3 pos, Vec3 velocity, float damage) {
        SpeedLightningEntity bolt = new SpeedLightningEntity(ModEntities.SPEED_LIGHTNING.get(), level);
        bolt.setOwner(owner);
        bolt.setPos(pos);
        bolt.setDeltaMovement(velocity);
        bolt.damage = damage;
        bolt.updateRotation();
        bolt.yRotO = bolt.getYRot();
        bolt.xRotO = bolt.getXRot();
        return bolt;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_LIFE) {
            discard();
            return;
        }
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
            if (isRemoved()) return;
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                double t = random.nextDouble();
                level().addParticle(ModParticles.SPEED_SPARK.get(), getX() - motion.x * t, getY() + 0.25 - motion.y * t, getZ() - motion.z * t,
                        (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof ConstructEntity) && !(target instanceof SpeedTornadoEntity)
                && !(target instanceof SpeedLightningEntity) && !(target instanceof EnergyBoltEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel level) {
            Entity target = result.getEntity();
            strike(level, target, damage, getDeltaMovement().normalize());
            chain(level, target);
        }
    }

    private void strike(ServerLevel level, Entity target, float amount, Vec3 push) {
        DamageSource source = ModDamageTypes.speedForce(level, this, getOwner());
        if (target.hurtServer(level, source, amount) && target instanceof LivingEntity living) {
            living.push(push.x * 0.6, 0.3, push.z * 0.6);
            living.hurtMarked = true;
        }
        level.sendParticles(ModParticles.SPEED_SPARK.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                16, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.25);
    }

    /** The lightning jumps to the closest creatures around the one it hit, for half damage. */
    private void chain(ServerLevel level, Entity first) {
        int jumps = GLConfig.LIGHTNING_CHAIN.get();
        if (jumps <= 0) return;
        Entity owner = getOwner();
        List<LivingEntity> hit = new ArrayList<>();
        Entity from = first;
        for (int i = 0; i < jumps; i++) {
            Entity source = from;
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(source.position(), source.position()).inflate(CHAIN_RANGE),
                    e -> e != first && e != owner && e.isAlive() && !hit.contains(e) && !e.isSpectator()
                            && !(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative()));
            near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(source)));
            if (near.isEmpty()) break;
            LivingEntity next = near.getFirst();
            hit.add(next);
            arc(level, source.position().add(0, source.getBbHeight() * 0.5, 0), next.position().add(0, next.getBbHeight() * 0.5, 0));
            strike(level, next, damage * 0.5F, next.position().subtract(source.position()).normalize());
            from = next;
        }
        if (!hit.isEmpty()) {
            level.playSound(null, first.getX(), first.getY(), first.getZ(), ModSounds.LIGHTNING_HIT.get(), SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    /** A jagged line of sparks between two points. */
    private void arc(ServerLevel level, Vec3 a, Vec3 b) {
        int steps = (int) Math.max(4, a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, (double) i / steps);
            double j = (i == 0 || i == steps) ? 0 : 0.25;
            level.sendParticles(ModParticles.SPEED_SPARK.get(), p.x + random.nextGaussian() * j, p.y + random.nextGaussian() * j, p.z + random.nextGaussian() * j,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            Vec3 p = result.getLocation();
            level.sendParticles(ModParticles.SPEED_RING.get(), p.x, p.y, p.z, 0, 0, 1, 0, 0.5);
            level.sendParticles(ModParticles.SPEED_SPARK.get(), p.x, p.y, p.z, 30, 0.2, 0.2, 0.2, 0.35);
            level.sendParticles(ModParticles.SPEED_STREAK.get(), p.x, p.y, p.z, 10, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, p.x, p.y, p.z, ModSounds.LIGHTNING_HIT.get(), SoundSource.PLAYERS, 1.4F, 0.9F + random.nextFloat() * 0.2F);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        damage = input.getFloatOr("Damage", 12.0F);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
