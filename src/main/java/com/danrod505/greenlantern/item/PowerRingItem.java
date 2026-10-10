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
 *     <li>Right click while suited up: creates the selected construct.</li>
 *     <li>Use the Power Battery (lantern) with the ring in hand to recharge it.</li>
 * </ul>
 */
public class PowerRingItem extends Item {
    private static final int HOLD_DURATION = 72000;

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

        Construct construct = ConstructRegistry.selected(ring);
        boolean toggle = construct.useMode() == Construct.UseMode.TOGGLE;
        // Toggle constructs can always be dismissed; only new constructs need energy.
        if (!toggle && !player.isCreative() && !RingEnergy.has(ring, Math.max(1, construct.activationCost()))) {
            if (player instanceof ServerPlayer serverPlayer) {
                notifyNoEnergy(serverPlayer);
            }
            return InteractionResult.FAIL;
        }

        if (construct.useMode() == Construct.UseMode.HOLD) {
            player.startUsingItem(hand);
            if (player instanceof ServerPlayer serverPlayer) {
                construct.activate(serverPlayer, ring);
            }
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            if (construct.activate(serverPlayer, ring) && construct.cooldown() > 0) {
                serverPlayer.getCooldowns().addCooldown(ring, construct.cooldown());
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void notifyNoEnergy(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.greenlantern.no_energy"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOW_ENERGY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack ring, int remainingTicks) {
        if (entity instanceof ServerPlayer player) {
            Construct construct = ConstructRegistry.selected(ring);
            if (construct.useMode() == Construct.UseMode.HOLD && RingHelper.isSuited(player)) {
                construct.holdTick(player, ring, getUseDuration(ring, entity) - remainingTicks);
            } else {
                player.stopUsingItem();
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack ring, Level level, LivingEntity entity, int remainingTicks) {
        if (entity instanceof ServerPlayer player) {
            Construct construct = ConstructRegistry.selected(ring);
            if (construct.useMode() == Construct.UseMode.HOLD) {
                construct.release(player, ring, getUseDuration(ring, entity) - remainingTicks);
                if (construct.cooldown() > 0) {
                    player.getCooldowns().addCooldown(ring, construct.cooldown());
                }
            }
        }
        return true;
    }

    @Override
    public int getUseDuration(ItemStack ring, LivingEntity entity) {
        return ConstructRegistry.selected(ring).useMode() == Construct.UseMode.HOLD ? HOLD_DURATION : 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack ring) {
        return ItemUseAnimation.NONE;
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
        tooltip.accept(Component.translatable("tooltip.greenlantern.construct", ConstructRegistry.selected(ring).name())
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.accept(Component.translatable("tooltip.greenlantern.ring_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
