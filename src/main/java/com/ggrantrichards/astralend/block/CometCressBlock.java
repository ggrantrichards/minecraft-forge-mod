package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A 4-stage crop that hangs from the underside of Stardust and grows downward, like a
// comet's tail. It doesn't need light, since the End has none to spare.
public class CometCressBlock extends CropBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    // Hanging from the top of the block space: taller as it grows.
    private static final VoxelShape[] SHAPES = com.ggrantrichards.astralend.compat.Compat.boxes(MAX_AGE, age -> com.ggrantrichards.astralend.compat.Compat.column(16.0, 14.0 - age * 2, 16.0));

    public CometCressBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.COMET_CRESS.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.STARDUST_BLOCK.get());
    }

    // Attached to the block ABOVE, not below.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return this.mayPlaceOn(level.getBlockState(above), level, above);
    }

    // Grows in any light: about as fast as beetroot does in daylight.
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.isMaxAge(state) && random.nextInt(6) == 0) {
            level.setBlock(pos, this.getStateForAge(this.getAge(state) + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 1;
    }
}
