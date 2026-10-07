package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// A withered stalk of the Ashen Barrens (after beta.4): a giant Lumineshroom long dead - a pale,
// bone-like stem seven to fourteen blocks tall, leaning as it rises, with a small cap at the top
// drooping round its edges. No light, no tendrils: nothing in it is alive.
public record WitheredStalkFeature() implements Feature {
    public static final MapCodec<WitheredStalkFeature> CODEC = MapCodec.unit(WitheredStalkFeature::new);

    @Override
    public MapCodec<WitheredStalkFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ground = Build.surface(level, origin.getX(), origin.getZ());
        if (ground == null || !level.getBlockState(ground).is(ModBlocks.PALE_END_STONE.get())) {
            return false;
        }
        int height = 7 + random.nextInt(8);
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockState stem = ModBlocks.LUMINE_STEM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState cap = ModBlocks.LUMINE_CAP.get().defaultBlockState();
        BlockPos p = ground;
        for (int k = 1; k <= height; k++) {
            if (k > 3 && k % 4 == 0) {
                BlockPos side = p.relative(lean);
                if (!Build.put(level, side, stem)) {
                    return true;
                }
                p = side;
            }
            BlockPos up = p.above();
            if (!Build.put(level, up, stem)) {
                return true;
            }
            p = up;
        }
        // The cap: the block on top and a ring round it, its edge drooping a block lower.
        Build.put(level, p.above(), cap);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Build.put(level, p.relative(d), cap);
            Build.put(level, p.relative(d).relative(d.getClockWise()).below(), cap);
        }
        return true;
    }
}
