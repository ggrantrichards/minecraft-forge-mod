package com.ggrantrichards.astralend.orrery;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// Orrery Bronze, the Orreries' metal, ages like copper: bright bronze, tarnished, weathered violet. It
// ages slowly on random ticks; an axe scrapes it back one stage; honeycomb waxes it so it stays as it
// is (and an axe scrapes the wax off again). The whole family does this: blocks, cut bronze and its
// stairs and slabs, and grates.
public final class Bronze {
    // What a piece of bronze turns into: aging (next), scraping (previous), waxing (waxed) and, for a
    // waxed piece, scraping the wax off (unwaxed). Any of them may be missing.
    public record Aging(@Nullable Supplier<Block> next, @Nullable Supplier<Block> previous, @Nullable Supplier<Block> waxed,
                        @Nullable Supplier<Block> unwaxed) {
        boolean ages() {
            return this.next != null && this.unwaxed == null;
        }
    }

    static void randomTick(Aging aging, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (aging.ages() && random.nextInt(160) == 0) {
            level.setBlockAndUpdate(pos, aging.next().get().withPropertiesOf(state));
        }
    }

    static InteractionResult use(Aging aging, ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand) {
        if (stack.is(Items.HONEYCOMB) && aging.waxed() != null) {
            level.playSound(player, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3003, pos, 0);
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, aging.waxed().get().withPropertiesOf(state));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ItemTags.AXES)) {
            Supplier<Block> to = aging.unwaxed() != null ? aging.unwaxed() : aging.previous();
            if (to == null) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            level.playSound(player, pos, aging.unwaxed() != null ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE.value(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, aging.unwaxed() != null ? 3004 : 3005, pos, 0);
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, to.get().withPropertiesOf(state));
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public static class Full extends Block {
        private final Aging aging;

        public Full(Aging aging, BlockBehaviour.Properties properties) {
            super(properties);
            this.aging = aging;
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return this.aging.ages();
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            Bronze.randomTick(this.aging, state, level, pos, random);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
            return Bronze.use(this.aging, stack, state, level, pos, player, hand);
        }
    }

    public static class Stairs extends StairBlock {
        private final Aging aging;

        public Stairs(Aging aging, BlockState base, BlockBehaviour.Properties properties) {
            super(base, properties);
            this.aging = aging;
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return this.aging.ages();
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            Bronze.randomTick(this.aging, state, level, pos, random);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
            return Bronze.use(this.aging, stack, state, level, pos, player, hand);
        }
    }

    public static class Slab extends SlabBlock {
        private final Aging aging;

        public Slab(Aging aging, BlockBehaviour.Properties properties) {
            super(properties);
            this.aging = aging;
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return this.aging.ages();
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            Bronze.randomTick(this.aging, state, level, pos, random);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
            return Bronze.use(this.aging, stack, state, level, pos, player, hand);
        }
    }

    public static class Grate extends WaterloggedTransparentBlock {
        private final Aging aging;

        public Grate(Aging aging, BlockBehaviour.Properties properties) {
            super(properties);
            this.aging = aging;
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return this.aging.ages();
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            Bronze.randomTick(this.aging, state, level, pos, random);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
            return Bronze.use(this.aging, stack, state, level, pos, player, hand);
        }
    }

    private Bronze() {}
}
