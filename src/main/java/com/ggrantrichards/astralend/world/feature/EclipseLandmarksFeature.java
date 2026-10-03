package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.EclipseDensity;
import com.ggrantrichards.astralend.world.EclipseLayout;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

// The Eclipse's landmarks, each built by whichever chunk it falls in (placed once in every chunk):
//   - the Wellspring, the heart of the heaven half: a round lake whose floor is Astral Glass over the
//     open hole the terrain leaves (EclipseDensity) - swim in it and look down past your feet into the
//     void. Its rim is built up to hold the water; reeds of Lumineshroom and Breathcomb ring it;
//   - the seeds: one burnt Solarite spire standing in the heaven half's dot of hell, and one small
//     living pool in the hell half's dot of heaven;
//   - three springs on the heaven half's rim, spilling off the edge into the void.
public record EclipseLandmarksFeature() implements Feature {
    public static final MapCodec<EclipseLandmarksFeature> CODEC = MapCodec.unit(EclipseLandmarksFeature::new);
    private static final int WATER_TOP = (int) EclipseDensity.TOP - 1;   // the Wellspring's surface
    private static final int GLASS_Y = WATER_TOP - 5;

    @Override
    public MapCodec<EclipseLandmarksFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        DensityFunction nebula = level.getLevel().getChunkSource().randomState().sampler().temperature();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        EclipseLayout.Eclipse e = EclipseLayout.at(x0 + 8, z0 + 8, nebula);
        if (e == null || e.distance(x0 + 8, z0 + 8) > EclipseLayout.RADIUS + 24) {
            return false;
        }
        boolean any = this.wellspring(level, e, x0, z0, random);
        any |= this.seeds(level, e, x0, z0, random);
        any |= this.springs(level, e, x0, z0);
        return any;
    }

    // The Wellspring: each column of this chunk within it gets its glass floor and water; the ring just
    // outside is built up to the water's height so nothing spills.
    private boolean wellspring(WorldGenLevel level, EclipseLayout.Eclipse e, int x0, int z0, RandomSource random) {
        double wx = e.worldX(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double wz = e.worldZ(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double radius = EclipseLayout.WELLSPRING_RADIUS * EclipseLayout.RADIUS;
        boolean placed = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState glass = ModBlocks.ASTRAL_GLASS.get().defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                double d = Math.sqrt((x + 0.5 - wx) * (x + 0.5 - wx) + (z + 0.5 - wz) * (z + 0.5 - wz));
                if (d < radius) {
                    level.setBlock(p.set(x, GLASS_Y, z), glass, 2);
                    for (int y = GLASS_Y + 1; y <= WATER_TOP; y++) {
                        level.setBlock(p.set(x, y, z), water, 2);
                    }
                    for (int y = WATER_TOP + 1; y < WATER_TOP + 8; y++) {
                        if (!level.getBlockState(p.set(x, y, z)).isAir()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    placed = true;
                } else if (d < radius + 3.0) {
                    // The rim: solid up to the water's surface, turf on top.
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    for (int y = Math.min(top, GLASS_Y - 1); y <= WATER_TOP; y++) {
                        BlockState here = level.getBlockState(p.set(x, y, z));
                        if (here.isAir() || here.canBeReplaced()) {
                            level.setBlock(p, y == WATER_TOP ? ModBlocks.LIVING_TURF.get().defaultBlockState()
                                : Blocks.END_STONE.defaultBlockState(), 2);
                        }
                    }
                    if (random.nextInt(5) == 0) {
                        Build.put(level, new BlockPos(x, Math.max(top, WATER_TOP) + 1, z), random.nextBoolean()
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

    // The two seeds, each built by the chunk its middle falls in (they reach at most 7 blocks out).
    private boolean seeds(WorldGenLevel level, EclipseLayout.Eclipse e, int x0, int z0, RandomSource random) {
        boolean placed = false;
        double hx = e.worldX(EclipseLayout.HELL_SEED[0], EclipseLayout.HELL_SEED[1]);
        double hz = e.worldZ(EclipseLayout.HELL_SEED[0], EclipseLayout.HELL_SEED[1]);
        if (inChunk(hx, hz, x0, z0)) {
            BlockPos ground = Build.surface(level, (int) Math.floor(hx), (int) Math.floor(hz));
            if (ground != null) {
                // A burnt patch of Sunscorched Stone for the spire to stand on.
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        if (dx * dx + dz * dz <= 16) {
                            BlockPos g = Build.surface(level, ground.getX() + dx, ground.getZ() + dz);
                            if (g != null) {
                                Build.force(level, g, ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState());
                            }
                        }
                    }
                }
                Build.force(level, ground, ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState());
                new SolariteSpireFeature().place(level, null, random, ground.above());
                placed = true;
            }
        }
        double px = e.worldX(EclipseLayout.HEAVEN_SEED[0], EclipseLayout.HEAVEN_SEED[1]);
        double pz = e.worldZ(EclipseLayout.HEAVEN_SEED[0], EclipseLayout.HEAVEN_SEED[1]);
        if (inChunk(px, pz, x0, z0)) {
            BlockPos ground = Build.surface(level, (int) Math.floor(px), (int) Math.floor(pz));
            if (ground != null) {
                int y = ground.getY();
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                for (int dx = -6; dx <= 6; dx++) {
                    for (int dz = -6; dz <= 6; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > 6.0) {
                            continue;
                        }
                        int x = ground.getX() + dx, z = ground.getZ() + dz;
                        for (int up = 1; up < 6; up++) {
                            Build.force(level, p.set(x, y + up, z), Blocks.AIR.defaultBlockState());
                        }
                        if (d < 4.0) {
                            Build.force(level, p.set(x, y - 2, z), Blocks.END_STONE.defaultBlockState());
                            Build.force(level, p.set(x, y - 1, z), Blocks.WATER.defaultBlockState());
                            Build.force(level, p.set(x, y, z), Blocks.WATER.defaultBlockState());
                        } else {
                            Build.force(level, p.set(x, y - 1, z), Blocks.END_STONE.defaultBlockState());
                            Build.force(level, p.set(x, y, z), ModBlocks.LIVING_TURF.get().defaultBlockState());
                            if (random.nextInt(3) == 0) {
                                Build.put(level, p.set(x, y + 1, z), ModBlocks.LUMINESHROOM.get().defaultBlockState());
                            }
                        }
                    }
                }
                placed = true;
            }
        }
        return placed;
    }

    // Three springs on the heaven half's rim, each a source block at the very edge of the land: the
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
                if (ground == null || !level.getBlockState(ground).is(ModBlocks.LIVING_TURF.get())) {
                    continue;
                }
                // A source in a notch of the turf at the very edge; it runs off once the chunk is done.
                level.setBlock(ground, Blocks.WATER.defaultBlockState(), 2);
                placed = true;
                break;
            }
        }
        return placed;
    }
}
