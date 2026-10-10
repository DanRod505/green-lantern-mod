package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.aquaman.Respirator;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Atlantean Respirator: gills of Atlantean make. Works from anywhere in the inventory (it is not
 * armor, so it goes with any hero suit). See {@link Respirator}.
 */
public class AtlanteanRespiratorItem extends Item {
    public AtlanteanRespiratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack respirator) {
        return Respirator.fraction(respirator) < 1.0F;
    }

    @Override
    public int getBarWidth(ItemStack respirator) {
        return Math.round(13.0F * Respirator.fraction(respirator));
    }

    @Override
    public int getBarColor(ItemStack respirator) {
        float f = Respirator.fraction(respirator);
        return f < 0.15F ? 0xFF4040 : Mth.hsvToRgb(0.52F, 0.7F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack respirator, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int seconds = Respirator.air(respirator) / 20;
        tooltip.accept(Component.translatable("tooltip.greenlantern.respirator_air", seconds / 60, String.format("%02d", seconds % 60))
                .withStyle(Respirator.fraction(respirator) < 0.15F ? ChatFormatting.RED : ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.greenlantern.respirator_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
