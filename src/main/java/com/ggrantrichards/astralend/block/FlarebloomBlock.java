package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.StellarFlares;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

// Flarebloom: a tall Corona flower that stays shut tight - until a Stellar Flare. Then it opens and
// glows (StellarFlares opens the ones near players), and an open bloom can be picked for Flare Pollen
// (use it, or break it). After the flare it closes again on its own.
public class FlarebloomBlock extends EndTallPlantBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public FlarebloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    // Opens or closes both halves of the bloom at `pos`.
    public static void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        if (state.getValue(OPEN) == open) {
            return;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        for (BlockPos half : new BlockPos[] {lower, lower.above()}) {
            BlockState s = level.getBlockState(half);
            if (s.getBlock() instanceof FlarebloomBlock) {
                level.setBlock(half, s.setValue(OPEN, open), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(OPEN);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (StellarFlares.phaseIn(level) != FlarePhase.FLARE) {
            setOpen(level, pos, state, false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(OPEN)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Block.popResource(server, pos, new ItemStack(ModItems.FLARE_POLLEN.get(), 1 + server.getRandom().nextInt(2)));
            server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.3F);
            setOpen(server, pos, state, false);
        }
        return InteractionResult.SUCCESS;
    }
}
