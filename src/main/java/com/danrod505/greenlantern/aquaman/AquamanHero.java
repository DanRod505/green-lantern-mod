package com.danrod505.greenlantern.aquaman;

import java.util.Set;
import com.danrod505.greenlantern.engine.LegacyWheel;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.AbilityHero;
import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.aqua.AquamanClient;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.item.AquamanEmblemItem;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Aquaman: the Atlantean Emblem holds the Power of the Seas and the armor of Atlantis: the golden
 * scale shirt, the green scaled leggings and boots (Aquaman wears no mask, so the player's own helmet
 * stays on). While it is worn Aquaman breathes underwater, sees clearly in the deep, mines at full
 * speed underwater and hits harder.
 */
public final class AquamanHero extends HeroDefinition implements AbilityHero {
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

    /** The powers of today: the abilities of the new wheel call them by name (preset poder_de_hoje). */
    private final HeroPowers<AquaPower> legacy = new HeroPowers<>(AquaPower.POWERS, SEA_FORCE, ChatFormatting.AQUA,
            0.8F, 0.08F, AquamanServer::usePower, WHEEL);
    /** The wheel in the new model (stage 3): the abilities of the catalog, with the powers of today inside them. */
    private final LegacyWheel wheel = new LegacyWheel("aquaman", SEA_FORCE, WHEEL, ChatFormatting.AQUA, 0.8F, 0.08F, legacy);

    private AquamanHero() {
        super("aquaman", AquamanHelper::isEmblem, new SuitSet(null, ModItems.AQUAMAN_SUIT, ModItems.AQUAMAN_LEGGINGS, ModItems.AQUAMAN_BOOTS,
                "message.greenlantern.no_emblem", ModSounds.AQUAMAN_SUIT_UP, ModSounds.AQUAMAN_SUIT_DOWN, Characters.suitModifiers("aquaman")));
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
        EngineServer.legacySuitDown(player);
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
    public HeroPowers<EngineAbility> powers() {
        return wheel.powers();
    }

    @Override
    public HeroPowers<AquaPower> legacyPowers() {
        return legacy;
    }

    @Override
    public CharacterData data() {
        return wheel.data();
    }

    @Override
    public HeroEnergy energy() {
        return SEA_FORCE;
    }

    @Override
    public Set<String> ownTraits() {
        return LegacyWheel.OWN_TRAITS;
    }

    @Override
    public void tick(ServerPlayer player) {
        AquamanServer.tick(player);
        EngineServer.legacyTick(player, this);
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

    /** The emblem's HUD is taller: the Kraken's health goes under the powers. */
    @Override
    public int hudHeight() {
        return LegacyWheel.HUD_HEIGHT + 12;
    }
}
