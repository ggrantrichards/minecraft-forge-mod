package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

// Breathcomb, the Eclipse's glowing comb. All of it in the world opens and closes together over about
// five minutes (BREATH_TICKS), so the heaven half's light slowly swells and fades - it breathes. Each
// block catches up with the breath on its random ticks (about once a minute), so the swell ripples
// across a field of it rather than flicking all at once.
public class BreathcombBlock extends Block {
    public static final IntegerProperty BREATH = IntegerProperty.create("breath", 0, 3);
    public static final long BREATH_TICKS = 6000;

    public BreathcombBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BREATH, 0));
    }

    // 0 (closed, dim) .. 3 (open, bright) at this moment of the breath.
    public static int breathAt(long gameTime) {
        double phase = (gameTime % BREATH_TICKS) / (double) BREATH_TICKS;
        return Mth.clamp((int) Math.round((Math.sin(phase * Math.PI * 2.0) + 1.0) * 1.5), 0, 3);
    }

    public static int light(BlockState state) {
        return 3 + 4 * state.getValue(BREATH);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BREATH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        return this.defaultBlockState().setValue(BREATH, breathAt(level.getGameTime()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int breath = breathAt(level.getGameTime());
        if (state.getValue(BREATH) != breath) {
            level.setBlock(pos, state.setValue(BREATH, breath), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }
}
