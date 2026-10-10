package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.ring.RingEnergy;
import com.danrod505.greenlantern.superman.SuperPower;
import com.danrod505.greenlantern.superman.SupermanHelper;
import com.danrod505.greenlantern.superman.SupermanHero;
import com.danrod505.greenlantern.superman.SupermanServer;
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
 * Superman's Kryptonian Crystal: the suit is kept in it, and it holds the sunlight his cells soak up.
 * <ul>
 *     <li>Right click while not suited: the suit with the S comes out of the crystal.</li>
 *     <li>Right click while suited: uses the selected power (also on the power key).</li>
 * </ul>
 */
public class KryptonianCrystalItem extends Item {
    public KryptonianCrystalItem(Properties properties) {
        super(properties);
    }

    /** Returns the given crystal full of sunlight (used for the creative tab). */
    public static ItemStack charged(ItemStack crystal) {
        SupermanHero.SOLAR_ENERGY.set(crystal, SupermanHero.SOLAR_ENERGY.capacity());
        return crystal;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack crystal = player.getItemInHand(hand);
        if (!SupermanHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) SupermanHero.INSTANCE.summonSuit(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            SupermanServer.usePower(serverPlayer, crystal, SuperPower.POWERS.selected(crystal));
        }
        return InteractionResult.SUCCESS;
    }

    // ---- energy bar on the item slot --------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack crystal) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack crystal) {
        return Math.round(13.0F * SupermanHero.SOLAR_ENERGY.get(crystal).fraction());
    }

    @Override
    public int getBarColor(ItemStack crystal) {
        float f = SupermanHero.SOLAR_ENERGY.get(crystal).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(0.11F + 0.04F * f, 0.85F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack crystal, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RingEnergy energy = SupermanHero.SOLAR_ENERGY.get(crystal);
        tooltip.accept(Component.translatable("tooltip.greenlantern.solar_energy", energy.stored(), energy.capacity())
                .withStyle(energy.fraction() < 0.2F ? ChatFormatting.RED : ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.greenlantern.power", SuperPower.POWERS.selected(crystal).displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.greenlantern.kryptonian_crystal_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
