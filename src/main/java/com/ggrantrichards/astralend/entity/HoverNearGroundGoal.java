package com.ggrantrichards.astralend.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

// Drifts to a random spot a few blocks above the ground nearby. It never picks a spot over the
// void, so flying mobs that use it stay over their islands instead of wandering off the edge.
public class HoverNearGroundGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private final int range;
    private final int minHeight;
    private final int maxHeight;

    public HoverNearGroundGoal(PathfinderMob mob, double speed, int range, int minHeight, int maxHeight) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.mob.getNavigation().isDone() && this.mob.getRandom().nextInt(reducedTickDelay(20)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        BlockPos target = pickSpot(this.mob, this.range, this.minHeight, this.maxHeight);
        if (target != null) {
            this.mob.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, this.speed);
        }
    }

    // An empty spot minHeight..maxHeight above solid ground within `range` blocks, or null.
    public static BlockPos pickSpot(PathfinderMob mob, int range, int minHeight, int maxHeight) {
        var level = mob.level();
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = mob.getBlockX() + mob.getRandom().nextInt(range * 2 + 1) - range;
            int z = mob.getBlockZ() + mob.getRandom().nextInt(range * 2 + 1) - range;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (ground <= level.getMinY() + 1 || Math.abs(ground - mob.getBlockY()) > range + maxHeight) {
                continue;  // over the void, or far above or below
            }
            BlockPos pos = new BlockPos(x, ground + minHeight + mob.getRandom().nextInt(maxHeight - minHeight + 1), z);
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                return pos;
            }
        }
        return null;
    }
}
