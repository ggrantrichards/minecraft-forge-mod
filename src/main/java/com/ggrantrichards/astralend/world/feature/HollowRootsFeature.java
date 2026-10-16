package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

// The Hollow Roots: under the Deep Hollows, through the thick of their ring islands, the hollows
// where the dead Nullwood's roots rotted out. A chamber where a great root met the island's heart,
// and tunnels running from it - out, and down, and some out through the island's underside, so you
// can find a way in from below. Inside it's never quite dark: Hollow Glowcaps on the floors, the
// roots that are left hanging from the roofs, Starsap - the old trees' amber - glowing in the walls,
// and fireflies drifting in the dark (the biome's ambient particles).
//
// It stays within 23 blocks of the chunk's middle (a feature may only write to the chunks next to its own).
public class HollowRootsFeature extends SimpleFeature {
    private static final int REACH = 21;


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = (origin.getX() & ~15) + 8, cz = (origin.getZ() & ~15) + 8;
        int[] span = span(level, cx, cz);
        if (span == null || span[1] - span[0] < 14) {
            return false;
        }
        double hubY = span[0] + (span[1] - span[0]) * 0.4;
        List<BlockPos> carved = new ArrayList<>();
        double hubR = 4.0 + random.nextDouble() * 2.0;
        ball(level, cx, hubY, cz, hubR, hubR * 0.65, carved, cx, cz);
        int roots = 3 + random.nextInt(3);
        for (int r = 0; r < roots; r++) {
            double yaw = (r + random.nextDouble() * 0.7) / roots * Math.PI * 2.0;
            double pitch = -0.25 - random.nextDouble() * 0.35;   // they run down, as roots do
            double x = cx, y = hubY, z = cz;
            int length = 16 + random.nextInt(12);
            for (int i = 0; i < length; i++) {
                double radius = Mth.lerp((double) i / length, 2.4, 1.3);
                x += Math.cos(yaw) * Math.cos(pitch);
                z += Math.sin(yaw) * Math.cos(pitch);
                y += Math.sin(pitch);
                yaw += (random.nextDouble() - 0.5) * 0.4;
                pitch = Mth.clamp(pitch + (random.nextDouble() - 0.5) * 0.2, -0.9, 0.1);
                if (Math.abs(x - cx) > REACH || Math.abs(z - cz) > REACH) {
                    break;
                }
                int[] here = span(level, Mth.floor(x), Mth.floor(z));
                if (here == null || y + radius > here[1] - 3) {
                    break;                                      // never through the top
                }
                ball(level, x, y, z, radius, radius * 0.9, carved, cx, cz);
                if (y < here[0]) {
                    break;                                      // out through the underside: a way in
                }
            }
        }
        decorate(level, random, carved);
        return !carved.isEmpty();
    }

    // The island's solid span in this column (the longest run), or null.
    private static int[] span(WorldGenLevel level, int x, int z) {
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
        return state.is(ModBlocks.NIGHTSTONE.get()) || state.is(Blocks.END_STONE);
    }

    private static void ball(WorldGenLevel level, double x, double y, double z, double rh, double rv, List<BlockPos> carved, int cx, int cz) {
        int r = Mth.ceil(Math.max(rh, rv));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if ((dx * dx + dz * dz) / (rh * rh) + dy * dy / (rv * rv) > 1.0) {
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

    private static void decorate(WorldGenLevel level, RandomSource random, List<BlockPos> carved) {
        BlockState glowcap = ModBlocks.HOLLOW_GLOWCAP.get().defaultBlockState();
        BlockState ore = ModBlocks.STARSAP_ORE.get().defaultBlockState();
        BlockState root = ModBlocks.NULLWOOD.log().get().defaultBlockState();
        for (BlockPos air : carved) {
            if (!level.getBlockState(air).isAir()) {
                continue;
            }
            BlockState below = level.getBlockState(air.below());
            BlockState above = level.getBlockState(air.above());
            float roll = random.nextFloat();
            if (below.is(ModBlocks.NIGHTSTONE.get()) && roll < 0.12F) {
                level.setBlock(air, glowcap, 2);
            } else if (carvable(above) && roll < 0.025F) {
                // A root left hanging from the roof.
                int length = 2 + random.nextInt(4);
                for (int k = 0; k < length && level.getBlockState(air.below(k)).isAir(); k++) {
                    level.setBlock(air.below(k), root, 2);
                }
            }
            for (Direction d : Direction.values()) {
                BlockPos wall = air.relative(d);
                if (level.getBlockState(wall).is(ModBlocks.NIGHTSTONE.get()) && random.nextFloat() < 0.008F) {
                    level.setBlock(wall, ore, 2);
                }
            }
        }
    }
}
