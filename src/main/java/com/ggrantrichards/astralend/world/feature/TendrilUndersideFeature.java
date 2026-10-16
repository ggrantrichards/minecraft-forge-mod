package com.ggrantrichards.astralend.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

// Nebula Tendrils hanging from the underside of an island, glowing at their tips.
public class TendrilUndersideFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ceiling = Underside.find(level, origin.getX(), origin.getZ(), 3);
        if (ceiling == null) {
            return false;
        }
        GiantLumineshroomFeature.hangTendril(level, random, ceiling.below(), 2 + random.nextInt(7));
        return true;
    }
}
