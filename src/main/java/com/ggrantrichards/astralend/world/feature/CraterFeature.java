package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// An impact crater: a round bowl blasted into a Comet Wake island with a raised rim, Stardust
// drifted in its floor and a lump of Meteoric Iron Ore at the bottom where the fragment struck.
public record CraterFeature() implements Feature {
    public static final MapCodec<CraterFeature> CODEC = MapCodec.unit(CraterFeature::new);

    @Override
    public MapCodec<CraterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos centre = origin.below();
        if (!level.getBlockState(centre).is(ModBlocks.CHARRED_END_STONE.get()) && !level.getBlockState(centre).is(ModBlocks.PALE_END_STONE.get())) {
            return false;
        }
        int radius = 5 + random.nextInt(5);
        BlockState charred = ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        BlockState dust = ModBlocks.STARDUST_BLOCK.get().defaultBlockState();
        BlockState ore = ModBlocks.METEORIC_IRON_ORE.get().defaultBlockState();
        int deepest = 0;
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                BlockPos top = Build.surface(level, centre.getX() + dx, centre.getZ() + dz);
                if (top == null || Math.abs(top.getY() - centre.getY()) > 4) {
                    continue;
                }
                if (d <= radius) {
                    int depth = (int) Math.round((radius * radius - d * d) / (radius * 1.8));
                    deepest = Math.max(deepest, depth);
                    for (int k = 0; k < depth; k++) {
                        Build.force(level, top.below(k), Blocks.AIR.defaultBlockState());
                    }
                    BlockPos floor = top.below(depth);
                    if (!level.getBlockState(floor).isAir()) {
                        Build.force(level, floor, random.nextInt(4) == 0 ? dust : charred);
                    }
                } else if (d <= radius + 2 && random.nextInt(3) != 0) {
                    Build.put(level, top.above(), charred);  // the rim of thrown-up rock
                }
            }
        }
        BlockPos bottom = centre.below(deepest);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 1 || random.nextBoolean()) {
                    Build.force(level, bottom.offset(dx, 0, dz), ore);
                }
            }
        }
        return true;
    }
}
