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
                RegistryOps.retrieveElement(ModBiomes.GLIMMERING_CRAGS),
                RegistryOps.retrieveElement(ModBiomes.DEEP_HOLLOWS),
                RegistryOps.retrieveElement(ModBiomes.SHATTERED_CRAGS),
                RegistryOps.retrieveElement(ModBiomes.ECLIPSE),
                RegistryOps.retrieveElement(ModBiomes.HOLLOW_ROOTS),
                RegistryOps.retrieveElement(ModBiomes.CRYSTAL_VAULTS)
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
    private final Holder<Biome> glimmeringCrags;
    private final Holder<Biome> deepHollows;
    private final Holder<Biome> shatteredCrags;
    private final Holder<Biome> eclipse;
    private final Holder<Biome> hollowRoots;
    private final Holder<Biome> crystalVaults;
    // The caves: under the Deep Hollows below this height, the Hollow Roots; under the Shattered Crags
    // below this one, the Crystal Vaults.
    public static final int ROOTS_BELOW = 46, VAULTS_BELOW = 38;

    private AstralEndBiomeSource(Holder<Biome> end, Holder<Biome> highlands, Holder<Biome> midlands,
                                 Holder<Biome> islands, Holder<Biome> barrens, Holder<Biome> nebulaFields,
                                 Holder<Biome> ashenBarrens, Holder<Biome> glimmeringCrags,
                                 Holder<Biome> deepHollows, Holder<Biome> shatteredCrags, Holder<Biome> eclipse,
                                 Holder<Biome> hollowRoots, Holder<Biome> crystalVaults) {
        this.end = end;
        this.highlands = highlands;
        this.midlands = midlands;
        this.islands = islands;
        this.barrens = barrens;
        this.nebulaFields = nebulaFields;
        this.ashenBarrens = ashenBarrens;
        this.glimmeringCrags = glimmeringCrags;
        this.deepHollows = deepHollows;
        this.shatteredCrags = shatteredCrags;
        this.eclipse = eclipse;
        this.hollowRoots = hollowRoots;
        this.crystalVaults = crystalVaults;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.end, this.highlands, this.midlands, this.islands, this.barrens, this.nebulaFields,
            this.ashenBarrens,
            this.glimmeringCrags, this.deepHollows, this.shatteredCrags, this.eclipse, this.hollowRoots, this.crystalVaults);
    }

    @Override
    protected MapCodec<AstralEndBiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX);
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= CENTRAL_RADIUS_SQR) {
            return this.end;
        }
        // Borders interlock rather than run as hard lines: the regions are read a little to one side
        // or the other of where we are (by up to ~24 blocks, varying smoothly), so near a border the
        // two biomes - astral or vanilla - reach into each other in lobes and patches over 30-50 blocks.
        // (And their grounds mix on top of that: BorderBlendFeature.)
        int warpedX = blockX + (int) Math.round(warp(blockX, blockZ, 0x51ED));
        int warpedZ = blockZ + (int) Math.round(warp(blockX, blockZ, 0x7A3B));
        // The rare Eclipse comes first.
        EclipseLayout.Eclipse e = EclipseLayout.at(warpedX, warpedZ, sampler);
        if (e != null && e.part(warpedX, warpedZ) == EclipseLayout.Part.HEAVEN) {
            return this.eclipse;
        }
        switch (region(sampler, warpedX, warpedZ)) {
            case NEBULA_FIELDS: return this.nebulaFields;
            case ASHEN_BARRENS: return this.ashenBarrens;
            case GLIMMERING_CRAGS: return this.glimmeringCrags;
            case DEEP_HOLLOWS: return blockY < ROOTS_BELOW ? this.hollowRoots : this.deepHollows;
            case SHATTERED_CRAGS: return blockY < VAULTS_BELOW ? this.crystalVaults : this.shatteredCrags;
            case VANILLA: break;
        }
        // Vanilla's outer-End choice, unchanged.
        int weirdBlockX = (chunkX * 2 + 1) * 8;
        int weirdBlockZ = (chunkZ * 2 + 1) * 8;
        double heightValue = com.ggrantrichards.astralend.compat.Compat.sample(sampler.erosion(), weirdBlockX, blockY, weirdBlockZ);
        if (heightValue > 0.25) {
            return this.highlands;
        } else if (heightValue >= -0.0625) {
            return this.midlands;
        } else {
            return heightValue < -0.21875 ? this.islands : this.barrens;
        }
    }

    // A smooth offset in blocks: two octaves of value noise (about 64 and 20 blocks across).
    public static double warp(int x, int z, int salt) {
        return valueNoise(x / 64.0, z / 64.0, salt) * 17.0 + valueNoise(x / 20.0, z / 20.0, salt * 31) * 7.0;
    }

    private static double valueNoise(double x, double z, int salt) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = lattice(x0, z0, salt), b = lattice(x0 + 1, z0, salt), c = lattice(x0, z0 + 1, salt), d = lattice(x0 + 1, z0 + 1, salt);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fz;
    }

    // A fixed value in [-1, 1] for a lattice point.
    private static double lattice(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    // The astral region at a block position (ignoring the central 1,024 blocks); also used by the survey.
    public static AstralRegions.Region region(Climate.Sampler sampler, int blockX, int blockZ) {
        return AstralRegions.pick(com.ggrantrichards.astralend.compat.Compat.sample(sampler.temperature(), blockX, 0, blockZ),
            com.ggrantrichards.astralend.compat.Compat.sample(sampler.humidity(), blockX, 0, blockZ),
            com.ggrantrichards.astralend.compat.Compat.sample(sampler.weirdness(), blockX, 0, blockZ),
            com.ggrantrichards.astralend.compat.Compat.sample(sampler.continentalness(), blockX, 0, blockZ));
    }
}
