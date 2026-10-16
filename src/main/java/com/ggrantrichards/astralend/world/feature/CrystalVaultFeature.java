package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// The Crystal Vaults: under the Shattered Crags, below the field of shards, great geodes drift in the
// void - shells of Nightstone lined with Glimmer Crystal, hollow inside, the walls grown over with
// glimmer clusters and spires of crystal rising from the floor. Each is split open somewhere: a crack
// you can climb in through. And in each, folded down among the clusters and looking like one of them,
// a Shardback waits.
public record CrystalVaultFeature() implements Feature {
    public static final MapCodec<CrystalVaultFeature> CODEC = MapCodec.unit(CrystalVaultFeature::new);

    @Override
    public MapCodec<CrystalVaultFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = (origin.getX() & ~15) + 8 + random.nextInt(5) - 2;
        int cz = (origin.getZ() & ~15) + 8 + random.nextInt(5) - 2;
        double r = 7.0 + random.nextDouble() * 4.5;
        int cy = level.getMinBuildHeight() + 16 + random.nextInt(14);
        // Only in open void: nothing already there.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int ri = Mth.ceil(r) + 1;
        for (int dx = -ri; dx <= ri; dx += 3) {
            for (int dy = -ri; dy <= ri; dy += 3) {
                for (int dz = -ri; dz <= ri; dz += 3) {
                    if (!level.getBlockState(p.set(cx + dx, cy + dy, cz + dz)).isAir()) {
                        return false;
                    }
                }
            }
        }
        // The crack: a direction, and a jagged slot through the shell along it.
        double ca = random.nextDouble() * Math.PI * 2;
        double cxv = Math.cos(ca), czv = Math.sin(ca);
        BlockState shell = ModBlocks.NIGHTSTONE.get().defaultBlockState();
        BlockState crystal = ModBlocks.GLIMMER_CRYSTAL.get().defaultBlockState();
        BlockState lining = ModBlocks.POLISHED_NIGHTSTONE.get().defaultBlockState();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.25 + dz * dz);
                    double bumpy = r + 0.6 * Math.sin(dx * 0.9 + dz * 0.7) * Math.cos(dy * 0.8);
                    if (d > bumpy) {
                        continue;
                    }
                    p.set(cx + dx, cy + dy, cz + dz);
                    double along = (dx * cxv + dz * czv) / Math.max(d, 0.01);
                    boolean crack = along > 0.82 && Math.abs(dy + Math.sin(dx * 1.3 + dz * 1.1) * 1.5) < 2.2;
                    if (d > bumpy - 1.3) {
                        level.setBlock(p, crack ? Blocks.AIR.defaultBlockState() : shell, 2);
                    } else if (d > bumpy - 2.3) {
                        level.setBlock(p, crack ? Blocks.AIR.defaultBlockState() : lining, 2);
                    } else if (d > bumpy - 3.2) {
                        level.setBlock(p, crack ? Blocks.AIR.defaultBlockState()
                            : random.nextFloat() < 0.04F ? ModBlocks.VOID_CRYSTAL_ORE.get().defaultBlockState() : crystal, 2);
                    } else {
                        level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Clusters on the inner walls, and spires from the floor.
        BlockState cluster = ModBlocks.GLIMMER_CLUSTER.get().defaultBlockState();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    p.set(cx + dx, cy + dy, cz + dz);
                    if (!level.getBlockState(p).is(Blocks.CAVE_AIR) || random.nextFloat() > 0.18F) {
                        continue;
                    }
                    for (Direction d : Direction.values()) {
                        if (level.getBlockState(p.relative(d)).is(ModBlocks.GLIMMER_CRYSTAL.get())) {
                            level.setBlock(p, cluster.setValue(AmethystClusterBlock.FACING, d.getOpposite()), 2);
                            break;
                        }
                    }
                }
            }
        }
        int spires = 2 + random.nextInt(3);
        for (int s = 0; s < spires; s++) {
            int sx = cx + random.nextInt(7) - 3, sz = cz + random.nextInt(7) - 3;
            int y = cy;
            while (y > cy - ri && level.getBlockState(p.set(sx, y - 1, sz)).is(Blocks.CAVE_AIR)) {
                y--;
            }
            int height = 2 + random.nextInt(4);
            for (int k = 0; k < height && level.getBlockState(p.set(sx, y + k, sz)).is(Blocks.CAVE_AIR); k++) {
                level.setBlock(p, crystal, 2);
            }
        }
        // Its Shardback, folded down on the floor.
        int fy = cy;
        while (fy > cy - ri && level.getBlockState(p.set(cx, fy - 1, cz)).is(Blocks.CAVE_AIR)) {
            fy--;
        }
        var hunter = ModEntities.SHARDBACK.get().create(level.getLevel(), MobSpawnType.CHUNK_GENERATION);
        if (hunter != null && level.getBlockState(p.set(cx, fy, cz)).is(Blocks.CAVE_AIR)) {
            hunter.snapTo(cx + 0.5, fy, cz + 0.5, random.nextFloat() * 360.0F, 0.0F);
            hunter.setPersistenceRequired();
            level.addFreshEntity(hunter);
        }
        return true;
    }
}
