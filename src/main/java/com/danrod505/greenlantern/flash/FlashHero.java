package com.danrod505.greenlantern.flash;

import java.util.Set;
import com.danrod505.greenlantern.engine.LegacyWheel;
import com.danrod505.greenlantern.engine.EngineServer;
import com.danrod505.greenlantern.engine.EngineAbility;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.AbilityHero;
import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.speed.FlashClient;
import com.danrod505.greenlantern.engine.Characters;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.item.FlashRingItem;
import com.danrod505.greenlantern.registry.ModDataComponents;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModParticles;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The Flash: the ring holds the suit and the Speed Force (it refills on its own, much faster while
 * running). The suit springs out of the ring (like Barry Allen's); while it is worn the player walks
 * faster and steps up whole blocks, so a run is never stopped by a single step.
 */
public final class FlashHero extends HeroDefinition implements AbilityHero {
    /** Speed Force stored in the Flash ring: pays for the speedster powers (tornado, phasing, lightning). */
    public static final HeroEnergy SPEED_FORCE = new HeroEnergy(ModDataComponents.SPEED_FORCE, () -> GLConfig.SPEED_FORCE_CAPACITY.get());

    /** Speed Force red and gold. */
    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("speed_wheel", 0x160604, 0x080202, 0xC8902A, 0x8A2A1C, 0x120504, 0x2A0A06,
                    0x6A1408, 0xC8301A, 0xFFD24A, 0xFFF0B0, 0xFFC830),
            GreenLantern.id("textures/gui/flash_powers.png"), 64, "wheel.greenlantern.speed_cost", "tooltip.greenlantern.speed_force",
            0x66100400, 0xFFD24A, 0xFFE6A0, 0xF0E0D0, 0xD8B8A8, 0xFFD27A, 0xC8B8A8);

    public static final FlashHero INSTANCE = new FlashHero();

    /** The powers of today: the abilities of the new wheel call them by name (preset poder_de_hoje). */
    private final HeroPowers<SpeedsterPower> legacy = new HeroPowers<>(SpeedsterPower.POWERS, SPEED_FORCE, ChatFormatting.YELLOW,
            1.0F, 0.08F, SpeedsterServer::usePower, WHEEL);
    /** The wheel in the new model (stage 3): the abilities of the catalog, with the powers of today inside them. */
    private final LegacyWheel wheel = new LegacyWheel("flash", SPEED_FORCE, WHEEL, ChatFormatting.YELLOW, 1.0F, 0.08F, legacy);

    private FlashHero() {
        super("flash", FlashHelper::isRing, new SuitSet(ModItems.FLASH_MASK, ModItems.FLASH_SUIT, ModItems.FLASH_LEGGINGS, ModItems.FLASH_BOOTS,
                "message.greenlantern.no_flash_ring", ModSounds.FLASH_SUIT_UP, ModSounds.FLASH_SUIT_DOWN, Characters.suitModifiers("flash")));
    }

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {
        output.accept(FlashRingItem.charged(ModItems.FLASH_RING.get().getDefaultInstance()));
        output.accept(ModItems.FLASH_RING.get());
    }

    @Override
    public Supplier<HeroClient> client() {
        return FlashClient::new;
    }

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {
        // The transformation of the catalog (stage 3, lot 2).
        EngineServer.legacySuitUp(player, this);
    }

    @Override
    protected void onSuitRemoving(ServerPlayer player) {
        EngineServer.legacySuitDown(player);
        SpeedsterServer.endPhase(player, false);
    }

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.9, 0.4, 0.1);
    }

    @Override
    public HeroPowers<EngineAbility> powers() {
        return wheel.powers();
    }

    @Override
    public HeroPowers<SpeedsterPower> legacyPowers() {
        return legacy;
    }

    @Override
    public CharacterData data() {
        return wheel.data();
    }

    @Override
    public HeroEnergy energy() {
        return SPEED_FORCE;
    }

    @Override
    public Set<String> ownTraits() {
        return LegacyWheel.OWN_TRAITS;
    }

    @Override
    public int hudHeight() {
        return LegacyWheel.HUD_HEIGHT;
    }

    @Override
    public void tick(ServerPlayer player) {
        SpeedsterServer.tick(player);
        EngineServer.legacyTick(player, this);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        SpeedsterServer.endPhase(player, false);
        SpeedsterServer.remove(player);
    }

    /** A speedster lands running. */
    @Override
    public float fallDamageMultiplier(Player player, double distance) {
        return 0.0F;
    }
}
