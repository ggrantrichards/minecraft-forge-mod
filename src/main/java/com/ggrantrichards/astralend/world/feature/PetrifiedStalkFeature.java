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
// the flares killed it. It leans a little, the tallest flare out at the base, and a few crooked
// dead branches still reach upward.
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
        int height = 7 + random.nextInt(12);
        if (!Build.clear(level, origin, height + 3)) {
            return false;
        }
        boolean thick = height >= 15;
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockState up = ModBlocks.PETRIFIED_STALK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockPos.MutableBlockPos pos = origin.mutable();
        for (int y = 0; y < height; y++) {
            Build.put(level, pos, up);
            if (thick && y < height / 2) {  // old, tall stalks flare out at the base
                Build.put(level, pos.east(), up);
                Build.put(level, pos.south(), up);
                Build.put(level, pos.south().east(), up);
            }
            if (y > 4 && random.nextInt(7) == 0) {
                pos.move(lean);
                Build.put(level, pos, up);
            }
            pos.move(Direction.UP);
        }
        // A few crooked, dead branches reaching upward and out, each ending in a stony frond.
        BlockPos top = pos.below().immutable();
        int branches = 2 + random.nextInt(2);
        for (int b = 0; b < branches; b++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos.MutableBlockPos branch = top.below(random.nextInt(Math.max(1, height / 3))).mutable();
            int length = 2 + random.nextInt(3);
            for (int i = 0; i < length; i++) {
                boolean rise = random.nextInt(3) != 0;
                if (rise) {
                    branch.move(Direction.UP);
                } else {
                    branch.move(dir);
                }
                Build.put(level, branch, ModBlocks.PETRIFIED_STALK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, rise ? Direction.Axis.Y : dir.getAxis()));
                if (random.nextBoolean()) {
                    branch.move(dir);
                    Build.put(level, branch, ModBlocks.PETRIFIED_STALK.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, dir.getAxis()));
                }
            }
            Build.put(level, branch.above(), ModBlocks.PETRIFIED_ASTRAL_VINE.get().defaultBlockState());
        }
        Build.put(level, top.above(), ModBlocks.PETRIFIED_ASTRAL_VINE.get().defaultBlockState());
        return true;
    }
}
