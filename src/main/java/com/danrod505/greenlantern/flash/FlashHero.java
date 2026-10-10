package com.danrod505.greenlantern.flash;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.speed.FlashClient;
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

/** The Flash: the ring holds the suit and the Speed Force (it refills on its own, much faster while running). */
public final class FlashHero extends HeroDefinition {
    /** Speed Force stored in the Flash ring: pays for the speedster powers (tornado, phasing, lightning). */
    public static final HeroEnergy SPEED_FORCE = new HeroEnergy(ModDataComponents.SPEED_FORCE, () -> GLConfig.SPEED_FORCE_CAPACITY.get());

    /** Speed Force red and gold. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("speed_wheel", 0x160604, 0x080202, 0xC8902A, 0x8A2A1C, 0x120504, 0x2A0A06,
                    0x6A1408, 0xC8301A, 0xFFD24A, 0xFFF0B0, 0xFFC830),
            GreenLantern.id("textures/gui/flash_powers.png"), 64, "wheel.greenlantern.speed_cost", "tooltip.greenlantern.speed_force",
            0x66100400, 0xFFD24A, 0xFFE6A0, 0xF0E0D0, 0xD8B8A8, 0xFFD27A, 0xC8B8A8);

    public static final FlashHero INSTANCE = new FlashHero();

    private final HeroPowers<SpeedsterPower> powers = new HeroPowers<>(SpeedsterPower.POWERS, SPEED_FORCE, ChatFormatting.YELLOW,
            1.0F, 0.08F, SpeedsterServer::usePower, WHEEL);

    private FlashHero() {
        super("flash", FlashHelper::isRing, ModItems.FLASH_SUIT);
    }

    @Override
    public Supplier<HeroClient> client() {
        return FlashClient::new;
    }

    @Override
    public boolean summonSuit(ServerPlayer player) {
        return FlashSuit.summon(player);
    }

    @Override
    public void dismissSuit(ServerPlayer player, boolean effects) {
        FlashSuit.dismiss(player, effects);
    }

    @Override
    public HeroPowers<SpeedsterPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        SpeedsterServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        SpeedsterServer.endPhase(player, false);
        SpeedsterServer.remove(player);
    }

    /** A speedster lands running. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }
}
