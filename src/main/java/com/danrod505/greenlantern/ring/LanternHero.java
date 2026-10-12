package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.LanternClient;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.flight.FlightStyle;
import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.engine.LegacyHero;
import com.danrod505.greenlantern.engine.preset.PresetRuntime;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * The Green Lantern: the Power Ring calls the uniform of hard light (it needs energy in the ring), flies and
 * builds constructs. Since stage 3 the constructs are the powers of its own code on the wheel of the catalog
 * (lantern.json): the ring's construct wheel is no longer opened.
 */
public final class LanternHero extends LegacyHero {
    /** Colours of the construct wheel: green hard light. */
    public static final WheelTheme WHEEL_THEME = new WheelTheme("wheel", 0x07120A, 0x030604, 0x2A8A44, 0x2E6A3E, 0x060D08, 0x0F2216,
            0x0F4A20, 0x2FA84E, 0x7CFF96, 0xC8FFD2, 0x3CE064);

    /** Green hard light: the shared default of power flight. */
    public static final FlightStyle FLIGHT_STYLE = new FlightStyle(FlightStyle.Look.LANTERN, 1.0F, ModSounds.SONIC_BOOM, ModParticles.SHOCKWAVE,
            ModParticles.GLOW, ModParticles.SPARK, ModParticles.SONIC_RING, ModParticles.SONIC_RING, ModParticles.GLOW, 1.8, 16,
            (level, player) -> ModDamageTypes.hardLight(level, player, player), 0.13, 0.2, 1.6, 
            new FlightStyle.Hud(0xD8FFE0, 0x9BFFB0, 0x2CFF5A, 0x2CFF5A, 0xFF6CFF86, 0xFFB6FFC6, 1.6F, 0xFF2EE65A, 0xFFC8FFD2, 0xFFE0FFE6),
            ModSounds.FLIGHT_THEME_BASE, ModSounds.FLIGHT_THEME_PEAK, false);

    /** The ring's charge, as the shared energy of the wheel. */
    public static final HeroEnergy RING = new HeroEnergy(ModDataComponents.RING_ENERGY, RingEnergy::configuredCapacity);

    /** The old construct wheel: green hard light, with the constructs' icons. */
    public static final WheelStyle WHEEL = new WheelStyle(WHEEL_THEME, com.danrod505.greenlantern.GreenLantern.id("textures/gui/constructs.png"), 256,
            "wheel.greenlantern.cost", "tooltip.greenlantern.energy", 0x66000A04, 0x7CFF96, 0x9BE8AA, 0xD8E8DC, 0xB8D8C0, 0x9BE8AA, 0xB0C8B6);

    /** How long the gun of the wheel fires by itself (ticks): the old one fired while the use key was held. */
    public static final int GUN_TICKS = 100;

    public static final LanternHero INSTANCE = new LanternHero();

    private LanternHero() {
        super("lantern", RingHelper::isRing, new SuitSet(ModItems.LANTERN_MASK, ModItems.LANTERN_SUIT, ModItems.LANTERN_LEGGINGS, ModItems.LANTERN_BOOTS,
                "message.greenlantern.no_ring", ModSounds.RING_ACTIVATE, ModSounds.RING_DEACTIVATE, Characters.suitModifiers("lantern")));
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(PowerRingItem.charged(ModItems.POWER_RING.get().getDefaultInstance()));
        output.accept(ModItems.POWER_RING.get());
        output.accept(ModItems.POWER_BATTERY.get());
        output.accept(ModItems.GUIDE_BOOK.get());
    }

    // ---- the new wheel (catalog): the constructs are the powers of his own code ------------------

    /** The constructs are not a power set: {@link #ownPower} runs them by the path of their id. */
    @Override
    public HeroPowers<? extends HeroPower> ownPowers() {
        return null;
    }

    @Override
    public WheelStyle ownWheel() {
        return WHEEL;
    }

    @Override
    public HeroEnergy energy() {
        return RING;
    }

    /** The construct with this id path ({@code hammer}, {@code minigun}...), or null. */
    public static Construct construct(String power) {
        for (Construct construct : ConstructRegistry.all()) {
            if (construct.id().getPath().equals(power)) return construct;
        }
        return null;
    }

    @Override
    public boolean hasOwnPower(String power) {
        return construct(power) != null;
    }

    @Override
    public int ownIconIndex(String power) {
        Construct construct = construct(power);
        return construct == null ? -1 : construct.iconIndex();
    }

    @Override
    protected IntSupplier ownCost(String power) {
        Construct construct = construct(power);
        return construct == null ? null : construct::activationCost;
    }

    /**
     * A construct, as the ring made it on a right click: the energy it needs, its cooldown on the
     * ring, a toggle one goes away when used again. The gun, that fired while the use key was held,
     * fires by itself for {@link #GUN_TICKS} (the key again stops it).
     */
    @Override
    public boolean ownPower(ServerPlayer player, ItemStack ring, String power) {
        Construct construct = construct(power);
        if (construct == null || !isSuited(player)) return false;
        String key = "lantern/" + power;
        if (construct.useMode() == Construct.UseMode.HOLD && PresetRuntime.stop(player, key)) return true;
        if (player.getCooldowns().isOnCooldown(ring)) return false;
        boolean toggle = construct.useMode() == Construct.UseMode.TOGGLE;
        if (!toggle && !player.isCreative() && !RingEnergy.has(ring, Math.max(1, construct.activationCost()))) {
            PowerRingItem.notifyNoEnergy(player);
            return false;
        }
        if (!construct.activate(player, ring)) return false;
        if (construct.useMode() == Construct.UseMode.HOLD) {
            PresetRuntime.startNamed(player, key, new PresetRuntime.Task() {
                @Override
                public boolean tick(int age) {
                    ItemStack held = findItem(player);
                    if (held.isEmpty() || !isSuited(player)) return false;
                    construct.holdTick(player, held, age);
                    return age < GUN_TICKS;
                }

                @Override
                public void stop() {
                    ItemStack held = findItem(player);
                    construct.release(player, held.isEmpty() ? ring : held, GUN_TICKS);
                    if (construct.cooldown() > 0 && !held.isEmpty()) player.getCooldowns().addCooldown(held, construct.cooldown());
                }
            });
        } else if (construct.cooldown() > 0) {
            player.getCooldowns().addCooldown(ring, construct.cooldown());
        }
        return true;
    }

    @Override
    public Supplier<HeroClient> client() {
        return LanternClient::new;
    }

    /** No energy in the ring, no uniform. */
    @Override
    protected boolean canSuitUp(ServerPlayer player, ItemStack ring) {
        if (RingEnergy.get(ring).stored() > 0) return true;
        player.displayClientMessage(Component.translatable("message.greenlantern.no_energy"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return false;
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        FlightHandler.refreshAbilities(player);
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.9, 0.4, 0.02);
        level.sendParticles(ModParticles.SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 1.0, 0.5, 0.15);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ModParticles.GLOW.get(), player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.9, 0.4, 0.01);
    }

    @Override
    public FlightProfile flightProfile() {
        return FlightProfile.lantern();
    }

    @Override
    public FlightStyle flightStyle() {
        return FLIGHT_STYLE;
    }

    /** The ring flies while it has energy. */
    @Override
    public boolean canFly(Player player, ItemStack ring) {
        return !ring.isEmpty() && RingEnergy.get(ring).stored() > 0;
    }

    @Override
    public void payFlight(ServerPlayer player, ItemStack ring, float multiplier) {
        int cost = Math.round(GLConfig.FLIGHT_COST_PER_SECOND.get() * multiplier);
        if (cost <= 0) return;
        RingEnergy.tryConsume(ring, cost);
        if (RingEnergy.get(ring).stored() <= 0) FlightHandler.outOfEnergy(player, "message.greenlantern.no_energy");
    }

    /** The uniform is made of the ring's light: no ring or no energy, no uniform (and the rest of the tick is skipped). */
    @Override
    public boolean checkSuit(ServerPlayer player) {
        if (!isSuited(player)) return true;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty()) {
            dismissSuit(player, true);
            return false;
        }
        RingEnergy energy = RingEnergy.get(ring);
        if (energy.stored() <= 0 && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.power_out").withStyle(ChatFormatting.RED), true);
            dismissSuit(player, true);
            return false;
        }
        if (energy.fraction() <= 0.1F && player.tickCount % 100 == 0) {
            player.displayClientMessage(Component.translatable("message.greenlantern.low_energy").withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        return true;
    }

    /** The ring cushions every landing. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }
}
