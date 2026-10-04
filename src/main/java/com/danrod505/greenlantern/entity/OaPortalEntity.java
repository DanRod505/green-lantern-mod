package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.oa.Oa;
import com.danrod505.greenlantern.oa.OaTravel;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A standing ring of hard light opened by the Portal construct. Walking through it takes a player
 * to Oa, or back home when the portal was opened on Oa. It stays open for a short while, so
 * friends can follow, then closes.
 */
public class OaPortalEntity extends Entity {
    public static final int LIFETIME = 400;
    public static final int OPEN_TICKS = 16;
    public static final int CLOSE_TICKS = 16;
    /** Half width and height of the oval opening. */
    public static final float HALF_WIDTH = 1.35F;
    public static final float HALF_HEIGHT = 1.85F;
    /** Height of the oval's centre above the portal's feet. */
    public static final float CENTER_Y = 2.0F;

    private final Set<UUID> travelled = new HashSet<>();

    public OaPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    /** Whether walking through leads back home (the portal stands on Oa). */
    public boolean leadsHome() {
        return Oa.is(level());
    }

    /** 0 while opening/closing, 1 when fully open. */
    public float openness(float partialTick) {
        float age = tickCount + partialTick;
        float open = Mth.clamp(age / OPEN_TICKS, 0.0F, 1.0F);
        float close = Mth.clamp((LIFETIME - age) / CLOSE_TICKS, 0.0F, 1.0F);
        return Math.min(open, close);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientParticles();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount >= LIFETIME) {
            level.sendParticles(ModParticles.GLOW.get(), getX(), getY() + CENTER_Y, getZ(), 40, 0.8, 1.2, 0.8, 0.03);
            discard();
            return;
        }
        if (tickCount % 40 == 0) {
            level.playSound(null, getX(), getY() + CENTER_Y, getZ(), ModSounds.PORTAL_HUM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        if (tickCount < OPEN_TICKS || tickCount > LIFETIME - CLOSE_TICKS) return;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(1.0))) {
            if (!player.isSpectator() && player.isAlive() && insideOpening(player) && travelled.add(player.getUUID())) {
                travel(player);
            }
        }
    }

    /** Sends a player who walked through the portal on their way. */
    protected void travel(ServerPlayer player) {
        OaTravel.travel(player);
    }

    /** Whether the player stands in the oval opening (in the portal's own frame). */
    public boolean insideOpening(Entity entity) {
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double depth = dx * -Mth.sin(yaw) + dz * Mth.cos(yaw);
        double lateral = dx * Mth.cos(yaw) + dz * Mth.sin(yaw);
        double dy = entity.getY() - getY();
        return Math.abs(depth) < 0.8 && Math.abs(lateral) < HALF_WIDTH && dy > -0.6 && dy < CENTER_Y + 0.6;
    }

    protected void clientParticles() {
        float open = openness(0.0F);
        if (open <= 0.05F) return;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        for (int i = 0; i < 3; i++) {
            float a = random.nextFloat() * Mth.TWO_PI;
            double lx = Mth.cos(a) * HALF_WIDTH * open;
            double ly = Mth.sin(a) * HALF_HEIGHT * open;
            double x = getX() + lx * Mth.cos(yaw);
            double z = getZ() + lx * Mth.sin(yaw);
            // Sparks drift from the rim towards the centre: the portal pulls light in.
            double vx = -lx * Mth.cos(yaw) * 0.05;
            double vz = -lx * Mth.sin(yaw) * 0.05;
            level().addParticle(ModParticles.SPARK.get(), x, getY() + CENTER_Y + ly, z, vx, -ly * 0.05, vz);
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
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
