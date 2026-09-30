package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.GravityAnomalyBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// A Gravity Inversion Pocket: a Gravity Anomaly set into the underside of a Dark Matter Belt
// island, with a sphere of flipped gravity hanging below it. The island is always above the
// pocket, so anything that falls "up" lands on its underside instead of flying into the void.
public record InversionPocketFeature() implements Feature {
    public static final MapCodec<InversionPocketFeature> CODEC = MapCodec.unit(InversionPocketFeature::new);

    @Override
    public MapCodec<InversionPocketFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int clear = (int) Math.ceil(GravityAnomalyBlockEntity.CENTRE_BELOW + GravityAnomalyBlockEntity.RADIUS);
        BlockPos core = Underside.find(level, origin.getX(), origin.getZ(), clear);
        // The core must be embedded: solid island above it, so the pocket has a "floor" to fall onto.
        if (core == null || level.getBlockState(core.above()).isAir()) {
            return false;
        }
        this.setBlock(level, core, ModBlocks.GRAVITY_ANOMALY.get().defaultBlockState());
        return true;
    }
}
