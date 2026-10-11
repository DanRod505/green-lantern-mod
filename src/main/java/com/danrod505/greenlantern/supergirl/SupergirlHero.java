package com.danrod505.greenlantern.supergirl;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.supergirl.SupergirlClient;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Supergirl: the Argo Pendant holds the Supergirl's Solar Energy and calls the suit. Made with the hero kit from
 * {@code docs/heroes/supergirl.md}; see {@link HeroDefinition} for every hook a hero can use (flight,
 * fall damage, damage taken, logout...).
 */
public final class SupergirlHero extends HeroDefinition {
    /** The Supergirl's Solar Energy, kept in the Argo Pendant: it pays for the powers and comes back on its own. */
    public static final HeroEnergy ENERGY = new HeroEnergy(SupergirlContent.ENERGY, () -> SupergirlConfig.CAPACITY.get());

    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("supergirl_wheel", 0x071328, 0x030811, 0xD8202E, 0x1D4D9C, 0x050F1F, 0x0C2143,
                    0x194286, 0x2A6FE0, 0xFFD447, 0xFFD447, 0xD8202E),
            GreenLantern.id("textures/gui/supergirl_powers.png"), 128, "wheel.greenlantern.supergirl_solar_cost", "tooltip.greenlantern.supergirl_solar",
            0x66040B16, 0xFFFFD447, 0xFFD8202E, 0xFFE8E8E8, 0xFFB0B0B0, 0xFFFFD447, 0xFFB0B0B0);

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
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.45, 0.9, 0.45, 0.05);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.45, 0.9, 0.45, 0.05);
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
    }
}
