package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.GLConfig;
import com.danrod505.greenlantern.atlantis.AtlantisTravel;
import com.danrod505.greenlantern.entity.AtlantisPortalEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Atlantean Gate: a conch of gold and prismarine that opens the same whirlpool portal as
 * Aquaman. Anywhere in the world it opens the way to Atlantis; in Atlantis it opens the way back to
 * where the traveller came from.
 */
public class AtlantisGateItem extends Item {
    public AtlantisGateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack gate = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(gate)) return InteractionResult.FAIL;
        if (player instanceof ServerPlayer serverPlayer) {
            if (!AtlantisTravel.available(serverPlayer)) return InteractionResult.FAIL;
            AtlantisPortalEntity portal = AtlantisPortalEntity.open(serverPlayer.level(), serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(portal.leadsHome()
                    ? "message.greenlantern.atlantis_portal_home" : "message.greenlantern.atlantis_portal").withStyle(ChatFormatting.AQUA), true);
            player.getCooldowns().addCooldown(gate, GLConfig.ATLANTIS_GATE_COOLDOWN.get() * 20);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack gate, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.greenlantern.atlantis_gate_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
