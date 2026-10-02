package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// A two-block-tall End plant, like a sunflower, that grows on the End's ground (#astralend:astral_soil):
// the Corona's Flarebloom and the Ashen Barrens' Shimmer Reed.
public class EndTallPlantBlock extends DoublePlantBlock {
    public EndTallPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.ASTRAL_SOIL);
    }
}
