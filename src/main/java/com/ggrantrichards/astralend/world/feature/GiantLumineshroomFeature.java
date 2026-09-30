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

// A giant Lumineshroom: a pale stem 6-15 blocks tall under a wide, glowing dome of Lumine Cap,
// with Nebula Tendrils dripping from its rim. The big ones have thick 2x2 stems.
public record GiantLumineshroomFeature() implements Feature {
    public static final MapCodec<GiantLumineshroomFeature> CODEC = MapCodec.unit(GiantLumineshroomFeature::new);

    @Override
    public MapCodec<GiantLumineshroomFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        return grow(level, random, origin);
    }

    public static boolean grow(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).is(ModBlocks.ASTRAL_SOIL)) {
            return false;
        }
        int height = 6 + random.nextInt(10);
        boolean thick = height >= 12;
        int radius = 3 + height / 5 + (thick ? 1 : 0);
        if (!Build.clear(level, origin, height + 3)) {
            return false;
        }
        BlockState stem = ModBlocks.LUMINE.log().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        int size = thick ? 2 : 1;
        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    Build.put(level, origin.offset(dx, y, dz), stem);
                }
            }
        }
        // The cap: a shallow dome, one block thick, whose rim droops.
        BlockState cap = ModBlocks.LUMINE_CAP.get().defaultBlockState();
        BlockPos top = origin.above(height);
        double cx = (size - 1) / 2.0;
        for (int dx = -radius; dx <= radius + size - 1; dx++) {
            for (int dz = -radius; dz <= radius + size - 1; dz++) {
                double d = Math.sqrt((dx - cx) * (dx - cx) + (dz - cx) * (dz - cx));
                if (d > radius + 0.5) {
                    continue;
                }
                int lift = (int) Math.round((1 - d / radius) * (radius / 2.0));
                BlockPos pos = top.offset(dx, lift, dz);
                Build.put(level, pos, cap);
                if (d > radius - 1.2) {
                    Build.put(level, pos.below(), cap);  // the drooping rim
                    if (random.nextInt(3) == 0) {
                        hangTendril(level, random, pos.below(2), 1 + random.nextInt(4));
                    }
                }
            }
        }
        return true;
    }

    static void hangTendril(WorldGenLevel level, RandomSource random, BlockPos start, int length) {
        for (int i = 0; i < length; i++) {
            BlockPos pos = start.below(i);
            if (!level.getBlockState(pos).isAir()) {
                return;
            }
            boolean tip = i == length - 1 || !level.getBlockState(pos.below()).isAir();
            level.setBlock(pos, (tip ? ModBlocks.NEBULA_TENDRIL : ModBlocks.NEBULA_TENDRIL_PLANT).get().defaultBlockState(), 2);
            if (tip) {
                return;
            }
        }
    }
}
