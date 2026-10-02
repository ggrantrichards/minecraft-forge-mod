package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.Blocks;

// A spire of blue ice: a shard of the comet driven into the island, tapering to a point.
public record CometIceSpireFeature() implements Feature {
    public static final MapCodec<CometIceSpireFeature> CODEC = MapCodec.unit(CometIceSpireFeature::new);

    @Override
    public MapCodec<CometIceSpireFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(ModBlocks.CHARRED_END_STONE.get())) {
            return false;
        }
        int height = 7 + random.nextInt(14);
        double baseRadius = 1.5 + random.nextDouble() * 1.5;
        // A slight lean: the shard came in at an angle.
        double leanX = (random.nextDouble() - 0.5) * 0.35;
        double leanZ = (random.nextDouble() - 0.5) * 0.35;
        BlockState ice = Blocks.BLUE_ICE.defaultBlockState();
        for (int y = -2; y < height; y++) {
            double r = baseRadius * (1 - Math.max(0, y) / (double) height);
            int cx = (int) Math.round(leanX * y);
            int cz = (int) Math.round(leanZ * y);
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz <= r * r + 0.3) {
                        BlockPos p = origin.offset(cx + dx, y, cz + dz);
                        if (y < 0) {
                            Build.force(level, p, ice);
                        } else {
                            Build.put(level, p, ice);
                        }
                    }
                }
            }
        }
        return true;
    }
}
