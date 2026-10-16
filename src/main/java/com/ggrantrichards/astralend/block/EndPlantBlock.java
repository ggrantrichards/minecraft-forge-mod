package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A small plant that grows on the End's ground blocks (the #astralend:astral_soil tag):
// Shimmer Grass, Ashen Lumineshrooms and Petrified Astral Vines.
public class EndPlantBlock extends BushBlock {
    private final VoxelShape shape;
    private final TagKey<Block> soil;

    // (1.21.1 port) Blocks name a codec; this one is made in code, so its codec is the block itself.
    @Override
    protected com.mojang.serialization.MapCodec<EndPlantBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public EndPlantBlock(VoxelShape shape, TagKey<Block> soil, BlockBehaviour.Properties properties) {
        super(properties);
        this.shape = shape;
        this.soil = soil;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(this.soil);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return this.mayPlaceOn(level.getBlockState(below), level, below);
    }
}
