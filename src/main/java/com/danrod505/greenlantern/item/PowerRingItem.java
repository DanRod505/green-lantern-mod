package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.construct.Construct;
import com.danrod505.greenlantern.construct.ConstructRegistry;
import com.danrod505.greenlantern.registry.ModSounds;
import com.danrod505.greenlantern.ring.LanternHero;
import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.ring.RingHelper;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Green Lantern Power Ring.
 * <ul>
 *     <li>Right click while not suited up: summons the uniform.</li>
 *     <li>Right click while suited up: uses the ability chosen on the wheel (its functions build the constructs).</li>
 *     <li>Use the Power Battery (lantern) with the ring in hand to recharge it.</li>
 * </ul>
 */
public class PowerRingItem extends Item {
    public PowerRingItem(Properties properties) {
        super(properties);
    }

    /** Returns the given ring stack with a full charge (used for the creative tab). */
    public static ItemStack charged(ItemStack ring) {
        RingEnergy.set(ring, RingEnergy.configuredCapacity());
        return ring;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack ring = player.getItemInHand(hand);

        if (!RingHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) {
                LanternHero.INSTANCE.summonSuit(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }

        // The ring's wheel (stage 3): the chosen ability, whose functions build today's constructs.
        if (player instanceof ServerPlayer serverPlayer) {
            LanternHero.INSTANCE.powers().use(serverPlayer, ring, -1);
        }
        return InteractionResult.SUCCESS;
    }

    public static void notifyNoEnergy(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_energy"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    // ---- Energy bar on the item slot ------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack ring) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack ring) {
        return Math.round(13.0F * RingEnergy.get(ring).fraction());
    }

    @Override
    public int getBarColor(ItemStack ring) {
        float f = RingEnergy.get(ring).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(0.33F, 0.85F, 0.55F + 0.45F * f);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack ring, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RingEnergy energy = RingEnergy.get(ring);
        tooltip.accept(Component.translatable("tooltip.greenlantern.energy", energy.stored(), energy.capacity())
                .withStyle(energy.fraction() < 0.2F ? ChatFormatting.RED : ChatFormatting.GREEN));
        var powers = LanternHero.INSTANCE.powers();
        tooltip.accept(Component.translatable("tooltip.greenlantern.power", powers.power(powers.selectedIndex(ring)).displayName())
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.accept(Component.translatable("tooltip.greenlantern.ring_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
