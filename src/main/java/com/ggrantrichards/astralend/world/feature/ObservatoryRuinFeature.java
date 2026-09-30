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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

// A fragment of the Shattered Observatory: a round quartz floor set into the island with a star
// mosaic at its centre, the stumps of the pillars that once stood around it, and one that fell.
public record ObservatoryRuinFeature() implements Feature {
    public static final MapCodec<ObservatoryRuinFeature> CODEC = MapCodec.unit(ObservatoryRuinFeature::new);

    @Override
    public MapCodec<ObservatoryRuinFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos floor = origin.below();
        if (level.getBlockState(floor).isAir()) {
            return false;
        }
        BlockState quartz = ModBlocks.OBSERVATORY_QUARTZ.get().defaultBlockState();
        BlockState inlay = ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get().defaultBlockState();
        BlockState pillar = ModBlocks.OBSERVATORY_QUARTZ_PILLAR.get().defaultBlockState();
        int radius = 3 + random.nextInt(2);

        // The floor: replaces the island's top blocks, so it looks set into the ground.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) {
                    continue;
                }
                BlockPos pos = surface(level, floor.getX() + dx, floor.getZ() + dz);
                if (pos != null && Math.abs(pos.getY() - floor.getY()) <= 2 && random.nextInt(6) != 0) {
                    this.setBlock(level, pos, dx == 0 && dz == 0 ? inlay : quartz);
                }
            }
        }

        // Broken pillars around the edge.
        int pillars = 2 + random.nextInt(3);
        for (int i = 0; i < pillars; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int px = floor.getX() + (int) Math.round(Math.cos(angle) * radius);
            int pz = floor.getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos base = surface(level, px, pz);
            if (base == null) {
                continue;
            }
            int height = 1 + random.nextInt(6);
            for (int y = 1; y <= height; y++) {
                this.setBlock(level, base.above(y), pillar);
            }
        }

        // One fallen pillar lying on the ground.
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockState fallen = pillar.setValue(RotatedPillarBlock.AXIS, direction.getAxis());
        int length = 3 + random.nextInt(3);
        int start = radius + 1;
        for (int i = 0; i < length; i++) {
            int x = floor.getX() + direction.getStepX() * (start + i);
            int z = floor.getZ() + direction.getStepZ() * (start + i);
            BlockPos ground = surface(level, x, z);
            if (ground == null) {
                break;
            }
            this.setBlock(level, ground.above(), fallen);
        }
        return true;
    }

    // The top solid block of a column, or null if the column is empty.
    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        if (y <= level.getMinY()) {
            return null;
        }
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isAir() ? null : pos;
    }
}
