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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// Builds a Rift Maw a chunk at a time. Every block is decided from shapes - the island, the cranium,
// the snout and jaw, the teeth - so each chunk can be built on its own and they all agree.
//
// The skull lies along w (the mouth toward +w), u across it, v up from the island's top. The cranium
// is a shell of bone 60 across, 56 high and 68 long; the snout and the fallen lower jaw run 80 blocks
// on past it, out over the island's edge, with teeth as long as a tree is tall. Inside the cranium is
// the hall where its brain was: a floor, two galleries round the walls joined by ladders, a colonnade
// of bone, lights hanging from the vault and the camp of whoever got here first. You get in at the
// back, through the hole where the spine went, or by the four eye sockets high on its sides. The
// throat - a great disc of dark stone - closes the back of the mouth; its eyes wake it (MawNetwork).
public class RiftMawPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/rift_maw"));
    // The cranium: an ellipsoid round (0, CV, CW), radii A across, B up, C along; its wall is the outer
    // 13% of that.
    private static final double CV = 30, CW = -22, A = 30, B = 28, C = 34, SHELL = 0.87;
    private static final int FLOOR = 4, LOW_GALLERY = 20, HIGH_GALLERY = 36;
    private static final int EYE_V = 39;
    private static final int[] EYE_W = {-17, -27};
    private static final int THROAT_V = 16, THROAT_W = 6;
    private static final double THROAT_R = 9.0;

    private final BlockPos origin;
    private final boolean alongZ;
    private final long seed;
    private final double[] phase = new double[6];
    private final List<Cone> cones = new ArrayList<>();

    // A bone between two points, its radius going from r0 to r1.
    private record Cone(double u0, double v0, double w0, double u1, double v1, double w1, double r0, double r1) {
        double reach() {
            return Math.max(this.r0, this.r1);
        }
    }

    public RiftMawPiece(BlockPos origin, boolean alongZ, long seed) {
        super(ModWorldgen.RIFT_MAW_PIECE.get(), 0, box(origin, alongZ));
        this.origin = origin;
        this.alongZ = alongZ;
        this.seed = seed;
        this.build();
    }

    public RiftMawPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.RIFT_MAW_PIECE.get(), tag);
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

    private static final int MIN_U = -50, MAX_U = 50, MIN_V = -42, MAX_V = 64, MIN_W = -82, MAX_W = 90;

    private static BoundingBox box(BlockPos o, boolean alongZ) {
        return alongZ
            ? new BoundingBox(o.getX() + MIN_U, o.getY() + MIN_V, o.getZ() + MIN_W, o.getX() + MAX_U, o.getY() + MAX_V, o.getZ() + MAX_W)
            : new BoundingBox(o.getX() + MIN_W, o.getY() + MIN_V, o.getZ() - MAX_U, o.getX() + MAX_W, o.getY() + MAX_V, o.getZ() - MIN_U);
    }

    // ------------------------------------------------------------------ the shapes

    private void build() {
        RandomSource rnd = RandomSource.create(this.seed);
        for (int i = 0; i < this.phase.length; i++) {
            this.phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        // The lower jaw: two great bars from the hinges under the cranium to the chin, fallen open.
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
        // The first vertebrae of the spine, fallen behind the skull onto the island.
        for (int k = 0; k < 3; k++) {
            double w = CW - C - 6 - k * 7;
            this.cones.add(new Cone(0, 4, w - 1.8, 0, 4 - k * 0.5, w + 1.8, 3.8 - k * 0.4, 3.8 - k * 0.4));
            this.cones.add(new Cone(0, 4, w, 0, 11 - k, w + 1.5, 1.3, 0.8));
            for (int s = -1; s <= 1; s += 2) {
                this.cones.add(new Cone(0, 4, w, s * 7, 3, w, 1.1, 0.7));
            }
        }
        // The colonnade down the hall: bone pillars from the floor to the vault.
        for (int s = -1; s <= 1; s += 2) {
            for (int w = -44; w <= 0; w += 11) {
                double top = innerCeiling(s * 11, w);
                this.cones.add(new Cone(s * 11, FLOOR, w, s * 11, top + 1, w, 1.4, 1.1));
            }
            // The posts the ladders climb.
            this.cones.add(new Cone(s * 19.5, FLOOR, CW, s * 19.5, LOW_GALLERY - 1, CW, 0.6, 0.6));
            this.cones.add(new Cone(s * 21.5, LOW_GALLERY + 1, CW + 8, s * 21.5, HIGH_GALLERY - 1, CW + 8, 0.6, 0.6));
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

    private double noise(double a, double b) {
        return (Math.sin(a * 0.11 + this.phase[0]) * Math.cos(b * 0.09 + this.phase[1])
            + 0.5 * Math.sin(a * 0.23 + b * 0.17 + this.phase[2])
            + 0.25 * Math.cos(b * 0.31 - a * 0.07 + this.phase[3])) / 1.75;
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
        RandomSource rnd = RandomSource.create(this.seed ^ (chunkPos.getMiddleBlockX() * 341873128712L + chunkPos.getMiddleBlockZ() * 132897987541L));
        for (int u = MIN_U; u <= MAX_U; u++) {
            for (int w = MIN_W; w <= MAX_W; w++) {
                if (!this.columnIn(chunk, u, w)) {
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
                        this.put(level, chunk, u, v, w, state);
                    }
                }
            }
        }
        this.furnish(level, chunk, rnd);
    }

    // What's at (u, v, w), or null for "leave it as it is".
    private BlockState blockAt(int u, int v, int w, List<Cone> near) {
        BlockState bone = ModBlocks.LEVIATHAN_BONE_BLOCK.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState result = null;
        // The island.
        double q = Math.sqrt(Math.pow(u / 44.0, 2) + Math.pow((w - 5) / 72.0, 2)) / (1 + 0.08 * this.noise(u, w));
        if (q <= 1 && v <= 0) {
            int bottom = (int) -Math.round((4 + 30 * Math.pow(1 - q, 0.9)) * (0.85 + 0.15 * this.noise(w * 1.3, u * 1.3)));
            if (v >= bottom) {
                double h = this.hash(u, v, w);
                result = v > -3 ? (h < 0.08 ? Blocks.END_STONE.defaultBlockState() : ModBlocks.PALE_END_STONE.get().defaultBlockState())
                    : (h < 0.5 ? Blocks.END_STONE.defaultBlockState() : ModBlocks.PALE_END_STONE.get().defaultBlockState());
            }
        }
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
                    result = bone.setValue(RotatedPillarBlock.AXIS, this.normalAxis(u / A / A, (v - CV) / B / B, (w - CW) / C / C));
                }
            } else if (v <= FLOOR) {
                result = bone;
            } else {
                result = air;
                double ring = Math.sqrt(Math.pow(u / A, 2) + Math.pow((w - CW) / C, 2));
                for (int g : new int[] {LOW_GALLERY, HIGH_GALLERY}) {
                    double inner = Math.sqrt(Math.max(0, SHELL * SHELL - Math.pow((g - CV) / B, 2)));
                    if (v == g && ring >= inner - 0.2) {
                        result = bone.setValue(RotatedPillarBlock.AXIS, this.worldAxis(Math.abs(u) > Math.abs(w - CW) * A / C ? 'w' : 'u'));
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
                    result = bone.setValue(RotatedPillarBlock.AXIS, this.worldAxis('w'));
                } else if (v > 5 && v < top - 3 && Math.abs(u) < half - 3) {
                    result = air;                                  // the mouth
                }
            }
        }
        // The jaw's floor, between its bars: the bed of the mouth.
        if (w >= 6 && w <= 76 && v >= 3 && v <= 5 && Math.abs(u) < jawHalf(w)) {
            result = bone.setValue(RotatedPillarBlock.AXIS, this.worldAxis('w'));
        }
        // Bones: jaw, teeth, brows, crest, spine, pillars.
        for (Cone c : near) {
            if (this.inside(c, u, v, w)) {
                result = bone.setValue(RotatedPillarBlock.AXIS, this.coneAxis(c));
            }
        }
        // The eye sockets: great holes through the wall, floored, rimmed in dark stone so they read as
        // sockets from far off.
        for (int ew : EYE_W) {
            double d = Math.sqrt(Math.pow(v - EYE_V, 2) + Math.pow(w - ew, 2));
            if (Math.abs(u) >= 20 && Math.abs(u) <= 32) {
                if (d < 5.2) {
                    result = v <= HIGH_GALLERY ? bone : air;
                } else if (d < 6.4 && e >= SHELL && e <= 1.0) {
                    result = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
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
                    result = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                }
            }
        }
        // The way in at the back, where the spine left the skull.
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

    // Everything else: eyes, ladders, lights, the camp.
    private void furnish(WorldGenLevel level, BoundingBox chunk, RandomSource rnd) {
        if (!this.overlaps(chunk, -34, 0, -60, 34, 60, 12)) {
            return;
        }
        // The eyes, set in the sockets, looking out.
        for (int s = -1; s <= 1; s += 2) {
            for (int ew : EYE_W) {
                this.put(level, chunk, s * 27, EYE_V, ew, ModBlocks.MAW_EYE.get().defaultBlockState()
                    .setValue(MawEyeBlock.FACING, this.dir(s, 0)).setValue(MawEyeBlock.LIT, false));
            }
        }
        // Ladders: from the floor to the low gallery, and on to the high one.
        for (int s = -1; s <= 1; s += 2) {
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, this.dir(-s, 0));
            for (int v = FLOOR + 1; v <= LOW_GALLERY + 1; v++) {
                this.put(level, chunk, s * 18, v, (int) CW, ladder);
            }
            for (int v = LOW_GALLERY + 1; v <= HIGH_GALLERY + 1; v++) {
                this.put(level, chunk, s * 20, v, (int) CW + 8, ladder);
            }
        }
        // Light: Whalelight hanging from the vault, lanterns along the galleries.
        for (int u = -14; u <= 14; u += 14) {
            for (int w = -46; w <= 2; w += 12) {
                int top = (int) Math.floor(innerCeiling(u, w));
                int len = 2 + (int) (this.hash(u, 0, w) * 4);
                for (int k = 0; k < len; k++) {
                    this.put(level, chunk, u, top - k, w, ModBlocks.WHALELIGHT_SCALE.get().defaultBlockState());
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
                this.put(level, chunk, u, g + 1, w, lantern);
            }
        }
        // The camp: a ring of bricks round a fire, chests, a table for charts.
        int cw = (int) CW - 12;
        BlockState bricks = ModBlocks.PALE_END_STONE_BRICKS.get().defaultBlockState();
        for (int du = -3; du <= 3; du++) {
            for (int dw = -3; dw <= 3; dw++) {
                double r = Math.sqrt(du * du + dw * dw);
                if (r >= 2.5 && r < 3.5) {
                    this.put(level, chunk, du, FLOOR + 1, cw + dw, ModBlocks.PALE_END_STONE_BRICK_SLAB.get().defaultBlockState());
                }
            }
        }
        this.put(level, chunk, 0, FLOOR + 1, cw, Blocks.SOUL_CAMPFIRE.defaultBlockState());
        this.chest(level, chunk, rnd, -5, FLOOR + 1, cw - 3);
        this.chest(level, chunk, rnd, 6, FLOOR + 1, cw + 4);
        this.put(level, chunk, 5, FLOOR + 1, cw - 4, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        this.put(level, chunk, -6, FLOOR + 1, cw + 3, Blocks.BARREL.defaultBlockState());
        this.put(level, chunk, -6, FLOOR + 2, cw + 3, lantern);
        for (int dw = -1; dw <= 1; dw++) {
            this.put(level, chunk, 7, FLOOR + 1, cw + dw, bricks);
        }
        // And a chest high up, by the eyes.
        this.chest(level, chunk, rnd, 22, HIGH_GALLERY + 1, EYE_W[1] - 4);
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
        return this.worldAxis(dw >= du ? 'w' : 'u');
    }

    // The grain of the bone on the cranium follows its surface: the axis of the surface's normal.
    private Direction.Axis normalAxis(double nu, double nv, double nw) {
        double au = Math.abs(nu), av = Math.abs(nv), aw = Math.abs(nw);
        if (av >= au && av >= aw) {
            return Direction.Axis.Y;
        }
        return this.worldAxis(aw >= au ? 'w' : 'u');
    }

    private Direction.Axis worldAxis(char local) {
        boolean alongW = local == 'w';
        return this.alongZ == alongW ? Direction.Axis.Z : Direction.Axis.X;
    }

    // The world direction of local +u (su = 1), -u (su = -1), +w (sw = 1) or -w (sw = -1).
    private Direction dir(int su, int sw) {
        if (su != 0) {
            return this.alongZ ? (su > 0 ? Direction.EAST : Direction.WEST) : (su > 0 ? Direction.NORTH : Direction.SOUTH);
        }
        return this.alongZ ? (sw > 0 ? Direction.SOUTH : Direction.NORTH) : (sw > 0 ? Direction.EAST : Direction.WEST);
    }

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

    // The middle of the throat, in the world (the survey looks for it).
    public BlockPos throat() {
        return this.world(0, THROAT_V, THROAT_W);
    }
}
