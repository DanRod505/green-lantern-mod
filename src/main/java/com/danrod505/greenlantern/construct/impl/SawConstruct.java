package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.SawConstructEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Construct 4: the giant saw vehicle. Click to summon and ride it, click again (or sneak) to dismiss. */
public class SawConstruct extends Construct {
    public SawConstruct() {
        super(GreenLantern.id("saw"), 3);
    }

    @Override
    public UseMode useMode() {
        return UseMode.TOGGLE;
    }

    @Override
    public int activationCost() {
        return GLConfig.SAW_COST.get();
    }

    @Override
    public int cooldown() {
        return 20;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        if (player.getVehicle() instanceof SawConstructEntity saw) {
            player.stopRiding();
            saw.dissipate();
            return true;
        }
        if (player.isPassenger()) return false;
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        ServerLevel level = player.level();
        SawConstructEntity saw = new SawConstructEntity(ModEntities.SAW_CONSTRUCT.get(), level);
        saw.setOwner(player);
        saw.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        if (!level.noCollision(saw)) {
            saw.setPos(player.getX(), player.getY() + 0.6, player.getZ());
        }
        level.addFreshEntity(saw);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.startRiding(saw);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SAW_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
