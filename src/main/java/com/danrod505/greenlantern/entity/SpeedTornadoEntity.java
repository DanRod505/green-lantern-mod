package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.flash.SpeedsterServer;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Flash's tornado. Its owner runs in circles around it (driven by the owner's client, see
 * SpeedController) and the vortex pulls in, lifts, spins and batters every creature, item and
 * projectile close to it. When it ends it flings everything it caught outwards.
 */
public class SpeedTornadoEntity extends Entity {
    /** Radius (blocks) of the circle the Flash runs around the center. */
    public static final double ORBIT_RADIUS = 3.2;
    /** Height (blocks) of the funnel. */
    public static final double HEIGHT = 10.0;
    /** Ticks the tornado takes to spin up to full strength. */
    public static final int SPIN_UP = 20;
    private static final int COLLAPSE_TICKS = 12;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(SpeedTornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLLAPSE = SynchedEntityData.defineId(SpeedTornadoEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID ownerUuid;
    private int life;
    private int maxLife = 160;

    public SpeedTornadoEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static SpeedTornadoEntity create(Level level, Player owner, Vec3 center) {
        SpeedTornadoEntity tornado = new SpeedTornadoEntity(ModEntities.SPEED_TORNADO.get(), level);
        tornado.ownerUuid = owner.getUUID();
        tornado.entityData.set(DATA_OWNER_ID, owner.getId());
        tornado.maxLife = (int) Math.round(GLConfig.TORNADO_SECONDS.get() * 20);
        tornado.setPos(center.x, Math.floor(owner.getY() + 0.01), center.z);
        return tornado;
    }

    /** The active (not collapsing) tornado of a player, if any. */
    public static @Nullable SpeedTornadoEntity find(Player owner) {
        List<SpeedTornadoEntity> list = owner.level().getEntitiesOfClass(SpeedTornadoEntity.class, owner.getBoundingBox().inflate(24),
                t -> t.isOwnedBy(owner) && !t.isCollapsing() && !t.isRemoved());
        return list.isEmpty() ? null : list.getFirst();
    }

    public boolean isOwnedBy(Entity entity) {
        return entity instanceof Player player && (player.getUUID().equals(ownerUuid) || player.getId() == entityData.get(DATA_OWNER_ID));
    }

    public int ownerId() {
        return entityData.get(DATA_OWNER_ID);
    }

    public boolean isCollapsing() {
        return entityData.get(DATA_COLLAPSE) > 0;
    }

    /** 0-1: grows while spinning up, shrinks while collapsing. */
    public float strength(float partialTick) {
        float grow = Mth.clamp((tickCount + partialTick) / SPIN_UP, 0.0F, 1.0F);
        int collapse = entityData.get(DATA_COLLAPSE);
        if (collapse > 0) grow *= Mth.clamp(1.0F - (collapse + partialTick) / COLLAPSE_TICKS, 0.0F, 1.0F);
        return grow;
    }

    /** Starts the end of the tornado (it fades out and flings everything it caught). */
    public void collapse() {
        if (!isCollapsing()) {
            entityData.set(DATA_COLLAPSE, 1);
            if (!level().isClientSide() && getOwner() instanceof ServerPlayer owner) {
                SpeedsterServer.sync(owner, -1);
            }
        }
    }

    private @Nullable Player getOwner() {
        if (level().isClientSide()) {
            return level().getEntity(entityData.get(DATA_OWNER_ID)) instanceof Player player ? player : null;
        }
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_ID, -1);
        builder.define(DATA_COLLAPSE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientEffects();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Player owner = getOwner();
        boolean ownerOk = owner instanceof ServerPlayer && owner.isAlive() && FlashHelper.isSuited(owner)
                && owner.level() == level && owner.distanceToSqr(this) < 24 * 24;
        int collapse = entityData.get(DATA_COLLAPSE);
        if (collapse == 0) {
            life++;
            if (!ownerOk || life >= maxLife) {
                collapse();
            } else if (life % 20 == 0 && !((ServerPlayer) owner).isCreative()) {
                ItemStack ring = FlashHelper.findRing(owner);
                if (ring.isEmpty() || !FlashHero.SPEED_FORCE.tryConsume(ring, GLConfig.TORNADO_COST_PER_SECOND.get())) {
                    SpeedsterServer.notifyNoSpeedForce((ServerPlayer) owner);
                    collapse();
                }
            }
        } else {
            entityData.set(DATA_COLLAPSE, collapse + 1);
            if (collapse + 1 >= COLLAPSE_TICKS) {
                fling(level, owner);
                discard();
                if (owner instanceof ServerPlayer serverOwner) SpeedsterServer.sync(serverOwner, -1);
                return;
            }
        }
        spin(level, owner);
        if (tickCount % 24 == 1) {
            level.playSound(null, getX(), getY() + 2, getZ(), ModSounds.TORNADO_LOOP.get(), SoundSource.PLAYERS, 1.2F * strength(0), 0.9F + 0.2F * strength(0));
        }
    }

    private AABB area(double radius) {
        return new AABB(getX() - radius, getY() - 1, getZ() - radius, getX() + radius, getY() + HEIGHT + 1, getZ() + radius);
    }

    private List<Entity> caught(Level level, @Nullable Player owner, double radius) {
        return level.getEntities(this, area(radius), e -> e != owner && e.isAlive() && !e.isSpectator()
                && !(e instanceof SpeedTornadoEntity) && !(e instanceof ConstructEntity)
                && !(e instanceof Player p && (p.isCreative() || (owner != null && p.getUUID().equals(owner.getUUID()))))
                && (e instanceof LivingEntity || e instanceof ItemEntity || e instanceof Projectile));
    }

    /** Pulls, lifts and spins everything in reach; hurts living things twice a second. */
    private void spin(ServerLevel level, @Nullable Player owner) {
        float strength = strength(0);
        double radius = GLConfig.TORNADO_RADIUS.get() * (0.4 + 0.6 * strength);
        float damage = GLConfig.TORNADO_DAMAGE.get().floatValue() * strength;
        for (Entity e : caught(level, owner, radius)) {
            Vec3 toCenter = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
            double dist = toCenter.length();
            if (dist > radius) continue;
            double t = 1.0 - dist / radius;
            Vec3 inward = dist < 1.0E-3 ? new Vec3(1, 0, 0) : toCenter.scale(1.0 / dist);
            // Counterclockwise seen from above, the same way the Flash runs around it.
            Vec3 tangent = new Vec3(inward.z, 0, -inward.x);
            double height = e.getY() - getY();
            double lift = height < HEIGHT - 1.5 ? (0.08 + 0.2 * t) * strength : -0.04;
            // Everything settles on a ring around the core instead of piling up in the middle.
            double pull = dist > 1.6 ? (0.06 + 0.12 * t) : -0.12;
            Vec3 v = e.getDeltaMovement().scale(0.5)
                    .add(inward.scale(pull * strength))
                    .add(tangent.scale((0.3 + 0.45 * t) * strength))
                    .add(0, lift, 0);
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            if (e.fallDistance > 4) e.fallDistance = 4;
            if (life % 10 == 0 && damage > 0 && e instanceof LivingEntity living) {
                living.hurtServer(level, ModDamageTypes.speedForce(level, this, owner), damage);
            }
        }
    }

    /** End of the tornado: everything caught is thrown outwards. */
    private void fling(ServerLevel level, @Nullable Player owner) {
        for (Entity e : caught(level, owner, GLConfig.TORNADO_RADIUS.get())) {
            Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.setDeltaMovement(away.scale(0.9).add(0, 0.35, 0));
            e.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2, getZ(), 40, 2.0, 2.0, 2.0, 0.2);
        level.sendParticles(ModParticles.SPEED_SPARK.get(), getX(), getY() + 2, getZ(), 30, 1.5, 2.0, 1.5, 0.3);
    }

    /** Dust, debris and sparks swirling around the funnel (client only). */
    private void clientEffects() {
        float strength = strength(0);
        if (strength <= 0.05F) return;
        Level level = level();
        for (int i = 0; i < 6; i++) {
            double h = random.nextDouble() * HEIGHT * strength;
            double r = 0.8 + h * 0.35 + random.nextDouble() * 0.6;
            double a = random.nextDouble() * Mth.TWO_PI;
            double x = getX() + Math.cos(a) * r;
            double z = getZ() + Math.sin(a) * r;
            // Tangential velocity: counterclockwise seen from above.
            double vx = -Math.sin(a) * 0.45;
            double vz = Math.cos(a) * 0.45;
            level.addParticle(i < 4 ? ParticleTypes.CLOUD : ParticleTypes.POOF, x, getY() + h, z, vx, 0.08, vz);
        }
        BlockPos ground = BlockPos.containing(getX(), getY() - 0.5, getZ());
        BlockState state = level.getBlockState(ground);
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double r = 1.0 + random.nextDouble() * 2.5;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), getX() + Math.cos(a) * r, getY() + 0.2, getZ() + Math.sin(a) * r,
                        -Math.sin(a) * 0.5, 0.35, Math.cos(a) * 0.5);
            }
        }
        if (random.nextFloat() < 0.5F) {
            double h = random.nextDouble() * HEIGHT * strength;
            double a = random.nextDouble() * Mth.TWO_PI;
            double r = 0.8 + h * 0.35;
            level.addParticle(ModParticles.SPEED_SPARK.get(), getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r, 0, 0, 0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
