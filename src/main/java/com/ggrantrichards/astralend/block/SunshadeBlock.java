package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Sunshade: a square of Skimmer Film on a bronze frame, hung at the top of a block space. It needs
// nothing to hold it up, so in the Corona you can put one over your head and keep mining through a
// flare: it counts as cover against Flare Burn. You can see the sky through it.
public class SunshadeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);

    public SunshadeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
