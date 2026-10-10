package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.LanternClient;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.flight.FlightStyle;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.registry.ModDamageTypes;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Green Lantern: the Power Ring calls the uniform of hard light (it needs energy in the ring), flies and
 * builds constructs (picked on the construct wheel).
 */
public final class LanternHero extends HeroDefinition {
    /** Colours of the construct wheel: green hard light. */
    public static final WheelTheme WHEEL_THEME = new WheelTheme("wheel", 0x07120A, 0x030604, 0x2A8A44, 0x2E6A3E, 0x060D08, 0x0F2216,
            0x0F4A20, 0x2FA84E, 0x7CFF96, 0xC8FFD2, 0x3CE064);

    /** Green hard light: the shared default of power flight. */
    public static final FlightStyle FLIGHT_STYLE = new FlightStyle(FlightStyle.Look.LANTERN, 1.0F, ModSounds.SONIC_BOOM, ModParticles.SHOCKWAVE,
            ModParticles.GLOW, ModParticles.SPARK, ModParticles.SONIC_RING, ModParticles.SONIC_RING, ModParticles.GLOW, 1.8, 16,
            (level, player) -> ModDamageTypes.hardLight(level, player, player), 0.13, 0.2, 1.6, 
            new FlightStyle.Hud(0xD8FFE0, 0x9BFFB0, 0x2CFF5A, 0x2CFF5A, 0xFF6CFF86, 0xFFB6FFC6, 1.6F, 0xFF2EE65A, 0xFFC8FFD2, 0xFFE0FFE6),
            ModSounds.FLIGHT_THEME_BASE, ModSounds.FLIGHT_THEME_PEAK, false);

    public static final LanternHero INSTANCE = new LanternHero();

    private LanternHero() {
        super("lantern", RingHelper::isRing, new SuitSet(ModItems.LANTERN_MASK, ModItems.LANTERN_SUIT, ModItems.LANTERN_LEGGINGS, ModItems.LANTERN_BOOTS,
                "message.greenlantern.no_ring", ModSounds.RING_ACTIVATE, ModSounds.RING_DEACTIVATE, List.of()));
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
