package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.registry.ModEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A citizen of Atlantis. Citizens drift between the houses, gardens and avenues of the city; the
 * royal guard, trident in hand, swims patrols around the palace and along the city wall, and drives
 * off any monster that comes close. They talk when spoken to and can't be harmed.
 */
public class AtlanteanEntity extends PathfinderMob {
    public static final int VARIANTS = 6;
    public static final int LINES = 10;
    public static final int GUARD_LINES = 6;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(AtlanteanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GUARD = SynchedEntityData.defineId(AtlanteanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(AtlanteanEntity.class, EntityDataSerializers.BOOLEAN);

    /** Center of the city and the floor height (first water block). */
    private BlockPos home = BlockPos.ZERO;
    // Guards: patrol circle.
    private float orbitRadius = 20.0F;
    private float orbitHeight = 26.0F;
    private float orbitSpeed = 0.01F;
    private float orbitPhase;
    // Citizens: where they are swimming to.
    private @Nullable Vec3 waypoint;
    private int waypointTicks;
    private int restTicks;
    // Guards: the monster they chase.
    private @Nullable Mob prey;
    private int attackCooldown;
    private int talkCooldown;

    public AtlanteanEntity(EntityType<? extends AtlanteanEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    public static void spawnCitizen(ServerLevel level, BlockPos home, double x, double y, double z, int variant) {
        AtlanteanEntity atlantean = ModEntities.ATLANTEAN.get().create(level, EntitySpawnReason.STRUCTURE);
        if (atlantean == null) return;
        atlantean.home = home;
        atlantean.setVariant(variant);
        atlantean.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(atlantean);
    }

    public static void spawnGuard(ServerLevel level, BlockPos home, float radius, float height, float speed, float phase, int variant) {
        AtlanteanEntity guard = ModEntities.ATLANTEAN.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guard == null) return;
        guard.home = home;
        guard.setVariant(variant);
        guard.entityData.set(DATA_GUARD, true);
        guard.orbitRadius = radius;
        guard.orbitHeight = height;
        guard.orbitSpeed = speed;
        guard.orbitPhase = phase;
        guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        guard.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        Vec3 p = guard.orbitPoint(level.getGameTime());
        guard.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
        level.addFreshEntity(guard);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_GUARD, false);
        builder.define(DATA_SWIMMING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, AtlanteanEntity.class, 6.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public int variant() {
        return Mth.clamp(entityData.get(DATA_VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int variant) {
        entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANTS));
    }

    public boolean isGuard() {
        return entityData.get(DATA_GUARD);
    }

    /** Swimming with the body level (moving fast) rather than floating upright. */
    public boolean isSwimmingFast() {
        return entityData.get(DATA_SWIMMING);
    }

    // ---- movement --------------------------------------------------------------------------------

    private Vec3 orbitPoint(long gameTime) {
        double a = orbitPhase + gameTime * orbitSpeed;
        return new Vec3(home.getX() + 0.5 + Math.cos(a) * orbitRadius, home.getY() + orbitHeight + Math.sin(a * 2.0) * 1.2,
                home.getZ() + 0.5 + Math.sin(a) * orbitRadius);
    }

    @Override
    public void aiStep() {
        if (talkCooldown > 0) talkCooldown--;
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 velocity = isGuard() ? guardVelocity(serverLevel) : citizenVelocity(serverLevel);
            setDeltaMovement(velocity);
            double speed = velocity.horizontalDistance();
            entityData.set(DATA_SWIMMING, speed > 0.12);
            if (speed > 0.01) {
                float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
                setYRot(Mth.approachDegrees(getYRot(), yaw, 12.0F));
                setYBodyRot(getYRot());
            }
        } else if (isSwimmingFast() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.BUBBLE, getX() + (random.nextDouble() - 0.5) * 0.5, getY() + 0.4, getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0.05, 0);
        }
        super.aiStep();
    }

    /** Citizens swim slowly from one spot of the city to another, resting a while at each. */
    private Vec3 citizenVelocity(ServerLevel level) {
        if (restTicks > 0) {
            restTicks--;
            // Treading water: a gentle bob.
            return new Vec3(0, Mth.sin(tickCount * 0.08F) * 0.01, 0);
        }
        if (waypoint == null || waypointTicks-- <= 0 || position().distanceToSqr(waypoint) < 1.0) {
            if (waypoint != null) restTicks = 40 + random.nextInt(160);
            double a = random.nextDouble() * Mth.TWO_PI;
            double d = 20 + random.nextDouble() * 34;
            waypoint = new Vec3(home.getX() + 0.5 + Math.cos(a) * d, home.getY() + 0.5 + random.nextDouble() * 6.0, home.getZ() + 0.5 + Math.sin(a) * d);
            waypointTicks = 400;
            return Vec3.ZERO;
        }
        Vec3 to = waypoint.subtract(position());
        double len = to.length();
        double speed = 0.07 + (variant() % 3) * 0.015;
        Vec3 velocity = to.scale(Math.min(speed, len) / Math.max(len, 1.0E-4));
        // Bumped into a wall: pick another spot.
        if (horizontalCollision) waypointTicks = Math.min(waypointTicks, 10);
        return velocity;
    }

    /** Guards circle their patrol and chase monsters that come near. */
    private Vec3 guardVelocity(ServerLevel level) {
        if (attackCooldown > 0) attackCooldown--;
        if (tickCount % 20 == 0 && (prey == null || !prey.isAlive())) {
            prey = null;
            List<Mob> monsters = level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(14.0), mob -> mob instanceof Enemy && mob.isAlive());
            if (!monsters.isEmpty()) prey = monsters.getFirst();
        }
        if (prey != null) {
            if (!prey.isAlive() || prey.distanceToSqr(this) > 32 * 32) {
                prey = null;
            } else {
                getLookControl().setLookAt(prey);
                Vec3 to = prey.position().add(0, prey.getBbHeight() * 0.5, 0).subtract(position().add(0, getBbHeight() * 0.5, 0));
                if (to.lengthSqr() < 2.5 * 2.5 && attackCooldown == 0) {
                    attackCooldown = 15;
                    swing(InteractionHand.MAIN_HAND);
                    prey.hurtServer(level, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                    level.sendParticles(ParticleTypes.BUBBLE_POP, prey.getX(), prey.getY() + prey.getBbHeight() * 0.5, prey.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                return to.lengthSqr() < 1.5 * 1.5 ? Vec3.ZERO : to.normalize().scale(0.35);
            }
        }
        Vec3 to = orbitPoint(level.getGameTime() + 1).subtract(position());
        double len = to.length();
        return len < 1.0E-4 ? Vec3.ZERO : to.scale(Math.min(0.55, len) / len);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide() && talkCooldown == 0) {
            talkCooldown = 20;
            restTicks = Math.max(restTicks, 60);
            Component line = isGuard()
                    ? Component.translatable("entity.greenlantern.atlantean.guard.say." + random.nextInt(GUARD_LINES))
                    : Component.translatable("entity.greenlantern.atlantean.say." + random.nextInt(LINES));
            player.displayClientMessage(Component.translatable("message.greenlantern.npc_says", getDisplayName(), line).withStyle(ChatFormatting.AQUA), false);
            getLookControl().setLookAt(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName() {
        return hasCustomName() ? super.getName()
                : Component.translatable(isGuard() ? "entity.greenlantern.atlantean.guard" : "entity.greenlantern.atlantean");
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
        output.putBoolean("Guard", isGuard());
        output.putInt("HomeX", home.getX());
        output.putInt("HomeY", home.getY());
        output.putInt("HomeZ", home.getZ());
        output.putFloat("OrbitRadius", orbitRadius);
        output.putFloat("OrbitHeight", orbitHeight);
        output.putFloat("OrbitSpeed", orbitSpeed);
        output.putFloat("OrbitPhase", orbitPhase);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
        entityData.set(DATA_GUARD, input.getBooleanOr("Guard", false));
        home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
        orbitRadius = input.getFloatOr("OrbitRadius", orbitRadius);
        orbitHeight = input.getFloatOr("OrbitHeight", orbitHeight);
        orbitSpeed = input.getFloatOr("OrbitSpeed", orbitSpeed);
        orbitPhase = input.getFloatOr("OrbitPhase", orbitPhase);
    }
}
