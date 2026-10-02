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
// Inside 1,024 blocks of the centre, and everywhere outside the astral regions, this is exactly
// vanilla's TheEndBiomeSource: the central island, then Highlands, Midlands, Barrens and Small
// Islands chosen by the End's island noise. Beyond 1,024 blocks, slow region noises (see
// AstralRegions) mark out the astral biomes, each several hundred blocks across.
public class AstralEndBiomeSource extends BiomeSource {
    public static final MapCodec<AstralEndBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                RegistryOps.retrieveElement(Biomes.THE_END),
                RegistryOps.retrieveElement(Biomes.END_HIGHLANDS),
                RegistryOps.retrieveElement(Biomes.END_MIDLANDS),
                RegistryOps.retrieveElement(Biomes.SMALL_END_ISLANDS),
                RegistryOps.retrieveElement(Biomes.END_BARRENS),
                RegistryOps.retrieveElement(ModBiomes.NEBULA_FIELDS),
                RegistryOps.retrieveElement(ModBiomes.ASHEN_BARRENS),
                RegistryOps.retrieveElement(ModBiomes.COMET_WAKE),
                RegistryOps.retrieveElement(ModBiomes.HOLLOW_ISLES),
                RegistryOps.retrieveElement(ModBiomes.CORONA)
            )
            .apply(i, i.stable(AstralEndBiomeSource::new))
    );

    // Vanilla's inner radius, in chunks squared (64 chunks = 1,024 blocks).
    private static final long CENTRAL_RADIUS_SQR = 4096L;

    private final Holder<Biome> end;
    private final Holder<Biome> highlands;
    private final Holder<Biome> midlands;
    private final Holder<Biome> islands;
    private final Holder<Biome> barrens;
    private final Holder<Biome> nebulaFields;
    private final Holder<Biome> ashenBarrens;
    private final Holder<Biome> cometWake;
    private final Holder<Biome> hollowIsles;
    private final Holder<Biome> corona;

    private AstralEndBiomeSource(Holder<Biome> end, Holder<Biome> highlands, Holder<Biome> midlands,
                                 Holder<Biome> islands, Holder<Biome> barrens, Holder<Biome> nebulaFields,
                                 Holder<Biome> ashenBarrens, Holder<Biome> cometWake, Holder<Biome> hollowIsles,
                                 Holder<Biome> corona) {
        this.end = end;
        this.highlands = highlands;
        this.midlands = midlands;
        this.islands = islands;
        this.barrens = barrens;
        this.nebulaFields = nebulaFields;
        this.ashenBarrens = ashenBarrens;
        this.cometWake = cometWake;
        this.hollowIsles = hollowIsles;
        this.corona = corona;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.end, this.highlands, this.midlands, this.islands, this.barrens, this.nebulaFields,
            this.ashenBarrens, this.cometWake, this.hollowIsles, this.corona);
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
        switch (region(sampler, blockX, blockZ)) {
            case NEBULA_FIELDS: return this.nebulaFields;
            case ASHEN_BARRENS: return this.ashenBarrens;
            case COMET_WAKE: return this.cometWake;
            case HOLLOW_ISLES: return this.hollowIsles;
            case CORONA: return this.corona;
            case VANILLA: break;
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

    // The astral region at a block position (ignoring the central 1,024 blocks); also used by the survey.
    public static AstralRegions.Region region(Climate.Sampler sampler, int blockX, int blockZ) {
        return AstralRegions.pick(sampler.temperature().sampleValue(blockX, 0, blockZ),
            sampler.humidity().sampleValue(blockX, 0, blockZ),
            sampler.weirdness().sampleValue(blockX, 0, blockZ));
    }
}
