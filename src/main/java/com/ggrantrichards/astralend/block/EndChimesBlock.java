package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// End Chimes: End Rods and amethyst hung from the underside of a block. They give light and a low
// hum. When something teleports within 16 blocks - an Enderman, a thrown Ender Pearl, a Chorus
// Fruit - they ring, and for 10 seconds after that nothing can teleport near them (EndChimesBlockEntity).
public class EndChimesBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.column(10.0, 2.0, 16.0);

    public EndChimesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndChimesBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // They hang: the block above must hold them up.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return direction == Direction.UP && !this.canSurvive(state, level, pos)
            ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    // Struck by hand, they ring too - and go still. (In a Leviathan's reliquary, that's how the
    // Curator's teleports are stopped: see Curator.)
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                  net.minecraft.world.entity.player.Player player,
                                                                  net.minecraft.world.phys.BlockHitResult hit) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && level.getBlockEntity(pos) instanceof EndChimesBlockEntity chime) {
            chime.ring(server);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(60) == 0) {
            level.playLocalSound(pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.25F, 0.5F, false);   // the hum
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.2,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, -0.01, 0.0);
        }
    }
}
