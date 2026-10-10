package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.flight.ServerFlightTracker;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.danrod505.greenlantern.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;

/**
 * Power flight of the heroes with a flight profile (the Green Lantern's ring, Superman's solar energy, Wonder Woman's gift of the gods).
 * While suited up the player can fly like in creative mode (double tap jump); each hero says when it may fly and what a second of flight
 * costs ({@link HeroDefinition#canFly}, {@link HeroDefinition#payFlight}).
 */
public final class FlightHandler {
    private static final float VANILLA_FLY_SPEED = 0.05F;

    private FlightHandler() {}

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player) {
        HeroDefinition hero = flyingHero(player);
        // The hero's item (ring, crystal, tiara...) powers the flight.
        ItemStack item = hero == null ? ItemStack.EMPTY : hero.findItem(player);
        boolean canFly = hero != null && hero.canFly(player, item);
        Abilities abilities = player.getAbilities();

        if (canFly) {
            float speed = GLConfig.FLIGHT_SPEED.get().floatValue();
            if (!abilities.mayfly || abilities.getFlyingSpeed() != speed) {
                abilities.mayfly = true;
                abilities.setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
            if (abilities.flying && !player.isCreative() && !player.isSpectator() && player.tickCount % 20 == 0) {
                // Faster power flight costs more: base cost at cruise, up to x supersonicCostMultiplier at top speed.
                float multiplier = 1.0F + (GLConfig.SUPERSONIC_COST_MULTIPLIER.get().floatValue() - 1.0F) * ServerFlightTracker.speedFraction(player);
                hero.payFlight(player, item, multiplier);
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

    /** The suited hero, if it power-flies. */
    private static HeroDefinition flyingHero(ServerPlayer player) {
        return HeroRegistry.suited(player).filter(hero -> hero.flightProfile() != null).orElse(null);
    }

    /** Tells the flyer the energy ran out (shared by the heroes whose flight costs energy). */
    public static void outOfEnergy(ServerPlayer player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Restores the abilities granted by the current game mode, then re-applies ring flight if allowed. */
    public static void refreshAbilities(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        boolean wasFlying = abilities.flying;
        player.gameMode().updatePlayerAbilities(abilities);
        abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
        HeroDefinition hero = flyingHero(player);
        if (hero != null && hero.canFly(player, hero.findItem(player))) {
            abilities.mayfly = true;
            abilities.flying = wasFlying;
            abilities.setFlyingSpeed(GLConfig.FLIGHT_SPEED.get().floatValue());
        }
        player.onUpdateAbilities();
    }
}
