package com.ggrantrichards.astralend.station;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A station bulkhead: a slab of armoured bronze sealing the vault. The Solar Core opens every
// bulkhead nearby for five minutes (OPEN: it retracts into the frame and you can walk through),
// then they grind shut again - unless someone is standing in the doorway.
public class BulkheadBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final int OPEN_TICKS = 6000;

    public BulkheadBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    public static void open(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, state.getBlock(), OPEN_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) {
            return;
        }
        if (!level.getEntitiesOfClass(Entity.class, new AABB(pos)).isEmpty()) {
            level.scheduleTick(pos, this, 20);   // someone's in the way; try again in a second
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_CLIENTS);
        if (random.nextInt(4) == 0) {
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
        }
    }
}
