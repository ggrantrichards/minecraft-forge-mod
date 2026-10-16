package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

// An oasis of the Nebula Fields: a pond of still water sunk into the Nebula Moss - an irregular
// shape a few blocks across, shallow at the edges and deeper in the middle - with glowing Astral Vine
// buds crowding its banks and Lumineshrooms and tufts behind them. Only on flat ground: a pond needs a
// level rim all the way round. Sea pickles glow on the bed and lily pads float on top.
// (Within 7 blocks of its origin, inside the chunks a feature may touch.)
public class OasisFeature extends SimpleFeature {


    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos center = Build.surface(level, origin.getX(), origin.getZ());
        if (center == null || !level.getBlockState(center).is(ModBlocks.CHORUS_MOSS.get())) {
            return false;
        }
        int y = center.getY();
        double rx = 2.5 + random.nextDouble() * 2.5, rz = 2.5 + random.nextDouble() * 2.5;
        double wobble = random.nextDouble() * Math.PI * 2;
        // The rim must be level ground all round (within a block), or there's no pond here.
        for (int a = 0; a < 16; a++) {
            double ang = a * Math.PI / 8;
            int x = center.getX() + (int) Math.round(Math.cos(ang) * (rx + 1.5));
            int z = center.getZ() + (int) Math.round(Math.sin(ang) * (rz + 1.5));
            BlockPos rim = Build.surface(level, x, z);
            if (rim == null || Math.abs(rim.getY() - y) > 1 || !level.getBlockState(rim).isSolid()) {
                return false;
            }
        }
        BlockState water = Blocks.WATER.defaultBlockState();
        int reach = (int) Math.ceil(Math.max(rx, rz)) + 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double ang = Math.atan2(dz, dx);
                double r = 1.0 + 0.18 * Math.sin(ang * 3 + wobble);     // a lobed, natural shoreline
                double d = Math.sqrt((dx * dx) / (rx * rx) + (dz * dz) / (rz * rz)) / r;
                if (d >= 1.0) {
                    continue;
                }
                int depth = d < 0.45 ? 3 : d < 0.75 ? 2 : 1;
                BlockPos top = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                for (int k = 0; k < depth; k++) {
                    Build.force(level, top.below(k), water);
                }
                // A soft bed: clay and sand under the water, so it doesn't drain away into a cave.
                BlockState bed = random.nextInt(3) == 0 ? Blocks.CLAY.defaultBlockState() : Blocks.SAND.defaultBlockState();
                if (level.getBlockState(top.below(depth)).isAir()) {
                    Build.force(level, top.below(depth), bed);
                } else if (random.nextInt(2) == 0) {
                    Build.force(level, top.below(depth), bed);
                }
                // Life in it: sea pickles glowing on the bed of the deep middle, lily pads on top.
                if (depth >= 2 && random.nextInt(7) == 0) {
                    Build.force(level, top.below(depth - 1), Blocks.SEA_PICKLE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 1 + random.nextInt(4))
                        .setValue(net.minecraft.world.level.block.SeaPickleBlock.WATERLOGGED, true));
                } else if (depth >= 2 && random.nextInt(10) == 0 && level.getBlockState(top.above()).isAir()) {
                    Build.force(level, top.above(), Blocks.LILY_PAD.defaultBlockState());
                    continue;
                }
                // Nothing left floating over the pond.
                for (int up = 1; up <= 3; up++) {
                    BlockState over = level.getBlockState(top.above(up));
                    if (!over.isAir() && over.canBeReplaced()) {
                        Build.force(level, top.above(up), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // The banks: glowing Astral Vine buds right at the water, lumineshrooms and tufts a little back.
        for (int i = 0; i < 40; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double out = 1.0 + random.nextDouble() * 2.2;
            double r = (1.0 + 0.18 * Math.sin(ang * 3 + wobble));
            int x = center.getX() + (int) Math.round(Math.cos(ang) * (rx * r + out));
            int z = center.getZ() + (int) Math.round(Math.sin(ang) * (rz * r + out));
            if (Math.abs(x - center.getX()) > 7 || Math.abs(z - center.getZ()) > 7) {
                continue;
            }
            BlockPos ground = Build.surface(level, x, z);
            if (ground == null || Math.abs(ground.getY() - y) > 1 || !level.getBlockState(ground).is(ModBlocks.CHORUS_MOSS.get())) {
                continue;
            }
            BlockPos at = ground.above();
            if (out < 1.8 && random.nextInt(3) != 0) {
                Build.put(level, at, ModBlocks.ASTRAL_VINE.get().defaultBlockState());   // glowing buds at the water's edge
            } else if (random.nextInt(3) == 0) {
                Build.put(level, at, ModBlocks.LUMINESHROOM.get().defaultBlockState());
            } else {
                Build.put(level, at, ModBlocks.NEBULA_TUFTS.get().defaultBlockState());
            }
        }
        return true;
    }
}
