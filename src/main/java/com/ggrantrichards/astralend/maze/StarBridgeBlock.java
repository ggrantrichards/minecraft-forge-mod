package com.ggrantrichards.astralend.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A star-bridge in a Constellation Maze. Dark, it's nothing at all: invisible, with nothing to stand
// on or click (only a rare glint gives it away). When someone at a Maze Telescope finds the
// matching constellation, every bridge of that constellation nearby lights up and is solid for 20
// seconds (see MazeScoping), then goes dark again.
public class StarBridgeBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty CONSTELLATION = IntegerProperty.create("constellation", 0, 3);
    public static final int LIT_TICKS = 400;

    public StarBridgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(CONSTELLATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, CONSTELLATION);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LIT) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LIT) ? Shapes.block() : Shapes.empty();
    }

    // Light up for LIT_TICKS.
    public static void light(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, state.getBlock(), LIT_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
            }
        } else if (random.nextInt(90) == 0) {
            // A dark bridge shows itself only as the odd glint, so a sharp eye can trace it.
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 0.5 + random.nextDouble() * 0.5,
                pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
