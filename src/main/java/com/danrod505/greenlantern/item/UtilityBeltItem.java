package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.batman.BatPower;
import com.danrod505.greenlantern.batman.BatmanHelper;
import com.danrod505.greenlantern.batman.BatmanHero;
import com.danrod505.greenlantern.batman.BatmanServer;
import com.danrod505.greenlantern.batman.BatmanSuit;
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
 * Batman's Utility Belt: the batsuit is folded inside it, and its power cells run the gadgets.
 * <ul>
 *     <li>Right click while not suited: the batsuit comes out of the belt.</li>
 *     <li>Right click while suited: uses the selected gadget (also on the power key).</li>
 * </ul>
 */
public class UtilityBeltItem extends Item {
    public UtilityBeltItem(Properties properties) {
        super(properties);
    }

    /** Returns the given belt fully charged (used for the creative tab). */
    public static ItemStack charged(ItemStack belt) {
        BatmanHero.BAT_CHARGE.set(belt, BatmanHero.BAT_CHARGE.capacity());
        return belt;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack belt = player.getItemInHand(hand);
        if (!BatmanHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) BatmanSuit.summon(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BatmanServer.usePower(serverPlayer, belt, BatPower.POWERS.selected(belt));
        }
        return InteractionResult.SUCCESS;
    }

    // ---- charge bar on the item slot --------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack belt) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack belt) {
        return Math.round(13.0F * BatmanHero.BAT_CHARGE.get(belt).fraction());
    }

    @Override
    public int getBarColor(ItemStack belt) {
        float f = BatmanHero.BAT_CHARGE.get(belt).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(0.13F + 0.02F * f, 0.9F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack belt, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RingEnergy charge = BatmanHero.BAT_CHARGE.get(belt);
        tooltip.accept(Component.translatable("tooltip.greenlantern.bat_charge", charge.stored(), charge.capacity())
                .withStyle(charge.fraction() < 0.2F ? ChatFormatting.RED : ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.greenlantern.gadget", BatPower.POWERS.selected(belt).displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.greenlantern.utility_belt_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
