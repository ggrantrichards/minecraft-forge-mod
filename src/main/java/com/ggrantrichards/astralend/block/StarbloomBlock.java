package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// A star-shaped flower. Grows anywhere normal flowers do, plus End Stone and Nebula Moss.
// In suspicious stew it gives Night Vision, like a poppy's cousin from the stars.
public class StarbloomBlock extends FlowerBlock {
    public StarbloomBlock(BlockBehaviour.Properties properties) {
        super(MobEffects.NIGHT_VISION, 5.0F, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(Blocks.END_STONE) || state.is(ModBlocks.NEBULA_MOSS.get());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return this.mayPlaceOn(level.getBlockState(below), level, below);
    }
}
