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

// The great ring of the Shattered Observatory: a circle of tall quartz pillars joined by lintels,
// around a star mosaic. Most of it has fallen; a few arches still stand. (A feature may only touch
// the chunks right next to its own, so everything stays within 14 blocks of the centre.)
public record ObservatoryRingFeature() implements Feature {
    public static final MapCodec<ObservatoryRingFeature> CODEC = MapCodec.unit(ObservatoryRingFeature::new);

    @Override
    public MapCodec<ObservatoryRingFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos centre = origin.below();
        if (level.getBlockState(centre).isAir()) {
            return false;
        }
        int radius = 8 + random.nextInt(3);
        int pillars = 12;
        int tall = 7 + random.nextInt(4);
        BlockState quartz = ModBlocks.OBSERVATORY_QUARTZ.get().defaultBlockState();
        BlockState inlay = ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get().defaultBlockState();
        BlockState pillar = ModBlocks.OBSERVATORY_QUARTZ_PILLAR.get().defaultBlockState();
        int[] heights = new int[pillars];
        BlockPos[] bases = new BlockPos[pillars];
        for (int i = 0; i < pillars; i++) {
            double a = 2 * Math.PI * i / pillars;
            int x = centre.getX() + (int) Math.round(Math.cos(a) * radius);
            int z = centre.getZ() + (int) Math.round(Math.sin(a) * radius);
            BlockPos base = Build.surface(level, x, z);
            if (base == null || Math.abs(base.getY() - centre.getY()) > 5) {
                continue;
            }
            bases[i] = base;
            // Standing, broken, or reduced to a stump.
            int roll = random.nextInt(10);
            heights[i] = roll < 4 ? tall : roll < 8 ? 2 + random.nextInt(tall - 2) : 1;
            for (int y = 1; y <= heights[i]; y++) {
                Build.put(level, base.above(y), pillar);
            }
            if (heights[i] < tall && random.nextBoolean()) {
                // The fallen top lies beside it.
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                for (int k = 2; k < Math.min(4, 2 + (tall - heights[i]) / 2 + 1); k++) {
                    BlockPos ground = Build.surface(level, base.getX() + dir.getStepX() * k, base.getZ() + dir.getStepZ() * k);
                    if (ground != null) {
                        Build.put(level, ground.above(), pillar.setValue(RotatedPillarBlock.AXIS, dir.getAxis()));
                    }
                }
            }
        }
        // Lintels between neighbouring pillars that both still stand at full height.
        for (int i = 0; i < pillars; i++) {
            int j = (i + 1) % pillars;
            if (bases[i] == null || bases[j] == null || heights[i] < tall || heights[j] < tall) {
                continue;
            }
            BlockPos a = bases[i].above(tall + 1);
            BlockPos b = bases[j].above(tall + 1);
            int steps = Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
            for (int s = 0; s <= steps; s++) {
                double t = steps == 0 ? 0 : (double) s / steps;
                Build.put(level, new BlockPos((int) Math.round(a.getX() + (b.getX() - a.getX()) * t),
                    Math.max(a.getY(), b.getY()), (int) Math.round(a.getZ() + (b.getZ() - a.getZ()) * t)), quartz);
            }
        }
        // The mosaic floor in the middle of the ring.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 10) {
                    continue;
                }
                BlockPos top = Build.surface(level, centre.getX() + dx, centre.getZ() + dz);
                if (top != null && Math.abs(top.getY() - centre.getY()) <= 2) {
                    Build.force(level, top, (dx == 0 && dz == 0) || (Math.abs(dx) == 2 && Math.abs(dz) == 2) ? inlay : quartz);
                }
            }
        }
        return true;
    }
}
