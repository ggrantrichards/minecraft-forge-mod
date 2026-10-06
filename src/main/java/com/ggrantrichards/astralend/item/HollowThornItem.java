package com.ggrantrichards.astralend.item;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.HollowLanternBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// The Hollow Thorn: a long black thorn from the Hollow Vines of the Deep Hollows, sharp as a knife.
// Use it on a pumpkin (or a carved one) and it carves a Hollow Lantern, its face toward you; use it on
// a Hollow Lantern and it carves the next face.
public class HollowThornItem extends Item {
    public HollowThornItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        boolean pumpkin = state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN);
        boolean lantern = state.is(ModBlocks.HOLLOW_LANTERN.get());
        if (!pumpkin && !lantern) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockState carved;
            if (lantern) {
                carved = state.setValue(HollowLanternBlock.FACE, (state.getValue(HollowLanternBlock.FACE) + 1) % 4);
            } else {
                Direction face = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace()
                    : player != null ? player.getDirection().getOpposite() : Direction.NORTH;
                carved = ModBlocks.HOLLOW_LANTERN.get().defaultBlockState().setValue(HollowLanternBlock.FACING, face);
                if (state.is(Blocks.PUMPKIN)) {
                    // The flesh comes out as seeds, as it does with shears.
                    ItemEntity seeds = new ItemEntity(server, pos.getX() + 0.5 + face.getStepX() * 0.65, pos.getY() + 0.1,
                        pos.getZ() + 0.5 + face.getStepZ() * 0.65, new ItemStack(Items.PUMPKIN_SEEDS, 4));
                    server.addFreshEntity(seeds);
                }
            }
            server.setBlock(pos, carved, Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 0.7F);
            server.sendParticles(new DustParticleOptions(0x9A6AE8, 1.0F), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                12, 0.4, 0.3, 0.4, 0.0);
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
