package com.danrod505.greenlantern.aquaman;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.aqua.AquamanClient;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.item.AquamanEmblemItem;
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
import net.minecraft.world.item.ItemStack;

/**
 * Aquaman: the Atlantean Emblem holds the Power of the Seas and the armor of Atlantis: the golden
 * scale shirt, the green scaled leggings and boots (Aquaman wears no mask, so the player's own helmet
 * stays on). While it is worn Aquaman breathes underwater, sees clearly in the deep, mines at full
 * speed underwater and hits harder.
 */
public final class AquamanHero extends HeroDefinition {
    /**
     * Power of the Seas stored in the Atlantean Emblem. It refills quickly while Aquaman is in water
     * (slowly on land) and pays for his powers: the trident, the shark and the call of the sea.
     */
    public static final HeroEnergy SEA_FORCE = new HeroEnergy(ModDataComponents.SEA_FORCE, () -> GLConfig.SEA_FORCE_CAPACITY.get());

    /** Sea teal and gold. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("sea_wheel", 0x021216, 0x010608, 0xC8A02A, 0x1C6A6A, 0x020C10, 0x06222A,
                    0x0A4A52, 0x1AA8A0, 0xF2C94A, 0xFFF0B0, 0x3CE0D0),
            GreenLantern.id("textures/gui/aquaman_powers.png"), 96, "wheel.greenlantern.sea_cost", "tooltip.greenlantern.sea_force",
            0x66001014, 0xF2C94A, 0xA8F0E0, 0xD8F0EC, 0xA8C8C4, 0xF2D27A, 0xA8C4C0);

    public static final AquamanHero INSTANCE = new AquamanHero();

    private final HeroPowers<AquaPower> powers = new HeroPowers<>(AquaPower.POWERS, SEA_FORCE, ChatFormatting.AQUA,
            0.8F, 0.08F, AquamanServer::usePower, WHEEL);

    private AquamanHero() {
        super("aquaman", AquamanHelper::isEmblem, new SuitSet(null, ModItems.AQUAMAN_SUIT, ModItems.AQUAMAN_LEGGINGS, ModItems.AQUAMAN_BOOTS,
                "message.greenlantern.no_emblem", ModSounds.AQUAMAN_SUIT_UP, ModSounds.AQUAMAN_SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.WATER_MOVEMENT_EFFICIENCY, "aquaman_water", 1.0),
                        SuitModifier.add(Attributes.SUBMERGED_MINING_SPEED, "aquaman_mining", 4.0),
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "aquaman_strength", 2.0),
                        SuitModifier.add(Attributes.KNOCKBACK_RESISTANCE, "aquaman_knockback", 0.4))));
    }

    /** The emblem, then Atlantis and the Trench (the gate, the respirator and the eggs of their creatures). */
    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(AquamanEmblemItem.charged(ModItems.AQUAMAN_EMBLEM.get().getDefaultInstance()));
        output.accept(ModItems.AQUAMAN_EMBLEM.get());
        output.accept(ModItems.ATLANTIS_GATE.get());
        output.accept(ModItems.ATLANTEAN_RESPIRATOR.get());
        output.accept(ModItems.MANTA_RAY_EGG.get());
        output.accept(ModItems.GIANT_SEAHORSE_EGG.get());
        output.accept(ModItems.ATLANTEAN_DOLPHIN_EGG.get());
        output.accept(ModItems.TRENCH_CREATURE_EGG.get());
        output.accept(ModItems.TRENCH_BRUTE_EGG.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return AquamanClient::new;
    }

    @Override
    protected void onSuitEquipped(ServerPlayer player) {
        player.setAirSupply(player.getMaxAirSupply());
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        splash(level, player, 50);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        AquamanServer.onSuitRemoved(player);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        splash(level, player, 25);
    }

    /** A spray of water and bubbles all over the body. */
    private static void splash(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), count, 0.4, 0.9, 0.4, 0.3);
        level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1.0, player.getZ(), count / 2, 0.4, 0.9, 0.4, 0.05);
        level.sendParticles(ParticleTypes.DOLPHIN, player.getX(), player.getY() + 1.0, player.getZ(), count / 2, 0.5, 0.9, 0.5, 0.05);
    }

    @Override
    public HeroPowers<AquaPower> powers() {
        return powers;
    }

    @Override
    public void tick(ServerPlayer player) {
        AquamanServer.tick(player);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSuited(player)) dismissSuit(player, false);
        AquamanServer.remove(player);
    }

    /** An Atlantean, built for the crushing deep, shrugs off any ordinary fall. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return distance < 24.0F ? 0.0F : -1.0F;
    }

    /** The trident is called from the sea and goes back to it. */
    @Override
    public boolean neverDropped(ItemStack stack) {
        return AquamanHelper.isTrident(stack);
    }
}
