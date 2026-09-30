package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

// A Nullwood tree: a black, twisting trunk that wanders as it climbs, a few crooked branches, and
// clumps of dark violet leaves. Grown by world generation in the Dark Matter Belt, and by saplings.
public record NullwoodTreeFeature() implements Feature {
    public static final MapCodec<NullwoodTreeFeature> CODEC = MapCodec.unit(NullwoodTreeFeature::new);

    @Override
    public MapCodec<NullwoodTreeFeature> codec() {
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
        int height = 8 + random.nextInt(8);
        if (!Build.clear(level, origin, height + 2)) {
            return false;
        }
        BlockState logY = log(Direction.Axis.Y);
        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        // The trunk drifts sideways every few blocks, like it's being pulled.
        BlockPos.MutableBlockPos pos = origin.mutable();
        for (int y = 0; y < height; y++) {
            if (y > 2 && y % 3 == 0) {
                Direction drift = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                if (Build.put(level, pos, logY)) {
                    logs.add(pos.immutable());
                }
                pos.move(drift);
            }
            if (Build.put(level, pos, logY)) {
                logs.add(pos.immutable());
            }
            pos.move(Direction.UP);
        }
        BlockPos top = pos.below().immutable();
        blob(level, random, top.above(), 3, leaves);
        // Crooked branches, each ending in a clump of leaves.
        int branches = 2 + random.nextInt(3);
        for (int b = 0; b < branches; b++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos.MutableBlockPos branch = top.below(2 + random.nextInt(Math.max(1, height / 2 - 2))).mutable();
            int length = 2 + random.nextInt(4);
            for (int i = 0; i < length; i++) {
                branch.move(dir);
                if (i % 2 == 1) {
                    branch.move(Direction.UP);
                }
                if (Build.put(level, branch, log(dir.getAxis()))) {
                    logs.add(branch.immutable());
                }
            }
            blob(level, random, branch.immutable(), 2, leaves);
        }
        setLeafDistances(level, logs, leaves);
        return true;
    }

    private static BlockState log(Direction.Axis axis) {
        return ModBlocks.NULLWOOD.log().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static void blob(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, Set<BlockPos> leaves) {
        BlockState leaf = ModBlocks.NULLWOOD_LEAVES.get().defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= radius - 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = dx * dx + dy * dy * 1.6 + dz * dz;
                    if (d <= radius * radius + 0.5 && (d < (radius - 1) * (radius - 1) || random.nextInt(4) != 0)) {
                        BlockPos p = centre.offset(dx, dy, dz);
                        if (Build.put(level, p, leaf)) {
                            leaves.add(p);
                        }
                    }
                }
            }
        }
    }

    // Leaves decay unless they're within 6 blocks of a log. World generation doesn't run block
    // updates, so work out each leaf's distance here, the way vanilla's tree generator does.
    private static void setLeafDistances(WorldGenLevel level, Set<BlockPos> logs, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : logs) {
            distance.put(log, 0);
            queue.add(log);
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = distance.get(p);
            if (d >= 7) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (leaves.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, d + 1);
                    queue.add(n);
                }
            }
        }
        for (BlockPos leaf : leaves) {
            int d = Math.min(7, distance.getOrDefault(leaf, 7));
            level.setBlock(leaf, level.getBlockState(leaf).setValue(LeavesBlock.DISTANCE, d), 2);
        }
    }
}
