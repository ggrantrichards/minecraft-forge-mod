package com.ggrantrichards.astralend.station;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.entity.StationSentinel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

// The Solar Core at the heart of an Orbital Station: sealed and dead. Slot three charged Flare Cells
// (from the Corona) into it and the station wakes - its lamps come on, every bulkhead nearby opens
// for five minutes, and the Station Sentinel guarding the vault wakes too. When the cells run down
// (after those five minutes) the core goes dark again, ready for three more.
public class SolarCoreBlock extends Block {
    public static final IntegerProperty CELLS = IntegerProperty.create("cells", 0, 3);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int REACH = 40;

    public SolarCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CELLS, 0).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CELLS, POWERED);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.CHARGED_FLARE_CELL.get()) || state.getValue(POWERED)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            stack.consume(1, player);
            int cells = state.getValue(CELLS) + 1;
            server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.8F + cells * 0.1F);
            if (cells < 3) {
                server.setBlock(pos, state.setValue(CELLS, cells), Block.UPDATE_ALL);
                player.sendSystemMessage(Component.translatable("station.astralend.cell", cells));
            } else {
                power(server, pos, state.setValue(CELLS, 3));
                player.sendSystemMessage(Component.translatable("station.astralend.powered"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable(state.getValue(POWERED) ? "station.astralend.running" : "station.astralend.dead",
                state.getValue(CELLS)));
        }
        return InteractionResult.SUCCESS;
    }

    // Wakes the station around the core. Public so the survey can test it.
    public static int power(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), BulkheadBlock.OPEN_TICKS);
        int opened = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-REACH, -12, -REACH), pos.offset(REACH, 12, REACH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.BULKHEAD.get()) && !s.getValue(BulkheadBlock.OPEN)) {
                BulkheadBlock.open(level, p.immutable(), s);
                opened++;
            } else if (s.is(ModBlocks.STATION_LAMP.get()) && !s.getValue(StationLampBlock.LIT)) {
                level.setBlock(p, s.setValue(StationLampBlock.LIT, true), Block.UPDATE_CLIENTS);
            }
        }
        for (StationSentinel sentinel : level.getEntitiesOfClass(StationSentinel.class, new AABB(pos).inflate(REACH, 16, REACH))) {
            sentinel.wake();
        }
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 4.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3.0F, 0.4F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.05);
        return opened;
    }

    // The cells are spent: the core goes dark and the lamps with it.
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, false).setValue(CELLS, 0), Block.UPDATE_ALL);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-REACH, -12, -REACH), pos.offset(REACH, 12, REACH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.STATION_LAMP.get()) && s.getValue(StationLampBlock.LIT)) {
                level.setBlock(p, s.setValue(StationLampBlock.LIT, false), Block.UPDATE_CLIENTS);
            }
        }
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 3.0F, 0.6F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.05,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
        }
    }
}
