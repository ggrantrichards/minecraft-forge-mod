package com.ggrantrichards.astralend.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

// Small helpers shared by the features.
final class Build {
    // Places a block only into air or a replaceable plant, so features never cut into other blocks.
    static boolean put(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.isInsideBuildHeight(pos)) {
            return false;
        }
        BlockState here = level.getBlockState(pos);
        if (here.isAir() || here.canBeReplaced()) {
            level.setBlock(pos, state, 2);
            return true;
        }
        return false;
    }

    // Places a block whatever is there (for carving into terrain).
    static void force(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.isInsideBuildHeight(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    // The top solid block of a column, or null if the column is empty.
    static BlockPos surface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        if (y <= level.getMinBuildHeight()) {
            return null;
        }
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isAir() ? null : pos;
    }

    // True if the column from `from` up `height` blocks is clear.
    static boolean clear(WorldGenLevel level, BlockPos from, int height) {
        for (int i = 0; i < height; i++) {
            BlockState state = level.getBlockState(from.above(i));
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private Build() {}
}
