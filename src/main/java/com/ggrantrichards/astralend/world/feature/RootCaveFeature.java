package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// The End's first caves: inside the Glimmering Crags' bigger islands, the hollows left where the roots
// of the colossal, long-dead Nullwood rotted away. A root hollow is a chamber deep in the island (where
// the trunk stood) with three to five tunnels running out and down from it, wandering like roots and
// tapering into channels too narrow to follow. They never break the surface or the underside: they're
// sealed, pitch dark and silent. Smoky Glimmer Clusters grow sparsely on their walls, and Void Crystal -
// the rarest ore - grows here and nowhere else, in the walls and around the chamber's heart.
//
// It all stays within 24 blocks of the chunk's middle (a feature may only write to the chunks next to
// its own); neighbouring hollows run into each other, so the tunnels join up into networks.
public record RootCaveFeature() implements Feature {
    public static final MapCodec<RootCaveFeature> CODEC = MapCodec.unit(RootCaveFeature::new);
    private static final int REACH = 22;

    @Override
    public MapCodec<RootCaveFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = (origin.getX() & ~15) + 8;
        int cz = (origin.getZ() & ~15) + 8;
        int[] span = solidSpan(level, cx, cz);
        if (span == null || span[1] - span[0] < 22) {
            return false;   // too thin an island for a hollow
        }
        double hubY = span[0] + (span[1] - span[0]) * 0.45;
        Set<BlockPos> carved = new HashSet<>();
        double hubR = 3.5 + random.nextDouble() * 1.5;
        carveBall(level, cx, hubY, cz, hubR, hubR * 0.7, carved, cx, cz);
        int roots = 3 + random.nextInt(3);
        for (int r = 0; r < roots; r++) {
            double yaw = (r + random.nextDouble() * 0.6) / roots * Math.PI * 2.0;
            double pitch = -0.15 - random.nextDouble() * 0.25;
            double x = cx, y = hubY, z = cz;
            int length = 18 + random.nextInt(14);
            for (int i = 0; i < length; i++) {
                double radius = Mth.lerp((double) i / length, 2.6, 0.8);
                x += Math.cos(yaw) * Math.cos(pitch);
                z += Math.sin(yaw) * Math.cos(pitch);
                y += Math.sin(pitch);
                yaw += (random.nextDouble() - 0.5) * 0.35;
                pitch = Mth.clamp(pitch + (random.nextDouble() - 0.5) * 0.15, -0.6, 0.15);
                if (Math.abs(x - cx) > REACH || Math.abs(z - cz) > REACH) {
                    break;
                }
                int[] here = solidSpan(level, Mth.floor(x), Mth.floor(z));
                if (here == null || y + radius > here[1] - 4 || y - radius < here[0] + 3) {
                    break;   // the root would break out of the island: it ends here, sealed
                }
                carveBall(level, x, y, z, radius, radius, carved, cx, cz);
            }
        }
        decorate(level, random, carved, cx, hubY, cz);
        return !carved.isEmpty();
    }

    // The lowest and highest solid block of the island in this column (the longest solid run).
    private static int[] solidSpan(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int best = 0, bestTop = 0, runTop = Integer.MIN_VALUE;
        for (int y = level.getMinBuildHeight() + 127; y >= level.getMinBuildHeight(); y--) {
            boolean solid = !level.getBlockState(p.set(x, y, z)).isAir();
            if (solid && runTop == Integer.MIN_VALUE) {
                runTop = y;
            }
            if ((!solid || y == level.getMinBuildHeight()) && runTop != Integer.MIN_VALUE) {
                int bottom = solid ? y : y + 1;
                if (runTop - bottom > best) {
                    best = runTop - bottom;
                    bestTop = runTop;
                }
                runTop = Integer.MIN_VALUE;
            }
        }
        return best == 0 ? null : new int[] {bestTop - best, bestTop};
    }

    private static boolean carvable(BlockState state) {
        return state.is(ModBlocks.NIGHTSTONE.get()) || state.is(Blocks.END_STONE) || state.is(ModBlocks.GLIMMER_CRYSTAL.get());
    }

    private static void carveBall(WorldGenLevel level, double x, double y, double z, double rh, double rv, Set<BlockPos> carved,
                                  int cx, int cz) {
        int r = Mth.ceil(Math.max(rh, rv));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = (dx * dx + dz * dz) / (rh * rh) + dy * dy / (rv * rv);
                    if (d > 1.0) {
                        continue;
                    }
                    p.set(Mth.floor(x) + dx, Mth.floor(y) + dy, Mth.floor(z) + dz);
                    if (Math.abs(p.getX() - cx) > 23 || Math.abs(p.getZ() - cz) > 23) {
                        continue;
                    }
                    if (carvable(level.getBlockState(p))) {
                        level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), 2);
                        carved.add(p.immutable());
                    }
                }
            }
        }
    }

    // Sparse crystal on the walls; Void Crystal ore here and there, and always a little round the heart.
    private static void decorate(WorldGenLevel level, RandomSource random, Set<BlockPos> carved, int cx, double hubY, int cz) {
        BlockState ore = ModBlocks.VOID_CRYSTAL_ORE.get().defaultBlockState();
        for (BlockPos air : carved) {
            for (Direction d : Direction.values()) {
                BlockPos wall = air.relative(d);
                BlockState state = level.getBlockState(wall);
                if (!state.is(ModBlocks.NIGHTSTONE.get()) && !state.is(Blocks.END_STONE)) {
                    continue;
                }
                float roll = random.nextFloat();
                boolean nearHeart = air.distSqr(new BlockPos(cx, Mth.floor(hubY), cz)) < 30;
                if (roll < (nearHeart ? 0.05F : 0.006F)) {
                    level.setBlock(wall, ore, 2);
                } else if (roll < 0.022F && level.getBlockState(air).isAir()) {
                    level.setBlock(air, ModBlocks.GLIMMER_CLUSTER.get().defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, d.getOpposite()), 2);
                    break;
                }
            }
        }
    }
}
