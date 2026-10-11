package com.danrod505.greenlantern.hero.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/**
 * What powers do to blocks, over the positions {@link Area} gives. Each method returns how many
 * blocks it changed, so a power can play a sound only when something happened.
 */
public final class BlockEffects {
    private BlockEffects() {
    }

    /** Glass blocks, glass panes and thin ice (never packed or blue ice). */
    public static boolean isGlass(BlockState state) {
        return state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(BlockTags.ICE) && !state.is(Blocks.PACKED_ICE) && !state.is(Blocks.BLUE_ICE);
    }

    /** Glass and thin ice shatter, dropping nothing. */
    public static int shatterGlass(ServerLevel level, Iterable<BlockPos> positions, Entity breaker) {
        int count = 0;
        for (BlockPos pos : positions) {
            if (isGlass(level.getBlockState(pos)) && level.destroyBlock(pos, false, breaker)) count++;
        }
        return count;
    }

    /** Fire goes out, campfires are put out and torches fall (they drop as items). */
    public static int snuffFire(ServerLevel level, Iterable<BlockPos> positions, Entity breaker, boolean torches) {
        int count = 0;
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
                level.levelEvent(null, 1009, pos, 0);
                count++;
            } else if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
                count++;
            } else if (torches && (state.getBlock() instanceof TorchBlock || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH))) {
                if (level.destroyBlock(pos, true, breaker)) count++;
            }
        }
        return count;
    }

    /** Still water with air above turns to ice, and lava with air above turns to obsidian when {@code lava}. */
    public static int freeze(ServerLevel level, Iterable<BlockPos> positions, boolean lava) {
        int count = 0;
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isSource() || !level.getBlockState(pos.above()).isAir()) continue;
            if (state.is(Blocks.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                count++;
            } else if (lava && state.is(Blocks.LAVA)) {
                level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                level.levelEvent(null, 1501, pos, 0);
                count++;
            }
        }
        return count;
    }
}
