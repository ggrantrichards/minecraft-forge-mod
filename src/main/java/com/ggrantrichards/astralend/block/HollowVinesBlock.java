package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

// The tip of the Hollow Vines that hang from the Deep Hollows' Nullwood to the ground. Grows downward
// and is climbable, like weeping vines, in the forest's own black violet.
public class HollowVinesBlock extends WeepingVinesBlock {
    public HollowVinesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.HOLLOW_VINES_PLANT.get();
    }
}
