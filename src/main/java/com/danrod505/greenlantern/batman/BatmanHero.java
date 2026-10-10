package com.danrod505.greenlantern.batman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.batman.BatmanClient;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.item.UtilityBeltItem;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Batman: the Utility Belt holds the charge of the gadgets and the batsuit: the cowl with its ears,
 * the grey suit with the bat on the chest and the cape, the leggings and the boots. While it is worn
 * Batman sees in the dark (the cowl's lenses), hits harder (years of training), takes knocks better
 * and glides with the cape.
 */
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
        super("batman", BatmanHelper::isBelt, new SuitSet(ModItems.BATMAN_COWL, ModItems.BATMAN_SUIT, ModItems.BATMAN_LEGGINGS, ModItems.BATMAN_BOOTS,
                "message.greenlantern.no_belt", ModSounds.BATMAN_SUIT_UP, ModSounds.BATMAN_SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "batman_strength", 3.0),
                        SuitModifier.add(Attributes.KNOCKBACK_RESISTANCE, "batman_knockback", 0.3),
                        SuitModifier.multiply(Attributes.MOVEMENT_SPEED, "batman_speed", 0.1))));
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(UtilityBeltItem.charged(ModItems.UTILITY_BELT.get().getDefaultInstance()));
        output.accept(ModItems.UTILITY_BELT.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return BatmanClient::new;
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        shadows(level, player, 40);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        BatmanServer.onSuitRemoved(player);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        shadows(level, player, 20);
    }

    /** A swirl of smoke and dark wisps all over the body (Batman comes out of the shadows). */
    private static void shadows(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), count, 0.4, 0.8, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), count / 2, 0.4, 0.8, 0.4, 0.05);
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
        if (isSuited(player)) dismissSuit(player, false);
        BatmanServer.remove(player);
    }

    /** The cape breaks a glide's landing; otherwise years of training soften the fall. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        if (!(player instanceof ServerPlayer batman)) return -1.0F;
        return BatmanServer.safeLanding(batman) ? 0.0F : 0.5F;
    }
}
