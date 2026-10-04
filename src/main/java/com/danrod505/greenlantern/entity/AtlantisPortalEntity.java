package com.danrod505.greenlantern.entity;

import com.danrod505.greenlantern.atlantis.AtlantisTravel;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A whirlpool standing upright, opened by Aquaman or the Atlantean Gate. Walking through it takes a
 * player to Atlantis, or, when it was opened in Atlantis, back to where they came from.
 */
public class AtlantisPortalEntity extends OaPortalEntity {
    private static final EntityDataAccessor<Boolean> DATA_HOME = SynchedEntityData.defineId(AtlantisPortalEntity.class, EntityDataSerializers.BOOLEAN);

    public AtlantisPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Opens a portal three blocks in front of the player, facing them. */
    public static AtlantisPortalEntity open(ServerLevel level, ServerPlayer player) {
        float yaw = player.getYRot();
        double x = player.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * 3.0;
        double z = player.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * 3.0;
        AtlantisPortalEntity portal = new AtlantisPortalEntity(ModEntities.ATLANTIS_PORTAL.get(), level);
        portal.snapTo(x, player.getY(), z, yaw + 180.0F, 0.0F);
        portal.entityData.set(DATA_HOME, AtlantisTravel.leadsHome(level, player.position()));
        level.addFreshEntity(portal);
        level.playSound(null, x, player.getY() + CENTER_Y, z, ModSounds.PORTAL_OPEN.get(), SoundSource.PLAYERS, 1.4F, 0.7F);
        level.sendParticles(ParticleTypes.SPLASH, x, player.getY() + CENTER_Y, z, 60, 0.7, 1.3, 0.7, 0.3);
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, player.getY() + CENTER_Y, z, 30, 0.6, 1.2, 0.6, 0.05);
        return portal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HOME, false);
    }

    @Override
    public boolean leadsHome() {
        return entityData.get(DATA_HOME);
    }

    @Override
    protected void travel(ServerPlayer player) {
        AtlantisTravel.travel(player, leadsHome());
    }

    @Override
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
            // Water spirals into the whirlpool.
            level().addParticle(ParticleTypes.BUBBLE_POP, x, getY() + CENTER_Y + ly, z, -lx * Mth.cos(yaw) * 0.04, -ly * 0.04, -lx * Mth.sin(yaw) * 0.04);
            level().addParticle(ParticleTypes.DOLPHIN, x, getY() + CENTER_Y + ly, z, 0, 0, 0);
        }
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SPLASH, getX(), getY() + 0.1, getZ(), 0, 0.1, 0);
        }
    }
}
