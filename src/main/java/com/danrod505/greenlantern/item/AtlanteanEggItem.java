package com.danrod505.greenlantern.item;

import com.danrod505.greenlantern.entity.AtlanteanMountEntity;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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

/**
 * Egg of one of the sea creatures of Atlantis. Use it on the water (it hatches right there) or on a
 * block (it hatches on that side of the block). Works like a vanilla spawn egg: a name given in an
 * anvil goes to the creature, and creative mode keeps the egg.
 */
public class AtlanteanEggItem extends Item {
    private final Supplier<? extends EntityType<? extends AtlanteanMountEntity>> type;

    public AtlanteanEggItem(Supplier<? extends EntityType<? extends AtlanteanMountEntity>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EntityType<? extends AtlanteanMountEntity> type() {
        return type.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        return hatch(serverLevel, context.getPlayer(), context.getItemInHand(), pos, face == Direction.UP && !pos.equals(clicked));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand))) return InteractionResult.FAIL;
        return hatch(serverLevel, player, player.getItemInHand(hand), pos, false);
    }

    private InteractionResult hatch(ServerLevel level, Player player, ItemStack egg, BlockPos pos, boolean onTop) {
        AtlanteanMountEntity creature = type().spawn(level, egg, player, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, onTop);
        if (creature == null) return InteractionResult.PASS;
        if (player != null) {
            creature.setYRot(player.getYRot() + 180.0F);
            creature.setYBodyRot(creature.getYRot());
        }
        egg.consume(1, player);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.greenlantern.atlantean_egg").withStyle(ChatFormatting.AQUA));
    }
}
