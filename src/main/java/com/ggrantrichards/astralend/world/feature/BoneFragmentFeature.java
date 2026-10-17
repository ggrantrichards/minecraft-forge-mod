package com.ggrantrichards.astralend.world.feature;

import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

// The Ashen Barrens are a graveyard (after beta.4): between the rare whole ribcages, the shelves are
// strewn with what's left of the rest - a vertebra half-sunk in the ash with its spine-spur standing
// up, or a single rib curving up out of the ground like a tusk. Every piece stands on the ground or is
// sunk into it; nothing hangs over an edge.
public record BoneFragmentFeature() implements Feature {
    public static final MapCodec<BoneFragmentFeature> CODEC = MapCodec.unit(BoneFragmentFeature::new);

    @Override
    public MapCodec<BoneFragmentFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ground = Build.surface(level, origin.getX(), origin.getZ());
        if (ground == null || !level.getBlockState(ground).is(ModBlocks.PALE_END_STONE.get())) {
            return false;
        }
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        return random.nextInt(3) == 0 ? rib(level, random, ground, along) : vertebra(level, random, ground, along);
    }

    // Two to four bone blocks in a row, sunk level with the ground, and a spur standing up from one.
    private static boolean vertebra(WorldGenLevel level, RandomSource random, BlockPos ground, Direction along) {
        int length = 2 + random.nextInt(3);
        BlockPos[] at = new BlockPos[length];
        for (int i = 0; i < length; i++) {
            BlockPos column = ground.relative(along, i);
            at[i] = Build.surface(level, column.getX(), column.getZ());
            if (at[i] == null || at[i].getY() != ground.getY()) {
                return false;                                  // only on flat, solid ground
            }
        }
        BlockState lying = bone(along.getAxis());
        for (BlockPos p : at) {
            Build.force(level, p, lying);
        }
        int spur = random.nextInt(length);
        int tall = 1 + random.nextInt(2);
        for (int k = 1; k <= tall; k++) {
            Build.put(level, at[spur].above(k), bone(Direction.Axis.Y));
        }
        return true;
    }

    // A rib rising from the ground and curving over: straight up for a few blocks, then leaning out.
    private static boolean rib(WorldGenLevel level, RandomSource random, BlockPos ground, Direction lean) {
        int height = 3 + random.nextInt(4);
        BlockPos p = ground;                                   // its foot is sunk into the ground
        Build.force(level, p, bone(Direction.Axis.Y));
        for (int k = 1; k <= height; k++) {
            // past halfway up it bends outward, a block over every block or two
            if (k > height / 2 && (k % 2 == 0 || k == height)) {
                BlockPos side = p.relative(lean);
                if (!Build.put(level, side, bone(lean.getAxis()))) {
                    break;
                }
                p = side;
                if (k == height) {
                    break;
                }
            }
            BlockPos up = p.above();
            if (!Build.put(level, up, bone(Direction.Axis.Y))) {
                break;
            }
            p = up;
        }
        return true;
    }

    private static BlockState bone(Direction.Axis axis) {
        return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }
}
