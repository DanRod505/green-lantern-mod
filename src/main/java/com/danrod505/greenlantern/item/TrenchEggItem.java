package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.entity.TrenchCreatureEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Egg of a creature (or a brute) of the Trench. Use it on the water or on a block: the creature it
 * hatches belongs to no nest, so it lurks around that spot and hunts whatever swims close.
 */
public class TrenchEggItem extends Item {
    private final boolean brute;

    public TrenchEggItem(boolean brute, Properties properties) {
        super(properties);
        this.brute = brute;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        return hatch(serverLevel, context.getPlayer(), context.getItemInHand(), pos);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand))) return InteractionResult.FAIL;
        return hatch(serverLevel, player, player.getItemInHand(hand), pos);
    }

    private InteractionResult hatch(ServerLevel level, Player player, ItemStack egg, BlockPos pos) {
        TrenchCreatureEntity creature = TrenchCreatureEntity.spawn(level, Vec3.atBottomCenterOf(pos), brute, -1, 0);
        if (creature == null) return InteractionResult.PASS;
        if (egg.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) creature.setCustomName(egg.getHoverName());
        egg.consume(1, player);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.greenlantern.trench_egg").withStyle(ChatFormatting.DARK_RED));
    }
}
