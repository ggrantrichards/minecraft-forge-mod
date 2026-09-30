package com.ggrantrichards.astralend.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// Nebula Tendrils hanging from the underside of an island, glowing at their tips.
public record TendrilUndersideFeature() implements Feature {
    public static final MapCodec<TendrilUndersideFeature> CODEC = MapCodec.unit(TendrilUndersideFeature::new);

    @Override
    public MapCodec<TendrilUndersideFeature> codec() {
        return CODEC;
    }

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
