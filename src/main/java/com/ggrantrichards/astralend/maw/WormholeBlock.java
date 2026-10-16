package com.ggrantrichards.astralend.maw;

import com.ggrantrichards.astralend.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// The surface of a woken Maw's throat: a churning starfield you can walk into (MawNetwork moves you).
// Someone who didn't wake this Maw can still attune to it, by using a Portal Lens on it.
public class WormholeBlock extends Block {
    public WormholeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.PORTAL_LENS.get())) {
            return net.minecraft.world.Itemnet.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            MawNetwork.attune(server, pos, serverPlayer);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6,
                (random.nextDouble() - 0.5) * 0.6);
        }
    }
}
