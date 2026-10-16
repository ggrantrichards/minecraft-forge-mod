package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.maw.MawEyeBlock;
import com.ggrantrichards.astralend.world.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

// The pedestal at the end of a Leviathan's reliquary, made for the dragon egg. Set the egg on it and
// the skull wakes to it: the Maw Eyes in its sockets light, and the Watchers come to look. Built into
// the structure; it can't be broken.
public class LeviathanPedestalBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final int REACH = 28;

    public LeviathanPedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT) || !stack.is(Items.DRAGON_EGG) || !level.getBlockState(pos.above()).isAir()) {
            return net.minecraft.world.Itemnet.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            stack.consume(1, player);
            server.setBlock(pos.above(), Blocks.DRAGON_EGG.defaultBlockState(), Block.UPDATE_ALL);
            enshrine(server, pos);
            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.award(serverPlayer, "keeper_of_the_egg");
            }
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    // The egg is home: light the pedestal and the four eyes of the skull (the Maw: wake it now
    // with a Portal Lens), and call two Watchers to bear witness.
    public static void enshrine(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(LIT, true), Block.UPDATE_ALL);
        // The skull's four eyes: asked of the Leviathan the pedestal is in (they're far up its body).
        var start = level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(com.ggrantrichards.astralend.maze.MazeScoping.LEVIATHAN));
        java.util.List<BlockPos> eyes = new java.util.ArrayList<>();
        if (start.isValid()) {
            for (var piece : start.getPieces()) {
                if (piece instanceof com.ggrantrichards.astralend.world.structure.LeviathanHeadPiece head) {
                    eyes.addAll(head.eyes());
                }
            }
        } else {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-REACH, -8, -REACH), pos.offset(REACH, 20, REACH))) {
                eyes.add(p.immutable());
            }
        }
        for (BlockPos p : eyes) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MAW_EYE.get()) && !s.getValue(MawEyeBlock.LIT)) {
                level.setBlock(p, s.setValue(MawEyeBlock.LIT, true), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.05);
            }
        }
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 120, 0.5, 1.0, 0.5, 0.6);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            BlockPos at = pos.offset((int) Math.round(Math.cos(a) * 8), 1, (int) Math.round(Math.sin(a) * 8));
            var watcher = ModEntities.WATCHER.get().create(level);
            if (watcher != null) {
                watcher.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, (float) Math.toDegrees(a) + 90.0F, 0.0F);
                watcher.linger(600);
                level.addFreshEntity(watcher);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble() * 1.5,
                pos.getZ() + random.nextDouble(), 0.0, -0.3, 0.0);
        }
    }
}
