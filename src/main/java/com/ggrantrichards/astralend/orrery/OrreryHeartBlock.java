package com.ggrantrichards.astralend.orrery;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.entity.OrrerySentinel;
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

// The Orrery Heart, at the hub of an Abandoned Orrery: cold and still. Pour three Bottled Flares (from
// the Corona) into it and the Orrery wakes - its lamps come on, every Bronze Shutter nearby slides open
// for five minutes, and the Orrery Sentinel guarding the vault wakes too. When the flarelight burns out
// (after those five minutes) the heart goes cold again, ready for three more.
public class OrreryHeartBlock extends Block {
    public static final IntegerProperty FLARES = IntegerProperty.create("flares", 0, 3);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int REACH = 40;

    public OrreryHeartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FLARES, 0).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLARES, POWERED);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.BOTTLED_FLARE.get()) || state.getValue(POWERED)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            stack.consume(1, player);
            int flares = state.getValue(FLARES) + 1;
            server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.8F + flares * 0.1F);
            if (flares < 3) {
                server.setBlock(pos, state.setValue(FLARES, flares), Block.UPDATE_ALL);
                player.sendSystemMessage(Component.translatable("orrery.astralend.flare", flares));
            } else {
                power(server, pos, state.setValue(FLARES, 3));
                player.sendSystemMessage(Component.translatable("orrery.astralend.powered"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable(state.getValue(POWERED) ? "orrery.astralend.running" : "orrery.astralend.dead",
                state.getValue(FLARES)));
        }
        return InteractionResult.SUCCESS;
    }

    // Wakes the Orrery around its heart. Public so the survey can test it.
    public static int power(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), BronzeShutterBlock.OPEN_TICKS);
        int opened = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-REACH, -12, -REACH), pos.offset(REACH, 12, REACH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.BRONZE_SHUTTER.get()) && !s.getValue(BronzeShutterBlock.OPEN)) {
                BronzeShutterBlock.open(level, p.immutable(), s);
                opened++;
            } else if (s.is(ModBlocks.ORRERY_LAMP.get()) && !s.getValue(OrreryLampBlock.LIT)) {
                level.setBlock(p, s.setValue(OrreryLampBlock.LIT, true), Block.UPDATE_CLIENTS);
            }
        }
        for (OrrerySentinel sentinel : level.getEntitiesOfClass(OrrerySentinel.class, new AABB(pos).inflate(REACH, 16, REACH))) {
            sentinel.wake();
        }
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 4.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3.0F, 0.4F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.05);
        return opened;
    }

    // The flarelight is spent: the heart goes cold and the lamps with it.
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, false).setValue(FLARES, 0), Block.UPDATE_ALL);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-REACH, -12, -REACH), pos.offset(REACH, 12, REACH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.ORRERY_LAMP.get()) && s.getValue(OrreryLampBlock.LIT)) {
                level.setBlock(p, s.setValue(OrreryLampBlock.LIT, false), Block.UPDATE_CLIENTS);
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
