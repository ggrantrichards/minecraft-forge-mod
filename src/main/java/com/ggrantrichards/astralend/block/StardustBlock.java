package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Stardust "falls" upward: like sand, but it drifts up one block at a time until it hits
// something. Stardust that rises past the top of the world drifts off into space.
public class StardustBlock extends Block {
    private static final int RISE_DELAY = 2;

    public StardustBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, RISE_DELAY);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        level.scheduleTick(pos, this, RISE_DELAY);
        return super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (!FallingBlock.isFree(level.getBlockState(above))) {
            return;
        }
        level.removeBlock(pos, false);
        if (above.getY() <= level.getMaxBuildHeight() - 1) {
            level.setBlockAndUpdate(above, state);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // A faint sparkle drifting up off loose stardust, the reverse of sand's falling dust.
        if (random.nextInt(12) == 0 && FallingBlock.isFree(level.getBlockState(pos.above()))) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
