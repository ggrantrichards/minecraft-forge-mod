package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// Things found on the very bottom of an island, where only someone hanging under it will look:
//   "voidgel" - a drip of Voidgel hanging from the underside. (Void Crystal used to grow here too; the
//               distillation moved it into the Glimmering Crags' root caves.)
public record UndersideFeature(String kind) implements Feature {
    public static final MapCodec<UndersideFeature> CODEC = Codec.STRING.fieldOf("kind").xmap(UndersideFeature::new, UndersideFeature::kind);

    @Override
    public MapCodec<UndersideFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // The lowest solid block in this column, with open air under it.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(origin.getX(), level.getMinBuildHeight() + 1, origin.getZ());
        int top = level.getMinBuildHeight() + 120;
        while (p.getY() < top && level.getBlockState(p).isAir()) {
            p.move(Direction.UP);
        }
        if (p.getY() >= top || p.getY() <= level.getMinBuildHeight() + 2) {
            return false;
        }
        BlockPos bottom = p.immutable();
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
        return true;
    }
}
