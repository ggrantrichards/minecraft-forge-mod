package com.ggrantrichards.astralend.station;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// Orrery Bronze, the Orbital Stations' metal. Like copper it ages, in three stages: bright bronze,
// tarnished, and weathered violet; an axe scrapes it back one stage. (It ages slowly, on random
// ticks, and only in the open air of the End.)
public class OrreryBronzeBlock extends Block {
    private final @Nullable Supplier<Block> next;
    private final @Nullable Supplier<Block> previous;

    public OrreryBronzeBlock(@Nullable Supplier<Block> next, @Nullable Supplier<Block> previous, BlockBehaviour.Properties properties) {
        super(properties);
        this.next = next;
        this.previous = previous;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return this.next != null;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.next != null && random.nextInt(160) == 0) {
            level.setBlockAndUpdate(pos, this.next.get().defaultBlockState());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (this.previous == null || !stack.is(ItemTags.AXES)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        level.playSound(player, pos, SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.levelEvent(player, 3005, pos, 0);   // the scrape particles copper uses
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, this.previous.get().defaultBlockState());
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (this.previous == null && random.nextInt(400) == 0) {
            level.addParticle(ParticleTypes.WAX_OFF, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}
