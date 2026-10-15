package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
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
// and thin at the rim, in a ring of empty void. (The Wellspring and the ponds, basins dug into it, are
// EclipseLandmarksFeature's.) Its ground (after beta.4, when it became one golden meadow): gold Living
// Turf over End Stone everywhere, but for a few bare outcrops of End Stone where Sunscale Lichen grows,
// and green moss on the wet ground round the water.
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
                // Green moss on the wet ground: a ragged ring a few blocks wide round the Wellspring and each pond.
                double ragged = 2.0 * hills(x * 3, z * 3);
                boolean wet = Math.hypot(x + 0.5 - wx, z + 0.5 - wz) < well + 4.0 + ragged;
                for (double[] pond : EclipseLayout.PONDS) {
                    wet |= Math.hypot(x + 0.5 - e.worldX(pond[0], pond[1]), z + 0.5 - e.worldZ(pond[0], pond[1])) < pond[2] + 2.5 + ragged * 0.6;
                }
                // Bare outcrops of End Stone here and there, a tenth of the meadow or so: low rocky knolls, a block
                // or three proud of the turf, their tops broken.
                double rock = valueNoise(x / 9.0, z / 9.0, 0x0C7);
                boolean outcrop = !wet && rock > 0.55;
                if (land && outcrop) {
                    top += Mth.clamp((int) ((rock - 0.55) * 14.0 + 1.5 * lattice(x, z, 0x0C8)), 0, 3);
                }
                BlockState ground = wet ? Blocks.MOSS_BLOCK.defaultBlockState()
                    : outcrop ? Blocks.END_STONE.defaultBlockState() : ModBlocks.LIVING_TURF.get().defaultBlockState();
                for (int y = minY; y < maxY; y++) {
                    BlockState want;
                    if (!land || y > top || y < bottom) {
                        want = air;
                    } else if (y == top) {
                        want = ground;
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

    // The top of the Eclipse's land at a column, as place() builds it (before anything is placed on it or
    // dug out of it), or Integer.MIN_VALUE off the disc. Structures use it: the world-gen heightmaps
    // don't know this terrain.
    public static int groundTop(EclipseLayout.Eclipse e, int x, int z) {
        double d = e.distance(x + 0.5, z + 0.5);
        if (d > EclipseLayout.RADIUS) {
            return Integer.MIN_VALUE;
        }
        double t = d / EclipseLayout.RADIUS;
        return Mth.floor(TOP + 2.5 * hills(x, z) - 6.0 * t * t * t * t);
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
