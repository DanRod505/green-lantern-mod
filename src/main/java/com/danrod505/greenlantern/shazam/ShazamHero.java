package com.danrod505.greenlantern.shazam;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.shazam.ShazamClient;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.flight.FlightStyle;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.FlightHandler;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Shazam: the Wizard's Bolt holds the Shazam's Magic and calls the suit with a lightning strike (the
 * magic word); red with the golden bolt on the chest, a short white cape and golden boots. Seven
 * powers, one for each elder of the name, built from the power blocks in {@code hero/power}, and
 * flight between the Lantern's and Supergirl's. Made with the hero kit from {@code docs/heroes/shazam.md}.
 */
public final class ShazamHero extends HeroDefinition {
    /** The Shazam's Magic, kept in the Wizard's Bolt: it pays for the powers and comes back on its own. */
    public static final HeroEnergy ENERGY = new HeroEnergy(ShazamContent.ENERGY, () -> ShazamConfig.CAPACITY.get());

    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("shazam_wheel", 0x240505, 0x100202, 0xF2B705, 0x8C1515, 0x1C0404, 0x3C0909,
                    0x781212, 0xC81E1E, 0xFFF6B0, 0xFFF6B0, 0xF2B705),
            GreenLantern.id("textures/gui/shazam_powers.png"), 128, "wheel.greenlantern.shazam_magic_cost", "tooltip.greenlantern.shazam_magic",
            0x66140303, 0xFFFFF6B0, 0xFFF2B705, 0xFFE8E8E8, 0xFFB0B0B0, 0xFFFFF6B0, 0xFFB0B0B0);

    /** Golden sparks of divine light with little arcs of lightning (drawn by the server): faster than the Lantern, slower than Supergirl. */
    public static final FlightStyle FLIGHT_STYLE = new FlightStyle(FlightStyle.Look.AMAZON, 1.05F, ModSounds.SONIC_BOOM, ModParticles.SUPER_SHOCKWAVE,
            ModParticles.SOLAR_GLOW, ModParticles.AMAZON_SPARK, ModParticles.SUPER_RING, ModParticles.SUPER_RING, () -> ParticleTypes.CLOUD, 2.0, 22,
            (level, player) -> level.damageSources().indirectMagic(player, player), 0.25, 0.35, 2.4,
            new FlightStyle.Hud(0xFFF6B0, 0xF2B705, 0xC81E1E, 0xF2B705, 0xFFFFF6B0, 0xFFF2B705, 2.0F, 0xFFC81E1E, 0xFFF2B705, 0xFFFFF6B0),
            ShazamContent.THEME_BASE, ShazamContent.THEME_PEAK, false);

    public static final ShazamHero INSTANCE = new ShazamHero();

    private final HeroPowers<ShazamPower> powers = new HeroPowers<>(ShazamPower.POWERS, ENERGY, ChatFormatting.GOLD,
            0.85F, 0.06F, ShazamServer::usePower, WHEEL);

    private ShazamHero() {
        super("shazam", stack -> stack.is(ShazamContent.ITEM.get()), new SuitSet(ShazamContent.MASK, ShazamContent.SUIT, ShazamContent.LEGGINGS,
                ShazamContent.BOOTS, "message.greenlantern.no_wizard_bolt", ShazamContent.SUIT_UP, ShazamContent.SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "shazam_strength", 4.0),
                        SuitModifier.add(Attributes.MAX_HEALTH, "shazam_health", 8.0),
                        SuitModifier.multiply(Attributes.MOVEMENT_SPEED, "shazam_speed", 0.15))));
    }

    @Override
    public void register(FMLJavaModLoadingContext context) {
        ShazamContent.register(context.getModBusGroup());
        context.registerConfig(ModConfig.Type.COMMON, ShazamConfig.SPEC, "greenlantern-shazam.toml");
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        FlightHandler.refreshAbilities(player);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        ShazamServer.remove(player);
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(ShazamContent.ITEM.get().charged(ShazamContent.ITEM.get().getDefaultInstance()));
        output.accept(ShazamContent.ITEM.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return ShazamClient::new;
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        // "Shazam!": the lightning forms the suit.
        ShazamServer.transform(player, true);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        // The lightning again, and Billy is back.
        ShazamServer.transform(player, false);
    }

    @Override
    public HeroPowers<ShazamPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        ShazamServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) dismissSuit(player, false);
        ShazamServer.remove(player);
    }

    @Override
    public FlightProfile flightProfile() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        double cruise = ShazamConfig.CRUISE_SPEED.get();
        return new FlightProfile(cruise, barrier, Math.max(cruise + 0.1, ShazamConfig.MAX_SPEED.get()), ShazamConfig.SECONDS_TO_SOUND_BARRIER.get(),
                2.0, ShazamConfig.LANDING_MULTIPLIER.get());
    }

    @Override
    public FlightStyle flightStyle() {
        return FLIGHT_STYLE;
    }

    /** Shazam's Magic pays for the flight. */
    @Override
    public boolean canFly(Player player, ItemStack item) {
        if (item.isEmpty()) return false;
        return ENERGY.get(item).stored() > 0 || player.isCreative();
    }

    @Override
    public void payFlight(ServerPlayer player, ItemStack item, float multiplier) {
        ShazamServer.payFlight(player, item, multiplier);
    }

    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }

    /** Lightning fills his magic instead of hurting him; Stamina of Atlas halves the rest. */
    @Override
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        return ShazamServer.damageTaken(player, source);
    }
}
