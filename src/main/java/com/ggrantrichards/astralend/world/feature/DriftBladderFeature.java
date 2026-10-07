package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// A Drift Bladder over the Nebula Fields: a sac of glowing gas floating six to fourteen blocks above
// the moss - one block, a lump of a few, or a rounder cluster - tethered to the ground below by a
// Nebula Tendril hanging from its underside almost to the moss. In the low gravity even the plants
// float.
public record DriftBladderFeature() implements Feature {
    public static final MapCodec<DriftBladderFeature> CODEC = MapCodec.unit(DriftBladderFeature::new);

    @Override
    public MapCodec<DriftBladderFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ground = Build.surface(level, origin.getX(), origin.getZ());
        if (ground == null || !level.getBlockState(ground).is(ModBlocks.CHORUS_MOSS.get())) {
            return false;
        }
        int height = 6 + random.nextInt(9);
        BlockPos bottom = ground.above(height);
        // The sac: its lowest block is `bottom`; bigger ones grow up and out from it.
        List<BlockPos> sac = new ArrayList<>();
        int kind = random.nextInt(20);
        sac.add(bottom);
        if (kind >= 10) {                    // a lump: the block above and two or three beside
            sac.add(bottom.above());
            for (BlockPos side : List.of(bottom.north(), bottom.east(), bottom.south(), bottom.west())) {
                if (random.nextInt(3) != 0) {
                    sac.add(side.above());
                }
            }
        }
        if (kind >= 16) {                    // a round one: a ring above and a crown
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        sac.add(bottom.offset(dx, 1, dz));
                    }
                }
            }
            sac.add(bottom.above(2));
        }
        // Clear air all round, and from the tendril's end to the sac.
        for (BlockPos p : sac) {
            if (!level.getBlockState(p).isAir()) {
                return false;
            }
        }
        for (int y = ground.getY() + 2; y < bottom.getY(); y++) {
            if (!level.getBlockState(new BlockPos(bottom.getX(), y, bottom.getZ())).isAir()) {
                return false;
            }
        }
        BlockState bladder = ModBlocks.NEBULA_BLADDER.get().defaultBlockState();
        for (BlockPos p : sac) {
            Build.put(level, p, bladder);
        }
        // The tether: a tendril hanging from the sac to a block or two above the moss, glowing at its tip.
        int length = height - 1 - random.nextInt(2);
        for (int k = 1; k <= length; k++) {
            BlockState part = k == length ? ModBlocks.NEBULA_TENDRIL.get().defaultBlockState()
                : ModBlocks.NEBULA_TENDRIL_PLANT.get().defaultBlockState();
            Build.put(level, bottom.below(k), part);
        }
        return true;
    }
}
