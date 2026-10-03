package com.ggrantrichards.astralend.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

// Small helpers the distillation's mobs share.
public final class EndMobs {
    // A spot to stand on (solid ground, two blocks of air) between `min` and `max` blocks from `from`,
    // within 6 blocks up or down; null if 24 tries find none (over the void there's often nothing).
    public static @Nullable BlockPos safeSpotNear(ServerLevel level, BlockPos from, int min, int max, RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = min + random.nextDouble() * (max - min);
            int x = from.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = from.getZ() + (int) Math.round(Math.sin(angle) * distance);
            for (int dy = 6; dy >= -6; dy--) {
                BlockPos feet = new BlockPos(x, from.getY() + dy, z);
                if (level.getBlockState(feet.below()).isSolidRender() && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) {
                    return feet;
                }
            }
        }
        return null;
    }

    // Is there open void below this spot (nothing solid for 64 blocks)? Used to find island edges.
    public static boolean voidBelow(ServerLevel level, BlockPos pos) {
        for (int dy = 1; dy <= 64; dy++) {
            BlockPos p = pos.below(dy);
            if (p.getY() < level.getMinY()) {
                return true;
            }
            if (!level.getBlockState(p).isAir()) {
                return false;
            }
        }
        return true;
    }

    private EndMobs() {}
}
