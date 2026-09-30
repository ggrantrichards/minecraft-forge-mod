package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TwistingVinesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

// The growing tip of an Astral Vine. Grows upward and is climbable, like twisting vines.
public class AstralVineBlock extends TwistingVinesBlock {
    public AstralVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.ASTRAL_VINE_PLANT.get();
    }
}
