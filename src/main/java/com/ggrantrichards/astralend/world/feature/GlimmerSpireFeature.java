package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// A crag of the Glimmering Crags: a jagged spike of Nightstone, leaning a little, its edges grown
// through with teal Glimmer Crystal, tipped with crystal, and clusters around its foot.
// (Within 7 blocks of its origin, so it stays inside the chunks a feature may touch.)
public record GlimmerSpireFeature() implements Feature {
    public static final MapCodec<GlimmerSpireFeature> CODEC = MapCodec.unit(GlimmerSpireFeature::new);

    @Override
    public MapCodec<GlimmerSpireFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos base = origin.below();
        if (!level.getBlockState(base).is(ModBlocks.NIGHTSTONE.get())) {
            return false;
        }
        BlockState stone = ModBlocks.NIGHTSTONE.get().defaultBlockState();
        BlockState crystal = ModBlocks.GLIMMER_CRYSTAL.get().defaultBlockState();
        int height = 8 + random.nextInt(16);
        double radius = 1.6 + random.nextDouble() * 1.6;
        double leanX = (random.nextDouble() - 0.5) * 0.35, leanZ = (random.nextDouble() - 0.5) * 0.35;
        for (int y = -2; y <= height; y++) {
            double t = Math.max(0, y) / (double) height;
            double r = radius * (1.0 - t) + 0.3;
            double cx = leanX * y, cz = leanZ * y;
            int reach = (int) Math.ceil(r);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d = Math.hypot(dx - cx, dz - cz);
                    if (d > r || Math.abs(dx) > 6 || Math.abs(dz) > 6) {
                        continue;
                    }
                    // The outside of the spike and its top third are crystal; the core is stone.
                    boolean shell = d > r - 1.0 && random.nextInt(3) != 0;
                    Build.force(level, base.offset(dx, y, dz), t > 0.66 || shell ? crystal : stone);
                }
            }
        }
        // Clusters round its foot.
        BlockState cluster = ModBlocks.GLIMMER_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
        for (int i = 0; i < 8; i++) {
            int x = base.getX() + random.nextInt(11) - 5, z = base.getZ() + random.nextInt(11) - 5;
            BlockPos ground = Build.surface(level, x, z);
            if (ground != null && Math.abs(ground.getY() - base.getY()) < 4 && level.getBlockState(ground).is(ModBlocks.NIGHTSTONE.get())) {
                Build.put(level, ground.above(), cluster);
            }
        }
        return true;
    }
}
