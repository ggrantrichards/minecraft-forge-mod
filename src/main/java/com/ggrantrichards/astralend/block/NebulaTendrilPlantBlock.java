package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.WeepingVinesPlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

// The body of a Nebula Tendril, above its glowing tip.
public class NebulaTendrilPlantBlock extends WeepingVinesPlantBlock {
    public NebulaTendrilPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return (GrowingPlantHeadBlock) ModBlocks.NEBULA_TENDRIL.get();
    }
}
