package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.wonderwoman.AmazonPower;
import com.danrod505.greenlantern.wonderwoman.DivinePower;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanServer;
import com.danrod505.greenlantern.wonderwoman.WonderWomanSuit;
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
 * Wonder Woman's Tiara of Themyscira: the armor of the Amazon princess is kept in it, and it holds
 * the divine power of the gifts of the gods.
 * <ul>
 *     <li>Right click while not suited: the armor of Themyscira comes out of the tiara.</li>
 *     <li>Right click while suited: uses the selected power (also on the power key).</li>
 * </ul>
 */
public class AmazonTiaraItem extends Item {
    public AmazonTiaraItem(Properties properties) {
        super(properties);
    }

    /** Returns the given tiara full of divine power (used for the creative tab). */
    public static ItemStack charged(ItemStack tiara) {
        DivinePower.set(tiara, DivinePower.capacity());
        return tiara;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack tiara = player.getItemInHand(hand);
        if (!WonderWomanHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) WonderWomanSuit.summon(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            WonderWomanServer.usePower(serverPlayer, tiara, AmazonPower.selected(tiara));
        }
        return InteractionResult.SUCCESS;
    }

    // ---- power bar on the item slot ------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack tiara) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack tiara) {
        return Math.round(13.0F * DivinePower.get(tiara).fraction());
    }

    @Override
    public int getBarColor(ItemStack tiara) {
        float f = DivinePower.get(tiara).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(0.12F + 0.02F * f, 0.8F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack tiara, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RingEnergy energy = DivinePower.get(tiara);
        tooltip.accept(Component.translatable("tooltip.greenlantern.divine_power", energy.stored(), energy.capacity())
                .withStyle(energy.fraction() < 0.2F ? ChatFormatting.RED : ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.greenlantern.power", AmazonPower.selected(tiara).displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.greenlantern.amazon_tiara_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
