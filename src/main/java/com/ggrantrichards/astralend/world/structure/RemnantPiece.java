package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.EclipseLayout;
import com.ggrantrichards.astralend.world.ModWorldgen;
import com.ggrantrichards.astralend.world.feature.EclipseTerrainFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// Builds the crashed Remnant a chunk at a time, every block decided from shapes (after beta.4: it used
// to hang in the sky; now it has fallen). Like a geode, it's layers round a hollow, half buried:
//   - a crater dug into the hell half, its floor glazed black (Obsidian) toward the middle, its rim
//     thrown up in a ragged ring of Charred End Stone, with lumps of ejecta round it;
//   - a furrow running out of the crater toward the Seam - the way it came down;
//   - in the middle, the cinder, sunk to its waist: a crust of Obsidian and Charred End Stone with veins
//     of raw Solarite glowing through its cracks, a mantle of Sunscorched Stone, and a lining of black
//     glass shot through with Solarite round a hollow. In the hollow, the star's last ember - a heart of raw Solarite
//     round a Solarite core - and the chest of whoever reached it first. A cleft split open on the side
//     away from the furrow leads in; anyone can also dig.
// Only End blocks and the mod's own: no Nether blocks.
public class RemnantPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/remnant"));
    public static final double CRATER = 24.0;
    private static final double DEPTH = 11.0, RIM = 3.5, SCORCH = 10.0;
    private static final double CINDER = 12.0, MANTLE = 2.0, CRUST = 2.0, HOLLOW = 5.5;
    private static final double FURROW_END = 46.0;
    private static final int REACH = 48;
    // The cinder's middle, relative to the origin: sunk to its waist in the crater floor, its top
    // standing well over the land round the crater.
    public static final int CINDER_Y = -3;

    private final BlockPos origin;      // the ground level at the crater's middle
    private final long seed;
    private final float heading;        // the furrow's direction (radians: x = cos, z = sin)
    private final double[] phase = new double[9];
    private final List<double[]> ejecta = new ArrayList<>();   // {x, y, z, r}: y is above the ground

    public RemnantPiece(BlockPos origin, long seed, float heading) {
        super(ModWorldgen.REMNANT_PIECE.get(), 0, new BoundingBox(origin).inflatedBy(REACH));
        this.origin = origin;
        this.seed = seed;
        this.heading = heading;
        this.build();
    }

    public RemnantPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.REMNANT_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.seed = tag.getLongOr("Seed", 0L);
        this.heading = tag.getFloatOr("Heading", 0.0F);
        this.build();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putLong("Seed", this.seed);
        tag.putFloat("Heading", this.heading);
    }

    private void build() {
        RandomSource rnd = RandomSource.create(this.seed);
        for (int i = 0; i < this.phase.length; i++) {
            this.phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        // Lumps thrown out by the impact, round the rim and beyond (none in the furrow's path).
        for (int i = 0; i < 9; i++) {
            double a = this.heading + Math.PI * (0.35 + 1.3 * rnd.nextDouble());
            double r = CRATER + 2 + rnd.nextDouble() * 15;
            this.ejecta.add(new double[] {Math.cos(a) * r, rnd.nextDouble() * 1.5, Math.sin(a) * r, 1.3 + rnd.nextDouble() * 1.4});
        }
    }

    // Smooth noise in [-1, 1].
    private double noise(double x, double y, double z, int k) {
        double[] p = this.phase;
        return (Math.sin(x * 0.19 + y * 0.07 + p[k]) + Math.sin(y * 0.23 - z * 0.11 + p[k + 1]) + Math.sin(z * 0.21 + x * 0.13 + p[k + 2])) / 3.0;
    }

    private double hash(int x, int y, int z) {
        long h = this.seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunk, ChunkPos chunkPos, BlockPos reference) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int ox = this.origin.getX(), oy = this.origin.getY(), oz = this.origin.getZ();
        double hx = Math.cos(this.heading), hz = Math.sin(this.heading);
        EclipseLayout.Eclipse eclipse = EclipseLayout.at(ox, oz, EclipseLayout.sampler(level.getLevel().getChunkSource().randomState()));
        for (int x = Math.max(chunk.minX(), ox - REACH); x <= Math.min(chunk.maxX(), ox + REACH); x++) {
            for (int z = Math.max(chunk.minZ(), oz - REACH); z <= Math.min(chunk.maxZ(), oz + REACH); z++) {
                int dx = x - ox, dz = z - oz;
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > REACH) {
                    continue;
                }
                // The land here before the fall (the Eclipse's terrain), relative to the origin - worked out
                // from the terrain's own shape, as the world-gen heightmaps don't know it (a feature builds it).
                int surface = eclipse == null ? Integer.MIN_VALUE : EclipseTerrainFeature.groundTop(eclipse, x, z);
                int ground = surface == Integer.MIN_VALUE ? -100 : surface - oy;
                boolean land = ground > -30;
                double ragged = 1.2 * this.noise(dx, 0, dz, 0) + 0.8 * this.noise(dx * 2.7, 0, dz * 2.7, 3);
                // The crater's surface: a bowl, with its rim thrown up.
                double rim = RIM * Math.exp(-Math.pow((r - CRATER) / 3.5, 2)) * (1.0 + 0.35 * ragged);
                int top;
                if (r < CRATER) {
                    top = (int) Math.round(-DEPTH * (1.0 - (r / CRATER) * (r / CRATER)) + rim + 0.5 * ragged);
                } else {
                    top = Math.max(ground, (int) Math.round(rim));
                }
                // The furrow, ploughed out toward the Seam.
                double along = dx * hx + dz * hz, across = Math.abs(-dx * hz + dz * hx);
                boolean furrow = false;
                if (land && along > CRATER - 6 && along < FURROW_END) {
                    double t = (along - (CRATER - 6)) / (FURROW_END - (CRATER - 6));
                    if (across < 4.5 - 3.0 * t + 0.6 * ragged) {
                        int floor = ground - (int) Math.round(3.0 * (1.0 - t) + 0.5);
                        if (floor < top) {
                            top = floor;
                            furrow = true;
                        }
                    }
                }
                boolean scorched = r < CRATER + SCORCH + 3.0 * ragged || furrow;
                for (int y = -REACH; y <= 16; y++) {
                    int wy = oy + y;
                    if (wy < chunk.minY() || wy > chunk.maxY()) {
                        continue;
                    }
                    BlockState state = this.cinderAt(dx, y, dz);
                    if (state == null && land) {
                        state = this.ejectaAt(dx, y - top, dz);
                    }
                    if (state == null && (land || r < CRATER) && scorched) {
                        state = this.groundAt(x, y, z, r, top, ground, furrow);
                    }
                    if (state != null) {
                        level.setBlock(p.set(x, wy, z), state, 2);
                    }
                }
            }
        }
        // The chest, on the floor of the hollow.
        BlockPos chest = this.origin.offset(3, CINDER_Y - 2, 0);
        if (chunk.isInside(chest)) {
            this.createChest(level, chunk, RandomSource.create(this.seed), chest, LOOT, null);
        }
    }

    // The crater's ground in one column: air above its surface, glazed or charred rock at it, and
    // solid under it however the land lay (so nothing hangs and nothing is left floating).
    private BlockState groundAt(int x, int y, int z, double r, int top, int ground, boolean furrow) {
        if (y > top) {
            // the old land cleared out of the crater and the furrow
            return y <= Math.max(ground, top) + 2 && (r < CRATER + 2 || furrow) ? Blocks.AIR.defaultBlockState() : null;
        }
        if (y < top - 3) {
            return y > ground ? ModBlocks.CHARRED_END_STONE.get().defaultBlockState() : null;   // fill under a raised rim
        }
        if (y == top) {
            // Glazed black where the heat was worst - the floor's middle and the furrow - and charred
            // round it, fading back into the Corona's crust: in patches, not a speckle.
            double v = 0.75 * (0.5 + 0.5 * this.noise(x * 0.8, 0, z * 0.8, 6)) + 0.25 * this.hash(x, y, z);
            double glaze = furrow ? 0.75 : r < CRATER ? 1.1 - r / CRATER : 0.0;
            if (v < glaze) {
                return Blocks.OBSIDIAN.defaultBlockState();
            }
            double fade = r < CRATER ? 0.1 : 0.1 + 0.9 * (r - CRATER) / SCORCH;
            return v > 1.0 - fade ? ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState() : ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        }
        return ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
    }

    // A lump of ejecta, with `y` measured from the ground under it.
    private BlockState ejectaAt(int x, int y, int z) {
        for (double[] e : this.ejecta) {
            double d = Math.sqrt((x - e[0]) * (x - e[0]) + (y - e[1]) * (y - e[1]) * 1.4 + (z - e[2]) * (z - e[2]));
            if (d <= e[3] * (1 + 0.25 * this.noise(x * 3, y * 3, z * 3, 6))) {
                return this.hash(x, y, z) < 0.4 ? Blocks.OBSIDIAN.defaultBlockState() : ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
            }
        }
        return null;
    }

    // The cinder: null outside it.
    private BlockState cinderAt(int x, int y, int z) {
        double cy = y - CINDER_Y;
        double d = Math.sqrt(x * x + cy * cy + z * z);
        double surface = CINDER + 1.4 * this.noise(x, y, z, 0) + 0.7 * this.noise(x * 2.3, y * 2.3, z * 2.3, 3);
        if (d > surface) {
            return null;
        }
        // The hollow, with the heart a little below its middle, and a flat floor for the chest.
        double heart = Math.sqrt(x * x + (cy + 1.0) * (cy + 1.0) + z * z);
        if (heart < 1.3) {
            return ModBlocks.SOLARITE_BLOCK.get().defaultBlockState();     // the last ember
        }
        if (heart < 2.3) {
            return ModBlocks.RAW_SOLARITE_BLOCK.get().defaultBlockState(); // its heart
        }
        if (d < HOLLOW && cy > -3.0) {
            return Blocks.AIR.defaultBlockState();
        }
        // The cleft: split open on the side away from the furrow, from the top down to the hollow.
        double away = -(x * Math.cos(this.heading) + z * Math.sin(this.heading));
        double side = Math.abs(-x * Math.sin(this.heading) + z * Math.cos(this.heading));
        if (away > 1.0 && cy > -2.0 && side < 1.2 + 0.11 * (cy + 2.0) + 0.4 * this.noise(x, y, z, 2)) {
            return Blocks.AIR.defaultBlockState();
        }
        double h = this.hash(x, y, z);
        double depth = surface - d;
        if (depth < CRUST) {
            // The crust, cracked through by veins that still glow.
            if (Math.abs(this.noise(x * 1.6, y * 1.6, z * 1.6, 6)) < 0.045) {
                return ModBlocks.RAW_SOLARITE_BLOCK.get().defaultBlockState();
            }
            return h < 0.6 ? Blocks.OBSIDIAN.defaultBlockState() : ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        }
        if (depth < CRUST + MANTLE) {
            return h < 0.85 ? ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState() : ModBlocks.SOLARITE_ORE.get().defaultBlockState();
        }
        // The lining round the hollow: fused black glass with the star's metal through it - its glow an
        // accent, as a geode's crystal is (the heart is the bright thing in here).
        return h < 0.12 ? ModBlocks.RAW_SOLARITE_BLOCK.get().defaultBlockState() : h < 0.35 ? ModBlocks.SOLARITE_ORE.get().defaultBlockState()
            : h < 0.85 ? Blocks.OBSIDIAN.defaultBlockState() : ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
    }
}
