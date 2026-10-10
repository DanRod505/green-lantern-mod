package com.danrod505.greenlantern.superman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.superman.SupermanClient;
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
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModItems;
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
import net.minecraft.world.item.ItemStack;

/**
 * Superman: the Kryptonian Crystal holds the solar energy his cells soak up and the suit: the blue
 * suit with the S on the chest and the red cape, the red trunks with the yellow belt, the red boots
 * (and the curl of black hair). While it is worn the Man of Steel is far stronger, tougher, faster
 * and can't be pushed around.
 */
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

    /** A white vapor trail, Kryptonian blue and red, the deeper boom of a much stronger flyer. */
    public static final FlightStyle FLIGHT_STYLE = new FlightStyle(FlightStyle.Look.SUPERMAN, 0.8F, ModSounds.SUPER_BOOM, ModParticles.SUPER_SHOCKWAVE,
            ModParticles.SOLAR_GLOW, ModParticles.SOLAR_GLOW, ModParticles.SUPER_RING, ModParticles.SUPER_RING, () -> ParticleTypes.CLOUD, 2.6, 26,
            (level, player) -> ModDamageTypes.superPunch(level, player), 0.2, 0.35, 2.6, 
            new FlightStyle.Hud(0xF0F4FF, 0x9BC0FF, 0x2E6BFF, 0xE8303A, 0xFF8FB4FF, 0xFFFFE070, 3.0F, 0xFF2E6BFF, 0xFFFFD040, 0xFFFFF0C0),
            ModSounds.SUPERMAN_THEME_BASE, ModSounds.SUPERMAN_THEME_PEAK, false);

    public static final SupermanHero INSTANCE = new SupermanHero();

    private final HeroPowers<SuperPower> powers = new HeroPowers<>(SuperPower.POWERS, SOLAR_ENERGY, ChatFormatting.AQUA,
            0.9F, 0.08F, SupermanServer::usePower, WHEEL);

    private SupermanHero() {
        super("superman", SupermanHelper::isCrystal, new SuitSet(ModItems.SUPERMAN_HAIR, ModItems.SUPERMAN_SUIT, ModItems.SUPERMAN_LEGGINGS, ModItems.SUPERMAN_BOOTS,
                "message.greenlantern.no_crystal", ModSounds.SUPERMAN_SUIT_UP, ModSounds.SUPERMAN_SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "superman_strength", 10.0),
                        SuitModifier.add(Attributes.ATTACK_KNOCKBACK, "superman_punch_knockback", 2.0),
                        SuitModifier.add(Attributes.MAX_HEALTH, "superman_health", 20.0),
                        SuitModifier.multiply(Attributes.MOVEMENT_SPEED, "superman_speed", 0.35),
                        SuitModifier.add(Attributes.KNOCKBACK_RESISTANCE, "superman_steady", 1.0),
                        SuitModifier.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, "superman_blast_proof", 1.0),
                        SuitModifier.add(Attributes.STEP_HEIGHT, "superman_step", 0.65),
                        SuitModifier.add(Attributes.JUMP_STRENGTH, "superman_jump", 0.25),
                        SuitModifier.multiply(Attributes.BLOCK_BREAK_SPEED, "superman_mining", 1.5),
                        SuitModifier.add(Attributes.ENTITY_INTERACTION_RANGE, "superman_reach", 1.5),
                        SuitModifier.add(Attributes.BLOCK_INTERACTION_RANGE, "superman_block_reach", 1.5),
                        SuitModifier.add(Attributes.OXYGEN_BONUS, "superman_breath", 8.0))));
    }

    @Override
    public Supplier<HeroClient> client() {
        return SupermanClient::new;
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        FlightHandler.refreshAbilities(player);
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        sunburst(level, player, 50);
        SupermanServer.sendEvent(player, SuperFlags.EVENT_SUIT_UP);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        SupermanServer.onSuitRemoved(player);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        sunburst(level, player, 20);
    }

    /** A burst of golden sunlight and blue sparks all over the body. */
    private static void sunburst(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ModParticles.SOLAR_GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), count, 0.45, 0.9, 0.45, 0.05);
        level.sendParticles(ModParticles.SUPER_RING.get(), player.getX(), player.getY() + 1.0, player.getZ(), 0, 0.0, 1.0, 0.0, 1.0);
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
    public FlightStyle flightStyle() {
        return FLIGHT_STYLE;
    }

    /** Solar energy pays for the flight (free in creative). */
    @Override
    public boolean canFly(Player player, ItemStack crystal) {
        return !crystal.isEmpty() && (SOLAR_ENERGY.get(crystal).stored() > 0 || player.isCreative());
    }

    @Override
    public void payFlight(ServerPlayer player, ItemStack crystal, float multiplier) {
        int cost = Math.round(GLConfig.SUPERMAN_FLIGHT_COST_PER_SECOND.get() * multiplier);
        if (cost <= 0) return;
        SOLAR_ENERGY.drain(crystal, cost);
        if (SOLAR_ENERGY.get(crystal).stored() <= 0) FlightHandler.outOfEnergy(player, "message.greenlantern.no_solar");
    }

    @Override
    public void tick(ServerPlayer player) {
        SupermanServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) dismissSuit(player, false);
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
