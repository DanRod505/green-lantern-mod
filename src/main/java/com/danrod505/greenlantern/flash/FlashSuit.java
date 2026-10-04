package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.FlightHandler;
import com.danrod505.greenlantern.ring.Uniform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * The Flash suit springs out of the Flash ring (like Barry Allen's). While it is worn the player
 * walks faster and steps up whole blocks, so a run is never stopped by a single step.
 */
public final class FlashSuit {
    private static final Identifier SPEED_MODIFIER = GreenLantern.id("flash_speed");
    private static final Identifier STEP_MODIFIER = GreenLantern.id("flash_step");

    private FlashSuit() {}

    public static void toggle(ServerPlayer player) {
        if (FlashHelper.isSuited(player)) {
            dismiss(player, true);
        } else {
            summon(player);
        }
    }

    public static boolean summon(ServerPlayer player) {
        if (FlashHelper.isSuited(player)) return true;
        ItemStack ring = FlashHelper.findRing(player);
        if (ring.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.no_flash_ring"), true);
            return false;
        }
        // Only one hero suit at a time.
        com.danrod505.greenlantern.hero.Hero.dismissOthers(player, com.danrod505.greenlantern.hero.Hero.FLASH);
        Uniform.equipSuit(player,
                ModItems.FLASH_MASK.get().getDefaultInstance(),
                ModItems.FLASH_SUIT.get().getDefaultInstance(),
                ModItems.FLASH_LEGGINGS.get().getDefaultInstance(),
                ModItems.FLASH_BOOTS.get().getDefaultInstance());
        updateModifiers(player, true);

        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASH_SUIT_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.9, 0.4, 0.2);
        level.sendParticles(ModParticles.SPEED_STREAK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.5, 0.8, 0.5, 0.05);
        return true;
    }

    public static void dismiss(ServerPlayer player, boolean effects) {
        SpeedsterServer.endPhase(player, false);
        Uniform.removeSuit(player);
        updateModifiers(player, false);
        FlightHandler.refreshAbilities(player);
        if (effects) {
            ServerLevel level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASH_SUIT_DOWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.9, 0.4, 0.1);
        }
    }

    /** Adds (suited) or removes the walking speed and step height bonuses of the suit. */
    public static void updateModifiers(ServerPlayer player, boolean suited) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (suited) {
            if (speed != null && !speed.hasModifier(SPEED_MODIFIER)) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_MODIFIER, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if (step != null && !step.hasModifier(STEP_MODIFIER)) {
                step.addOrUpdateTransientModifier(new AttributeModifier(STEP_MODIFIER, 0.65, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            if (speed != null) speed.removeModifier(SPEED_MODIFIER);
            if (step != null) step.removeModifier(STEP_MODIFIER);
        }
    }
}
