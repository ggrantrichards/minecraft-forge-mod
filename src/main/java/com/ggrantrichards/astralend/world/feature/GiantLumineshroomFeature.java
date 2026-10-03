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

// A giant Lumineshroom: a pale stem under a glowing cap of Lumine Cap, with Nebula Tendrils
// dripping from its rim. Three sizes: small ones with a round cap, medium ones with a wide dome,
// and rare towering ones with a thick 2x2 stem and a broad, flat-topped cap.
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
        int kind = random.nextInt(20);           // 0-8 small, 9-16 medium, 17-19 towering
        boolean thick = kind >= 17;
        int height = kind < 9 ? 4 + random.nextInt(4) : thick ? 13 + random.nextInt(5) : 7 + random.nextInt(5);
        int radius = kind < 9 ? 2 + random.nextInt(2) : thick ? 6 + random.nextInt(2) : 3 + random.nextInt(3);
        double domeHeight = kind < 9 ? radius * 0.9 : thick ? radius * 0.35 : radius * 0.55;
        if (!Build.clear(level, origin, height + 3)) {
            return false;
        }
        BlockState stem = ModBlocks.LUMINE_STEM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
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
                int lift = (int) Math.round(Math.sqrt(Math.max(0, 1 - (d / radius) * (d / radius))) * domeHeight);
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
