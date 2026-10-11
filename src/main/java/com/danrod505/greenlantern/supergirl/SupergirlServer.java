package com.danrod505.greenlantern.supergirl;

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

/** The server side of Supergirl: what each power does, and the Supergirl's Solar Energy coming back. */
public final class SupergirlServer {
    private static final int GLOW = 0xFFD447;

    private SupergirlServer() {}

    /** Uses a power with the Argo Pendant; returns whether it went off. */
    public static boolean usePower(ServerPlayer player, ItemStack item, SupergirlPower power) {
        if (!SupergirlHero.INSTANCE.isSuited(player)) return false;
        if (player.getCooldowns().isOnCooldown(item)) return false;
        if (!player.isCreative() && !SupergirlHero.ENERGY.tryConsume(item, power.cost())) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_supergirl_solar").withStyle(ChatFormatting.RED), true);
            return false;
        }
        // Each power still runs the placeholder: write the real one here (see the hero sheet).
        return switch (power) {
            case HEAT_BOLTS -> placeholder(player, power);
            case METEOR_DASH -> placeholder(player, power);
            case THUNDER_CLAP -> placeholder(player, power);
            case FROST_WALL -> placeholder(player, power);
            case SUPER_HEARING -> placeholder(player, power);
            case KRYPTONIAN_THROW -> placeholder(player, power);
            case SOLAR_FLARE -> placeholder(player, power);
        };
    }

    /** Until the power is written: a flash of the hero's colour in front of them, a sound and a note in the action bar. */
    private static boolean placeholder(ServerPlayer player, SupergirlPower power) {
        ServerLevel level = player.level();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        level.sendParticles(new DustParticleOptions(GLOW, 1.5F), at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(Component.translatable("message.greenlantern.power_todo", power.displayName()), true);
        player.getCooldowns().addCooldown(player.getMainHandItem(), 10);
        GreenLantern.LOGGER.debug("supergirl: placeholder power {}", power.id());
        return true;
    }

    /** Every second the Supergirl's Solar Energy in the Argo Pendant comes back a little. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        ItemStack item = SupergirlHero.INSTANCE.findItem(player);
        if (!item.isEmpty()) SupergirlHero.ENERGY.add(item, SupergirlConfig.RECHARGE_PER_SECOND.get());
    }
}
