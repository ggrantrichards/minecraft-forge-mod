package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.AstralEndBiomeSource;
import com.ggrantrichards.astralend.world.EclipseLayout;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// The Eclipse's island, built column by column in each chunk it covers, before anything else is
// placed (it's added to every End biome, at the first generation step, by a Forge biome modifier:
// data/astralend/forge/biome_modifier/eclipse_terrain.json). Whatever terrain was there is cleared,
// and in its place: one great disc of land RADIUS blocks across, gently rolling, thick in the middle
// and thin at the rim, in a ring of empty void. (The Wellspring, a basin dug into it, is
// EclipseLandmarksFeature's.) Its ground follows the yin-yang - bare End Stone in the
// heaven half (moss, Living Turf, only round the Wellspring's water, as vanilla's End has no ground
// cover), a crimson crust of Sunscorched Stone over Charred End Stone in the hell half -
// read with the same warp as the biome source, so the ground and the biomes agree.
// Where the halves meet, the Seam (after beta.4): the scar the Remnant tore when it fell - a shallow
// gouge of Obsidian, fused black glass, fraying into either half's ground at its edges, with a broken
// line of embers (Solarite ore, still warm) down its middle.
public record EclipseTerrainFeature() implements Feature {
    public static final MapCodec<EclipseTerrainFeature> CODEC = MapCodec.unit(EclipseTerrainFeature::new);
    public static final double TOP = 62.0;          // the Eclipse's ground level
    private static final double VOID_RING = 36.0;   // cleared round the disc

    @Override
    public MapCodec<EclipseTerrainFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        var sampler = EclipseLayout.sampler(level.getLevel().getChunkSource().randomState());
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        EclipseLayout.Eclipse e = EclipseLayout.at(x0 + 8, z0 + 8, sampler);
        if (e == null || e.distance(x0 + 8, z0 + 8) > EclipseLayout.RADIUS + VOID_RING + 12) {
            return false;
        }
        double r = EclipseLayout.RADIUS;
        double wx = e.worldX(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double wz = e.worldZ(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double well = EclipseLayout.WELLSPRING_RADIUS * r;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int minY = level.getMinY(), maxY = minY + 128;
        boolean changed = false;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                double d = e.distance(x + 0.5, z + 0.5);
                if (d > r + VOID_RING) {
                    continue;
                }
                changed = true;
                boolean land = d <= r;          // (the Wellspring is a basin dug into it: EclipseLandmarksFeature)
                int top = minY - 1, bottom = minY;
                if (land) {
                    double t = d / r;
                    double h = hills(x, z);
                    top = Mth.floor(TOP + 2.5 * h - 6.0 * t * t * t * t);
                    bottom = top - Mth.floor(10.0 + 34.0 * Math.sqrt(Math.max(0.0, 1.0 - t * t)) + 6.0 * h);
                }
                int wxp = x + (int) Math.round(AstralEndBiomeSource.warp(x, z, 0x51ED));
                int wzp = z + (int) Math.round(AstralEndBiomeSource.warp(x, z, 0x7A3B));
                boolean hell = e.part(wxp, wzp) == EclipseLayout.Part.HELL;
                // The Seam: 0 at its middle line, 1 at its edges, more outside.
                double seam = e.seam(wxp, wzp) / EclipseLayout.SEAM_HALF;
                boolean scar = land && seam < 1.0;
                if (scar) {
                    top -= (int) Math.round(2.0 * (1.0 - seam) * (1.0 - seam));   // gouged out, deepest down the middle
                }
                double grain = (lattice(x, z, 0x5EA) + 1.0) * 0.5;
                boolean glass = scar && (seam < 0.45 || grain > (seam - 0.45) / 0.55);
                boolean ember = scar && seam < 0.14 && valueNoise(x / 5.0, z / 5.0, 0xE3B) > -0.15;
                // Moss only round the water: a ragged ring a few blocks wide about the Wellspring.
                boolean oasis = Math.hypot(x + 0.5 - wx, z + 0.5 - wz) < well + 6.0 + 2.5 * hills(x * 3, z * 3);
                for (int y = minY; y < maxY; y++) {
                    BlockState want;
                    if (!land || y > top || y < bottom) {
                        want = air;
                    } else if (ember && y == top) {
                        want = ModBlocks.SOLARITE_ORE.get().defaultBlockState();   // (ore, not raw blocks: the scar is long)
                    } else if (glass && y > top - (seam < 0.45 ? 3 : 1)) {
                        want = Blocks.OBSIDIAN.defaultBlockState();
                    } else if (y == top) {
                        want = hell ? ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState()
                            : oasis ? ModBlocks.LIVING_TURF.get().defaultBlockState() : Blocks.END_STONE.defaultBlockState();
                    } else if (hell) {
                        want = y > top - 3 ? ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState()
                            : ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
                    } else {
                        want = Blocks.END_STONE.defaultBlockState();
                    }
                    p.set(x, y, z);
                    if (level.getBlockState(p) != want) {
                        level.setBlock(p, want, 2);
                    }
                }
            }
        }
        return changed;
    }

    // Gentle rolls across the island: two octaves of smooth value noise, about -1..1.
    static double hills(int x, int z) {
        return valueNoise(x / 48.0, z / 48.0, 0x3C1) * 0.7 + valueNoise(x / 16.0, z / 16.0, 0x9A7) * 0.3;
    }

    private static double valueNoise(double x, double z, int salt) {
        int x0 = Mth.floor(x), z0 = Mth.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = lattice(x0, z0, salt), b = lattice(x0 + 1, z0, salt), c = lattice(x0, z0 + 1, salt), d = lattice(x0 + 1, z0 + 1, salt);
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    private static double lattice(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }
}
