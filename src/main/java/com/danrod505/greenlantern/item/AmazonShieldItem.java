package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.wonderwoman.WonderWomanHelper;
import com.danrod505.greenlantern.wonderwoman.WonderWomanServer;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jspecify.annotations.Nullable;

/**
 * The Amazon shield: a round shield of Themysciran bronze and silver with the golden star. Called by
 * the tiara, it only exists on Wonder Woman's arm (it vanishes with the armor).
 * <ul>
 *     <li>Hold right click: block, like any shield (it never breaks and an axe barely staggers it).</li>
 *     <li>Sneak + right click (or the shield power): throw it. It bounces from enemy to enemy and
 *     always comes back to her arm, like Aquaman's trident to his hand.</li>
 * </ul>
 */
public class AmazonShieldItem extends net.minecraft.world.item.ShieldItem {
    public AmazonShieldItem(Properties properties) {
        super(properties);
    }

    public static Properties properties() {
        return new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                        0.1F,
                        0.4F,
                        List.of(new BlocksAttacks.DamageReduction(100.0F, Optional.empty(), 0.0F, 1.0F)),
                        BlocksAttacks.ItemDamageFunction.DEFAULT,
                        Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK)));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() && WonderWomanHelper.isSuited(player)) {
            if (player instanceof ServerPlayer serverPlayer) WonderWomanServer.throwShieldFromItem(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action == ToolActions.SHIELD_BLOCK || super.canPerformAction(stack, action);
    }

    // ---- only on Wonder Woman's arm ----------------------------------------------------------------------

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player && !WonderWomanHelper.isSuited(player) && !player.isCreative()) {
            stack.setCount(0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.greenlantern.amazon_shield_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
