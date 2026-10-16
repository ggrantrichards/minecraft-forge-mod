package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

// A patch of Memory Moss in the Ashen Barrens' pale ground: a ragged blob 2-4 blocks across.
public class MemoryMossFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int r = 2 + random.nextInt(3);
        boolean any = false;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + random.nextInt(3) - 1) {
                    continue;
                }
                BlockPos top = Build.surface(level, origin.getX() + dx, origin.getZ() + dz);
                if (top != null && level.getBlockState(top).is(ModBlocks.PALE_END_STONE.get()) && level.getBlockState(top.above()).isAir()) {
                    Build.force(level, top, ModBlocks.MEMORY_MOSS.get().defaultBlockState());
                    any = true;
                }
            }
        }
        return any;
    }
}
