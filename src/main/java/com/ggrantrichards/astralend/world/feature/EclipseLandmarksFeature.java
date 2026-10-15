package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.EclipseLayout;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// The Eclipse's water, each piece built by whichever chunk it falls in (placed once in every chunk):
//   - the Wellspring, the heart of the meadow: a round lake on a bed of End Stone and Pale End Stone,
//     its rim built up to hold the water, mossy, with Lumineshrooms and Breathcomb round it;
//   - the ponds round it (EclipseLayout.PONDS), little basins of the same water, each level with the
//     ground at its middle;
//   - three springs on the rim, spilling off the edge into the void.
public record EclipseLandmarksFeature() implements Feature {
    public static final MapCodec<EclipseLandmarksFeature> CODEC = MapCodec.unit(EclipseLandmarksFeature::new);
    private static final int WATER_TOP = (int) EclipseTerrainFeature.TOP - 1;   // the Wellspring's surface

    @Override
    public MapCodec<EclipseLandmarksFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        var nebula = EclipseLayout.sampler(level.getLevel().getChunkSource().randomState());
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        EclipseLayout.Eclipse e = EclipseLayout.at(x0 + 8, z0 + 8, nebula);
        if (e == null || e.distance(x0 + 8, z0 + 8) > EclipseLayout.RADIUS + 24) {
            return false;
        }
        double r = EclipseLayout.RADIUS;
        boolean any = this.basin(level, e, x0, z0, random, e.worldX(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]),
            e.worldZ(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]), EclipseLayout.WELLSPRING_RADIUS * r, WATER_TOP, 6.0);
        for (double[] pond : EclipseLayout.PONDS) {
            double px = e.worldX(pond[0], pond[1]), pz = e.worldZ(pond[0], pond[1]);
            int top = EclipseTerrainFeature.groundTop(e, (int) Math.floor(px), (int) Math.floor(pz)) - 1;
            any |= this.basin(level, e, x0, z0, random, px, pz, pond[2], top, 2.5);
        }
        any |= this.springs(level, e, x0, z0);
        return any;
    }

    // A basin of water dug into the meadow, like the lakes of BetterEnd: deepest in the middle, a bed of End
    // Stone and Pale End Stone, water to `waterTop`; the ring just outside is built up to the water's height
    // (moss on top) so nothing spills.
    private boolean basin(WorldGenLevel level, EclipseLayout.Eclipse e, int x0, int z0, RandomSource random, double wx, double wz,
                          double radius, int waterTop,
                          double depth) {
        boolean placed = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                double d = Math.sqrt((x + 0.5 - wx) * (x + 0.5 - wx) + (z + 0.5 - wz) * (z + 0.5 - wz));
                if (d < radius) {
                    double t = d / radius;
                    int floor = waterTop - 1 - (int) Math.round(depth * Math.sqrt(Math.max(0.0, 1.0 - t * t)) + 0.6 * random.nextDouble());
                    for (int y = floor - 3; y < floor; y++) {
                        level.setBlock(p.set(x, y, z), Blocks.END_STONE.defaultBlockState(), 2);
                    }
                    level.setBlock(p.set(x, floor, z), random.nextInt(3) == 0 ? ModBlocks.PALE_END_STONE.get().defaultBlockState()
                        : Blocks.END_STONE.defaultBlockState(), 2);
                    for (int y = floor + 1; y <= waterTop; y++) {
                        level.setBlock(p.set(x, y, z), water, 2);
                    }
                    for (int y = waterTop + 1; y < waterTop + 8; y++) {
                        if (!level.getBlockState(p.set(x, y, z)).isAir()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    placed = true;
                } else if (d < radius + 3.0) {
                    // The rim: solid up to the water's surface, moss on top.
                    // (the ground from the terrain's own shape: the world-gen heightmaps don't know it)
                    int top = EclipseTerrainFeature.groundTop(e, x, z);
                    if (top == Integer.MIN_VALUE) {
                        continue;
                    }
                    for (int y = Math.min(top, waterTop - 8); y <= waterTop; y++) {
                        BlockState here = level.getBlockState(p.set(x, y, z));
                        if (here.isAir() || here.canBeReplaced()) {
                            level.setBlock(p, y == waterTop ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.END_STONE.defaultBlockState(), 2);
                        }
                    }
                    if (random.nextInt(5) == 0) {
                        Build.put(level, new BlockPos(x, Math.max(top, waterTop) + 1, z), random.nextBoolean()
                            ? ModBlocks.LUMINESHROOM.get().defaultBlockState() : ModBlocks.BREATHCOMB.get().defaultBlockState());
                    }
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean inChunk(double x, double z, int x0, int z0) {
        return x >= x0 && x < x0 + 16 && z >= z0 && z < z0 + 16;
    }

    // Three springs on the rim, each a source block at the very edge of the land: the
    // water runs off and falls into the void.
    private boolean springs(WorldGenLevel level, EclipseLayout.Eclipse e, int x0, int z0) {
        boolean placed = false;
        for (double angle : new double[] {-0.9, -0.2, 0.55}) {
            double u = Math.cos(angle), v = Math.sin(angle);
            for (double reach = 0.99; reach > 0.85; reach -= 0.01) {
                double x = e.worldX(u * reach, v * reach), z = e.worldZ(u * reach, v * reach);
                if (!inChunk(x, z, x0, z0)) {
                    break;
                }
                BlockPos ground = Build.surface(level, (int) Math.floor(x), (int) Math.floor(z));
                if (ground == null || !(level.getBlockState(ground).is(ModBlocks.LIVING_TURF.get()) || level.getBlockState(ground).is(Blocks.END_STONE))) {
                    continue;
                }
                // A source in a notch at the very edge, moss round it; it runs off once the chunk is done.
                level.setBlock(ground, Blocks.WATER.defaultBlockState(), 2);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos g = ground.offset(dx, 0, dz);
                        if ((dx != 0 || dz != 0) && dx * dx + dz * dz <= 5
                            && (level.getBlockState(g).is(Blocks.END_STONE) || level.getBlockState(g).is(ModBlocks.LIVING_TURF.get()))
                            && level.getBlockState(g.above()).isAir()) {
                            level.setBlock(g, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                        }
                    }
                }
                placed = true;
                break;
            }
        }
        return placed;
    }
}
