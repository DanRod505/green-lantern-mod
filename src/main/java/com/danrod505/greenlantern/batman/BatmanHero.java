package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.batman.BatmanClient;
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

/** Batman: the Utility Belt holds the batsuit and the charge of the gadgets. */
public final class BatmanHero extends HeroDefinition {
    /**
     * Charge of the Utility Belt's power cells. It refills steadily while the batsuit is worn (faster
     * in the dark: the night belongs to Batman) and pays for the gadgets and the Batmobile's boost.
     */
    public static final HeroEnergy BAT_CHARGE = new HeroEnergy(ModDataComponents.BAT_CHARGE, () -> GLConfig.BAT_CHARGE_CAPACITY.get());

    /** Night black and utility-belt yellow. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("bat_wheel", 0x0C0C0E, 0x040405, 0xD8B020, 0x4A4A52, 0x08080A, 0x1A1A1F,
                    0x2A2A30, 0x55555E, 0xF2D03A, 0xFFF4B0, 0xF2D03A),
            GreenLantern.id("textures/gui/batman_powers.png"), 80, "wheel.greenlantern.bat_cost", "tooltip.greenlantern.bat_charge",
            0x88050506, 0xF2D03A, 0xE8D890, 0xE0E0E4, 0x9A9AA4, 0xF2D03A, 0xA8A8B0);

    public static final BatmanHero INSTANCE = new BatmanHero();

    private final HeroPowers<BatPower> powers = new HeroPowers<>(BatPower.POWERS, BAT_CHARGE, ChatFormatting.YELLOW,
            0.7F, 0.08F, BatmanServer::usePower, WHEEL);

    private BatmanHero() {
        super("batman", BatmanHelper::isBelt, ModItems.BATMAN_SUIT);
    }

    @Override
    public Supplier<HeroClient> client() {
        return BatmanClient::new;
    }

    @Override
    public boolean summonSuit(ServerPlayer player) {
        return BatmanSuit.summon(player);
    }

    @Override
    public void dismissSuit(ServerPlayer player, boolean effects) {
        BatmanSuit.dismiss(player, effects);
    }

    @Override
    public HeroPowers<BatPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        BatmanServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) BatmanSuit.dismiss(player, false);
        BatmanServer.remove(player);
    }

    /** The cape breaks a glide's landing; otherwise years of training soften the fall. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        if (!(player instanceof ServerPlayer batman)) return -1.0F;
        return BatmanServer.safeLanding(batman) ? 0.0F : 0.5F;
    }
}
