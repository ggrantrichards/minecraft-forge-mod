package com.ggrantrichards.astralend.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

// The fossil ribcage of a Star Leviathan lying on an Ashen Barrens shelf: a spine half-sunk in
// the ground, with rib arches rising over it, tallest in the middle. Some ribs have broken away.
public class LeviathanRibcageFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (level.getBlockState(origin.below()).isAir()) {
            return false;
        }
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction across = along.getClockWise();
        int length = 12 + random.nextInt(11);
        int halfWidth = 4 + random.nextInt(3);
        int maxHeight = 5 + random.nextInt(4);
        BlockState spine = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
        BlockState rib = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState ribAcross = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, across.getAxis());
        BlockPos start = origin.relative(along, -length / 2);
        // Only on ground that carries the whole spine: no part of it hangs over an edge or a dip.
        BlockPos[] grounds = new BlockPos[length];
        for (int i = 0; i < length; i++) {
            BlockPos s = start.relative(along, i);
            grounds[i] = Build.surface(level, s.getX(), s.getZ());
            if (grounds[i] == null || Math.abs(grounds[i].getY() - origin.getY() + 1) > 1) {
                return false;
            }
        }
        for (int i = 0; i < length; i++) {
            BlockPos ground = grounds[i];
            Build.force(level, ground, spine);  // half-sunk into the shelf
            if (i % 3 != 1 || i < 2 || i > length - 3 || random.nextInt(6) == 0) {
                continue;
            }
            // A rib: an arch across the spine, tallest near the middle of the body.
            double body = Math.sin(Math.PI * i / length);
            int w = Math.max(2, (int) Math.round(halfWidth * (0.6 + 0.4 * body)));
            int h = Math.max(2, (int) Math.round(maxHeight * (0.5 + 0.5 * body)));
            // A quarter of the ribs have one end snapped off somewhere along it.
            int brokenSide = random.nextInt(4) == 0 ? (random.nextBoolean() ? 1 : -1) : 0;
            int brokenFrom = 1 + random.nextInt(w);
            // Both feet must stand on the ground the spine lies on, or the rib isn't raised at all.
            boolean footed = true;
            for (int side : new int[] {-w, w}) {
                if (brokenSide != 0 && side * brokenSide >= brokenFrom) {
                    continue;
                }
                BlockPos foot = Build.surface(level, ground.relative(across, side).getX(), ground.relative(across, side).getZ());
                footed &= foot != null && Math.abs(foot.getY() - ground.getY()) <= 1;
            }
            if (!footed) {
                continue;
            }
            for (int side = -w; side <= w; side++) {
                if (brokenSide != 0 && side * brokenSide >= brokenFrom) {
                    continue;
                }
                int y = archHeight(side, w, h);
                // fill down to the next step of the arch, so the rib has no gaps
                int outer = Math.abs(side) + 1 <= w ? archHeight(Math.abs(side) + 1, w, h) : 0;
                // (the feet start down at the ground itself: k from -1 at the ends)
                for (int k = Math.abs(side) == w ? -1 : Math.min(y, outer); k <= y; k++) {
                    Build.put(level, ground.relative(across, side).above(k + 1), Math.abs(side) < w / 2 ? ribAcross : rib);
                }
            }
        }
        return true;
    }

    private static int archHeight(int side, int halfWidth, int height) {
        double t = (double) side / halfWidth;
        return (int) Math.round(height * Math.sqrt(Math.max(0, 1 - t * t)));
    }
}
