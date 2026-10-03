package com.danrod505.greenlantern.construct.impl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.entity.OaPortalEntity;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.oa.Oa;
import com.danrod505.greenlantern.registry.ModEntities;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.RingEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Construct 8: a portal to Oa. On Oa, the same construct opens the way back home. */
public class PortalConstruct extends Construct {
    public PortalConstruct() {
        super(GreenLantern.id("portal"), 8);
    }

    @Override
    public UseMode useMode() {
        return UseMode.INSTANT;
    }

    @Override
    public int activationCost() {
        return GLConfig.PORTAL_COST.get();
    }

    @Override
    public int cooldown() {
        return 60;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack ring) {
        if (!player.isCreative() && !RingEnergy.tryConsume(ring, activationCost())) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        ServerLevel level = player.level();
        OaPortalEntity portal = open(level, player);
        level.playSound(null, portal.getX(), portal.getY() + OaPortalEntity.CENTER_Y, portal.getZ(), ModSounds.PORTAL_OPEN.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        level.sendParticles(ModParticles.GLOW.get(), portal.getX(), portal.getY() + OaPortalEntity.CENTER_Y, portal.getZ(), 40, 0.6, 1.2, 0.6, 0.04);
        boolean home = Oa.is(level);
        player.displayClientMessage(Component.translatable(home ? "message.greenlantern.portal_home" : "message.greenlantern.portal_oa").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Opens a portal three blocks in front of the player, facing them. */
    public static OaPortalEntity open(ServerLevel level, ServerPlayer player) {
        float yaw = player.getYRot();
        double x = player.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * 3.0;
        double z = player.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * 3.0;
        OaPortalEntity portal = new OaPortalEntity(ModEntities.OA_PORTAL.get(), level);
        portal.snapTo(x, player.getY(), z, yaw + 180.0F, 0.0F);
        level.addFreshEntity(portal);
        return portal;
    }
}
