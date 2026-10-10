package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.superman.SupermanClient;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/** Superman: the Kryptonian Crystal holds the suit and the solar energy his cells soak up. */
public final class SupermanHero extends HeroDefinition {
    /**
     * Solar energy of Superman, kept in the Kryptonian Crystal. His cells soak up the light of the
     * (yellow) sun under the open sky, and it pays for flight and every power.
     */
    public static final HeroEnergy SOLAR_ENERGY = new HeroEnergy(ModDataComponents.SOLAR_ENERGY, () -> GLConfig.SOLAR_CAPACITY.get());

    /** Kryptonian blue, red and gold. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("super_wheel", 0x06102A, 0x020614, 0xF2C21A, 0xB01820, 0x050C22, 0x0E1E4A,
                    0x1E4AB8, 0x2E6BFF, 0xFFD84A, 0xFFF4C0, 0xE8303A),
            GreenLantern.id("textures/gui/superman_powers.png"), 80, "wheel.greenlantern.solar_cost", "tooltip.greenlantern.solar_energy",
            0x66020A24, 0xFFD84A, 0xFFE8A0, 0xE6ECFF, 0xA8B4D8, 0xFFD84A, 0xA8B4D0);

    public static final SupermanHero INSTANCE = new SupermanHero();

    private final HeroPowers<SuperPower> powers = new HeroPowers<>(SuperPower.POWERS, SOLAR_ENERGY, ChatFormatting.AQUA,
            0.9F, 0.08F, SupermanServer::usePower, WHEEL);

    private SupermanHero() {
        super("superman", SupermanHelper::isCrystal, ModItems.SUPERMAN_SUIT);
    }

    @Override
    public Supplier<HeroClient> client() {
        return SupermanClient::new;
    }

    @Override
    public boolean summonSuit(ServerPlayer player) {
        return SupermanSuit.summon(player);
    }

    @Override
    public void dismissSuit(ServerPlayer player, boolean effects) {
        SupermanSuit.dismiss(player, effects);
    }

    @Override
    public HeroPowers<SuperPower> powers() {
        return powers;
    }

    /** Much faster and stronger than the Lantern's flight. */
    @Override
    public FlightProfile flightProfile() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        return new FlightProfile(GLConfig.SUPERMAN_CRUISE_SPEED.get(), barrier, Math.max(barrier, GLConfig.SUPERMAN_MAX_SPEED.get()),
                GLConfig.SUPERMAN_SECONDS_TO_SOUND_BARRIER.get(), 2.0, GLConfig.SUPERMAN_LANDING_MULTIPLIER.get());
    }

    @Override
    public void tick(ServerPlayer player) {
        SupermanServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) SupermanSuit.dismiss(player, false);
        SupermanServer.remove(player);
    }

    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }

    /** The Man of Steel: most blows barely hurt him while the sun charges his cells. */
    @Override
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        if (!SupermanHelper.isPowered(player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return 1.0F;
        return 1.0F - GLConfig.SUPERMAN_DAMAGE_REDUCTION.get().floatValue();
    }
}
