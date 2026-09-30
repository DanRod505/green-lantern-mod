package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.BubbleConstructEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Construct 3: protective bubble. Click to raise it, click again to lower it. */
public class BubbleConstruct extends Construct {
    public BubbleConstruct() {
        super(GreenLantern.id("bubble"), 2);
    }

    @Override
    public UseMode useMode() {
        return UseMode.TOGGLE;
    }

    @Override
    public int activationCost() {
        return GLConfig.BUBBLE_COST.get();
    }

    @Override
    public int cooldown() {
        return 10;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        BubbleConstructEntity existing = BubbleConstructEntity.find(player);
        if (existing != null) {
            existing.dissipate();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RING_DEACTIVATE.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            return true;
        }
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        BubbleConstructEntity bubble = new BubbleConstructEntity(ModEntities.BUBBLE_CONSTRUCT.get(), player.level());
        bubble.setOwner(player);
        bubble.setPos(player.getX(), player.getY() + player.getBbHeight() / 2 - bubble.getBbHeight() / 2, player.getZ());
        player.level().addFreshEntity(bubble);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BUBBLE_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
