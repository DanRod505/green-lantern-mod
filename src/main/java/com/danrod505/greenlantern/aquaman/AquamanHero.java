package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.aqua.AquamanClient;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Aquaman: the Atlantean Emblem calls the scale armor and holds the Power of the Seas. */
public final class AquamanHero extends HeroDefinition {
    /**
     * Power of the Seas stored in the Atlantean Emblem. It refills quickly while Aquaman is in water
     * (slowly on land) and pays for his powers: the trident, the shark and the call of the sea.
     */
    public static final HeroEnergy SEA_FORCE = new HeroEnergy(ModDataComponents.SEA_FORCE, () -> GLConfig.SEA_FORCE_CAPACITY.get());

    /** Sea teal and gold. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("sea_wheel", 0x021216, 0x010608, 0xC8A02A, 0x1C6A6A, 0x020C10, 0x06222A,
                    0x0A4A52, 0x1AA8A0, 0xF2C94A, 0xFFF0B0, 0x3CE0D0),
            GreenLantern.id("textures/gui/aquaman_powers.png"), 96, "wheel.greenlantern.sea_cost", "tooltip.greenlantern.sea_force",
            0x66001014, 0xF2C94A, 0xA8F0E0, 0xD8F0EC, 0xA8C8C4, 0xF2D27A, 0xA8C4C0);

    public static final AquamanHero INSTANCE = new AquamanHero();

    private final HeroPowers<AquaPower> powers = new HeroPowers<>(AquaPower.POWERS, SEA_FORCE, ChatFormatting.AQUA,
            0.8F, 0.08F, AquamanServer::usePower, WHEEL);

    private AquamanHero() {
        super("aquaman", AquamanHelper::isEmblem, ModItems.AQUAMAN_SUIT);
    }

    @Override
    public Supplier<HeroClient> client() {
        return AquamanClient::new;
    }

    @Override
    public boolean summonSuit(ServerPlayer player) {
        return AquamanSuit.summon(player);
    }

    @Override
    public void dismissSuit(ServerPlayer player, boolean effects) {
        AquamanSuit.dismiss(player, effects);
    }

    @Override
    public HeroPowers<AquaPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        AquamanServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) AquamanSuit.dismiss(player, false);
        AquamanServer.remove(player);
    }

    /** An Atlantean, built for the crushing deep, shrugs off any ordinary fall. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return distance < 24.0F ? 0.0F : -1.0F;
    }

    /** The trident is called from the sea and goes back to it. */
    @Override
    public boolean neverDropped(ItemStack stack) {
        return AquamanHelper.isTrident(stack);
    }
}
