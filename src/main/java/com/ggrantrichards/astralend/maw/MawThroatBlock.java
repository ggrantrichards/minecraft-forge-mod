package com.ggrantrichards.astralend.maw;

import com.ggrantrichards.astralend.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// The back of a dormant Maw's throat (a Leviathan's skull): dark stone. With all four eye sockets lit, use a
// Portal Lens on it and the Maw wakes (MawNetwork.wake).
public class MawThroatBlock extends Block {
    public MawThroatBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.PORTAL_LENS.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            MawNetwork.wake(server, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
