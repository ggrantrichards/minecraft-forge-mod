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

// A petrified stalk: the stone-hard trunk of a giant Astral Vine that grew in the Corona before
// the flares killed it. It leans slightly and ends in a crown of dead, stony shoots.
public record PetrifiedStalkFeature() implements Feature {
    public static final MapCodec<PetrifiedStalkFeature> CODEC = MapCodec.unit(PetrifiedStalkFeature::new);

    @Override
    public MapCodec<PetrifiedStalkFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(ModBlocks.ASTRAL_SOIL)) {
            return false;
        }
        int height = 8 + random.nextInt(13);
        if (!Build.clear(level, origin, height + 2)) {
            return false;
        }
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockState up = ModBlocks.PETRIFIED_STALK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockPos.MutableBlockPos pos = origin.mutable();
        for (int y = 0; y < height; y++) {
            Build.put(level, pos, up);
            if (y > 3 && random.nextInt(6) == 0) {
                pos.move(lean);
                Build.put(level, pos, up);
            }
            pos.move(Direction.UP);
        }
        // The crown: stubby stone shoots in every direction, with dead fronds on top.
        BlockPos top = pos.below().immutable();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (random.nextInt(4) == 0) {
                continue;
            }
            int length = 1 + random.nextInt(3);
            BlockPos shoot = top;
            for (int i = 0; i < length; i++) {
                shoot = shoot.relative(dir);
                Build.put(level, shoot, ModBlocks.PETRIFIED_STALK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, dir.getAxis()));
            }
            Build.put(level, shoot.above(), ModBlocks.PETRIFIED_ASTRAL_VINE.get().defaultBlockState());
        }
        Build.put(level, top.above(), ModBlocks.PETRIFIED_ASTRAL_VINE.get().defaultBlockState());
        return true;
    }
}
