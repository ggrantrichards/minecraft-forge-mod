package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.entity.Curator;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// Builds a Leviathan's body - everything behind the skull - a chunk at a time from its shape
// (LeviathanShape), and furnishes it: the reliquary's pylons, pedestal and Curator, the ladder down to it
// from the hall, End Rods, the towers' chests and shulkers, the stomach's chest, and the Constellation Maze
// on its tower. The skull is LeviathanHeadPiece.
//
// It lies along a compass direction (the frame turned 90 degrees times `turn`), so the skull's square
// lines and the towers stay square in the world's grid; at 200 blocks it fits in the 8 chunks round its
// start that a structure may reach.
public class LeviathanPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan"));
    private static final ResourceKey<LootTable> STOMACH_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan_stomach"));
    private static final ResourceKey<LootTable> MAZE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/constellation_maze"));
    public static final int MIN_U = -30, MAX_U = 30, MIN_V = 0, MAX_V = 112, MIN_W = -124, MAX_W = LeviathanShape.SKULL_W - 2;
    // The Constellation Maze over its tower: its platforms (u, height over the tower's roof, w from the
    // tower) - the first is the tower's own roof - and the dais.
    private static final int[][] MAZE = {{0, 0, 0}, {-17, 3, -14}, {-4, 6, -32}, {15, 9, -22}, {19, 13, 2}};

    private final BlockPos origin;
    private final int turn;      // 0..3: the frame is turned 90 degrees times this
    private final long seed;
    private final LeviathanShape shape;

    public LeviathanPiece(BlockPos origin, int turn, long seed) {
        super(ModWorldgen.LEVIATHAN_PIECE.get(), 0, box(origin, turn, MIN_U, MAX_U, MIN_V, MAX_V, MIN_W, MAX_W));
        this.origin = origin;
        this.turn = turn;
        this.seed = seed;
        this.shape = new LeviathanShape(seed);
    }

    public LeviathanPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.LEVIATHAN_PIECE.get(), tag);
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

    // ------------------------------------------------------------------ the frame (shared with the skull)

    static final int[][] CS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    static BlockPos world(BlockPos o, int turn, double u, double v, double w) {
        int c = CS[turn][0], s = CS[turn][1];
        double dw = w - LeviathanShape.W0;
        return new BlockPos(o.getX() + Mth.floor(dw * c - u * s + 0.5), o.getY() + Mth.floor(v), o.getZ() + Mth.floor(dw * s + u * c + 0.5));
    }

    static BoundingBox box(BlockPos o, int turn, int minU, int maxU, int minV, int maxV, int minW, int maxW) {
        BlockPos a = world(o, turn, minU, minV, minW), b = world(o, turn, maxU, maxV, maxW);
        return new BoundingBox(Math.min(a.getX(), b.getX()), o.getY() + minV, Math.min(a.getZ(), b.getZ()),
            Math.max(a.getX(), b.getX()), o.getY() + maxV, Math.max(a.getZ(), b.getZ()));
    }

    // The world direction of the frame's +u (s = 1) or -u (s = -1).
    static Direction across(int turn, int s) {
        int x = -CS[turn][1] * s, z = CS[turn][0] * s;
        return x > 0 ? Direction.EAST : x < 0 ? Direction.WEST : z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static Direction.Axis along(int turn) {
        return turn % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z;
    }

    // Vanilla's purple stained glass, looked up by name (26.3 doesn't name the dyed blocks in Blocks).
    private static final java.util.function.Supplier<BlockState> PURPLE_GLASS = com.google.common.base.Suppliers.memoize(() ->
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("minecraft", "purple_stained_glass"))
            .map(net.minecraft.world.level.block.Block::defaultBlockState).orElse(ModBlocks.ASTRAL_GLASS.get().defaultBlockState()));

    static BlockState state(LeviathanShape.Material m, int turn) {
        BlockState bone = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState();
        return switch (m) {
            case BONE -> bone;
            case SPINE -> bone.setValue(RotatedPillarBlock.AXIS, along(turn));
            case WEATHERED -> ModBlocks.WEATHERED_LEVIATHAN_BONE.get().defaultBlockState();
            case WEATHERED_SPINE -> ModBlocks.WEATHERED_LEVIATHAN_BONE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, along(turn));
            case LICHENED -> ModBlocks.LICHENED_LEVIATHAN_BONE.get().defaultBlockState();
            case LICHENED_SPINE -> ModBlocks.LICHENED_LEVIATHAN_BONE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, along(turn));
            case PURPUR -> Blocks.PURPUR_BLOCK.defaultBlockState();
            case PURPUR_SLAB -> Blocks.PURPUR_SLAB.defaultBlockState();
            case PURPUR_PILLAR -> Blocks.PURPUR_PILLAR.defaultBlockState();
            case END_BRICK -> Blocks.END_STONE_BRICKS.defaultBlockState();
            case END_BRICK_WALL -> Blocks.END_STONE_BRICK_WALL.defaultBlockState();
            case GLASS -> PURPLE_GLASS.get();
            case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
            case THROAT -> ModBlocks.MAW_THROAT.get().defaultBlockState();
            case FLESH -> ModBlocks.LEVIATHAN_FLESH.get().defaultBlockState();
            case HEART -> ModBlocks.LEVIATHAN_HEART.get().defaultBlockState();
            case BREATHCOMB -> ModBlocks.BREATHCOMB.get().defaultBlockState();
            case VOIDGEL -> ModBlocks.VOIDGEL.get().defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
        };
    }

    // Builds the frame's slice [minW, maxW] of the shape into this chunk.
    static void build(WorldGenLevel level, BoundingBox chunk, BoundingBox box, BlockPos o, int turn, LeviathanShape shape, int minW, int maxW) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int c = CS[turn][0], s = CS[turn][1];
        for (int x = Math.max(chunk.minX(), box.minX()); x <= Math.min(chunk.maxX(), box.maxX()); x++) {
            for (int z = Math.max(chunk.minZ(), box.minZ()); z <= Math.min(chunk.maxZ(), box.maxZ()); z++) {
                int dx = x - o.getX(), dz = z - o.getZ();
                int w = dx * c + dz * s + LeviathanShape.W0;
                int u = -dx * s + dz * c;
                if (w < minW || w > maxW) {
                    continue;
                }
                for (int y = Math.max(chunk.minY(), box.minY()); y <= Math.min(chunk.maxY(), box.maxY()); y++) {
                    LeviathanShape.Material m = shape.block(u, y - o.getY(), w);
                    if (m != null) {
                        level.setBlock(p.set(x, y, z), state(m, turn), 2);
                    }
                }
            }
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunk, ChunkPos chunkPos, BlockPos reference) {
        build(level, chunk, this.boundingBox, this.origin, this.turn, this.shape, MIN_W, MAX_W);
        this.furnish(level, chunk);
    }

    private BlockPos at(double u, double v, double w) {
        return world(this.origin, this.turn, u, v, w);
    }

    // The first open block over a floor that's solid while h < `solidBelow` (h measured from the spine).
    private static int above(double w, double solidBelow) {
        return Mth.ceil(LeviathanShape.sv(w) + solidBelow);
    }

    // ------------------------------------------------------------------ furnishing

    private void furnish(WorldGenLevel level, BoundingBox chunk) {
        RandomSource rnd = RandomSource.create(this.seed);
        int floor = LeviathanShape.floor();
        // The reliquary's pylons: an obsidian column, a purpur arm toward the arena's middle, End Chimes under it.
        BlockPos centre = this.at(0, floor + 1, LeviathanShape.ARENA_W);
        for (int[] pylon : LeviathanShape.PYLONS) {
            BlockPos base = this.at(pylon[0], floor + 1, pylon[1]);
            for (int h = 0; h < 6; h++) {
                this.put(level, chunk, base.above(h), h == 5 ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
            }
            int dx = centre.getX() - base.getX(), dz = centre.getZ() - base.getZ();
            BlockPos arm = Math.abs(dx) >= Math.abs(dz) ? base.offset(Integer.signum(dx), 5, 0) : base.offset(0, 5, Integer.signum(dz));
            this.put(level, chunk, arm, Blocks.PURPUR_BLOCK.defaultBlockState());
            this.put(level, chunk, arm.below(), ModBlocks.END_CHIMES.get().defaultBlockState());
            this.put(level, chunk, base.above(6), Blocks.END_ROD.defaultBlockState());
        }
        // The egg's pedestal, under the heart, and a chest beside it.
        this.put(level, chunk, this.pedestal(), ModBlocks.LEVIATHAN_PEDESTAL.get().defaultBlockState());
        this.chest(level, chunk, rnd, this.at(4, floor + 1, LeviathanShape.PEDESTAL_W - 3), LOOT);
        // The ladder from the hall down to the reliquary, against a column of bone.
        int lw = LeviathanShape.LADDER_W, lu = LeviathanShape.LADDER_U;
        int top = above(lw, 5);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, across(this.turn, 1));
        for (int y = floor + 1; y <= top; y++) {
            if (y < top) {
                this.put(level, chunk, this.at(lu - 1, y, lw), ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState());
            }
            this.put(level, chunk, this.at(lu, y, lw), ladder);
        }
        // End Rods: hanging down the middle of the hall's roof.
        BlockState down = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN);
        for (int w = this.shape.hallFrom() + 4; w <= LeviathanShape.SKULL_W - 2; w += 10) {
            if (!this.inTower(w)) {
                this.put(level, chunk, this.at(0, above(w, 10) - 1, w), down);
            }
        }
        // And standing on the roof's ridge every other bay, so the Leviathan shows from far off in the End's dark.
        for (int w = this.shape.hallFrom(); w <= LeviathanShape.SKULL_W - 2; w += 12) {
            if (!this.inTower(w) && (w < this.shape.collapse || w > this.shape.collapse + 5)) {
                this.put(level, chunk, this.at(0, above(w, 12), w), Blocks.END_ROD.defaultBlockState());
            }
        }
        // The towers: a chest inside each, an End Rod on each crown's cap and two shulkers on each roof (but the maze's).
        for (int t : this.shape.towers()) {
            this.chest(level, chunk, rnd, this.at(-2, above(t, 5), t + 2), LOOT);
            int roof = above(t, this.shape.towerHeight(t)) - 1;
            this.put(level, chunk, this.at(0, roof + 3, t), Blocks.END_ROD.defaultBlockState());
            // (A shulker can't hold on to the top of a slab - it teleports off - so each sits on a purpur block.)
            for (int side = -1; side <= 1; side += 2) {
                this.put(level, chunk, this.at(2 * side, roof, t + 2), Blocks.PURPUR_BLOCK.defaultBlockState());
                this.spawn(level, chunk, EntityTypes.SHULKER, 2 * side, roof + 1, t + 2, null);
            }
        }
        this.chest(level, chunk, rnd, this.at(-2, above(this.shape.mazeTower(), 5), this.shape.mazeTower() + 2), LOOT);
        // The stomach: what it last swallowed, on a lump of Voidgel.
        BlockPos gut = this.at(0, floor + 2, LeviathanShape.STOMACH_W);
        this.put(level, chunk, gut.below(), ModBlocks.VOIDGEL.get().defaultBlockState());
        this.chest(level, chunk, rnd, gut, STOMACH_LOOT);
        this.put(level, chunk, this.at(2, floor + 2, LeviathanShape.STOMACH_W + 1), ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState());
        this.put(level, chunk, this.at(-2, floor + 2, LeviathanShape.STOMACH_W - 1), ModBlocks.WHALELIGHT_SCALE.get().defaultBlockState());
        this.maze(level, chunk, rnd);
        // The garrison: Void Wanderers in the hall, Hollowbacks in the reliquary, the Curator in its arena.
        for (int w : new int[] {-50, -22, 4}) {
            if (w >= this.shape.hallFrom() + 2 && !this.inTower(w)) {
                this.spawn(level, chunk, ModEntities.VOID_WANDERER.get(), 0, above(w, 5), w, null);
            }
        }
        for (int w : new int[] {-40, -30, -14, -9}) {
            this.spawn(level, chunk, ModEntities.HOLLOWBACK.get(), w % 4 == 0 ? 5 : -5, floor + 1, w, null);
        }
        this.spawn(level, chunk, ModEntities.CURATOR.get(), 0, floor + 1, LeviathanShape.ARENA_W, this.arena());
    }

    private boolean inTower(int w) {
        if (Math.abs(w - this.shape.mazeTower()) <= 4) {
            return true;
        }
        for (int t : this.shape.towers()) {
            if (Math.abs(w - t) <= 4) {
                return true;
            }
        }
        return false;
    }

    // The Constellation Maze over its tower: platforms floating out from its roof, joined by star-bridges
    // that show only when their constellation is sighted through a spyglass from the platform before
    // (MazeScoping); the last leads to a dais and its chest.
    private void maze(WorldGenLevel level, BoundingBox chunk, RandomSource rnd) {
        int t = this.shape.mazeTower();
        BlockPos start = this.mazeStart();
        double[][] at = new double[MAZE.length][];
        for (int i = 0; i < MAZE.length; i++) {
            int[] m = MAZE[i];
            BlockPos c = this.at(m[0], start.getY() - this.origin.getY() + m[1], t + m[2]);
            at[i] = new double[] {c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5};
            boolean dais = i == MAZE.length - 1;
            if (i > 0) {
                this.platform(level, chunk, c, dais ? 5.4 : 3.2, dais);
            }
            if (!dais) {
                this.put(level, chunk, c, ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get().defaultBlockState());
                this.put(level, chunk, c.offset(2, 1, 2), ModBlocks.POLISHED_NIGHTSTONE_WALL.get().defaultBlockState());
                this.put(level, chunk, c.offset(2, 2, 2), ModBlocks.NULL_LANTERN.get().defaultBlockState());
            }
            if (i == 0) {
                // A spyglass left at the start for whoever came without one.
                BlockPos barrel = c.offset(-2, 1, -2);
                this.put(level, chunk, barrel, Blocks.BARREL.defaultBlockState());
                if (chunk.isInside(barrel) && level.getBlockEntity(barrel) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity b) {
                    b.setItem(13, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPYGLASS));
                }
            }
            if (dais) {
                for (int[] o : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                    for (int k = 1; k <= 3; k++) {
                        this.put(level, chunk, c.offset(o[0], k, o[1]), ModBlocks.OBSERVATORY_QUARTZ_PILLAR.get().defaultBlockState());
                    }
                    this.put(level, chunk, c.offset(o[0], 4, o[1]), ModBlocks.NULL_LANTERN.get().defaultBlockState());
                }
                this.chest(level, chunk, rnd, c.above(), MAZE_LOOT);
            }
        }
        // The bridges, each answering to one constellation (the one sighted from the platform it starts at).
        for (int i = 0; i + 1 < MAZE.length; i++) {
            double r0 = i == 0 ? 5.6 : 3.6, r1 = i + 2 == MAZE.length ? 5.8 : 3.6;
            double[] a = at[i], b = at[i + 1];
            double dx = b[0] - a[0], dz = b[2] - a[2], len = Math.hypot(dx, dz);
            double ux = dx / len, uz = dz / len;
            double[] from = {a[0] + ux * r0, a[1], a[2] + uz * r0};
            double[] to = {b[0] - ux * r1, b[1], b[2] - uz * r1};
            double bend = i % 2 == 0 ? 5 : -5;
            double[] mid = {(from[0] + to[0]) / 2 - uz * bend, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2 + ux * bend};
            BlockState bridge = ModBlocks.STAR_BRIDGE.get().defaultBlockState()
                .setValue(com.ggrantrichards.astralend.maze.StarBridgeBlock.CONSTELLATION, i)
                .setValue(com.ggrantrichards.astralend.maze.StarBridgeBlock.LIT, false);
            for (double[][] leg : new double[][][] {{from, mid}, {mid, to}}) {
                double[] p = leg[0], q = leg[1];
                int steps = (int) (Math.max(Math.abs(q[0] - p[0]), Math.abs(q[2] - p[2])) * 2) + 1;
                for (int k = 0; k <= steps; k++) {
                    double f = k / (double) steps;
                    BlockPos s = BlockPos.containing(p[0] + (q[0] - p[0]) * f, p[1] + (q[1] - p[1]) * f, p[2] + (q[2] - p[2]) * f);
                    for (BlockPos o : new BlockPos[] {s, s.east(), s.south(), s.east().south()}) {
                        if (chunk.isInside(o) && level.getBlockState(o).isAir()) {
                            level.setBlock(o, bridge, 2);
                        }
                    }
                }
            }
        }
    }

    // A broken platform floating in the void: polished stone on a rough cone of Nightstone (the dais:
    // Observatory Quartz with a ring of stars).
    private void platform(WorldGenLevel level, BoundingBox chunk, BlockPos c, double radius, boolean dais) {
        int r = (int) Math.ceil(radius) + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > radius + 0.4) {
                    continue;
                }
                boolean edge = d > radius - 0.9;
                double h = this.shape.hash(c.getX() + dx, c.getY(), c.getZ() + dz);
                if (edge && h < 0.25) {
                    continue;                                  // a chipped edge
                }
                BlockState top = dais
                    ? (d > 2.6 && d < 3.4 && (dx + dz) % 2 == 0 ? ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get() : ModBlocks.OBSERVATORY_QUARTZ.get()).defaultBlockState()
                    : (edge ? ModBlocks.NIGHTSTONE_BRICKS.get() : ModBlocks.POLISHED_NIGHTSTONE.get()).defaultBlockState();
                this.put(level, chunk, c.offset(dx, 0, dz), top);
                for (int dy = 1; dy < 5; dy++) {
                    if (d <= radius * (1 - dy / 4.5) + (h - 0.6) * 0.8) {
                        this.put(level, chunk, c.offset(dx, -dy, dz), ModBlocks.NIGHTSTONE.get().defaultBlockState());
                    }
                }
            }
        }
    }

    private void put(WorldGenLevel level, BoundingBox chunk, BlockPos p, BlockState state) {
        if (chunk.isInside(p)) {
            level.setBlock(p, state, 2);
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunk, RandomSource rnd, BlockPos p, ResourceKey<LootTable> loot) {
        if (chunk.isInside(p)) {
            this.createChest(level, chunk, rnd, p, loot, null);
        }
    }

    private void spawn(WorldGenLevel level, BoundingBox chunk, EntityType<?> type, double u, double v, double w, BlockPos home) {
        BlockPos p = this.at(u, v, w);
        if (!chunk.isInside(p)) {
            return;
        }
        var entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (entity == null) {
            return;
        }
        entity.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, (float) (this.turn * 90 - 90), 0.0F);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
        }
        if (entity instanceof Curator curator && home != null) {
            curator.setHome(home);
        }
        level.addFreshEntityWithPassengers(entity);
    }

    // ------------------------------------------------------------------ where things are (for the survey and the mechanics)

    // The reliquary's pedestal.
    public BlockPos pedestal() {
        return this.at(0, LeviathanShape.floor() + 1, LeviathanShape.PEDESTAL_W);
    }

    // Where the Curator waits.
    public BlockPos arena() {
        return this.at(0, LeviathanShape.floor() + 1, LeviathanShape.ARENA_W);
    }

    // The heart's middle.
    public BlockPos heart() {
        return this.at(0, Math.round(LeviathanShape.sv(LeviathanShape.HEART_W) - 8), LeviathanShape.HEART_W);
    }

    // The maze's first platform - the middle of its tower's roof - where anyone falling through it is set back.
    public BlockPos mazeStart() {
        int t = this.shape.mazeTower();
        return this.at(0, above(t, LeviathanShape.MAZE_HEIGHT) - 1, t);
    }

    // The frame: for the screenshots.
    public BlockPos frame(double u, double v, double w) {
        return this.at(u, v, w);
    }

    public int turn() {
        return this.turn;
    }
}
