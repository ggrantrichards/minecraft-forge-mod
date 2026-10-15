package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.Sunstorms;
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

    // A Sunstorm coming or breaking over this spot (the warning counts: time to find cover).
    public static boolean stormy(net.minecraft.world.level.Level level, BlockPos pos) {
        FlarePhase phase = Sunstorms.phaseIn(level);
        return (phase == FlarePhase.WARNING || phase == FlarePhase.FLARE) && Sunstorms.inSunstorms(level, pos);
    }

    // The nearest spot to stand within `range` blocks that has cover overhead (a Lumineshroom's cap,
    // a ledge, a roof), searched ring by ring outward: null if there's none.
    public static @Nullable BlockPos coverNear(ServerLevel level, BlockPos from, int range, RandomSource random) {
        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= range; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = 4; dy >= -6; dy--) {
                        feet.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
                        // (grass and flowers don't count: a mob walks through them)
                        if (level.getBlockState(feet.below()).isSolidRender() && passable(level, feet) && passable(level, feet.above())) {
                            if (!Sunstorms.underOpenSky(level, feet)) {
                                return feet.immutable();
                            }
                            // open (the top of a roof, say): look further down the column, under it
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean passable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    // The nearest water source with air over it within `range` blocks (a Hydragrip's pond), or null.
    public static @Nullable BlockPos waterNear(ServerLevel level, BlockPos from, int range) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-range, -6, -range), from.offset(range, 3, range))) {
            if (level.getFluidState(p).isSource() && level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(p.above()).isAir()) {
                double d = p.distSqr(from);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private EndMobs() {}
}
