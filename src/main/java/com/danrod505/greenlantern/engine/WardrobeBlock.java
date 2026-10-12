package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Heroes' Wardrobe (catalog, stage 13): used with a character's item in hand, or with the
 * character's suit on, it changes to the next skin that item has unlocked (the same as the skin key).
 */
public class WardrobeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WardrobeBlock> CODEC = simpleCodec(WardrobeBlock::new);

    public WardrobeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        for (HeroDefinition hero : HeroRegistry.all()) {
            if (hero instanceof EngineHero engine && engine.isActivator(stack)) {
                if (player instanceof ServerPlayer serverPlayer) EngineServer.cycleSkin(serverPlayer, engine, stack);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (HeroRegistry.context(player).orElse(null) instanceof EngineHero engine) {
            if (player instanceof ServerPlayer serverPlayer) EngineServer.cycleSkin(serverPlayer, engine, engine.findItem(player));
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.greenlantern.wardrobe_empty"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
