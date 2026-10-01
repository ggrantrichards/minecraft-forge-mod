package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.StellarFlares;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Sunscale Lichen: a ground cover (a carpet) with a faint glow. A Stellar Flare charges it, and then
// it burns bright gold (light 12 instead of 5) through the afterglow, fading out a while later.
public class SunscaleLichenBlock extends CarpetBlock {
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    public SunscaleLichenBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(CHARGED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FlarePhase phase = StellarFlares.phaseIn(level);
        if (phase != FlarePhase.FLARE && phase != FlarePhase.AFTERGLOW && random.nextInt(3) == 0) {
            level.setBlock(pos, state.setValue(CHARGED, false), Block.UPDATE_CLIENTS);
        }
    }
}
