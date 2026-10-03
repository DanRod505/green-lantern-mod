package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.MechaEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Construct 7: the giant mecha. Click to build it around you and take the controls, click again (or sneak) to climb out. */
public class MechaConstruct extends Construct {
    public MechaConstruct() {
        super(GreenLantern.id("mecha"), 7);
    }

    @Override
    public UseMode useMode() {
        return UseMode.TOGGLE;
    }

    @Override
    public int activationCost() {
        return GLConfig.MECHA_COST.get();
    }

    @Override
    public int cooldown() {
        return 30;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        if (player.getVehicle() instanceof MechaEntity mecha) {
            player.stopRiding();
            mecha.dissipate();
            return true;
        }
        if (player.isPassenger()) return false;
        ServerLevel level = player.level();
        MechaEntity mecha = MechaEntity.create(level, player);
        mecha.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        // Needs room for a 10 block giant: try a little higher before giving up.
        boolean fits = level.noCollision(mecha);
        for (int i = 1; i <= 6 && !fits; i++) {
            mecha.setPos(player.getX(), player.getY() + i * 0.5, player.getZ());
            fits = level.noCollision(mecha);
        }
        if (!fits) {
            player.displayClientMessage(Component.translatable("message.greenlantern.mecha_no_room").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        level.addFreshEntity(mecha);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.startRiding(mecha);
        level.playSound(null, mecha.getX(), mecha.getY() + 5, mecha.getZ(), ModSounds.MECHA_SUMMON.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        level.sendParticles(ModParticles.SHOCKWAVE.get(), mecha.getX(), mecha.getY() + 0.1, mecha.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.GLOW.get(), mecha.getX(), mecha.getY() + 5, mecha.getZ(), 60, 1.2, 3.0, 1.2, 0.02);
        player.displayClientMessage(Component.translatable("message.greenlantern.mecha_controls").withStyle(ChatFormatting.GREEN), true);
        return true;
    }
}
