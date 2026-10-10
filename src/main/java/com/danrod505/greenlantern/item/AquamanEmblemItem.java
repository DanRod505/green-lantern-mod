package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.aquaman.AquaPower;
import com.danrod505.greenlantern.aquaman.AquamanHelper;
import com.danrod505.greenlantern.aquaman.AquamanHero;
import com.danrod505.greenlantern.aquaman.AquamanServer;
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
 * The Atlantean Emblem: the golden "A" of the King of Atlantis. The armor of Atlantis lives inside
 * it and it stores the Power of the Seas.
 * <ul>
 *     <li>Right click while not suited: the armor springs out of the emblem.</li>
 *     <li>Right click while suited: uses the selected power (also on the power key).</li>
 * </ul>
 */
public class AquamanEmblemItem extends Item {
    public AquamanEmblemItem(Properties properties) {
        super(properties);
    }

    /** Returns the given emblem full of Power of the Seas (used for the creative tab). */
    public static ItemStack charged(ItemStack emblem) {
        AquamanHero.SEA_FORCE.set(emblem, AquamanHero.SEA_FORCE.capacity());
        return emblem;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack emblem = player.getItemInHand(hand);
        if (!AquamanHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) AquamanHero.INSTANCE.summonSuit(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AquamanServer.usePower(serverPlayer, emblem, AquaPower.POWERS.selected(emblem));
        }
        return InteractionResult.SUCCESS;
    }

    // ---- Power of the Seas bar on the item slot ---------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack emblem) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack emblem) {
        return Math.round(13.0F * AquamanHero.SEA_FORCE.get(emblem).fraction());
    }

    @Override
    public int getBarColor(ItemStack emblem) {
        float f = AquamanHero.SEA_FORCE.get(emblem).fraction();
        return f < 0.2F ? 0xFF4040 : Mth.hsvToRgb(0.47F + 0.03F * f, 0.85F, 1.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack emblem, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        RingEnergy force = AquamanHero.SEA_FORCE.get(emblem);
        tooltip.accept(Component.translatable("tooltip.greenlantern.sea_force", force.stored(), force.capacity())
                .withStyle(force.fraction() < 0.2F ? ChatFormatting.RED : ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.greenlantern.power", AquaPower.POWERS.selected(emblem).displayName()).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.greenlantern.aquaman_emblem_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
