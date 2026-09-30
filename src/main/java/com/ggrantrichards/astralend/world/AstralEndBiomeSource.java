package com.ggrantrichards.astralend.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

// Decides which biome goes where in the End.
//
// Inside 1,024 blocks of the centre, and everywhere outside Nebula Fields regions, this is exactly
// vanilla's TheEndBiomeSource: the central island, then Highlands, Midlands, Barrens and Small
// Islands chosen by the End's island noise. Beyond 1,024 blocks, a separate low-frequency noise
// (the "temperature" slot of our noise settings, which doesn't shape terrain) marks out regions
// of Nebula Fields a few hundred blocks across.
public class AstralEndBiomeSource extends BiomeSource {
    public static final MapCodec<AstralEndBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                RegistryOps.retrieveElement(Biomes.THE_END),
                RegistryOps.retrieveElement(Biomes.END_HIGHLANDS),
                RegistryOps.retrieveElement(Biomes.END_MIDLANDS),
                RegistryOps.retrieveElement(Biomes.SMALL_END_ISLANDS),
                RegistryOps.retrieveElement(Biomes.END_BARRENS),
                RegistryOps.retrieveElement(ModBiomes.NEBULA_FIELDS)
            )
            .apply(i, i.stable(AstralEndBiomeSource::new))
    );

    // Region noise above this value becomes Nebula Fields (checked by the worldgen survey:
    // roughly a quarter to a third of the outer End).
    public static final double NEBULA_THRESHOLD = 0.18;
    // Vanilla's inner radius, in chunks squared (64 chunks = 1,024 blocks).
    private static final long CENTRAL_RADIUS_SQR = 4096L;

    private final Holder<Biome> end;
    private final Holder<Biome> highlands;
    private final Holder<Biome> midlands;
    private final Holder<Biome> islands;
    private final Holder<Biome> barrens;
    private final Holder<Biome> nebulaFields;

    private AstralEndBiomeSource(Holder<Biome> end, Holder<Biome> highlands, Holder<Biome> midlands,
                                 Holder<Biome> islands, Holder<Biome> barrens, Holder<Biome> nebulaFields) {
        this.end = end;
        this.highlands = highlands;
        this.midlands = midlands;
        this.islands = islands;
        this.barrens = barrens;
        this.nebulaFields = nebulaFields;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.end, this.highlands, this.midlands, this.islands, this.barrens, this.nebulaFields);
    }

    @Override
    protected MapCodec<AstralEndBiomeSource> codec() {
        return CODEC;
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return (quartX, quartY, quartZ) -> this.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    private Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX);
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= CENTRAL_RADIUS_SQR) {
            return this.end;
        }
        if (regionValue(sampler, blockX, blockZ) > NEBULA_THRESHOLD) {
            return this.nebulaFields;
        }
        // Vanilla's outer-End choice, unchanged.
        int weirdBlockX = (chunkX * 2 + 1) * 8;
        int weirdBlockZ = (chunkZ * 2 + 1) * 8;
        double heightValue = sampler.erosion().sampleValue(weirdBlockX, blockY, weirdBlockZ);
        if (heightValue > 0.25) {
            return this.highlands;
        } else if (heightValue >= -0.0625) {
            return this.midlands;
        } else {
            return heightValue < -0.21875 ? this.islands : this.barrens;
        }
    }

    // The region noise at a block position; also used by the worldgen survey.
    public static double regionValue(Climate.Sampler sampler, int blockX, int blockZ) {
        return sampler.temperature().sampleValue(blockX, 0, blockZ);
    }
}
