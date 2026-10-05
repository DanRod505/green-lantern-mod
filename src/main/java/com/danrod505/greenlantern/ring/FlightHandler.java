package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.superman.SolarEnergy;
import com.danrod505.greenlantern.superman.SupermanHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;

/**
 * Power flight of the Green Lantern (ring-powered) and Superman (solar-powered). While suited up the
 * player can fly like in creative mode (double tap jump), at the cost of a little energy per second.
 */
public final class FlightHandler {
    private static final float VANILLA_FLY_SPEED = 0.05F;

    private FlightHandler() {}

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player) {
        boolean superman = SupermanHelper.isSuited(player);
        boolean suited = superman || RingHelper.isSuited(player);
        // The Lantern's ring or Superman's crystal pays for the flight.
        ItemStack source = !suited ? ItemStack.EMPTY : superman ? SupermanHelper.findCrystal(player) : RingHelper.findRing(player);
        boolean canFly = suited && !source.isEmpty() && (energy(source, superman) > 0 || (superman && player.isCreative()));
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
                int base = superman ? GLConfig.SUPERMAN_FLIGHT_COST_PER_SECOND.get() : GLConfig.FLIGHT_COST_PER_SECOND.get();
                int cost = Math.round(base * multiplier);
                if (player.tickCount % 20 == 0 && cost > 0) {
                    if (superman) {
                        SolarEnergy.drain(source, cost);
                    } else {
                        RingEnergy.tryConsume(source, cost);
                    }
                    if (energy(source, superman) <= 0) {
                        player.displayClientMessage(Component.translatable(superman ? "message.greenlantern.no_solar" : "message.greenlantern.no_energy"), true);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                    }
                }
            }
        } else if (com.danrod505.greenlantern.flash.SpeedsterServer.isPhasing(player)) {
            // A phasing speedster hovers inside walls: keep the server's flight check happy.
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (abilities.getFlyingSpeed() != VANILLA_FLY_SPEED || (abilities.mayfly && !player.isCreative() && !player.isSpectator())) {
            refreshAbilities(player);
        }
    }

    private static int energy(ItemStack source, boolean superman) {
        return superman ? SolarEnergy.get(source).stored() : RingEnergy.get(source).stored();
    }

    /** Restores the abilities granted by the current game mode, then re-applies ring flight if allowed. */
    public static void refreshAbilities(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        boolean wasFlying = abilities.flying;
        player.gameMode().updatePlayerAbilities(abilities);
        abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
        if (SupermanHelper.isSuited(player)) {
            ItemStack crystal = SupermanHelper.findCrystal(player);
            if (!crystal.isEmpty() && (SolarEnergy.get(crystal).stored() > 0 || player.isCreative())) {
                abilities.mayfly = true;
                abilities.flying = wasFlying;
                abilities.setFlyingSpeed(GLConfig.FLIGHT_SPEED.get().floatValue());
            }
        } else if (RingHelper.isSuited(player)) {
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
