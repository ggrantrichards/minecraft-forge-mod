package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.feature.ChorusTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A Chorus Sprout, dropped by Chorus Bloom. Plant it on the End's ground; it grows into a chorus tree on
// its own after a while, like any sapling.
public class ChorusSproutBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 12.0);

    public ChorusSproutBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.ASTRAL_SOIL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return this.mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0) {
            this.grow(level, random, pos);
        }
    }

    private void grow(ServerLevel level, RandomSource random, BlockPos pos) {
        level.removeBlock(pos, false);
        if (!ChorusTreeFeature.grow(level, random, pos)) {
            level.setBlock(pos, this.defaultBlockState(), 3);  // no room: stay a sapling
        }
    }
}
