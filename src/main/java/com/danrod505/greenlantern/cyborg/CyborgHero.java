package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.cyborg.CyborgClient;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Cyborg: the Mother Box holds the Cyborg Battery and calls the suit: silver armored plates, the red
 * eye and core, thrusters in the back and feet, the sonic cannon in the arm and a missile pod on the
 * shoulder. Made with the hero kit from {@code docs/heroes/cyborg.md}.
 */
public final class CyborgHero extends HeroDefinition {
    /** The Cyborg Battery, kept in the Mother Box: it pays for the powers and comes back on its own. */
    public static final HeroEnergy ENERGY = new HeroEnergy(CyborgContent.ENERGY, () -> CyborgConfig.CAPACITY.get());

    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("cyborg_wheel", 0x212224, 0x0E0F10, 0xC81E1E, 0x80858C, 0x191A1C, 0x37393C,
                    0x6E7278, 0xB8BEC8, 0xFF5A3C, 0xFF5A3C, 0xC81E1E),
            GreenLantern.id("textures/gui/cyborg_powers.png"), 128, "wheel.greenlantern.cyborg_power_cost", "tooltip.greenlantern.cyborg_power",
            0x66121314, 0xFFFF5A3C, 0xFFC81E1E, 0xFFE8E8E8, 0xFFB0B0B0, 0xFFFF5A3C, 0xFFB0B0B0);

    /** The thrusters: a white vapor trail lit blue and red (the Superman look), Cyborg's own theme; never reaches the sound barrier by default. */
    public static final FlightStyle FLIGHT_STYLE = new FlightStyle(FlightStyle.Look.SUPERMAN, 1.25F, ModSounds.SONIC_BOOM, ModParticles.SUPER_SHOCKWAVE,
            ModParticles.SUPER_RING, ModParticles.SUPER_RING, ModParticles.SUPER_RING, ModParticles.SUPER_RING, () -> ParticleTypes.CLOUD, 1.8, 16,
            (level, player) -> level.damageSources().playerAttack(player), 0.18, 0.3, 2.0,
            new FlightStyle.Hud(0xFFE8E0, 0x9FDCFF, 0x4FB8FF, 0xC81E1E, 0xFFFF5A3C, 0xFFFF5A3C, 1.0F, 0xFFC81E1E, 0xFFFF5A3C, 0xFFFFE0D0),
            CyborgContent.THEME_BASE, CyborgContent.THEME_PEAK, true);

    public static final CyborgHero INSTANCE = new CyborgHero();

    private final HeroPowers<CyborgPower> powers = new HeroPowers<>(CyborgPower.POWERS, ENERGY, ChatFormatting.GOLD,
            0.85F, 0.06F, CyborgServer::usePower, WHEEL);

    private CyborgHero() {
        super("cyborg", stack -> stack.is(CyborgContent.ITEM.get()), new SuitSet(CyborgContent.MASK, CyborgContent.SUIT, CyborgContent.LEGGINGS,
                CyborgContent.BOOTS, "message.greenlantern.no_mother_box", CyborgContent.SUIT_UP, CyborgContent.SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "cyborg_strength", 3.0),
                        SuitModifier.add(Attributes.MAX_HEALTH, "cyborg_health", 8.0),
                        SuitModifier.multiply(Attributes.MOVEMENT_SPEED, "cyborg_speed", 0.15))));
    }

    @Override
    public void register(FMLJavaModLoadingContext context) {
        CyborgContent.register(context.getModBusGroup());
        context.registerConfig(ModConfig.Type.COMMON, CyborgConfig.SPEC, "greenlantern-cyborg.toml");
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(CyborgContent.ITEM.get().charged(CyborgContent.ITEM.get().getDefaultInstance()));
        output.accept(CyborgContent.ITEM.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return CyborgClient::new;
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.45, 0.9, 0.45, 0.05);
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        FlightHandler.refreshAbilities(player);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        CyborgServer.remove(player);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.45, 0.9, 0.45, 0.05);
    }

    @Override
    public HeroPowers<CyborgPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        CyborgServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) dismissSuit(player, false);
        CyborgServer.remove(player);
    }

    /** Thrusters in the feet and back: faster than Wonder Woman, slower than Superman, below the sound barrier by default. */
    @Override
    public FlightProfile flightProfile() {
        double barrier = GLConfig.SOUND_BARRIER_SPEED.get();
        double cruise = CyborgConfig.CRUISE_SPEED.get();
        double max = Math.max(cruise + 0.1, CyborgConfig.MAX_SPEED.get());
        double toMax = CyborgConfig.SECONDS_TO_MAX.get();
        // The flight accelerates at (barrier - cruise) / seconds: scaled so top speed comes after toMax seconds.
        double seconds = max < barrier ? toMax * (barrier - cruise) / (max - cruise) : toMax;
        return new FlightProfile(cruise, barrier, max, seconds, 1.5, 0.9);
    }

    @Override
    public FlightStyle flightStyle() {
        return FLIGHT_STYLE;
    }

    /** The thrusters run on the battery: none left, no flight. */
    @Override
    public boolean canFly(Player player, ItemStack item) {
        return !item.isEmpty() && ENERGY.get(item).stored() > 0;
    }

    /** Burns battery while moving; hovering in place is free. */
    @Override
    public void payFlight(ServerPlayer player, ItemStack item, float multiplier) {
        if (!CyborgServer.thrustersBurning(player)) return;
        int cost = Math.round(CyborgConfig.FLIGHT_COST_PER_SECOND.get() * multiplier);
        if (cost <= 0) return;
        ENERGY.drain(item, cost);
        if (ENERGY.get(item).stored() <= 0) FlightHandler.outOfEnergy(player, "message.greenlantern.no_cyborg_power");
    }

    /** The thrusters cushion every landing. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }

    @Override
    public float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
        if (!isSuited(player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return 1.0F;
        return 1.0F - CyborgConfig.DAMAGE_REDUCTION.get().floatValue();
    }
}
