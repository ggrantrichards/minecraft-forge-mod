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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// Builds a Leviathan's body a chunk at a time: every block is decided from shapes in the skeleton's
// own frame, so each chunk is built on its own and they all agree. The frame: w along the body (the head
// toward +w), u across it, v up from the origin. It lies diagonally in the world (a turn of 45 + 90k
// degrees), which is what lets a skeleton 370 blocks long, skull and all, fit inside the 8 chunks round
// its start that a structure may reach. The skull is LeviathanHeadPiece: the Maw.
//
// What's here:
//   - the frame, all bone block: a spine of vertebrae (each with its spinous and side processes) on a
//     cord of bone, tapering into the tail, running into the back of the skull; ribs from the shoulders
//     to the hips;
//   - what lives on in it, dormant: in the ribcage, hung from the spine, its heart - Leviathan Heart in a
//     muscle of crimson, slowly beating, with the great vessels running up into the spine and out to
//     the ribs; two lungs of Breathcomb the length of the cathedral, breathing light; and low at the
//     back, its stomach, a sac of Voidgel with what it last swallowed still inside;
//   - what was built over it: a hall of purpur along the spine, walled in End Stone bricks with Astral
//     Glass windows and lit with End Rods, running into the skull; four towers rising from the
//     vertebrae, the last of them crowned with a Constellation Maze; and the ribcage closed into a
//     cathedral with Astral Glass between the ribs - the reliquary: a purpur floor broken by gaps into
//     the void, four End Chimes pylons, the Curator, dormant, and under the heart the egg's pedestal.
//     A ladder goes down to it from the hall, through the spine;
//   - its garrison: shulkers on the towers, Void Wanderers in the hall, Hollowbacks in the cathedral.
public class LeviathanPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan"));
    private static final ResourceKey<LootTable> STOMACH_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan_stomach"));
    private static final ResourceKey<LootTable> MAZE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/constellation_maze"));
    public static final int MIN_U = -30, MAX_U = 30, MIN_V = 0, MAX_V = 106, MIN_W = -186, MAX_W = 70;
    // The spine runs on into the skull (LeviathanHeadPiece) up to here.
    private static final int SPINE_TO = LeviathanHeadPiece.HEAD_W - 30;
    private static final int RIB_FROM = -96, RIB_TO = 14, RIB_STEP = 11;
    private static final double RIB_R = 15;
    private static final int HALL_FROM = -120, HALL_TO = 56;
    private static final int[] TOWERS = {-104, -62, -20, 22};
    private static final int TOWER_H = 26, MAZE_TOWER = -104, MAZE_H = 32;
    // The reliquary, in the cathedral: its middle (where the Curator waits), the pylons, the pedestal.
    private static final int ARENA_W = -46;
    private static final int[][] PYLONS = {{-10, -58}, {10, -58}, {-10, -34}, {10, -34}};
    private static final int PEDESTAL_W = 2;
    // The ladder down from the hall to the cathedral.
    private static final double SHAFT_U = -2.5;
    private static final int SHAFT_W = -26;
    // The organs: the heart over the pedestal, the lungs high along the cathedral, the stomach at its back.
    private static final double HEART_W = 2, STOMACH_W = -86;
    // The Constellation Maze over the last tower: its platforms (u, height over the tower's top, w) - the
    // first is the tower's own roof - and the dais.
    private static final double[][] MAZE = {{0, 0, MAZE_TOWER}, {-17, 3, -118}, {-4, 6, -136}, {15, 9, -126}, {19, 13, -102}};

    private record Cone(double u0, double v0, double w0, double u1, double v1, double w1, double r) {
        boolean contains(double u, double v, double w) {
            double ex = this.u1 - this.u0, ey = this.v1 - this.v0, ez = this.w1 - this.w0;
            double len = ex * ex + ey * ey + ez * ez;
            double t = len < 1.0E-9 ? 0 : Mth.clamp(((u - this.u0) * ex + (v - this.v0) * ey + (w - this.w0) * ez) / len, 0.0, 1.0);
            double dx = u - (this.u0 + ex * t), dy = v - (this.v0 + ey * t), dz = w - (this.w0 + ez * t);
            return dx * dx + dy * dy + dz * dz <= this.r * this.r;
        }
    }

    // The great vessels: up from the heart into the spine, and out to the ribs.
    private static final Cone[] VESSELS = {
        new Cone(0, spineV(HEART_W) - 6, HEART_W, 0, spineV(HEART_W) - 0.5, HEART_W + 3, 1.4),
        new Cone(1.5, spineV(HEART_W) - 6.5, HEART_W - 1, 2, spineV(HEART_W) - 1, HEART_W - 9, 1.0),
        new Cone(-2.5, spineV(HEART_W) - 9, HEART_W, -12.5, spineV(HEART_W) - 15, HEART_W + 7, 0.9),
        new Cone(2.5, spineV(HEART_W) - 9, HEART_W, 12.5, spineV(HEART_W) - 15, HEART_W + 7, 0.9),
        new Cone(-2, spineV(HEART_W) - 11, HEART_W - 2, -12, spineV(HEART_W) - 8, HEART_W - 14, 0.8),
        new Cone(2, spineV(HEART_W) - 11, HEART_W - 2, 12, spineV(HEART_W) - 8, HEART_W - 14, 0.8),
    };
    // The airways: from each lung's front, together, and on along the spine toward the skull.
    private static final Cone[] AIRWAYS = {
        new Cone(-8, spineV(-26) - 5, -26, -1.5, spineV(-14) - 3, -14, 1.0),
        new Cone(8, spineV(-26) - 5, -26, 1.5, spineV(-14) - 3, -14, 1.0),
    };

    private final BlockPos origin;
    private final int turn;      // 0..3: the frame is turned 45 + 90*turn degrees
    private final long seed;
    private final double cos;
    private final double sin;

    public LeviathanPiece(BlockPos origin, int turn, long seed) {
        super(ModWorldgen.LEVIATHAN_PIECE.get(), 0, box(origin, turn));
        this.origin = origin;
        this.turn = turn;
        this.seed = seed;
        double a = Math.PI / 4 + turn * Math.PI / 2;
        this.cos = Math.cos(a);
        this.sin = Math.sin(a);
    }

    public LeviathanPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.LEVIATHAN_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.turn = tag.getIntOr("Turn", 0);
        this.seed = tag.getLongOr("Seed", 0L);
        double a = Math.PI / 4 + this.turn * Math.PI / 2;
        this.cos = Math.cos(a);
        this.sin = Math.sin(a);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putInt("Turn", this.turn);
        tag.putLong("Seed", this.seed);
    }

    private static BoundingBox box(BlockPos o, int turn) {
        double a = Math.PI / 4 + turn * Math.PI / 2;
        double c = Math.cos(a), s = Math.sin(a);
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int u : new int[] {MIN_U, MAX_U}) {
            for (int w : new int[] {MIN_W, MAX_W}) {
                int x = o.getX() + (int) Math.round(w * c - u * s);
                int z = o.getZ() + (int) Math.round(w * s + u * c);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new BoundingBox(minX - 1, o.getY() + MIN_V, minZ - 1, maxX + 1, o.getY() + MAX_V, maxZ + 1);
    }

    // ------------------------------------------------------------------ the frame

    // The spine's height at w: a long, slow undulation.
    public static double spineV(double w) {
        return 40.0 + 5.0 * Math.sin(w / 55.0);
    }

    private BlockPos world(double u, double v, double w) {
        return new BlockPos(this.origin.getX() + Mth.floor(w * this.cos - u * this.sin + 0.5), this.origin.getY() + Mth.floor(v),
            this.origin.getZ() + Mth.floor(w * this.sin + u * this.cos + 0.5));
    }

    private double hash(int a, int b, int c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L ^ this.seed;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunk, ChunkPos chunkPos, BlockPos reference) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(chunk.minX(), this.boundingBox.minX()); x <= Math.min(chunk.maxX(), this.boundingBox.maxX()); x++) {
            for (int z = Math.max(chunk.minZ(), this.boundingBox.minZ()); z <= Math.min(chunk.maxZ(), this.boundingBox.maxZ()); z++) {
                double dx = x - this.origin.getX(), dz = z - this.origin.getZ();
                double w = dx * this.cos + dz * this.sin;
                double u = -dx * this.sin + dz * this.cos;
                if (u < MIN_U || u > MAX_U || w < MIN_W || w > MAX_W) {
                    continue;
                }
                for (int v = MIN_V; v <= MAX_V; v++) {
                    BlockState state = this.blockAt(u, v + 0.5, w);
                    if (state != null) {
                        p.set(x, this.origin.getY() + v, z);
                        if (chunk.isInside(p)) {
                            level.setBlock(p, state, 2);
                        }
                    }
                }
            }
        }
        this.furnish(level, chunk);
    }

    // What's at (u, v, w), or null to leave it as it is.
    private BlockState blockAt(double u, double v, double w) {
        if (w > SPINE_TO) {
            return null;
        }
        double sv = spineV(w);
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        // Towers rising from the vertebrae.
        for (int t : TOWERS) {
            double rho = Math.hypot(u, w - t);
            int height = t == MAZE_TOWER ? MAZE_H : TOWER_H;
            if (rho <= 5.5 && v >= sv + 5 && v <= sv + height + 1) {
                return this.towerAt(u, v - sv, w - t, rho, height);
            }
        }
        // The hall along the spine, on into the back of the skull.
        if (w >= HALL_FROM && w <= HALL_TO && Math.abs(u) <= 4.5 && v >= sv + 5 && v < sv + 12) {
            return this.hallAt(u, v - sv, w);
        }
        // The spine: its cord, the vertebrae and their processes. Inside the skull, only the cord.
        double r = Math.hypot(u, v - sv);
        double vr = 1.5 + 2.5 * Mth.clamp((w - MIN_W) / 100.0, 0.0, 1.0);
        double k = w - 8 * Math.round(w / 8.0);
        if (r <= Math.max(1.0, vr * 0.4)) {
            return bone;
        }
        if (w > HALL_TO - 6) {
            return null;
        }
        if (Math.abs(k) <= 1.5 && r <= vr) {
            return bone;
        }
        boolean underHall = w >= HALL_FROM && w <= HALL_TO;
        if (Math.abs(k) <= 1.0 && Math.abs(u) <= 1.0 && v >= sv && v <= (underHall ? sv + 5 : sv + vr + 5)) {
            return bone;            // the spinous process (holding the hall's floor up where there is one)
        }
        if (Math.abs(k) <= 1.0 && Math.abs(v - sv) <= 1.0 && Math.abs(u) <= vr + 3) {
            return bone;            // the side processes
        }
        // The ribcage, closed into a cathedral, and what's inside it.
        if (w >= RIB_FROM - 1 && w <= RIB_TO + 1 && v < sv + 2.5 && v > sv - 29) {
            double cv = sv - 13;
            double d = Math.hypot(u, v - cv);
            double kr = w - (RIB_FROM + RIB_STEP * Math.round((w - RIB_FROM) / (double) RIB_STEP));
            if (Math.abs(d - RIB_R) <= 1.2 && Math.abs(kr) <= 1.0) {
                return bone;
            }
            if (Math.abs(d - RIB_R) <= 0.6 && w >= RIB_FROM && w <= RIB_TO) {
                return ModBlocks.ASTRAL_GLASS.get().defaultBlockState();
            }
            if (d < RIB_R - 0.6 && w > RIB_FROM && w < RIB_TO) {
                BlockState organ = this.organAt(u, v, w);
                if (organ != null) {
                    return organ;
                }
                double floor = sv - 20;
                if (v >= floor - 0.5 && v < floor + 0.5) {
                    return this.gap(u, w) ? Blocks.AIR.defaultBlockState() : ModBlocks.PURPUR_BRICKS.get().defaultBlockState();
                }
                return Blocks.AIR.defaultBlockState();
            }
        }
        return null;
    }

    // The organs, or null where there's none.
    private BlockState organAt(double u, double v, double w) {
        // The heart: two lobes of Leviathan Heart in a muscle of crimson, the heart showing through.
        double hv = spineV(HEART_W) - 9;
        double h1 = Math.sqrt(u * u + Math.pow(v - hv, 2) + Math.pow(w - HEART_W, 2));
        double h2 = Math.sqrt(Math.pow(u - 1.5, 2) + Math.pow(v - (hv - 2.2), 2) + Math.pow(w - (HEART_W - 2.5), 2) * 1.2);
        double h = Math.min(h1 / 3.8, h2 / 2.8);
        if (h <= 1.0) {
            boolean core = h1 <= 2.6 || h2 <= 1.8;
            if (core || this.hash(Mth.floor(u), Mth.floor(v), Mth.floor(w)) < 0.22) {
                return ModBlocks.LEVIATHAN_HEART.get().defaultBlockState();
            }
            return Blocks.NETHER_WART_BLOCK.defaultBlockState();
        }
        for (Cone c : VESSELS) {
            if (c.contains(u, v, w)) {
                return Blocks.NETHER_WART_BLOCK.defaultBlockState();
            }
        }
        // The lungs: long and high, one each side, hung from the spine.
        for (int s = -1; s <= 1; s += 2) {
            double sv = spineV(w);
            double q = Math.pow((u - s * 8) / 3.6, 2) + Math.pow((v - (sv - 5)) / 3.6, 2) + Math.pow((w + 50) / 22.0, 2);
            if (q <= 1.0) {
                return ModBlocks.BREATHCOMB.get().defaultBlockState();
            }
        }
        for (Cone c : AIRWAYS) {
            if (c.contains(u, v, w)) {
                return ModBlocks.BREATHCOMB.get().defaultBlockState();
            }
        }
        // The stomach: a sac of Voidgel, resting on the floor, hollow.
        double sv = spineV(STOMACH_W);
        double st = Math.sqrt(Math.pow(u / 5.5, 2) + Math.pow((v - (sv - 15)) / 4.6, 2) + Math.pow((w - STOMACH_W) / 6.5, 2));
        if (st <= 1.0) {
            return st >= 0.8 ? ModBlocks.VOIDGEL.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return null;
    }

    // Floor gaps in the reliquary: cells 3 blocks square, about one in five open to the void below -
    // never in the middle (where the Curator waits), by the pylons, the pedestal, the ladder's foot or
    // under the stomach.
    private boolean gap(double u, double w) {
        if (Math.abs(u) < 5 && Math.abs(w - ARENA_W) < 6 || Math.abs(u) < 5 && w > PEDESTAL_W - 8
                || Math.abs(u - SHAFT_U) < 4 && Math.abs(w - SHAFT_W) < 4 || w < STOMACH_W + 9) {
            return false;
        }
        for (int[] pylon : PYLONS) {
            if (Math.abs(u - pylon[0]) < 3 && Math.abs(w - pylon[1]) < 3) {
                return false;
            }
        }
        return this.hash(Mth.floor(u / 3.0), Mth.floor(w / 3.0), 77) < 0.2;
    }

    // The hall: a purpur floor, End Stone brick walls with Astral Glass windows, a purpur roof hung with End Rods.
    private BlockState hallAt(double u, double h, double w) {
        int iw = Mth.floor(w);
        if (h < 6) {
            return ModBlocks.PURPUR_BRICKS.get().defaultBlockState();
        }
        if (h >= 11) {
            return Blocks.PURPUR_BLOCK.defaultBlockState();
        }
        if (Math.abs(u) > 3.5) {
            boolean window = Math.floorMod(iw, 6) >= 2 && Math.floorMod(iw, 6) <= 3 && h >= 7 && h < 9;
            return window ? ModBlocks.ASTRAL_GLASS.get().defaultBlockState() : Blocks.END_STONE_BRICKS.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    // A tower: a hollow purpur round, a floor every eight blocks, slit windows, a platform on top and an
    // End Rod at its peak (the maze's tower: a solid roof, the maze's first platform).
    private BlockState towerAt(double u, double h, double dw, double rho, int height) {
        if (h >= height) {
            if (rho <= 5.5 && h < height + 1) {
                return height == MAZE_H ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.PURPUR_SLAB.defaultBlockState();
            }
            return rho <= 5.5 ? Blocks.AIR.defaultBlockState() : null;
        }
        if (rho > 4.5) {
            return null;
        }
        if (rho > 3.5) {
            boolean window = Math.floorMod(Mth.floor(h), 8) == 4 && (Math.abs(u) < 0.6 || Math.abs(dw) < 0.6);
            boolean door = h >= 6 && h < 9 && Math.abs(u) < 1.5;   // onto the hall, both ways
            if (door) {
                return Blocks.AIR.defaultBlockState();
            }
            return window ? ModBlocks.ASTRAL_GLASS.get().defaultBlockState() : Blocks.PURPUR_PILLAR.defaultBlockState();
        }
        boolean shaft = Math.abs(u - 2.6) < 0.9 && Math.abs(dw) < 0.9;   // the ladder's shaft, by the wall
        if (h < 6 || Math.floorMod(Mth.floor(h), 8) == 5 && h > 8 && !shaft) {
            return Blocks.PURPUR_BLOCK.defaultBlockState();
        }
        if (shaft && h >= 6) {
            return Blocks.LADDER.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    // ------------------------------------------------------------------ set in the world's grid

    // The small things - columns a block across, lamps, the pedestal, the maze - set block by block in
    // the world's own grid. (Lying diagonally, a shape thinner than a block and a half can fall between
    // the blocks.)
    private void furnish(WorldGenLevel level, BoundingBox chunk) {
        RandomSource rnd = RandomSource.create(this.seed);
        double floor = spineV(ARENA_W) - 20;
        BlockPos centre = this.world(0, floor, ARENA_W);
        // The pylons: an obsidian column, a purpur arm toward the arena's middle, End Chimes under it.
        for (int[] pylon : PYLONS) {
            BlockPos base = this.world(pylon[0], spineV(pylon[1]) - 19, pylon[1]);
            for (int h = 0; h < 6; h++) {
                this.put(level, chunk, base.above(h), h == 5 ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
            }
            int dx = centre.getX() - base.getX(), dz = centre.getZ() - base.getZ();
            BlockPos arm = Math.abs(dx) >= Math.abs(dz) ? base.offset(Integer.signum(dx), 5, 0) : base.offset(0, 5, Integer.signum(dz));
            this.put(level, chunk, arm, Blocks.PURPUR_BLOCK.defaultBlockState());
            this.put(level, chunk, arm.below(), ModBlocks.END_CHIMES.get().defaultBlockState());
        }
        // The egg's pedestal, under the heart.
        this.put(level, chunk, this.pedestal(), ModBlocks.LEVIATHAN_PEDESTAL.get().defaultBlockState());
        // The ladder from the hall down through the spine to the cathedral, on a column of bone.
        BlockPos top = this.world(SHAFT_U, spineV(SHAFT_W) + 5.5, SHAFT_W);
        BlockPos foot = this.world(SHAFT_U, spineV(SHAFT_W) - 19, SHAFT_W);
        Direction wall = this.across(1);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, wall.getOpposite());
        for (int y = foot.getY(); y <= top.getY(); y++) {
            this.put(level, chunk, foot.atY(y).relative(wall), Blocks.BONE_BLOCK.defaultBlockState());
            this.put(level, chunk, foot.atY(y), ladder);
        }
        // End Rods: down the middle of the hall's roof, and one on top of each tower but the maze's.
        BlockState rod = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN);
        for (int w = HALL_FROM + 6; w <= HALL_TO - 4; w += 12) {
            this.put(level, chunk, this.world(0, spineV(w) + 10.5, w), rod);
        }
        for (int t : TOWERS) {
            if (t != MAZE_TOWER) {
                this.put(level, chunk, this.world(0, spineV(t) + TOWER_H + 1.5, t), Blocks.END_ROD.defaultBlockState());
            }
        }
        // Chests: one at the foot of each tower, one by the pedestal, and in the stomach, what it swallowed.
        for (int t : TOWERS) {
            this.chest(level, chunk, rnd, this.world(-2, spineV(t) + 6.5, t + 2), LOOT);
        }
        this.chest(level, chunk, rnd, this.world(4, spineV(PEDESTAL_W) - 19, PEDESTAL_W - 3), LOOT);
        BlockPos gut = this.world(0, spineV(STOMACH_W) - 18.5, STOMACH_W);
        this.chest(level, chunk, rnd, gut, STOMACH_LOOT);
        this.put(level, chunk, gut.offset(2, 0, 1), Blocks.BONE_BLOCK.defaultBlockState());
        this.put(level, chunk, gut.offset(-2, 0, -1), ModBlocks.WHALELIGHT_SCALE.get().defaultBlockState());
        this.put(level, chunk, gut.offset(1, 0, -2), ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState());
        this.maze(level, chunk, rnd);
        // The garrison.
        // (A shulker can't hold on to the top of a slab - it teleports off looking for a full face -
        // so each sits on a purpur block set into the tower's roof.)
        for (int t : TOWERS) {
            if (t == MAZE_TOWER) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                this.put(level, chunk, this.world(2.5 * side, spineV(t) + TOWER_H + 0.5, t + 2), Blocks.PURPUR_BLOCK.defaultBlockState());
                this.spawn(level, chunk, EntityTypes.SHULKER, 2.5 * side, spineV(t) + TOWER_H + 1.5, t + 2, null);
            }
        }
        for (int w : new int[] {-90, -40, 10}) {
            this.spawn(level, chunk, ModEntities.VOID_WANDERER.get(), 0, spineV(w) + 7, w + 3, null);
        }
        for (int w : new int[] {-72, -62, -28, -16}) {
            this.spawn(level, chunk, ModEntities.HOLLOWBACK.get(), w % 4 == 0 ? 5 : -5, spineV(w) - 19, w, null);
        }
        BlockPos home = this.world(0, floor + 1, ARENA_W);
        this.spawn(level, chunk, ModEntities.CURATOR.get(), 0, floor + 1, ARENA_W, home);
    }

    // The Constellation Maze over the last tower: platforms floating out from its roof, joined by
    // star-bridges that show only when their constellation is sighted through a spyglass from the
    // platform before (MazeScoping); the last leads to a dais and its chest.
    private void maze(WorldGenLevel level, BoundingBox chunk, RandomSource rnd) {
        double base = spineV(MAZE_TOWER) + MAZE_H;
        double[][] at = new double[MAZE.length][];
        for (int i = 0; i < MAZE.length; i++) {
            double[] m = MAZE[i];
            at[i] = this.exact(m[0], base + 0.5 + m[1], m[2]);   // (+ 0.5: the block the roof is built in)
            BlockPos c = BlockPos.containing(at[i][0], at[i][1], at[i][2]);
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
                double h = this.hash(c.getX() + dx, c.getY(), c.getZ() + dz);
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

    // A point of the frame in the world, exactly (not rounded to a block).
    private double[] exact(double u, double v, double w) {
        return new double[] {this.origin.getX() + 0.5 + w * this.cos - u * this.sin, this.origin.getY() + v,
            this.origin.getZ() + 0.5 + w * this.sin + u * this.cos};
    }

    // The world direction closest to the frame's +u (s = 1) or -u (s = -1).
    private Direction across(int s) {
        double x = -this.sin * s, z = this.cos * s;
        if (Math.abs(x) > Math.abs(z) + 1.0E-6) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
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
        BlockPos p = this.world(u, v, w);
        if (!chunk.isInside(p)) {
            return;
        }
        var entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (entity == null) {
            return;
        }
        entity.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, (float) (this.turn * 90 + 45), 0.0F);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
        }
        if (entity instanceof Curator curator && home != null) {
            curator.setHome(home);
        }
        level.addFreshEntityWithPassengers(entity);
    }

    // The reliquary's pedestal, in the world (the survey looks for it).
    public BlockPos pedestal() {
        return this.world(0, spineV(PEDESTAL_W) - 19, PEDESTAL_W);
    }

    // Where the Curator waits.
    public BlockPos arena() {
        return this.world(0, spineV(ARENA_W) - 19, ARENA_W);
    }

    // The heart's middle.
    public BlockPos heart() {
        return this.world(0, spineV(HEART_W) - 9, HEART_W);
    }

    // The maze's first platform - the top of its tower - where anyone falling through it is set back.
    public BlockPos mazeStart() {
        double[] p = this.exact(0, spineV(MAZE_TOWER) + MAZE_H, MAZE_TOWER);
        return BlockPos.containing(p[0], p[1], p[2]);
    }
}
