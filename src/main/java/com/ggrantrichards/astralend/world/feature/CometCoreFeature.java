package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Blocks;

// A Comet Core: a boulder of the comet that broke apart over the Comet Wake, half-sunk into an
// island. Its shell is Charred End Stone and blue ice; its heart is Meteoric Iron Ore.
public class CometCoreFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(ModBlocks.CHARRED_END_STONE.get()) && !level.getBlockState(origin.below()).is(ModBlocks.PALE_END_STONE.get())) {
            return false;
        }
        int radius = 2 + random.nextInt(2);          // 2 or 3
        BlockPos centre = origin.below(radius / 2 + 1);
        BlockState shell = ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        BlockState ice = Blocks.BLUE_ICE.defaultBlockState();
        BlockState ore = ModBlocks.METEORIC_IRON_ORE.get().defaultBlockState();
        double r2 = (radius + 0.5) * (radius + 0.5);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            double d2 = pos.distSqr(centre);
            if (d2 > r2) {
                continue;
            }
            BlockState state;
            if (d2 <= (radius - 2) + 0.01) {
                state = ore;                          // the heart: 1 block, or 7 in a big core
            } else if (d2 <= 2.01 && random.nextInt(3) == 0) {
                state = ore;                          // a few more nuggets of ore around it
            } else {
                state = random.nextInt(5) < 2 ? ice : shell;
            }
            this.setBlock(level, pos, state);
        }
        return true;
    }
}
