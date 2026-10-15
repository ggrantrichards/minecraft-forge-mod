package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.maw.MawEyeBlock;
import com.ggrantrichards.astralend.world.ModWorldgen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// The Leviathan's skull - the Maw - built from its shape (LeviathanShape) in the same frame as the body,
// and furnished: the four eyes in their sockets (dark until the egg is home), the explorers' camp on the
// cranium's floor, a ladder up to the ledge that runs past the eyes, End Rods. Light the four eyes and a
// Portal Lens wakes the throat into a wormhole (MawNetwork); its travellers come out on the tongue-bone.
public class LeviathanHeadPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan_skull"));
    public static final int MIN_U = -16, MAX_U = 16, MIN_V = 0, MAX_V = 70, MIN_W = LeviathanShape.SKULL_W - 1, MAX_W = LeviathanShape.SNOUT + 4;

    private final BlockPos origin;     // the structure's origin (the frame's middle)
    private final int turn;
    private final long seed;
    private final LeviathanShape shape;

    public LeviathanHeadPiece(BlockPos origin, int turn, long seed) {
        super(ModWorldgen.LEVIATHAN_HEAD_PIECE.get(), 0, LeviathanPiece.box(origin, turn, MIN_U, MAX_U, MIN_V, MAX_V, MIN_W, MAX_W));
        this.origin = origin;
        this.turn = turn;
        this.seed = seed;
        this.shape = new LeviathanShape(seed);
    }

    public LeviathanHeadPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.LEVIATHAN_HEAD_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.turn = tag.getIntOr("Turn", 0);
        this.seed = tag.getLongOr("Seed", 0L);
        this.shape = new LeviathanShape(this.seed);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putInt("Turn", this.turn);
        tag.putLong("Seed", this.seed);
    }

    private BlockPos at(double u, double v, double w) {
        return LeviathanPiece.world(this.origin, this.turn, u, v, w);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunk, ChunkPos chunkPos, BlockPos reference) {
        LeviathanPiece.build(level, chunk, this.boundingBox, this.origin, this.turn, this.shape, MIN_W, MAX_W);
        this.furnish(level, chunk);
    }

    private void furnish(WorldGenLevel level, BoundingBox chunk) {
        RandomSource rnd = RandomSource.create(this.seed ^ 0xCA4FL);
        int b = LeviathanShape.b(), cv = b + 11, sw = LeviathanShape.SKULL_W;
        // The eyes, set in the sockets, looking out.
        for (int[] e : LeviathanShape.eyes()) {
            this.put(level, chunk, this.at(e[0], e[1], e[2]), ModBlocks.MAW_EYE.get().defaultBlockState()
                .setValue(MawEyeBlock.FACING, LeviathanPiece.across(this.turn, e[0] > 0 ? 1 : -1)).setValue(MawEyeBlock.LIT, false));
        }
        // A ladder each side from the floor up to the ledge past the eyes, against a post of bone.
        for (int s = -1; s <= 1; s += 2) {
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, LeviathanPiece.across(this.turn, -s));
            for (int v = b + 4; v <= cv + 2; v++) {
                this.put(level, chunk, this.at(s * 10, v, sw + 6), ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState());
                this.put(level, chunk, this.at(s * 9, v, sw + 6), ladder);
            }
        }
        // End Rods standing on the floor at the hall's corners, and on the ledge by the eyes.
        for (int s = -1; s <= 1; s += 2) {
            for (int w : new int[] {sw + 4, sw + 18}) {
                this.put(level, chunk, this.at(s * 6, b + 4, w), Blocks.END_ROD.defaultBlockState());
            }
            this.put(level, chunk, this.at(s * 8, cv + 3, sw + 15), Blocks.END_ROD.defaultBlockState());
        }
        // Three End Rods standing on the crest down its crown, lights to see the skull by from far off.
        for (int s : new int[] {4, 10, 16}) {
            this.put(level, chunk, this.at(0, cv + 13, sw + s), Blocks.END_ROD.defaultBlockState());
        }
        // The camp of whoever got here first: a ring of slabs round their lantern, chests, a table for charts.
        BlockPos fire = this.at(0, b + 4, sw + 10);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r >= 2.5 && r < 3.5) {
                    this.put(level, chunk, fire.offset(dx, 0, dz), ModBlocks.PALE_END_STONE_BRICK_SLAB.get().defaultBlockState());
                }
            }
        }
        this.put(level, chunk, fire, ModBlocks.NULL_LANTERN.get().defaultBlockState());
        this.chest(level, chunk, rnd, this.at(-5, b + 4, sw + 7));
        this.chest(level, chunk, rnd, this.at(5, b + 4, sw + 14));
        this.put(level, chunk, this.at(5, b + 4, sw + 6), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        this.put(level, chunk, this.at(-5, b + 4, sw + 14), Blocks.BARREL.defaultBlockState());
        // And a chest up on the ledge, between the eyes.
        this.chest(level, chunk, rnd, this.at(9, cv + 3, sw + 15));
    }

    private void put(WorldGenLevel level, BoundingBox chunk, BlockPos p, BlockState state) {
        if (chunk.isInside(p)) {
            level.setBlock(p, state, 2);
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunk, RandomSource rnd, BlockPos p) {
        if (chunk.isInside(p)) {
            this.createChest(level, chunk, rnd, p, LOOT, null);
        }
    }

    // The four eyes, in the world.
    public List<BlockPos> eyes() {
        List<BlockPos> out = new ArrayList<>();
        for (int[] e : LeviathanShape.eyes()) {
            out.add(this.at(e[0], e[1], e[2]));
        }
        return out;
    }

    // The middle of the throat, in the world (the survey looks for it).
    public BlockPos throat() {
        int[] t = LeviathanShape.throat();
        return this.at(t[0], t[1], t[2]);
    }

    // The middle of the cranium's floor, in the world (for the survey's photographs).
    public BlockPos hall() {
        return this.at(0, LeviathanShape.b() + 4, LeviathanShape.SKULL_W + 12);
    }

    // A point of the frame, in the world (for the survey's photographs).
    public BlockPos frame(double u, double v, double w) {
        return this.at(u, v, w);
    }

    // Which way the mouth faces.
    public Direction facing() {
        int[] cs = LeviathanPiece.CS[this.turn];
        return cs[0] > 0 ? Direction.EAST : cs[0] < 0 ? Direction.WEST : cs[1] > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
