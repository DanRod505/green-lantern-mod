package com.danrod505.greenlantern.cyborg;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.cyborg.CyborgClient;
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
 * Cyborg: the Mother Box holds the Cyborg Battery and calls the suit. Made with the hero kit from
 * {@code docs/heroes/cyborg.md}; see {@link HeroDefinition} for every hook a hero can use (flight,
 * fall damage, damage taken, logout...).
 */
public final class CyborgHero extends HeroDefinition {
    /** The Cyborg Battery, kept in the Mother Box: it pays for the powers and comes back on its own. */
    public static final HeroEnergy ENERGY = new HeroEnergy(CyborgContent.ENERGY, () -> CyborgConfig.CAPACITY.get());

    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("cyborg_wheel", 0x212224, 0x0E0F10, 0xC81E1E, 0x80858C, 0x191A1C, 0x37393C,
                    0x6E7278, 0xB8BEC8, 0xFF5A3C, 0xFF5A3C, 0xC81E1E),
            GreenLantern.id("textures/gui/cyborg_powers.png"), 128, "wheel.greenlantern.cyborg_power_cost", "tooltip.greenlantern.cyborg_power",
            0x66121314, 0xFFFF5A3C, 0xFFC81E1E, 0xFFE8E8E8, 0xFFB0B0B0, 0xFFFF5A3C, 0xFFB0B0B0);

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
    }
}
