package com.danrod505.greenlantern.supergirl;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.supergirl.SupergirlClient;
import com.danrod505.greenlantern.flight.FlightAction;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.flight.FlightStyle;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.registry.ModDamageTypes;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Supergirl: the Argo Pendant holds her solar energy and calls the suit: lighter blue than Superman's
 * with the red S on gold, a short red cape, red skirt with a golden belt and tall red boots. The
 * same Kryptonian base as Superman, each power with its own twist, the most agile flight of all and
 * Krypto, the Super-Dog, at her side. Made with the hero kit from {@code docs/heroes/supergirl.md}.
 */
public final class SupergirlHero extends HeroDefinition {
    /** The Supergirl's Solar Energy, kept in the Argo Pendant: it pays for the powers and comes back on its own. */
    public static final HeroEnergy ENERGY = new HeroEnergy(SupergirlContent.ENERGY, () -> SupergirlConfig.CAPACITY.get());

    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("supergirl_wheel", 0x071328, 0x030811, 0xD8202E, 0x1D4D9C, 0x050F1F, 0x0C2143,
                    0x194286, 0x2A6FE0, 0xFFD447, 0xFFD447, 0xD8202E),
            GreenLantern.id("textures/gui/supergirl_powers.png"), 128, "wheel.greenlantern.supergirl_solar_cost", "tooltip.greenlantern.supergirl_solar",
            0x66040B16, 0xFFFFD447, 0xFFD8202E, 0xFFE8E8E8, 0xFFB0B0B0, 0xFFFFD447, 0xFFB0B0B0);

    /** A golden, crimson-edged trail with a short red cape: the quickest, most agile flyer (second fastest after Superman). */
    public static final FlightStyle FLIGHT_STYLE = new FlightStyle(FlightStyle.Look.AMAZON, 1.15F, ModSounds.SONIC_BOOM, ModParticles.SUPER_SHOCKWAVE,
            ModParticles.SOLAR_GLOW, ModParticles.SOLAR_GLOW, ModParticles.SUPER_RING, ModParticles.SUPER_RING, () -> ParticleTypes.CLOUD, 2.0, 20,
            (level, player) -> ModDamageTypes.superPunch(level, player), 0.3, 0.4, 2.3,
            new FlightStyle.Hud(0xFFF6D8, 0xFFD447, 0xD8202E, 0x2A6FE0, 0xFFFFD447, 0xFFFFE89A, 2.2F, 0xFFD8202E, 0xFFFFD447, 0xFFFFF0C0),
            SupergirlContent.THEME_BASE, SupergirlContent.THEME_PEAK, false);

    public static final SupergirlHero INSTANCE = new SupergirlHero();

    private final HeroPowers<SupergirlPower> powers = new HeroPowers<>(SupergirlPower.POWERS, ENERGY, ChatFormatting.GOLD,
            0.85F, 0.06F, SupergirlServer::usePower, WHEEL);

    private SupergirlHero() {
        super("supergirl", stack -> stack.is(SupergirlContent.ITEM.get()), new SuitSet(SupergirlContent.MASK, SupergirlContent.SUIT, SupergirlContent.LEGGINGS,
                SupergirlContent.BOOTS, "message.greenlantern.no_argo_pendant", SupergirlContent.SUIT_UP, SupergirlContent.SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "supergirl_strength", 3.0),
                        SuitModifier.add(Attributes.MAX_HEALTH, "supergirl_health", 8.0),
                        SuitModifier.multiply(Attributes.MOVEMENT_SPEED, "supergirl_speed", 0.15))));
    }

    @Override
    public void register(FMLJavaModLoadingContext context) {
        SupergirlContent.register(context.getModBusGroup());
        context.registerConfig(ModConfig.Type.COMMON, SupergirlConfig.SPEC, "greenlantern-supergirl.toml");
        EntityAttributeCreationEvent.getBus(context.getModBusGroup()).addListener(event -> event.put(SupergirlContent.KRYPTO.get(), KryptoEntity.createAttributes().build()));
        LivingHurtEvent.BUS.addListener(SupergirlServer::onLivingHurt);
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> SupergirlServer.tickWalls(event.server()));
        ServerStoppingEvent.BUS.addListener(event -> SupergirlServer.meltAll(event.getServer()));
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(SupergirlContent.ITEM.get().charged(SupergirlContent.ITEM.get().getDefaultInstance()));
        output.accept(SupergirlContent.ITEM.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return SupergirlClient::new;
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        // The suit forms in golden light.
        level.sendParticles(ModParticles.SOLAR_GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.45, 0.9, 0.45, 0.06);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.9, 0.4, 0.04);
        level.sendParticles(ModParticles.SUPER_RING.get(), player.getX(), player.getY() + 1.0, player.getZ(), 0, 0.0, 1.0, 0.0, 1.0);
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        FlightHandler.refreshAbilities(player);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        SupergirlServer.remove(player);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ModParticles.SOLAR_GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.45, 0.9, 0.45, 0.05);
    }

    @Override
    public HeroPowers<SupergirlPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        SupergirlServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) dismissSuit(player, false);
        SupergirlServer.remove(player);
    }

    /** The most agile flight: quicker to the sound barrier than Superman and sharper in the turns, with a lower top speed. */
    @Override
    public FlightProfile flightProfile() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        double cruise = SupergirlConfig.CRUISE_SPEED.get();
        return new FlightProfile(cruise, barrier, Math.max(cruise + 0.1, SupergirlConfig.MAX_SPEED.get()), SupergirlConfig.SECONDS_TO_SOUND_BARRIER.get(),
                2.2, SupergirlConfig.LANDING_MULTIPLIER.get());
    }

    @Override
    public FlightStyle flightStyle() {
        return FLIGHT_STYLE;
    }

    /** Solar energy pays for the flight; after a Solar Flare she can't fly until she recovers. */
    @Override
    public boolean canFly(Player player, ItemStack item) {
        if (item.isEmpty() || SupergirlServer.isWeak(player)) return false;
        return ENERGY.get(item).stored() > 0 || player.isCreative();
    }

    @Override
    public void payFlight(ServerPlayer player, ItemStack item, float multiplier) {
        int cost = Math.round(SupergirlConfig.FLIGHT_COST_PER_SECOND.get() * multiplier);
        if (cost <= 0) return;
        ENERGY.drain(item, cost);
        if (ENERGY.get(item).stored() <= 0) FlightHandler.outOfEnergy(player, "message.greenlantern.no_supergirl_solar");
    }

    /** Her barrel roll is a real dodge: it costs energy and projectiles miss her for a moment. */
    @Override
    public void onFlightAction(ServerPlayer player, FlightAction action) {
        if (action == FlightAction.ROLL_LEFT || action == FlightAction.ROLL_RIGHT) SupergirlServer.roll(player);
    }

    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }

    @Override
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        return SupergirlServer.damageTaken(player, source);
    }
}
