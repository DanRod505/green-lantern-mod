package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Construct 1: a heavy ball of hard light that explodes on impact (no block damage). */
public class EnergyBlastConstruct extends Construct {
    public EnergyBlastConstruct() {
        super(GreenLantern.id("energy_blast"), 0);
    }

    @Override
    public UseMode useMode() {
        return UseMode.INSTANT;
    }

    @Override
    public int activationCost() {
        return GLConfig.BLAST_COST.get();
    }

    @Override
    public int cooldown() {
        return 10;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) return false;
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(look.scale(0.8)).subtract(0, 0.25, 0);
        EnergyBoltEntity bolt = EnergyBoltEntity.create(level, player, start, look.scale(1.6), GLConfig.BLAST_DAMAGE.get().floatValue(), true);
        level.addFreshEntity(bolt);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BLAST_FIRE.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        level.sendParticles(ModParticles.SPARK.get(), start.x, start.y, start.z, 8, 0.1, 0.1, 0.1, 0.12);
        return true;
    }
}
