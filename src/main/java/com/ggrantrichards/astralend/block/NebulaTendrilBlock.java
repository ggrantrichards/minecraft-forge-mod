package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

// The glowing tip of a Nebula Tendril. Tendrils hang from the undersides of Nebula Fields islands
// and grow downward, like weeping vines.
public class NebulaTendrilBlock extends WeepingVinesBlock {
    public NebulaTendrilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.NEBULA_TENDRIL_PLANT.get();
    }
}
