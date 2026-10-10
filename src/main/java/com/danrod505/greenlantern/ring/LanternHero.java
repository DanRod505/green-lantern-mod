package com.danrod505.greenlantern.ring;

import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.LanternClient;
import com.danrod505.greenlantern.flight.FlightProfile;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.WheelTheme;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The Green Lantern: the Power Ring calls the uniform, flies and builds constructs (picked on the construct wheel). */
public final class LanternHero extends HeroDefinition {
    /** Colours of the construct wheel: green hard light. */
    public static final WheelTheme WHEEL_THEME = new WheelTheme("wheel", 0x07120A, 0x030604, 0x2A8A44, 0x2E6A3E, 0x060D08, 0x0F2216,
            0x0F4A20, 0x2FA84E, 0x7CFF96, 0xC8FFD2, 0x3CE064);

    public static final LanternHero INSTANCE = new LanternHero();

    private LanternHero() {
        super("lantern", RingHelper::isRing, ModItems.LANTERN_SUIT);
    }

    @Override
    public Supplier<HeroClient> client() {
        return LanternClient::new;
    }

    @Override
    public boolean summonSuit(ServerPlayer player) {
        return Uniform.summon(player);
    }

    @Override
    public void dismissSuit(ServerPlayer player, boolean effects) {
        Uniform.dismiss(player, effects);
    }

    @Override
    public FlightProfile flightProfile() {
        return FlightProfile.lantern();
    }

    /** The uniform is made of the ring's light: no ring or no energy, no uniform (and the rest of the tick is skipped). */
    @Override
    public boolean checkSuit(ServerPlayer player) {
        if (!isSuited(player)) return true;
        ItemStack ring = RingHelper.findRing(player);
        if (ring.isEmpty()) {
            Uniform.dismiss(player, true);
            return false;
        }
        RingEnergy energy = RingEnergy.get(ring);
        if (energy.stored() <= 0 && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.greenlantern.power_out").withStyle(ChatFormatting.RED), true);
            Uniform.dismiss(player, true);
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
