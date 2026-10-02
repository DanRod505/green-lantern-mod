package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;

/**
 * Ring-powered flight. While suited up the player can fly like in creative mode (double tap jump),
 * at the cost of a small amount of energy per second.
 */
public final class FlightHandler {
    private static final float VANILLA_FLY_SPEED = 0.05F;

    private FlightHandler() {}

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player) {
        boolean suited = RingHelper.isSuited(player);
        ItemStack ring = suited ? RingHelper.findRing(player) : ItemStack.EMPTY;
        boolean canFly = suited && !ring.isEmpty() && RingEnergy.get(ring).stored() > 0;
        Abilities abilities = player.getAbilities();

        if (canFly) {
            float speed = GLConfig.FLIGHT_SPEED.get().floatValue();
            if (!abilities.mayfly || abilities.getFlyingSpeed() != speed) {
                abilities.mayfly = true;
                abilities.setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
            if (abilities.flying && !player.isCreative() && !player.isSpectator()) {
                // Faster power flight costs more: base cost at cruise, up to x supersonicCostMultiplier at top speed.
                float multiplier = 1.0F + (GLConfig.SUPERSONIC_COST_MULTIPLIER.get().floatValue() - 1.0F) * ServerFlightTracker.speedFraction(player);
                int cost = Math.round(GLConfig.FLIGHT_COST_PER_SECOND.get() * multiplier);
                if (player.tickCount % 20 == 0 && cost > 0) {
                    RingEnergy.tryConsume(ring, cost);
                    if (RingEnergy.get(ring).stored() <= 0) {
                        player.displayClientMessage(Component.translatable("message.greenlantern.no_energy"), true);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                    }
                }
            }
        } else if (abilities.getFlyingSpeed() != VANILLA_FLY_SPEED || (abilities.mayfly && !player.isCreative() && !player.isSpectator())) {
            refreshAbilities(player);
        }
    }

    /** Restores the abilities granted by the current game mode, then re-applies ring flight if allowed. */
    public static void refreshAbilities(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        boolean wasFlying = abilities.flying;
        player.gameMode().updatePlayerAbilities(abilities);
        abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
        if (RingHelper.isSuited(player)) {
            ItemStack ring = RingHelper.findRing(player);
            if (!ring.isEmpty() && RingEnergy.get(ring).stored() > 0) {
                abilities.mayfly = true;
                abilities.flying = wasFlying;
                abilities.setFlyingSpeed(GLConfig.FLIGHT_SPEED.get().floatValue());
            }
        }
        player.onUpdateAbilities();
    }
}
