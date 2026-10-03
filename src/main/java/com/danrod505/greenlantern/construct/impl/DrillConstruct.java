package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.DrillConstructEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Construct 6: the giant drill vehicle. Click to summon and ride it, click again (or sneak) to dismiss. */
public class DrillConstruct extends Construct {
    public DrillConstruct() {
        super(GreenLantern.id("drill"), 6);
    }

    @Override
    public UseMode useMode() {
        return UseMode.TOGGLE;
    }

    @Override
    public int activationCost() {
        return GLConfig.DRILL_COST.get();
    }

    @Override
    public int cooldown() {
        return 20;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        if (player.getVehicle() instanceof DrillConstructEntity drill) {
            player.stopRiding();
            drill.dissipate();
            return true;
        }
        if (player.isPassenger()) return false;
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        ServerLevel level = player.level();
        DrillConstructEntity drill = new DrillConstructEntity(ModEntities.DRILL_CONSTRUCT.get(), level);
        drill.setOwner(player);
        drill.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        if (!level.noCollision(drill)) {
            drill.setPos(player.getX(), player.getY() + 0.6, player.getZ());
        }
        level.addFreshEntity(drill);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.startRiding(drill);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRILL_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
