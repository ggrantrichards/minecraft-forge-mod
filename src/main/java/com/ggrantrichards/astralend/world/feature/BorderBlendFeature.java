package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.EclipseLayout;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

// Biome borders, blended. Every biome's ground is its own - Chorus Moss over End Stone, Nightstone,
// Pale End Stone, plain End Stone - and left alone, it changes in a line from one column to the next.
// This runs on every End chunk before anything grows, and near a border (within REACH blocks of
// another biome's ground) it mixes that ground into this one's: soft blots of it, half and half at the
// border, thinning out to nothing REACH blocks in - on the top few blocks, so an island that sits
// across a border is a mottled mix rather than cut in two. The Eclipse keeps to itself: it has its
// ring of void.
public record BorderBlendFeature() implements Feature {
    public static final MapCodec<BorderBlendFeature> CODEC = MapCodec.unit(BorderBlendFeature::new);
    private static final int REACH = 24;
    private static final int STEP = 4;
    private static final int DEPTH = 4;

    private enum Ground { MOSS, NIGHT, PALE, END, NONE }

    @Override
    public MapCodec<BorderBlendFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        if ((long) x0 * x0 + (long) z0 * z0 < 1000L * 1000L) {
            return false;                                   // the main island and its gap: vanilla's own
        }
        Climate.Sampler sampler = EclipseLayout.sampler(level.getLevel().getChunkSource().randomState());
        var resolver = generator.getBiomeSource().createResolver(sampler);
        // The ground on a coarse grid round the chunk.
        int n = (16 + 2 * REACH) / STEP + 1;
        Ground[][] grid = new Ground[n][n];
        boolean mixed = false;
        Ground first = null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int gx = x0 - REACH + i * STEP, gz = z0 - REACH + j * STEP;
                Holder<Biome> biome = resolver.getNoiseBiome(QuartPos.fromBlock(gx), QuartPos.fromBlock(64), QuartPos.fromBlock(gz));
                grid[i][j] = ground(biome);
                if (first == null) {
                    first = grid[i][j];
                } else if (grid[i][j] != first) {
                    mixed = true;
                }
            }
        }
        if (!mixed) {
            return false;
        }
        boolean changed = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                int ci = Math.round((x - (x0 - REACH)) / (float) STEP), cj = Math.round((z - (z0 - REACH)) / (float) STEP);
                Ground own = grid[ci][cj];
                if (own == Ground.NONE) {
                    continue;
                }
                // The nearest other ground, and how far.
                Ground other = null;
                double best = REACH;
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        Ground g = grid[i][j];
                        if (g == own || g == Ground.NONE) {
                            continue;
                        }
                        double d = Math.hypot(x0 - REACH + i * STEP - x, z0 - REACH + j * STEP - z) - STEP * 0.5;
                        if (d < best) {
                            best = d;
                            other = g;
                        }
                    }
                }
                if (other == null) {
                    continue;
                }
                double chance = 0.5 * Math.pow(1.0 - Math.max(0.0, best) / REACH, 1.2);
                if (blots(x, z) >= chance) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                boolean surface = true;
                int done = 0;
                for (int y = top; y >= level.getMinY() && done < DEPTH; y--) {
                    p.set(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) {
                        if (done > 0) {
                            break;
                        }
                        continue;
                    }
                    if (!isGround(own, state)) {
                        break;
                    }
                    level.setBlock(p, surface ? top(other, level.getBlockState(p.above()).isAir()) : body(other), 2);
                    surface = false;
                    done++;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static Ground ground(Holder<Biome> biome) {
        String key = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        return switch (key) {
            case "astralend:nebula_fields" -> Ground.MOSS;
            case "astralend:glimmering_crags", "astralend:shattered_crags", "astralend:deep_hollows" -> Ground.NIGHT;
            case "astralend:ashen_barrens" -> Ground.PALE;
            case "astralend:eclipse", "astralend:corona" -> Ground.NONE;
            default -> Ground.END;
        };
    }

    private static boolean isGround(Ground g, BlockState state) {
        return switch (g) {
            case MOSS -> state.is(ModBlocks.CHORUS_MOSS.get()) || state.is(Blocks.END_STONE);
            case NIGHT -> state.is(ModBlocks.NIGHTSTONE.get());
            case PALE -> state.is(ModBlocks.PALE_END_STONE.get());
            case END -> state.is(Blocks.END_STONE);
            case NONE -> false;
        };
    }

    private static BlockState top(Ground g, boolean open) {
        return g == Ground.MOSS && open ? ModBlocks.CHORUS_MOSS.get().defaultBlockState() : body(g);
    }

    private static BlockState body(Ground g) {
        return switch (g) {
            case NIGHT -> ModBlocks.NIGHTSTONE.get().defaultBlockState();
            case PALE -> ModBlocks.PALE_END_STONE.get().defaultBlockState();
            default -> Blocks.END_STONE.defaultBlockState();
        };
    }

    // Soft blots, a few blocks across, with a little grain at their edges: in [0, 1).
    private static double blots(int x, int z) {
        return Math.min(0.999, 0.85 * value(x / 5.0, z / 5.0) + 0.15 * hash(x, z));
    }

    private static double value(double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash(x0, z0), b = hash(x0 + 1, z0), c = hash(x0, z0 + 1), d = hash(x0 + 1, z0 + 1);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fz;
    }

    private static double hash(int x, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ 0xB1E4DL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }
}
