package com.ggrantrichards.astralend.starforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

// A tile of rift that Rift Walker boots lay under your feet over a drop (like Frost Walker's ice).
// It cracks and crumbles a second later, back into whatever was there (air, or lava it covered).
// Nothing drops from it and it can't be picked.
public class RiftTileBlock extends Block {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty OVER_LAVA = BooleanProperty.create("over_lava");
    private static final int TICKS_PER_STAGE = 5;     // 4 stages: a second in all

    public RiftTileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(OVER_LAVA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, OVER_LAVA);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        level.scheduleTick(pos, this, TICKS_PER_STAGE);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 3) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, TICKS_PER_STAGE);
            return;
        }
        level.setBlock(pos, state.getValue(OVER_LAVA) ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.4F, 1.6F);
    }
}
