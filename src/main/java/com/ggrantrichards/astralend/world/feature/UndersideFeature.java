package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// Things found on the very bottom of an island, where only someone hanging under it will look:
//   "void_crystal" - a seam of Void Crystal Ore in the underside, with a Glimmer Cluster hanging
//                    under it to give it away (the Glimmering Crags; the only place it grows);
//   "voidgel"      - a drip of Voidgel hanging from the underside.
public record UndersideFeature(String kind) implements Feature {
    public static final MapCodec<UndersideFeature> CODEC = Codec.STRING.fieldOf("kind").xmap(UndersideFeature::new, UndersideFeature::kind);

    @Override
    public MapCodec<UndersideFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // The lowest solid block in this column, with open air under it.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(origin.getX(), level.getMinY() + 1, origin.getZ());
        int top = level.getMinY() + 120;
        while (p.getY() < top && level.getBlockState(p).isAir()) {
            p.move(Direction.UP);
        }
        if (p.getY() >= top || p.getY() <= level.getMinY() + 2) {
            return false;
        }
        BlockPos bottom = p.immutable();
        if ("void_crystal".equals(this.kind)) {
            if (!level.getBlockState(bottom).is(ModBlocks.NIGHTSTONE.get())) {
                return false;
            }
            BlockState ore = ModBlocks.VOID_CRYSTAL_ORE.get().defaultBlockState();
            Build.force(level, bottom, ore);
            for (int i = 0; i < 2; i++) {
                BlockPos q = bottom.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random)).above(random.nextInt(2));
                if (level.getBlockState(q).is(ModBlocks.NIGHTSTONE.get()) && random.nextBoolean()) {
                    Build.force(level, q, ore);
                }
            }
            Build.put(level, bottom.below(), ModBlocks.GLIMMER_CLUSTER.get().defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, Direction.DOWN));
        } else {
            BlockState gel = ModBlocks.VOIDGEL.get().defaultBlockState();
            int length = 1 + random.nextInt(3);
            for (int i = 1; i <= length; i++) {
                Build.put(level, bottom.below(i), gel);
                if (i == 1) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (random.nextBoolean()) {
                            Build.put(level, bottom.below().relative(d), gel);
                        }
                    }
                }
            }
        }
        return true;
    }
}
