package com.ggrantrichards.astralend.maw;

import com.ggrantrichards.astralend.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

// One of a Maw's four eye sockets (a Leviathan's skull). Set a Charged Void Crystal in it and it lights, and the dark eye
// nearest it with it (two crystals for a Maw); with all four lit, a Portal Lens wakes the Maw (MawThroatBlock).
public class MawEyeBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MawEyeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT) || !stack.is(ModItems.CHARGED_VOID_CRYSTAL.get())) {
            return net.minecraft.world.Itemnet.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            // One crystal lights two eyes: this one and the nearest dark one, so two wake a Maw.
            BlockPos twin = null;
            double best = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-12, -6, -12), pos.offset(12, 6, 12))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof MawEyeBlock && !s.getValue(LIT) && p.distSqr(pos) < best) {
                    best = p.distSqr(pos);
                    twin = p.immutable();
                }
            }
            if (twin != null) {
                level.setBlock(twin, level.getBlockState(twin).setValue(LIT, true), Block.UPDATE_ALL);
                level.playSound(null, twin, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.7F);
            if (level instanceof ServerLevel server) {
                Direction f = state.getValue(FACING);
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + f.getStepX() * 0.6, pos.getY() + 0.5,
                    pos.getZ() + 0.5 + f.getStepZ() * 0.6, 20, 0.2, 0.2, 0.2, 0.05);
            }
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            Direction f = state.getValue(FACING);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + f.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.5,
                pos.getY() + 0.3 + random.nextDouble() * 0.4, pos.getZ() + 0.5 + f.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.5,
                f.getStepX() * 0.02, 0.01, f.getStepZ() * 0.02);
        }
    }
}
