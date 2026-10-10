package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.flash.FlashHelper;
import com.danrod505.greenlantern.flash.FlashHero;
import com.danrod505.greenlantern.flash.FlashSuit;
import com.danrod505.greenlantern.flash.SpeedsterPower;
import com.danrod505.greenlantern.flash.SpeedsterServer;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Flash ring. Like Barry Allen's, the suit is stored compressed inside it.
 * <ul>
 *     <li>Right click while not suited: the suit springs out of the ring.</li>
 *     <li>Right click while suited: uses the selected speedster power (also on the power key).</li>
 *     <li>Run (sprint while suited) to go fast; running also charges the Speed Force.</li>
 * </ul>
 */
public class FlashRingItem extends Item {
    public FlashRingItem(Properties properties) {
        super(properties);
    }

    /** Returns the given ring with a full Speed Force (used for the creative tab). */
    public static ItemStack charged(ItemStack ring) {
        FlashHero.SPEED_FORCE.set(ring, FlashHero.SPEED_FORCE.capacity());
        return ring;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack ring = player.getItemInHand(hand);
        if (!FlashHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) FlashSuit.summon(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            SpeedsterServer.usePower(serverPlayer, ring, SpeedsterPower.POWERS.selected(ring));
        }
        return InteractionResult.SUCCESS;
    }

    // ---- Speed Force bar on the item slot ---------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack ring) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack ring) {
        return Math.round(13.0F * FlashHero.SPEED_FORCE.get(ring).fraction());
    }

    @Override
    public int getBarColor(ItemStack ring) {
        float f = FlashHero.SPEED_FORCE.get(ring).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(0.12F + 0.03F * f, 0.9F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack ring, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RingEnergy force = FlashHero.SPEED_FORCE.get(ring);
        tooltip.accept(Component.translatable("tooltip.greenlantern.speed_force", force.stored(), force.capacity())
                .withStyle(force.fraction() < 0.2F ? ChatFormatting.RED : ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.greenlantern.power", SpeedsterPower.POWERS.selected(ring).displayName()).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.greenlantern.flash_ring_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
