package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// A chorus tree: a vanilla chorus plant grown to the size of a tree, in Chorus Wood. A stalk climbs,
// forks into a few arms that step out and climb again, and every arm ends in a crown of Chorus Bloom.
// Grown by world generation in the Chorus Thicket, and by Chorus Sprouts.
public record ChorusTreeFeature() implements Feature {
    public static final MapCodec<ChorusTreeFeature> CODEC = MapCodec.unit(ChorusTreeFeature::new);

    @Override
    public MapCodec<ChorusTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        return grow(level, random, origin);
    }

    public static boolean grow(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(ModBlocks.ASTRAL_SOIL)) {
            return false;
        }
        int trunk = 4 + random.nextInt(4);
        if (!Build.clear(level, origin, trunk + 4)) {
            return false;
        }
        Set<BlockPos> logs = new HashSet<>();
        BlockPos top = column(level, origin, trunk, logs);
        arm(level, random, top, 0, logs);
        return true;
    }

    // An arm: from `from`, maybe fork into 2-3 arms that step sideways then climb, or end in a crown.
    private static void arm(WorldGenLevel level, RandomSource random, BlockPos from, int depth, Set<BlockPos> logs) {
        if (depth >= 2 || (depth == 1 && random.nextInt(3) == 0)) {
            crown(level, random, from.above(), logs);
            return;
        }
        int forks = 2 + random.nextInt(2);
        Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int f = 0; f < forks; f++) {
            Direction dir = f == 0 ? first : f == 1 ? first.getOpposite() : first.getClockWise();
            BlockPos.MutableBlockPos p = from.mutable();
            int reach = 1 + random.nextInt(3);
            BlockState side = log(dir.getAxis());
            for (int i = 0; i < reach; i++) {
                p.move(dir);
                if (Build.put(level, p, side)) {
                    logs.add(p.immutable());
                }
            }
            BlockPos end = column(level, p.immutable().above(), 2 + random.nextInt(4), logs);
            arm(level, random, end, depth + 1, logs);
        }
        if (random.nextBoolean()) {
            BlockPos end = column(level, from.above(), 2 + random.nextInt(3), logs);
            crown(level, random, end.above(), logs);
        }
    }

    // A straight stalk `height` blocks up from `from`; returns its top block.
    private static BlockPos column(WorldGenLevel level, BlockPos from, int height, Set<BlockPos> logs) {
        BlockState up = log(Direction.Axis.Y);
        BlockPos.MutableBlockPos p = from.mutable();
        BlockPos top = from.below();
        for (int i = 0; i < height; i++) {
            if (!Build.put(level, p, up)) {
                break;
            }
            logs.add(p.immutable());
            top = p.immutable();
            p.move(Direction.UP);
        }
        return top;
    }

    // A round crown of Chorus Bloom (distance 1-2 from the stalk, so it never decays).
    private static void crown(WorldGenLevel level, RandomSource random, BlockPos centre, Set<BlockPos> logs) {
        BlockState bloom = ModBlocks.CHORUS_BLOOM.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int d = dx * dx + dy * dy * 2 + dz * dz;
                    if (d > 5 || (d >= 4 && random.nextInt(3) == 0)) {
                        continue;
                    }
                    BlockPos p = centre.offset(dx, dy, dz);
                    int distance = Math.max(1, Math.abs(dx) + Math.abs(dz) + Math.abs(dy));
                    Build.put(level, p, bloom.setValue(LeavesBlock.DISTANCE, Math.min(6, distance)));
                }
            }
        }
        if (Build.put(level, centre.below(), log(Direction.Axis.Y))) {
            logs.add(centre.below());
        }
        Build.force(level, centre, log(Direction.Axis.Y));
    }

    private static BlockState log(Direction.Axis axis) {
        return ModBlocks.CHORUS.log().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }
}
