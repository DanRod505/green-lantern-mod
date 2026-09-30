package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.EnergyBoltEntity;
import com.danrod505.greenlantern.entity.GunConstructEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Construct 2: a floating gatling gun that fires while the use key is held. */
public class MinigunConstruct extends Construct {
    private static final int SPIN_UP_TICKS = 6;
    private static final int FIRE_INTERVAL = 2;

    public MinigunConstruct() {
        super(GreenLantern.id("minigun"), 1);
    }

    @Override
    public UseMode useMode() {
        return UseMode.HOLD;
    }

    @Override
    public int activationCost() {
        return GLConfig.MINIGUN_COST_PER_SHOT.get();
    }

    @Override
    public int cooldown() {
        return 6;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        GunConstructEntity gun = findGun(player);
        if (gun == null) {
            gun = new GunConstructEntity(ModEntities.GUN_CONSTRUCT.get(), player.level());
            gun.setOwner(player);
            gun.setPos(player.getX(), player.getEyeY() - 0.5, player.getZ());
            player.level().addFreshEntity(gun);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CONSTRUCT_SELECT.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        return true;
    }

    @Override
    public void holdTick(ServerPlayer player, ItemStack ring, int ticksHeld) {
        GunConstructEntity gun = findGun(player);
        if (gun == null) {
            activate(player, ring);
            return;
        }
        gun.setSpinTicks(ticksHeld);
        if (ticksHeld < SPIN_UP_TICKS || ticksHeld % FIRE_INTERVAL != 0) return;

        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) {
            PowerRingItem.notifyNoEnergy(player);
            player.stopUsingItem();
            return;
        }
        ServerLevel level = player.level();
        Vec3 muzzle = gun.muzzlePosition();
        // Aim at what the player is looking at, with a little spread.
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(48));
        Vec3 dir = target.subtract(muzzle).normalize();
        double spread = 0.025;
        dir = dir.add(level.random.nextGaussian() * spread, level.random.nextGaussian() * spread, level.random.nextGaussian() * spread).normalize();
        EnergyBoltEntity bolt = EnergyBoltEntity.create(level, player, muzzle, dir.scale(2.6), GLConfig.MINIGUN_DAMAGE.get().floatValue(), false);
        level.addFreshEntity(bolt);
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, ModSounds.GUN_FIRE.get(), SoundSource.PLAYERS, 0.55F, 0.9F + level.random.nextFloat() * 0.2F);
        level.sendParticles(ModParticles.SPARK.get(), muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.08);
    }

    @Override
    public void release(ServerPlayer player, ItemStack ring, int ticksHeld) {
        GunConstructEntity gun = findGun(player);
        if (gun != null) gun.dissipate();
    }

    private static GunConstructEntity findGun(ServerPlayer player) {
        List<GunConstructEntity> guns = player.level().getEntitiesOfClass(GunConstructEntity.class,
                player.getBoundingBox().inflate(4), gun -> gun.isOwnedBy(player) && gun.isAlive());
        return guns.isEmpty() ? null : guns.getFirst();
    }
}
