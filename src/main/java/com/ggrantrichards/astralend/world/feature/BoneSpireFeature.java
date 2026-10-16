package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

// The Ashen Barrens' spires (after beta.4): towers of pale stone ten to twenty-odd blocks tall,
// weathered like old bone or dead coral - wide at the foot, tapering, a little crooked, and pitted
// right through with holes. They stand on the shelves; their solid core runs from the ground to the
// tip, so nothing of them hangs loose.
public class BoneSpireFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ground = Build.surface(level, origin.getX(), origin.getZ());
        if (ground == null || !level.getBlockState(ground).is(ModBlocks.PALE_END_STONE.get())) {
            return false;
        }
        // The whole foot on ground (no edge or dip under it).
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dz = -2; dz <= 2; dz += 2) {
                BlockPos g = Build.surface(level, ground.getX() + dx, ground.getZ() + dz);
                if (g == null || Math.abs(g.getY() - ground.getY()) > 1) {
                    return false;
                }
            }
        }
        int height = 10 + random.nextInt(14);
        double base = 1.6 + random.nextDouble() * 0.9;
        double leanX = (random.nextDouble() - 0.5) * 0.12, leanZ = (random.nextDouble() - 0.5) * 0.12;
        BlockState stone = ModBlocks.PALE_END_STONE.get().defaultBlockState();
        BlockState bricks = ModBlocks.PALE_END_STONE_BRICKS.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            double t = (double) y / height;
            double radius = Math.max(0.5, base * Math.pow(1.0 - t, 0.7) + 0.3 * Math.sin(y * 0.9 + base));
            double cx = ground.getX() + leanX * y, cz = ground.getZ() + leanZ * y;
            int reach = Mth.ceil(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d = Math.hypot(dx + ground.getX() - cx, dz + ground.getZ() - cz);
                    if (d > radius) {
                        continue;
                    }
                    BlockPos p = new BlockPos(Mth.floor(cx) + dx, ground.getY() + 1 + y, Mth.floor(cz) + dz);
                    boolean core = d < 0.75;
                    if (!core && random.nextInt(5) == 0) {
                        continue;                              // pitted: holes right through the shell
                    }
                    Build.put(level, p, random.nextInt(9) == 0 ? bricks : stone);
                }
            }
        }
        return true;
    }
}
