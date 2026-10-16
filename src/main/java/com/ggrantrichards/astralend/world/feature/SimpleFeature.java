package com.ggrantrichards.astralend.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// (1.21.1 port) 26.3's features are records with place(level, generator, random, origin); on 1.21.1
// a feature extends Feature<config>. This is that, for the features that take no configuration.
public abstract class SimpleFeature extends Feature<NoneFeatureConfiguration> {
    protected SimpleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public abstract boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin);

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return this.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
    }
}
