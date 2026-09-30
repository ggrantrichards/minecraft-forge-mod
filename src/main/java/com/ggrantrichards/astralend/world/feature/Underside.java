package com.ggrantrichards.astralend.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

// Finds island undersides: the lowest solid block of an island, with open air beneath it.
final class Underside {
    // Scans one column from the top down; returns the first solid block that has at least
    // `clearBelow` air blocks under it, or null.
    static BlockPos find(WorldGenLevel level, int x, int z, int clearBelow) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
        int top = Math.min(level.getMaxY() - 1, 120);
        int bottom = level.getMinY() + clearBelow + 1;
        boolean wasSolid = false;
        for (int y = top; y >= bottom; y--) {
            boolean solid = !level.getBlockState(pos.setY(y)).isAir();
            if (wasSolid && !solid && clearBelow(level, x, y, z, clearBelow)) {
                return new BlockPos(x, y + 1, z);
            }
            wasSolid = solid;
        }
        return null;
    }

    private static boolean clearBelow(WorldGenLevel level, int x, int y, int z, int n) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        for (int i = 0; i < n; i++) {
            if (!level.getBlockState(pos.setY(y - i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    private Underside() {}
}
