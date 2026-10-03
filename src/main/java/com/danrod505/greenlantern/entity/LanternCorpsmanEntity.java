package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.oa.Oa;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A fellow member of the Green Lantern Corps living on Oa. Each one belongs to one of several
 * species ({@link #variant()}); some stroll around the plaza, others patrol the sky in circles
 * around the Central Power Battery. They chat when spoken to and can't be harmed.
 */
public class LanternCorpsmanEntity extends PathfinderMob {
    public static final int VARIANTS = 8;
    public static final int LINES = 10;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(LanternCorpsmanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(LanternCorpsmanEntity.class, EntityDataSerializers.BOOLEAN);

    private float orbitRadius = 18.0F;
    private float orbitHeight = Oa.GROUND_Y + 20;
    private float orbitSpeed = 0.015F;
    private float orbitPhase;
    private int talkCooldown;

    public LanternCorpsmanEntity(EntityType<? extends LanternCorpsmanEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    public static void spawnWalker(ServerLevel level, double x, double y, double z, int variant, float yaw) {
        LanternCorpsmanEntity lantern = ModEntities.LANTERN_CORPSMAN.get().create(level, EntitySpawnReason.STRUCTURE);
        if (lantern == null) return;
        lantern.setVariant(variant);
        lantern.snapTo(x, y, z, yaw, 0.0F);
        lantern.setYHeadRot(yaw);
        lantern.setYBodyRot(yaw);
        lantern.setHomeTo(Oa.CENTER, 42);
        level.addFreshEntity(lantern);
    }

    public static void spawnFlyer(ServerLevel level, float radius, float height, float speed, float phase, int variant) {
        LanternCorpsmanEntity lantern = ModEntities.LANTERN_CORPSMAN.get().create(level, EntitySpawnReason.STRUCTURE);
        if (lantern == null) return;
        lantern.setVariant(variant);
        lantern.entityData.set(DATA_FLYING, true);
        lantern.orbitRadius = radius;
        lantern.orbitHeight = height;
        lantern.orbitSpeed = speed;
        lantern.orbitPhase = phase;
        lantern.setNoGravity(true);
        double[] p = lantern.orbitPoint(level.getGameTime());
        lantern.snapTo(p[0], p[1], p[2], 0.0F, 0.0F);
        level.addFreshEntity(lantern);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_FLYING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, LanternCorpsmanEntity.class, 6.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public int variant() {
        return Mth.clamp(entityData.get(DATA_VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int variant) {
        entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANTS));
    }

    public boolean isFlyingLantern() {
        return entityData.get(DATA_FLYING);
    }

    /** Point of the patrol circle at the given game time: {x, y, z, yaw}. */
    private double[] orbitPoint(long gameTime) {
        double a = orbitPhase + gameTime * orbitSpeed;
        double x = Oa.CENTER.getX() + 0.5 + Math.cos(a) * orbitRadius;
        double z = Oa.CENTER.getZ() + 0.5 + Math.sin(a) * orbitRadius;
        double y = orbitHeight + Math.sin(a * 3.0) * 1.5;
        // Velocity direction of the circle (sign follows the direction of travel).
        double vx = -Math.sin(a) * Math.signum(orbitSpeed);
        double vz = Math.cos(a) * Math.signum(orbitSpeed);
        double yaw = Math.toDegrees(Math.atan2(-vx, vz));
        return new double[] {x, y, z, yaw};
    }

    @Override
    public void aiStep() {
        if (talkCooldown > 0) talkCooldown--;
        if (!isFlyingLantern()) {
            super.aiStep();
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            setNoGravity(true);
            double[] p = orbitPoint(serverLevel.getGameTime());
            float yaw = (float) p[3];
            setDeltaMovement(p[0] - getX(), p[1] - getY(), p[2] - getZ());
            setPos(p[0], p[1], p[2]);
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        } else {
            // Client: keep interpolating the server's positions and leave a trail of green light.
            super.aiStep();
            if (random.nextInt(2) == 0) {
                level().addParticle(ModParticles.GLOW.get(), getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.4, getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide() && talkCooldown == 0) {
            talkCooldown = 20;
            Component line = Component.translatable("entity.greenlantern.lantern_corpsman.say." + random.nextInt(LINES));
            player.displayClientMessage(Component.translatable("message.greenlantern.npc_says", getDisplayName(), line).withStyle(ChatFormatting.GREEN), false);
            getLookControl().setLookAt(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isFlyingLantern() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
        output.putBoolean("Flying", isFlyingLantern());
        output.putFloat("OrbitRadius", orbitRadius);
        output.putFloat("OrbitHeight", orbitHeight);
        output.putFloat("OrbitSpeed", orbitSpeed);
        output.putFloat("OrbitPhase", orbitPhase);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
        entityData.set(DATA_FLYING, input.getBooleanOr("Flying", false));
        orbitRadius = input.getFloatOr("OrbitRadius", orbitRadius);
        orbitHeight = input.getFloatOr("OrbitHeight", orbitHeight);
        orbitSpeed = input.getFloatOr("OrbitSpeed", orbitSpeed);
        orbitPhase = input.getFloatOr("OrbitPhase", orbitPhase);
    }
}
