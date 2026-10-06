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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// The Leviathan's skull - the Maw. Built a chunk at a time like the body (LeviathanPiece), in the same
// diagonal frame, every block decided from shapes so each chunk agrees with the rest.
//
// The skull's own frame: w along it (the mouth toward +w), u across, v up from the bottom of the
// cranium. Its w = 0 sits at the body's w = HEAD_W and its v = 0 at the body's v = HEAD_V, so the
// spine runs into the hole at the back of the cranium. The cranium is a shell of bone 60 across, 56
// high and 68 long; the snout and the open lower jaw run 80 blocks on past it, with teeth as long as a
// tree is tall. Inside the cranium is the hall where its brain was: a floor, two galleries round the
// walls joined by ladders, a colonnade of bone, lights hanging from the vault and the camp of whoever
// got here first. You come in from the citadel's hall, through the hole where the spine goes in, or by
// the four eye sockets high on its sides. Its face: heavy brows over the sockets, cheekbones flaring
// back under them, a crest down the crown, two nostrils near the tip of the snout. The throat - a
// great disc of dark stone - closes the back of the mouth; light the four eyes and it wakes into a
// wormhole (MawNetwork), one stop on the road between every Leviathan you've woken.
public class LeviathanHeadPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/leviathan_skull"));
    // Where the skull sits on the body (see LeviathanPiece): its w = 0 at body w = HEAD_W.
    public static final int HEAD_W = 98;
    // The cranium: an ellipsoid round (0, CV, CW), radii A across, B up, C along; its wall is the outer
    // 13% of that.
    private static final double CV = 30, CW = -22, A = 30, B = 28, C = 34, SHELL = 0.87;
    private static final int FLOOR = 4, LOW_GALLERY = 20, HIGH_GALLERY = 36;
    private static final int EYE_V = 39;
    private static final int[] EYE_W = {-17, -27};
    private static final int THROAT_V = 16, THROAT_W = 6;
    private static final double THROAT_R = 9.0;
    private static final int MIN_U = -37, MAX_U = 37, MIN_V = 0, MAX_V = 64, MIN_W = -58, MAX_W = 86;

    private final BlockPos origin;     // the body's origin (LeviathanPiece)
    private final int turn;
    private final long seed;
    private final double cos;
    private final double sin;
    private final double[] phase = new double[6];
    private final List<Cone> cones = new ArrayList<>();

    // A bone between two points, its radius going from r0 to r1.
    private record Cone(double u0, double v0, double w0, double u1, double v1, double w1, double r0, double r1) {
        double reach() {
            return Math.max(this.r0, this.r1);
        }
    }

    public LeviathanHeadPiece(BlockPos origin, int turn, long seed) {
        super(ModWorldgen.LEVIATHAN_HEAD_PIECE.get(), 0, box(origin, turn));
        this.origin = origin;
        this.turn = turn;
        this.seed = seed;
        double a = Math.PI / 4 + turn * Math.PI / 2;
        this.cos = Math.cos(a);
        this.sin = Math.sin(a);
        this.build();
    }

    public LeviathanHeadPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.LEVIATHAN_HEAD_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.turn = tag.getIntOr("Turn", 0);
        this.seed = tag.getLongOr("Seed", 0L);
        double a = Math.PI / 4 + this.turn * Math.PI / 2;
        this.cos = Math.cos(a);
        this.sin = Math.sin(a);
        this.build();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putInt("Turn", this.turn);
        tag.putLong("Seed", this.seed);
    }

    // The skull's v = 0, in the body's frame: level with the spine where it goes into the skull.
    public static int headV() {
        return Mth.floor(LeviathanPiece.spineV(HEAD_W - 56));
    }

    private static BoundingBox box(BlockPos o, int turn) {
        double a = Math.PI / 4 + turn * Math.PI / 2;
        double c = Math.cos(a), s = Math.sin(a);
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int u : new int[] {MIN_U, MAX_U}) {
            for (int w : new int[] {MIN_W + HEAD_W, MAX_W + HEAD_W}) {
                int x = o.getX() + (int) Math.round(w * c - u * s);
                int z = o.getZ() + (int) Math.round(w * s + u * c);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        int y = o.getY() + headV();
        return new BoundingBox(minX - 1, y + MIN_V, minZ - 1, maxX + 1, y + MAX_V, maxZ + 1);
    }

    // ------------------------------------------------------------------ the shapes

    private void build() {
        RandomSource rnd = RandomSource.create(this.seed ^ 0x5EEDL);
        for (int i = 0; i < this.phase.length; i++) {
            this.phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        // The lower jaw: two great bars from the hinges under the cranium to the chin, hanging open.
        for (int s = -1; s <= 1; s += 2) {
            this.cones.add(new Cone(s * 25, 8, 4, s * 11, 5, 74, 3.8, 3.2));
            this.cones.add(new Cone(s * 11, 5, 74, 0, 5, 80, 3.2, 3.0));
            // Its teeth, rising from the bars: staggered against the upper ones.
            for (int w = 12; w <= 70; w += 5) {
                double half = jawHalf(w);
                double len = 4 + rnd.nextInt(4) + (w > 58 && w < 66 ? 5 : 0);
                this.cones.add(new Cone(s * half, 8, w, s * (half - 0.6), 8 + len, w + 0.8, 1.6, 0.4));
            }
            // The upper teeth, hanging from the rim of the snout; the longest near the front, fangs.
            for (int w = 14; w <= 76; w += 5) {
                double f = snoutF(w);
                double half = snoutHalf(w) - 1.6;
                double rim = snoutTop(w) - 14 + 5 * f;
                double len = 6 + rnd.nextInt(4) + (w >= 62 && w <= 68 ? 7 : 0);
                this.cones.add(new Cone(s * half, rim + 1, w + 2.5, s * (half - 0.8), rim - len, w + 3.3, 1.8, 0.4));
            }
            // Brows over the eye sockets.
            this.cones.add(new Cone(s * 28.5, EYE_V + 5.5, EYE_W[1] - 6, s * 28.5, EYE_V + 5.5, EYE_W[0] + 6, 2.3, 2.3));
            // Cheekbones, flaring back from the snout under the eyes.
            this.cones.add(new Cone(s * 22, 24, 10, s * 30, 26, -12, 3.0, 2.2));
        }
        // A crest along the top of the cranium.
        this.cones.add(new Cone(0, CV + B - 1, CW - 22, 0, CV + B + 1, CW + 6, 2.4, 1.6));
        // The colonnade down the hall: bone pillars from the floor to the vault.
        for (int s = -1; s <= 1; s += 2) {
            for (int w = -44; w <= 0; w += 11) {
                double top = innerCeiling(s * 11, w);
                this.cones.add(new Cone(s * 11, FLOOR, w, s * 11, top + 1, w, 1.4, 1.1));
            }
        }
    }

    static double jawHalf(double w) {
        return 25 - 14 * Math.max(0, Math.min(1, (w - 4) / 70.0));
    }

    static double snoutF(double w) {
        return Math.max(0, Math.min(1, (w - 6) / 70.0));
    }

    static double snoutTop(double w) {
        return 46 - 24 * snoutF(w);
    }

    static double snoutHalf(double w) {
        double half = 24 - 11 * snoutF(w);
        if (w > 74) {
            half *= Math.sqrt(Math.max(0, 1 - Math.pow((w - 74) / 8.0, 2)));
        }
        return half;
    }

    // How high the inside of the vault is above (u, w).
    static double innerCeiling(double u, double w) {
        double r = SHELL * SHELL - Math.pow(u / A, 2) - Math.pow((w - CW) / C, 2);
        return r <= 0 ? CV : CV + B * Math.sqrt(r);
    }

    // A fixed pseudo-random number in [0, 1) for a block: the same whichever chunk asks.
    private double hash(int u, int v, int w) {
        long h = this.seed ^ (u * 73856093L) ^ (v * 19349663L) ^ (w * 83492791L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    // ------------------------------------------------------------------ placing it

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunk, ChunkPos chunkPos, BlockPos reference) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int y0 = this.origin.getY() + headV();
        for (int x = Math.max(chunk.minX(), this.boundingBox.minX()); x <= Math.min(chunk.maxX(), this.boundingBox.maxX()); x++) {
            for (int z = Math.max(chunk.minZ(), this.boundingBox.minZ()); z <= Math.min(chunk.maxZ(), this.boundingBox.maxZ()); z++) {
                double dx = x - this.origin.getX(), dz = z - this.origin.getZ();
                int w = (int) Math.round(dx * this.cos + dz * this.sin) - HEAD_W;
                int u = (int) Math.round(-dx * this.sin + dz * this.cos);
                if (u < MIN_U || u > MAX_U || w < MIN_W || w > MAX_W) {
                    continue;
                }
                List<Cone> near = new ArrayList<>();
                for (Cone c : this.cones) {
                    double r = c.reach() + 1;
                    if (u >= Math.min(c.u0, c.u1) - r && u <= Math.max(c.u0, c.u1) + r && w >= Math.min(c.w0, c.w1) - r && w <= Math.max(c.w0, c.w1) + r) {
                        near.add(c);
                    }
                }
                for (int v = MIN_V; v <= MAX_V; v++) {
                    BlockState state = this.blockAt(u, v, w, near);
                    if (state != null) {
                        p.set(x, y0 + v, z);
                        if (chunk.isInside(p)) {
                            level.setBlock(p, state, 2);
                        }
                    }
                }
            }
        }
        this.furnish(level, chunk);
    }

    // What's at (u, v, w), or null for "leave it as it is".
    private BlockState blockAt(int u, int v, int w, List<Cone> near) {
        BlockState bone = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState result = null;
        // The cranium: its wall, and the hall inside.
        double e = Math.sqrt(Math.pow(u / A, 2) + Math.pow((v - CV) / B, 2) + Math.pow((w - CW) / C, 2));
        if (e <= 1.0) {
            if (e >= SHELL) {
                double h = this.hash(u, v, w);
                if (e > 0.985 && h < 0.03) {
                    result = air;                                  // weathered pits in the surface
                } else if (e > 0.96 && h < 0.02) {
                    result = ModBlocks.WHALELIGHT_SCALE.get().defaultBlockState();
                } else {
                    result = bone;
                }
            } else if (v <= FLOOR) {
                result = bone;
            } else {
                result = air;
                double ring = Math.sqrt(Math.pow(u / A, 2) + Math.pow((w - CW) / C, 2));
                for (int g : new int[] {LOW_GALLERY, HIGH_GALLERY}) {
                    double inner = Math.sqrt(Math.max(0, SHELL * SHELL - Math.pow((g - CV) / B, 2)));
                    if (v == g && ring >= inner - 0.2) {
                        result = bone;
                    } else if (v == g + 1 && ring >= inner - 0.2 && ring < inner - 0.165 && (u + w) % 6 != 0) {
                        result = ModBlocks.NULLWOOD.fence().get().defaultBlockState();
                    }
                }
            }
        }
        // The snout: a long roof of bone curving down at the sides into the cheeks.
        if (w >= 6 && w <= 82) {
            double half = snoutHalf(w);
            if (half > 0 && Math.abs(u) <= half) {
                double f = snoutF(w);
                double top = snoutTop(w) - 3 * Math.pow(u / half, 2);
                boolean roof = v <= top && v >= top - 3;
                boolean cheek = Math.abs(u) >= half - 3 && v <= top && v >= snoutTop(w) - 14 + 5 * f;
                if (roof || cheek) {
                    result = bone;
                } else if (v > 5 && v < top - 3 && Math.abs(u) < half - 3) {
                    result = air;                                  // the mouth
                }
            }
        }
        // The jaw's floor, between its bars: the bed of the mouth.
        if (w >= 6 && w <= 76 && v >= 3 && v <= 5 && Math.abs(u) < jawHalf(w)) {
            result = bone;
        }
        // Bones: jaw, teeth, brows, cheekbones, crest, pillars.
        for (Cone c : near) {
            if (this.inside(c, u, v, w)) {
                result = bone.setValue(RotatedPillarBlock.AXIS, this.coneAxis(c));
            }
        }
        // The eye sockets: great holes through the wall, floored, rimmed in obsidian so they read as
        // sockets from far off.
        for (int ew : EYE_W) {
            double d = Math.sqrt(Math.pow(v - EYE_V, 2) + Math.pow(w - ew, 2));
            if (Math.abs(u) >= 20 && Math.abs(u) <= 32) {
                if (d < 5.2) {
                    result = v <= HIGH_GALLERY ? bone : air;
                } else if (d < 6.4 && e >= SHELL && e <= 1.0) {
                    result = Blocks.OBSIDIAN.defaultBlockState();
                }
            }
        }
        // Two nostrils on top of the snout, near its tip.
        for (int s = -1; s <= 1; s += 2) {
            double d = Math.sqrt(Math.pow(u - s * 5, 2) + Math.pow(w - 70, 2) * 0.6);
            if (w > 60 && w < 80 && v > snoutTop(w) - 6 && v <= snoutTop(w) + 1) {
                if (d < 2.3) {
                    result = air;
                } else if (d < 3.2 && result != null && result.is(ModBlocks.LEVIATHAN_BONE_BLOCK.get())) {
                    result = Blocks.OBSIDIAN.defaultBlockState();
                }
            }
        }
        // The way in at the back, where the spine goes into the skull.
        if (w < CW - 18 && v > FLOOR && u * u + Math.pow(v - 10, 2) < 49 && e <= 1.0) {
            result = air;
        }
        // A crack down one side.
        if (u > 0 && e >= SHELL && e <= 1.0 && v >= 6 && v <= 24 && Math.abs(w + 34 + 3 * Math.sin(v * 0.7)) < 1.2) {
            result = air;
        }
        // The throat: a disc of dark stone in a wall of bone that closes the back of the mouth.
        double t = u * u + Math.pow(v - THROAT_V, 2);
        if (w >= THROAT_W - 1 && w <= THROAT_W + 2 && v >= 5 && (e <= 1.0 || t <= 11.5 * 11.5)) {
            if ((w == THROAT_W || w == THROAT_W + 1) && t <= THROAT_R * THROAT_R) {
                result = ModBlocks.MAW_THROAT.get().defaultBlockState();
            } else if (t > THROAT_R * THROAT_R) {
                result = bone;
            }
        }
        if (w >= THROAT_W - 4 && w < THROAT_W && t <= THROAT_R * THROAT_R && v > FLOOR) {
            result = air;                                          // the hall reaches the throat
        }
        return result;
    }

    // Everything set block by block in the world's grid: eyes, ladders, lights, the camp.
    private void furnish(WorldGenLevel level, BoundingBox chunk) {
        RandomSource rnd = RandomSource.create(this.seed ^ 0xCA4FL);
        // The eyes, set in the sockets, looking out.
        for (BlockPos eye : this.eyes()) {
            int s = this.side(eye);
            this.put(level, chunk, eye, ModBlocks.MAW_EYE.get().defaultBlockState()
                .setValue(MawEyeBlock.FACING, this.across(s)).setValue(MawEyeBlock.LIT, false));
        }
        // Ladders: from the floor to the low gallery, and on to the high one, each on a post of bone.
        for (int s = -1; s <= 1; s += 2) {
            Direction out = this.across(s);
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, out.getOpposite());
            BlockPos low = this.world(s * 18, 0, (int) CW);
            for (int v = FLOOR + 1; v <= LOW_GALLERY + 1; v++) {
                this.put(level, chunk, low.atY(this.y(v)).relative(out), ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState());
                this.put(level, chunk, low.atY(this.y(v)), ladder);
            }
            BlockPos high = this.world(s * 20, 0, (int) CW + 8);
            for (int v = LOW_GALLERY + 1; v <= HIGH_GALLERY + 1; v++) {
                this.put(level, chunk, high.atY(this.y(v)).relative(out), ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState());
                this.put(level, chunk, high.atY(this.y(v)), ladder);
            }
        }
        // Light: Whalelight hanging from the vault, lanterns along the galleries.
        for (int u = -14; u <= 14; u += 14) {
            for (int w = -46; w <= 2; w += 12) {
                int top = (int) Math.floor(innerCeiling(u, w));
                int len = 2 + (int) (this.hash(u, 0, w) * 4);
                for (int k = 0; k < len; k++) {
                    this.put(level, chunk, this.world(u, top - k, w), ModBlocks.WHALELIGHT_SCALE.get().defaultBlockState());
                }
            }
        }
        BlockState lantern = ModBlocks.NULL_LANTERN.get().defaultBlockState();
        for (int g : new int[] {LOW_GALLERY, HIGH_GALLERY}) {
            double inner = Math.sqrt(SHELL * SHELL - Math.pow((g - CV) / B, 2));
            for (int k = 0; k < 10; k++) {
                double ang = k * Math.PI / 5 + 0.3;
                int u = (int) Math.round(Math.cos(ang) * A * (inner - 0.07));
                int w = (int) Math.round(CW + Math.sin(ang) * C * (inner - 0.07));
                this.put(level, chunk, this.world(u, g + 1, w), lantern);
            }
        }
        // The camp: a ring of slabs round a lantern, chests, a table for charts.
        int cw = (int) CW - 12;
        BlockPos fire = this.world(0, FLOOR + 1, cw);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r >= 2.5 && r < 3.5) {
                    this.put(level, chunk, fire.offset(dx, 0, dz), ModBlocks.PALE_END_STONE_BRICK_SLAB.get().defaultBlockState());
                }
            }
        }
        this.put(level, chunk, fire, ModBlocks.NULL_LANTERN.get().defaultBlockState());   // their light, left burning
        this.chest(level, chunk, rnd, this.world(-5, FLOOR + 1, cw - 3));
        this.chest(level, chunk, rnd, this.world(6, FLOOR + 1, cw + 4));
        this.put(level, chunk, this.world(5, FLOOR + 1, cw - 4), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        this.put(level, chunk, this.world(-6, FLOOR + 1, cw + 3), Blocks.BARREL.defaultBlockState());
        this.put(level, chunk, this.world(-6, FLOOR + 2, cw + 3), lantern);
        // And a chest high up, by the eyes.
        this.chest(level, chunk, rnd, this.world(22, HIGH_GALLERY + 1, EYE_W[1] - 4));
    }

    // ------------------------------------------------------------------ helpers

    private boolean inside(Cone c, double u, double v, double w) {
        double ex = c.u1 - c.u0, ey = c.v1 - c.v0, ez = c.w1 - c.w0;
        double len = ex * ex + ey * ey + ez * ez;
        double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, ((u - c.u0) * ex + (v - c.v0) * ey + (w - c.w0) * ez) / len));
        double dx = u - (c.u0 + ex * t), dy = v - (c.v0 + ey * t), dz = w - (c.w0 + ez * t);
        double r = c.r0 + (c.r1 - c.r0) * t;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    private Direction.Axis coneAxis(Cone c) {
        double du = Math.abs(c.u1 - c.u0), dv = Math.abs(c.v1 - c.v0), dw = Math.abs(c.w1 - c.w0);
        if (dv >= du && dv >= dw) {
            return Direction.Axis.Y;
        }
        return (this.turn % 2 == 0) == (dw >= du) ? Direction.Axis.X : Direction.Axis.Z;
    }

    // The world direction closest to the skull's +u (s = 1) or -u (s = -1): it lies diagonally, so
    // either of two would do; one is picked the same way everywhere.
    private Direction across(int s) {
        double x = -this.sin * s, z = this.cos * s;
        if (Math.abs(x) > Math.abs(z) + 1.0E-6) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private int side(BlockPos p) {
        double dx = p.getX() - this.origin.getX(), dz = p.getZ() - this.origin.getZ();
        return -dx * this.sin + dz * this.cos >= 0 ? 1 : -1;
    }

    private int y(int v) {
        return this.origin.getY() + headV() + v;
    }

    private BlockPos world(int u, int v, int w) {
        double bw = w + HEAD_W;
        return new BlockPos(this.origin.getX() + Mth.floor(bw * this.cos - u * this.sin + 0.5), this.y(v),
            this.origin.getZ() + Mth.floor(bw * this.sin + u * this.cos + 0.5));
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
        for (int s = -1; s <= 1; s += 2) {
            for (int ew : EYE_W) {
                out.add(this.world(s * 27, EYE_V, ew));
            }
        }
        return out;
    }

    // The middle of the throat, in the world (the survey looks for it).
    public BlockPos throat() {
        return this.world(0, THROAT_V, THROAT_W);
    }

    // The middle of the hall's floor, in the world (for the survey's photographs).
    public BlockPos hall() {
        return this.world(0, FLOOR + 1, (int) CW);
    }
}
