package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// A tall Corona flower, two blocks high like a sunflower. Its bud stays tightly shut against
// the Remnant's glare. (Stellar Flares, which open it, arrive in Phase 5.)
public class FlarebloomBlock extends DoublePlantBlock {
    public FlarebloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.ASTRAL_SOIL);
    }
}
