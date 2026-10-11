package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The server side of Cyborg: what each power does, and the Cyborg Battery coming back. */
public final class CyborgServer {
    private static final int GLOW = 0xFF5A3C;

    private CyborgServer() {}

    /** Uses a power with the Mother Box; returns whether it went off. */
    public static boolean usePower(ServerPlayer player, ItemStack item, CyborgPower power) {
        if (!CyborgHero.INSTANCE.isSuited(player)) return false;
        if (player.getCooldowns().isOnCooldown(item)) return false;
        if (!player.isCreative() && !CyborgHero.ENERGY.tryConsume(item, power.cost())) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_cyborg_power").withStyle(ChatFormatting.RED), true);
            return false;
        }
        // Each power still runs the placeholder: write the real one here (see the hero sheet).
        return switch (power) {
            case SONIC_CANNON -> placeholder(player, power);
            case SHOULDER_MISSILES -> placeholder(player, power);
            case TECH_SCAN -> placeholder(player, power);
            case MACHINE_HACK -> placeholder(player, power);
            case EMP_BURST -> placeholder(player, power);
            case SELF_REPAIR -> placeholder(player, power);
            case BOOM_TUBE -> placeholder(player, power);
        };
    }

    /** Until the power is written: a flash of the hero's colour in front of them, a sound and a note in the action bar. */
    private static boolean placeholder(ServerPlayer player, CyborgPower power) {
        ServerLevel level = player.level();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        level.sendParticles(new DustParticleOptions(GLOW, 1.5F), at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(Component.translatable("message.greenlantern.power_todo", power.displayName()), true);
        player.getCooldowns().addCooldown(player.getMainHandItem(), 10);
        GreenLantern.LOGGER.debug("cyborg: placeholder power {}", power.id());
        return true;
    }

    /** Every second the Cyborg Battery in the Mother Box comes back a little. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        ItemStack item = CyborgHero.INSTANCE.findItem(player);
        if (!item.isEmpty()) CyborgHero.ENERGY.add(item, CyborgConfig.RECHARGE_PER_SECOND.get());
    }
}
