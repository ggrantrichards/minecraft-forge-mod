package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// The fossil ribcage of a Star Leviathan lying on an Ashen Barrens shelf: a spine half-sunk in
// the ground, with rib arches rising over it, tallest in the middle. Some ribs have broken away.
public record LeviathanRibcageFeature() implements Feature {
    public static final MapCodec<LeviathanRibcageFeature> CODEC = MapCodec.unit(LeviathanRibcageFeature::new);

    @Override
    public MapCodec<LeviathanRibcageFeature> codec() {
        return CODEC;
    }

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
        BlockState spine = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
        BlockState rib = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState ribAcross = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, across.getAxis());
        BlockPos start = origin.relative(along, -length / 2);
        for (int i = 0; i < length; i++) {
            BlockPos s = start.relative(along, i);
            BlockPos ground = Build.surface(level, s.getX(), s.getZ());
            if (ground == null || Math.abs(ground.getY() - origin.getY()) > 3) {
                continue;
            }
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
            for (int side = -w; side <= w; side++) {
                if (brokenSide != 0 && side * brokenSide >= brokenFrom) {
                    continue;
                }
                int y = archHeight(side, w, h);
                // fill down to the next step of the arch, so the rib has no gaps
                int outer = Math.abs(side) + 1 <= w ? archHeight(Math.abs(side) + 1, w, h) : 0;
                for (int k = Math.min(y, outer); k <= y; k++) {
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
