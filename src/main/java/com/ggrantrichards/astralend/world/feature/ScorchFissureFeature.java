package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

// A scorch fissure of the Ashen Barrens (since the golden-meadow rework): a crack wandering across the
// ground, a few blocks deep, its lips charred black and raw Solarite glowing at the bottom - where a star's
// flare split the stone long ago - and round the lips a halo of Sunscorched Stone, the pale stone baked
// amber by the heat. (Within 9 blocks.)
public class ScorchFissureFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos start = Build.surface(level, origin.getX(), origin.getZ());
        if (start == null || !scorchable(level.getBlockState(start))) {
            return false;
        }
        BlockState hot = ModBlocks.RAW_SOLARITE_BLOCK.get().defaultBlockState();   // raw sun-metal, still hot
        BlockState charred = ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        BlockState baked = ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState();
        double angle = random.nextDouble() * Math.PI * 2;
        int length = 9 + random.nextInt(5);
        for (int i = -length / 2; i <= length / 2; i++) {
            angle += (random.nextDouble() - 0.5) * 0.6;
            int bx = (int) Math.round(Math.cos(angle) * i), bz = (int) Math.round(Math.sin(angle) * i);
            if (Math.abs(bx) > 6 || Math.abs(bz) > 6) {
                continue;
            }
            BlockPos top = Build.surface(level, start.getX() + bx, start.getZ() + bz);
            if (top == null || Math.abs(top.getY() - start.getY()) > 3) {
                continue;
            }
            double taper = 1.0 - Math.abs(i) / (length / 2.0 + 1.0);
            int depth = 1 + (int) Math.round(taper * 3.5);
            int width = taper > 0.5 ? 1 : 0;
            for (int w = -width; w <= width; w++) {
                BlockPos col = top.offset((int) Math.round(-Math.sin(angle) * w), 0, (int) Math.round(Math.cos(angle) * w));
                for (int k = 0; k < depth; k++) {
                    Build.force(level, col.below(k), Blocks.AIR.defaultBlockState());
                }
                Build.force(level, col.below(depth), hot);
                // The lips, charred.
                for (int s = -1; s <= 1; s += 2) {
                    BlockPos lip = col.offset((int) Math.round(-Math.sin(angle) * s * (width + 1)), 0, (int) Math.round(Math.cos(angle) * s * (width + 1)));
                    if (scorchable(level.getBlockState(lip)) && random.nextInt(3) != 0) {
                        Build.force(level, lip, charred);
                    }
                    // Beyond the lip, the baked stone: a ragged band one or two blocks wide.
                    for (int r = 1; r <= 2; r++) {
                        BlockPos halo = col.offset((int) Math.round(-Math.sin(angle) * s * (width + 1 + r)), 0,
                            (int) Math.round(Math.cos(angle) * s * (width + 1 + r)));
                        BlockState here = level.getBlockState(halo);
                        if (here.is(ModBlocks.PALE_END_STONE.get()) && random.nextInt(r == 1 ? 4 : 2) != 0) {
                            Build.force(level, halo, baked);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean scorchable(BlockState state) {
        return state.is(ModBlocks.PALE_END_STONE.get()) || state.is(ModBlocks.SUNSCORCHED_STONE.get());
    }
}
