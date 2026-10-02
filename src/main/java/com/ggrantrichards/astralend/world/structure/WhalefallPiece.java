package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// Builds the Whalefall, a chunk at a time, from a few shapes: the skull (a hollow shell with three
// floors inside, glazed eye sockets and a doorway for a mouth), the jaw, a spine of vertebrae arching
// over the ribcage and down to the tail, thirteen pairs of ribs curling under it, and a village built
// inside the ribcage - two decks of planks with huts on them, ladders up bone pillars, lanterns. All
// of it hangs in the void.
//
// Positions are local: u across (left/right), v up from the bottom of the skeleton, w along the body
// (the skull at w = -110, the tail at w = +150). It lies along Z or along X.
public class WhalefallPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/whalefall"));
    private static final double SKULL_W = -110.0, SKULL_V = 30.0, SKULL_A = 30.0, SKULL_B = 34.0, SKULL_C = 38.0;
    private static final int[] SKULL_FLOORS = {14, 28, 42};
    private static final int LOW_DECK = 18, HIGH_DECK = 40;

    private final BlockPos origin;
    private final boolean alongZ;
    private final long seed;
    private final List<Capsule> bones = new ArrayList<>();

    private record Capsule(double u0, double v0, double w0, double u1, double v1, double w1, double r) {}

    public WhalefallPiece(BlockPos origin, boolean alongZ, long seed) {
        super(ModWorldgen.WHALEFALL_PIECE.get(), 0, box(origin, alongZ));
        this.origin = origin;
        this.alongZ = alongZ;
        this.seed = seed;
        this.build();
    }

    public WhalefallPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.WHALEFALL_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.alongZ = tag.getBooleanOr("AlongZ", true);
        this.seed = tag.getLongOr("Seed", 0L);
        this.build();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putBoolean("AlongZ", this.alongZ);
        tag.putLong("Seed", this.seed);
    }

    private static BoundingBox box(BlockPos o, boolean alongZ) {
        int minU = -52, maxU = 52, minW = -150, maxW = 158;
        return alongZ
            ? new BoundingBox(o.getX() + minU, o.getY() - 4, o.getZ() + minW, o.getX() + maxU, o.getY() + 112, o.getZ() + maxW)
            : new BoundingBox(o.getX() + minW, o.getY() - 4, o.getZ() - maxU, o.getX() + maxW, o.getY() + 112, o.getZ() - minU);
    }

    // How high the spine runs at w: from the back of the skull it arches up over the ribcage
    // (highest at w = 0) and runs down to the tail.
    static double spine(double w) {
        if (w < 0) {
            double t = Math.max(-1.0, w / 72.0);
            return 56.0 + 22.0 * (1.0 - t * t);
        }
        return Math.max(4.0, 78.0 * (1.0 - Math.pow(Math.min(1.0, w / 152.0), 1.6)));
    }

    // ------------------------------------------------------------------ the shapes

    private void build() {
        RandomSource rnd = RandomSource.create(this.seed);
        // Vertebrae from the back of the skull to the tail: a disc, a spike up, a short process each side.
        for (double w = -70; w <= 148; w += 7) {
            double v = spine(w);
            double r = w < 90 ? 4.5 : 4.5 - (w - 90) / 20.0;
            this.bones.add(new Capsule(0, v, w - 1.5, 0, v, w + 1.5, Math.max(1.5, r)));
            double spike = w < 100 ? 6 + rnd.nextInt(6) : 3;
            this.bones.add(new Capsule(0, v, w, 0, v + spike, w + 2, 1.2));
            for (int s = -1; s <= 1; s += 2) {
                this.bones.add(new Capsule(0, v, w, s * (r + 3), v - 1, w, 1.0));
            }
        }
        // The flukes of the tail, spread flat.
        for (int s = -1; s <= 1; s += 2) {
            this.bones.add(new Capsule(0, 4, 146, s * 22, 3, 156, 2.2));
            this.bones.add(new Capsule(s * 22, 3, 156, s * 30, 2, 150, 1.8));
        }
        // Ribs: thirteen pairs, each an arch from the spine out and down under the body.
        for (int i = 0; i < 13; i++) {
            double w0 = -60 + i * 10;
            double top = spine(w0) - 2;
            double reach = 34 + 6 * Math.cos(Math.PI * w0 / 120.0);
            for (int s = -1; s <= 1; s += 2) {
                double pu = s * 3, pv = top, pw = w0;
                for (int k = 1; k <= 18; k++) {
                    double t = k / 18.0;
                    double u = s * (3 + (reach - 3) * Math.sin(t * Math.PI / 2) + 6 * Math.sin(t * Math.PI));
                    double v = top * Math.pow(Math.cos(t * Math.PI / 2), 0.9);
                    double w = w0 + 5 * t;
                    this.bones.add(new Capsule(pu, pv, pw, u, v, w, 2.2 - 0.8 * t));
                    pu = u;
                    pv = v;
                    pw = w;
                }
            }
        }
        // The jaw: two long bars under the skull, meeting at the chin.
        for (int s = -1; s <= 1; s += 2) {
            this.bones.add(new Capsule(s * 24, 6, SKULL_W + 20, s * 16, 3, SKULL_W - 30, 2.6));
            this.bones.add(new Capsule(s * 16, 3, SKULL_W - 30, s * 4, 2, SKULL_W - 40, 2.4));
        }
        // Bone pillars for the ladders: two from the bottom to the high deck, one inside the skull.
        for (int s = -1; s <= 1; s += 2) {
            this.bones.add(new Capsule(s * 10, 0, s * 30, s * 10, HIGH_DECK, s * 30, 0.6));
        }
        this.bones.add(new Capsule(0, 1, SKULL_W, 0, SKULL_FLOORS[2] + 1, SKULL_W, 0.6));
    }

    // ------------------------------------------------------------------ placing it

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunk, ChunkPos chunkPos, BlockPos reference) {
        RandomSource rnd = RandomSource.create(this.seed ^ chunkPos.toLong());
        BlockState bone = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState();
        // The bones.
        for (Capsule c : this.bones) {
            int minU = (int) Math.floor(Math.min(c.u0, c.u1) - c.r), maxU = (int) Math.ceil(Math.max(c.u0, c.u1) + c.r);
            int minV = (int) Math.floor(Math.min(c.v0, c.v1) - c.r), maxV = (int) Math.ceil(Math.max(c.v0, c.v1) + c.r);
            int minW = (int) Math.floor(Math.min(c.w0, c.w1) - c.r), maxW = (int) Math.ceil(Math.max(c.w0, c.w1) + c.r);
            if (!this.overlaps(chunk, minU, minV, minW, maxU, maxV, maxW)) {
                continue;
            }
            Direction.Axis axis = this.axisOf(c);
            for (int u = minU; u <= maxU; u++) {
                for (int w = minW; w <= maxW; w++) {
                    BlockPos column = this.world(u, 0, w);
                    if (column.getX() < chunk.minX() || column.getX() > chunk.maxX() || column.getZ() < chunk.minZ() || column.getZ() > chunk.maxZ()) {
                        continue;
                    }
                    for (int v = Math.max(minV, -3); v <= maxV; v++) {
                        if (distance(c, u, v, w) <= c.r) {
                            this.put(level, chunk, u, v, w, bone.setValue(RotatedPillarBlock.AXIS, axis));
                        }
                    }
                }
            }
        }
        this.skull(level, chunk, rnd);
        this.village(level, chunk, rnd);
    }

    // The skull: a shell of bone, hollow inside, with three floors joined by a ladder, the eye
    // sockets glazed with Astral Glass, and the mouth an open doorway.
    private void skull(WorldGenLevel level, BoundingBox chunk, RandomSource rnd) {
        int minU = (int) -SKULL_A - 1, maxU = (int) SKULL_A + 1;
        int minW = (int) (SKULL_W - SKULL_C) - 1, maxW = (int) (SKULL_W + SKULL_C) + 1;
        if (!this.overlaps(chunk, minU, -4, minW, maxU, (int) (SKULL_V + SKULL_B) + 1, maxW)) {
            return;
        }
        BlockState bone = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState();
        BlockState glass = ModBlocks.ASTRAL_GLASS.get().defaultBlockState();
        BlockState planks = ModBlocks.NULLWOOD.planks().get().defaultBlockState();
        for (int u = minU; u <= maxU; u++) {
            for (int w = minW; w <= maxW; w++) {
                if (!this.columnIn(chunk, u, w)) {
                    continue;
                }
                for (int v = -4; v <= SKULL_V + SKULL_B + 1; v++) {
                    double du = u / SKULL_A, dv = (v - SKULL_V) / SKULL_B, dw = (w - SKULL_W) / SKULL_C;
                    double d = du * du + dv * dv + dw * dw;
                    if (d > 1.0) {
                        continue;
                    }
                    boolean eye = false;
                    for (int s = -1; s <= 1; s += 2) {
                        double eu = u - s * 19, ev = v - 44, ew = w - (SKULL_W - 24);
                        if (eu * eu + ev * ev + ew * ew < 49) {
                            eye = true;
                        }
                    }
                    boolean mouth = w < SKULL_W - 28 && v >= 1 && v <= 9 && Math.abs(u) <= 5;
                    if (d >= 0.82) {
                        this.put(level, chunk, u, v, w, mouth ? Blocks.AIR.defaultBlockState() : eye ? glass : bone);
                    } else if (v < 1) {
                        this.put(level, chunk, u, v, w, bone);          // the floor of the skull
                    } else {
                        BlockState inside = Blocks.AIR.defaultBlockState();
                        for (int floor : SKULL_FLOORS) {
                            if (v == floor && d < 0.8 && !(u == 0 && w == (int) SKULL_W - 1)) {
                                inside = planks;
                            }
                        }
                        if (u == 0 && w == (int) SKULL_W - 1 && v <= SKULL_FLOORS[2] + 1) {
                            inside = this.ladder(Direction.NORTH);
                        }
                        if (u != 0 || w != (int) SKULL_W) {
                            this.put(level, chunk, u, v, w, inside);
                        }
                    }
                }
            }
        }
        // Lanterns under each floor, a chest on each.
        for (int floor : SKULL_FLOORS) {
            for (int[] at : new int[][] {{-10, -8}, {10, 8}, {-6, 14}}) {
                this.put(level, chunk, at[0], floor - 1, (int) SKULL_W + at[1],
                    Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            this.chest(level, chunk, rnd, 9, floor + 1, (int) SKULL_W - 6);
        }
    }

    // The village in the ribcage: a low deck and a high deck, huts on both, ladders up bone pillars.
    private void village(WorldGenLevel level, BoundingBox chunk, RandomSource rnd) {
        if (!this.overlaps(chunk, -26, LOW_DECK - 1, -60, 26, HIGH_DECK + 8, 62)) {
            return;
        }
        BlockState planks = ModBlocks.NULLWOOD.planks().get().defaultBlockState();
        BlockState fence = ModBlocks.NULLWOOD.fence().get().defaultBlockState();
        this.deck(level, chunk, LOW_DECK, 24, 58, planks, fence);
        this.deck(level, chunk, HIGH_DECK, 15, 40, planks, fence);
        // Ladders up the pillars, through holes in the decks.
        for (int s = -1; s <= 1; s += 2) {
            for (int v = 1; v <= HIGH_DECK; v++) {
                this.put(level, chunk, s * 10, v, s * 30 - s, this.ladder(s > 0 ? Direction.NORTH : Direction.SOUTH));
            }
        }
        // Huts.
        int[][] huts = {{-14, -45}, {12, -15}, {-12, 15}, {14, 45}};
        for (int[] h : huts) {
            this.hut(level, chunk, rnd, h[0], LOW_DECK + 1, h[1]);
        }
        this.hut(level, chunk, rnd, 0, HIGH_DECK + 1, -20);
        this.hut(level, chunk, rnd, 0, HIGH_DECK + 1, 20);
        // Lanterns hung under the decks.
        for (int w = -54; w <= 54; w += 12) {
            for (int u : new int[] {-16, 0, 16}) {
                this.put(level, chunk, u, LOW_DECK - 1, w, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
    }

    private void deck(WorldGenLevel level, BoundingBox chunk, int v, int halfU, int halfW, BlockState planks, BlockState fence) {
        for (int u = -halfU; u <= halfU; u++) {
            for (int w = -halfW; w <= halfW; w++) {
                boolean hole = (u == 10 && w == 29) || (u == -10 && w == -29);     // where the ladders come up
                this.put(level, chunk, u, v, w, hole ? Blocks.AIR.defaultBlockState() : planks);
                boolean edge = Math.abs(u) == halfU || Math.abs(w) == halfW;
                if (edge && (u + w) % 5 != 0) {
                    this.put(level, chunk, u, v + 1, w, fence);
                }
            }
        }
    }

    private void hut(WorldGenLevel level, BoundingBox chunk, RandomSource rnd, int cu, int v, int cw) {
        BlockState wall = ModBlocks.PALE_END_STONE_BRICKS.get().defaultBlockState();
        BlockState roof = ModBlocks.CHORUS.planks().get().defaultBlockState();
        for (int u = -3; u <= 3; u++) {
            for (int w = -3; w <= 3; w++) {
                for (int h = 0; h <= 5; h++) {
                    boolean shell = Math.abs(u) == 3 || Math.abs(w) == 3;
                    boolean door = Math.abs(w) <= 0 && u == (cu > 0 ? -3 : 3) && h <= 2 && h >= 0;
                    boolean window = shell && h == 2 && (Math.abs(u) == 1 || Math.abs(w) == 1) && !(Math.abs(u) == 3 && Math.abs(w) == 3);
                    BlockState s = h == 5 ? roof : shell ? (door ? Blocks.AIR.defaultBlockState()
                        : window ? ModBlocks.ASTRAL_GLASS.get().defaultBlockState() : wall) : Blocks.AIR.defaultBlockState();
                    this.put(level, chunk, cu + u, v + h, cw + w, s);
                }
            }
        }
        this.put(level, chunk, cu, v + 4, cw, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        this.chest(level, chunk, rnd, cu + (cu > 0 ? 2 : -2), v, cw + 2);
    }

    // ------------------------------------------------------------------ helpers

    private BlockPos world(int u, int v, int w) {
        return this.alongZ
            ? new BlockPos(this.origin.getX() + u, this.origin.getY() + v, this.origin.getZ() + w)
            : new BlockPos(this.origin.getX() + w, this.origin.getY() + v, this.origin.getZ() - u);
    }

    private boolean columnIn(BoundingBox chunk, int u, int w) {
        BlockPos p = this.world(u, 0, w);
        return p.getX() >= chunk.minX() && p.getX() <= chunk.maxX() && p.getZ() >= chunk.minZ() && p.getZ() <= chunk.maxZ();
    }

    private boolean overlaps(BoundingBox chunk, int minU, int minV, int minW, int maxU, int maxV, int maxW) {
        BlockPos a = this.world(minU, minV, minW), b = this.world(maxU, maxV, maxW);
        BoundingBox box = new BoundingBox(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
            Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        return box.intersects(chunk);
    }

    private void put(WorldGenLevel level, BoundingBox chunk, int u, int v, int w, BlockState state) {
        BlockPos p = this.world(u, v, w);
        if (chunk.isInside(p)) {
            level.setBlock(p, state, 2);
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunk, RandomSource rnd, int u, int v, int w) {
        BlockPos p = this.world(u, v, w);
        if (chunk.isInside(p)) {
            this.createChest(level, chunk, rnd, p, LOOT, null);
        }
    }

    private BlockState ladder(Direction localFacing) {
        // Local north is -w: world north when it lies along Z, west when it lies along X.
        Direction facing = this.alongZ ? localFacing : (localFacing == Direction.NORTH ? Direction.WEST
            : localFacing == Direction.SOUTH ? Direction.EAST : localFacing);
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
    }

    private Direction.Axis axisOf(Capsule c) {
        double du = Math.abs(c.u1 - c.u0), dv = Math.abs(c.v1 - c.v0), dw = Math.abs(c.w1 - c.w0);
        if (dv >= du && dv >= dw) {
            return Direction.Axis.Y;
        }
        boolean alongW = dw >= du;
        return this.alongZ == alongW ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static double distance(Capsule c, double u, double v, double w) {
        double ex = c.u1 - c.u0, ey = c.v1 - c.v0, ez = c.w1 - c.w0;
        double len = ex * ex + ey * ey + ez * ez;
        double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, ((u - c.u0) * ex + (v - c.v0) * ey + (w - c.w0) * ez) / len));
        double dx = u - (c.u0 + ex * t), dy = v - (c.v0 + ey * t), dz = w - (c.w0 + ez * t);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
