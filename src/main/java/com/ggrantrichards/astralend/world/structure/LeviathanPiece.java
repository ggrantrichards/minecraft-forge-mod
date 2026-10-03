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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// Builds a Leviathan a chunk at a time: every block is decided from shapes in the skeleton's own frame,
// so each chunk is built on its own and they all agree. The frame: w along the body (the skull toward
// +w), u across it, v up from the origin. It lies diagonally in the world (a turn of 45 + 90k degrees),
// which is what lets a skeleton 320 blocks long fit inside the 8 chunks round its start that a
// structure may reach.
//
// What's there:
//   - the frame, all bone block: a spine of vertebrae (each with its spinous and side processes) on a
//     cord of bone, tapering into the tail; ribs from the shoulders to the hips; the skull, with the
//     eye sockets set with Maw Eyes in rims of crying obsidian; the lower jaw, its teeth, and in it a
//     captured island, chained shut with obsidian;
//   - what was built over it: a hall of purpur along the spine, walled in End Stone bricks with
//     Astral Glass windows and lit with End Rods; four towers rising from the vertebrae; the ribcage closed
//     into a cathedral with Astral Glass between the ribs and a purpur floor; and in the skull the
//     reliquary - a floor broken by gaps into the void, four End Chimes pylons, the egg pedestal, and
//     the Curator, dormant;
//   - its garrison: shulkers on the towers, Void Wanderers in the hall, Hollowbacks in the cathedral.
public class LeviathanPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan"));
    public static final int MIN_U = -26, MAX_U = 26, MIN_V = -46, MAX_V = 82, MIN_W = -162, MAX_W = 166;
    // The skull: an ellipsoid round (0, skull v, SKULL_W), radii SA across, SB up, SC along.
    private static final double SA = 18, SB = 15, SC = 30, SKULL_W = 128, SHELL = 0.82;
    private static final int RIB_FROM = -44, RIB_TO = 86, RIB_STEP = 11;
    private static final double RIB_R = 15;
    private static final int HALL_FROM = -96, HALL_TO = 92;
    private static final int[] TOWERS = {-80, -30, 20, 70};
    private static final int TOWER_H = 26;
    private static final int[][] PYLONS = {{-10, 116}, {10, 116}, {-10, 140}, {10, 140}};
    private static final int[][] CHAINS = {{-7, 126}, {7, 126}, {-6, 152}, {6, 152}};
    private static final int PEDESTAL_W = 150;

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
    private static double spineV(double w) {
        return 40.0 + 5.0 * Math.sin(w / 55.0);
    }

    private static double skullV() {
        return spineV(100) + 2.0;
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
        BlockState skull = w > 96 ? this.skullAt(u, v, w) : null;
        if (skull != null) {
            return skull;
        }
        if (w > 100) {
            return null;
        }
        double sv = spineV(w);
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        // Towers rising from the vertebrae.
        for (int t : TOWERS) {
            double rho = Math.hypot(u, w - t);
            if (rho <= 5.5 && v >= sv + 5 && v <= sv + TOWER_H + 1) {
                return this.towerAt(u, v - sv, w - t, rho);
            }
        }
        // The hall along the spine.
        if (w >= HALL_FROM && w <= HALL_TO && Math.abs(u) <= 4.5 && v >= sv + 5 && v < sv + 12) {
            return this.hallAt(u, v - sv, w);
        }
        // The spine: its cord, the vertebrae and their processes.
        double r = Math.hypot(u, v - sv);
        double vr = 1.5 + 2.5 * Mth.clamp((w + 160) / 100.0, 0.0, 1.0);
        double k = w - 8 * Math.round(w / 8.0);
        if (r <= Math.max(1.0, vr * 0.4)) {
            return bone;
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
        // The ribcage, closed into a cathedral.
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
                double floor = sv - 20;
                if (v >= floor - 0.5 && v < floor + 0.5) {
                    return ModBlocks.PURPUR_BRICKS.get().defaultBlockState();
                }
                return Blocks.AIR.defaultBlockState();
            }
        }
        return null;
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
    // End Rod at its peak.
    private BlockState towerAt(double u, double h, double dw, double rho) {
        if (h >= TOWER_H) {
            if (rho <= 5.5 && h < TOWER_H + 1) {
                return Blocks.PURPUR_SLAB.defaultBlockState();
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

    // The skull, its jaw and the captured island, and the reliquary inside.
    private BlockState skullAt(double u, double v, double w) {
        double sv = skullV();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        double q = Math.sqrt(Math.pow(u / SA, 2) + Math.pow((v - sv) / SB, 2) + Math.pow((w - SKULL_W) / SC, 2));
        double floor = sv - 6;
        boolean gap = this.gap(u, w);
        if (q <= 1.0) {
            // The eye sockets: Maw Eyes in rims of crying obsidian, high on each side.
            for (int side = -1; side <= 1; side += 2) {
                double e = Math.sqrt(Math.pow(u - side * (SA - 1.5), 2) + Math.pow(v - (sv + 4), 2) + Math.pow(w - (SKULL_W + 8), 2));
                if (e <= 1.6) {
                    return ModBlocks.MAW_EYE.get().defaultBlockState();
                }
                if (e <= 3.2 && q >= SHELL) {
                    return Blocks.CRYING_OBSIDIAN.defaultBlockState();
                }
            }
            if (q >= SHELL) {
                return gap && v < floor ? Blocks.AIR.defaultBlockState() : bone;
            }
            if (v >= floor - 0.5 && v < floor + 0.5) {
                return gap ? Blocks.AIR.defaultBlockState() : ModBlocks.PURPUR_BRICKS.get().defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        // The lower jaw: two bars of bone from under the skull to the chin, with teeth.
        for (int side = -1; side <= 1; side += 2) {
            double t = Mth.clamp((w - 108) / 62.0, 0.0, 1.0);
            double ju = side * Mth.lerp(t, 12, 5), jv = sv - Mth.lerp(t, 13, 17);
            if (w >= 104 && w <= 172 && Math.hypot(u - ju, v - jv) <= 2.2) {
                return bone;
            }
            if (w >= 112 && w <= 164 && Math.floorMod(Mth.floor(w), 5) == 0 && Math.abs(u - ju) <= 0.6 && v > jv && v <= jv + 4) {
                return bone;     // a tooth
            }
        }
        if (w >= 166 && w <= 172 && Math.abs(u) <= 5 && Math.abs(v - (sv - 17)) <= 1.5) {
            return bone;         // the chin
        }
        // The captured island, held in the jaw, chained to the skull with obsidian.
        double iq = Math.pow(u / 11.0, 2) + Math.pow((v - (sv - 24)) / 7.0, 2) + Math.pow((w - 140) / 15.0, 2);
        if (iq <= 1.0) {
            return v > sv - 19 ? ModBlocks.PALE_END_STONE.get().defaultBlockState() : Blocks.END_STONE.defaultBlockState();
        }
        return null;
    }

    // Floor gaps in the reliquary: cells 3 blocks square, about one in five open to the void below -
    // never in the middle (where the Curator waits), by the pylons or before the pedestal.
    private boolean gap(double u, double w) {
        int cu = Mth.floor(u / 3.0), cw = Mth.floor((w - SKULL_W) / 3.0);
        if (Math.abs(u) < 5 && Math.abs(w - SKULL_W) < 6 || Math.abs(u) < 4 && w > SKULL_W + 12) {
            return false;
        }
        for (int[] pylon : PYLONS) {
            if (Math.abs(u - pylon[0]) < 3 && Math.abs(w - pylon[1]) < 3) {
                return false;
            }
        }
        return this.hash(cu, cw, 77) < 0.22;
    }


    // ------------------------------------------------------------------ chests and the garrison

    // The small things - columns a block across, lamps, the pedestal - set block by block in the world's
    // own grid. (Lying diagonally, a shape thinner than a block and a half can fall between the blocks.)
    private void furnish(WorldGenLevel level, BoundingBox chunk) {
        RandomSource rnd = RandomSource.create(this.seed);
        double sv = skullV();
        double floor = sv - 6;
        BlockPos centre = this.world(0, floor, SKULL_W);
        // The pylons: an obsidian column, a purpur arm toward the arena's middle, End Chimes under it.
        for (int[] pylon : PYLONS) {
            BlockPos base = this.world(pylon[0], floor + 1, pylon[1]);
            for (int h = 0; h < 6; h++) {
                this.put(level, chunk, base.above(h), h == 5 ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
            }
            int dx = centre.getX() - base.getX(), dz = centre.getZ() - base.getZ();
            BlockPos arm = Math.abs(dx) >= Math.abs(dz) ? base.offset(Integer.signum(dx), 5, 0) : base.offset(0, 5, Integer.signum(dz));
            this.put(level, chunk, arm, Blocks.PURPUR_BLOCK.defaultBlockState());
            this.put(level, chunk, arm.below(), ModBlocks.END_CHIMES.get().defaultBlockState());
        }
        // The egg's pedestal, and the chains that hold the captured island to the skull.
        this.put(level, chunk, this.pedestal(), ModBlocks.LEVIATHAN_PEDESTAL.get().defaultBlockState());
        for (int[] chain : CHAINS) {
            BlockPos top = this.world(chain[0], sv - 10, chain[1]);
            for (int d = 0; d < 11; d++) {
                this.put(level, chunk, top.below(d), Blocks.OBSIDIAN.defaultBlockState());
            }
        }
        // End Rods: down the middle of the hall's roof, and one on top of each tower.
        BlockState rod = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN);
        for (int w = HALL_FROM + 6; w <= HALL_TO - 4; w += 12) {
            this.put(level, chunk, this.world(0, spineV(w) + 10.5, w), rod);
        }
        for (int t : TOWERS) {
            this.put(level, chunk, this.world(0, spineV(t) + TOWER_H + 1.5, t), Blocks.END_ROD.defaultBlockState());
        }
        // Chests: two in the reliquary, one at the foot of each tower, two in the cathedral.
        this.chest(level, chunk, rnd, -4, sv - 5, PEDESTAL_W + 1);
        this.chest(level, chunk, rnd, 4, sv - 5, PEDESTAL_W + 1);
        for (int t : TOWERS) {
            this.chest(level, chunk, rnd, -2, spineV(t) + 6.5, t + 2);
        }
        this.chest(level, chunk, rnd, 6, spineV(0) - 19, 0);
        this.chest(level, chunk, rnd, -6, spineV(40) - 19, 40);
        // The garrison.
        for (int t : TOWERS) {
            this.spawn(level, chunk, EntityTypes.SHULKER, 2, spineV(t) + TOWER_H + 1.5, t + 2, null);
        }
        for (int w : new int[] {-60, 0, 50}) {
            this.spawn(level, chunk, ModEntities.VOID_WANDERER.get(), 0, spineV(w) + 7, w + 3, null);
        }
        for (int w : new int[] {-30, -8, 14, 36}) {
            this.spawn(level, chunk, ModEntities.HOLLOWBACK.get(), w % 2 == 0 ? 5 : -5, spineV(w) - 19, w, null);
        }
        BlockPos home = this.world(0, sv - 5, SKULL_W);
        this.spawn(level, chunk, ModEntities.CURATOR.get(), 0, sv - 5, SKULL_W, home);
    }

    private void put(WorldGenLevel level, BoundingBox chunk, BlockPos p, BlockState state) {
        if (chunk.isInside(p)) {
            level.setBlock(p, state, 2);
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunk, RandomSource rnd, double u, double v, double w) {
        BlockPos p = this.world(u, v, w);
        if (chunk.isInside(p)) {
            this.createChest(level, chunk, rnd, p, LOOT, null);
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
        return this.world(0, skullV() - 5, PEDESTAL_W);
    }
}
