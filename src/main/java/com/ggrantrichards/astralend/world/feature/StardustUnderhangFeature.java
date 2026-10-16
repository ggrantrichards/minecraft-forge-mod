package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.CometCressBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

// Stardust that drifted up under an island and stuck there, with Comet Cress hanging from it.
// (Stardust "falls" upward, so an island's underside is where it gathers.)
public class StardustUnderhangFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ceiling = Underside.find(level, origin.getX(), origin.getZ(), 3);
        if (ceiling == null) {
            return false;
        }
        BlockState stardust = ModBlocks.STARDUST_BLOCK.get().defaultBlockState();
        boolean placed = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5 || random.nextInt(4) == 0) {
                    continue;
                }
                // Follow the underside's shape: it may be a block higher or lower here.
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = ceiling.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isAir()
                        && !level.getBlockState(pos.above()).isAir()) {
                        this.setBlock(level, pos, stardust);
                        if (random.nextInt(3) != 0) {
                            this.setBlock(level, pos.below(), ModBlocks.COMET_CRESS.get().defaultBlockState()
                                .setValue(CometCressBlock.AGE, random.nextInt(4)));
                        }
                        placed = true;
                        break;
                    }
                }
            }
        }
        return placed;
    }
}
