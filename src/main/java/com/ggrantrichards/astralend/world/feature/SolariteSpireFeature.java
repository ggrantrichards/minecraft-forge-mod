package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// A spire of the Corona: a leaning fang of Sunscorched Stone the flares have glazed - Solarite
// showing through its sides, its tip fused to raw Solarite that still glows - with a scatter of
// fallen shards round its foot. (Within 7 blocks of its origin.)
public record SolariteSpireFeature() implements Feature {
    public static final MapCodec<SolariteSpireFeature> CODEC = MapCodec.unit(SolariteSpireFeature::new);

    @Override
    public MapCodec<SolariteSpireFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos base = origin.below();
        if (!level.getBlockState(base).is(ModBlocks.SUNSCORCHED_STONE.get())) {
            return false;
        }
        BlockState stone = ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState();
        BlockState ore = ModBlocks.SOLARITE_ORE.get().defaultBlockState();
        BlockState raw = ModBlocks.RAW_SOLARITE_BLOCK.get().defaultBlockState();
        BlockState charred = ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        int height = 10 + random.nextInt(18);
        double radius = 1.8 + random.nextDouble() * 1.8;
        double leanX = (random.nextDouble() - 0.5) * 0.4, leanZ = (random.nextDouble() - 0.5) * 0.4;
        for (int y = -3; y <= height; y++) {
            double t = Math.max(0, y) / (double) height;
            double r = radius * Math.pow(1.0 - t, 0.8) + 0.35;
            double cx = leanX * y, cz = leanZ * y;
            int reach = (int) Math.ceil(r);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d = Math.hypot(dx - cx, dz - cz);
                    if (d > r || Math.abs(dx) > 6 || Math.abs(dz) > 6) {
                        continue;
                    }
                    BlockState s;
                    if (t > 0.8) {
                        s = raw;
                    } else if (d > r - 1.0 && random.nextInt(5) == 0) {
                        s = ore;
                    } else if (d > r - 1.0 && y < height / 3 && random.nextInt(4) == 0) {
                        s = charred;
                    } else {
                        s = stone;
                    }
                    Build.force(level, base.offset(dx, y, dz), s);
                }
            }
        }
        // Shards round its foot.
        for (int i = 0; i < 6; i++) {
            int x = base.getX() + random.nextInt(11) - 5, z = base.getZ() + random.nextInt(11) - 5;
            BlockPos ground = Build.surface(level, x, z);
            if (ground != null && Math.abs(ground.getY() - base.getY()) < 4 && level.getBlockState(ground).is(ModBlocks.SUNSCORCHED_STONE.get())) {
                Build.put(level, ground.above(), random.nextInt(3) == 0 ? ore : stone);
            }
        }
        return true;
    }
}
