package com.danrod505.greenlantern.wonderwoman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.wonderwoman.WonderWomanClient;
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
import net.minecraft.world.item.ItemStack;

/** Wonder Woman: the Tiara of Themyscira calls her armor and keeps the gift of the gods. */
public final class WonderWomanHero extends HeroDefinition {
    /**
     * The gift of the gods, kept in the Tiara of Themyscira: it pays for Wonder Woman's powers. It
     * comes back slowly on its own, and faster in battle (every blow of the Amazon sword feeds it).
     */
    public static final HeroEnergy DIVINE_POWER = new HeroEnergy(ModDataComponents.DIVINE_POWER, () -> GLConfig.DIVINE_CAPACITY.get());

    /** Amazon crimson, gold and blue. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("amazon_wheel", 0x1A0408, 0x0A0204, 0xF2B71C, 0x9A1020, 0x14040A, 0x2E0A12,
                    0x7A0E1C, 0xC8102E, 0xFFD24A, 0xFFF4C8, 0x2E5BD8),
            GreenLantern.id("textures/gui/wonder_woman_powers.png"), 144, "wheel.greenlantern.divine_cost", "tooltip.greenlantern.divine_power",
            0x661A0408, 0xFFD24A, 0xFFE6A0, 0xFFE8E0, 0xD8B0A8, 0xFFD24A, 0xD0B0A8);

    public static final WonderWomanHero INSTANCE = new WonderWomanHero();

    private final HeroPowers<AmazonPower> powers = new HeroPowers<>(AmazonPower.POWERS, DIVINE_POWER, ChatFormatting.GOLD,
            0.85F, 0.06F, WonderWomanServer::usePower, WHEEL);

    private WonderWomanHero() {
        super("wonder_woman", WonderWomanHelper::isTiara, ModItems.WONDER_WOMAN_SUIT);
    }

    @Override
    public Supplier<HeroClient> client() {
        return WonderWomanClient::new;
    }

    @Override
    public boolean summonSuit(ServerPlayer player) {
        return WonderWomanSuit.summon(player);
    }

    @Override
    public void dismissSuit(ServerPlayer player, boolean effects) {
        WonderWomanSuit.dismiss(player, effects);
    }

    @Override
    public HeroPowers<AmazonPower> powers() {
        return powers;
    }

    /** A strong, steady flight, but slower than the Lantern's (by default she never reaches the sound barrier). */
    @Override
    public FlightProfile flightProfile() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        double cruise = GLConfig.WONDER_WOMAN_CRUISE_SPEED.get();
        double max = Math.max(cruise + 0.1, GLConfig.WONDER_WOMAN_MAX_SPEED.get());
        double toMax = GLConfig.WONDER_WOMAN_SECONDS_TO_MAX.get();
        // The flight accelerates at (barrier - cruise) / seconds: scaled so top speed comes after toMax seconds.
        double seconds = max < barrier ? toMax * (barrier - cruise) / (max - cruise) : toMax;
        return new FlightProfile(cruise, barrier, max, seconds, 1.4, 0.8);
    }

    @Override
    public void tick(ServerPlayer player) {
        WonderWomanServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) WonderWomanSuit.dismiss(player, false);
        WonderWomanServer.remove(player);
    }

    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }

    /** An Amazon princess, made of clay and given life by the gods: tough, though not as tough as Superman. */
    @Override
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        if (!isSuited(player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return 1.0F;
        return 1.0F - GLConfig.WONDER_WOMAN_DAMAGE_REDUCTION.get().floatValue();
    }

    /** The Amazon sword and shield come from the tiara and never lie around. */
    @Override
    public boolean neverDropped(ItemStack stack) {
        return WonderWomanHelper.isSword(stack) || WonderWomanHelper.isShield(stack);
    }
}
