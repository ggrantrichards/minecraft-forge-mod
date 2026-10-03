package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.ModWorldgen;
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
import net.minecraft.world.phys.Vec3;

// Builds the Remnant a chunk at a time, every block decided from shapes:
//   - a lumpy ball about 40 blocks across: a crust of charred and scorched stone and blackstone,
//     split all over by glowing fissures of magma and shroomlight;
//   - under the crust, a mantle still molten (magma, shroomlight, Solarite);
//   - at the middle, a hollow chamber with the star's last ember in it - a heart of Solarite round a
//     core of light - and the chest of whoever reached it first; two rifts lead in from outside;
//   - three loops of fire arching off the surface, and a ring of broken rock circling it.
public class RemnantPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "chests/remnant"));
    private static final double CRUST = 19.0, MANTLE = 11.5, CHAMBER = 7.0;
    private static final int REACH = 48;

    private final BlockPos origin;
    private final long seed;
    private final double[] phase = new double[9];
    private final List<Vec3[]> loops = new ArrayList<>();      // {centre, normal, (radius, 0, 0)}
    private final List<double[]> rocks = new ArrayList<>();    // {x, y, z, r}
    private final List<Vec3> rifts = new ArrayList<>();        // directions of the ways in

    public RemnantPiece(BlockPos origin, long seed) {
        super(ModWorldgen.REMNANT_PIECE.get(), 0, new BoundingBox(origin).inflatedBy(REACH));
        this.origin = origin;
        this.seed = seed;
        this.build();
    }

    public RemnantPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModWorldgen.REMNANT_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.seed = tag.getLongOr("Seed", 0L);
        this.build();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putLong("Seed", this.seed);
    }

    private void build() {
        RandomSource rnd = RandomSource.create(this.seed);
        for (int i = 0; i < this.phase.length; i++) {
            this.phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        for (int i = 0; i < 3; i++) {
            Vec3 at = randomDir(rnd).scale(CRUST - 1);
            Vec3 normal = at.normalize().cross(randomDir(rnd)).normalize();
            this.loops.add(new Vec3[] {at, normal, new Vec3(7 + rnd.nextDouble() * 6, 0, 0)});
        }
        double tilt = rnd.nextDouble() * 0.5;
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16 + rnd.nextDouble() * 0.3;
            double r = 30 + rnd.nextDouble() * 13;
            double y = Math.sin(a) * r * tilt + (rnd.nextDouble() - 0.5) * 6;
            this.rocks.add(new double[] {Math.cos(a) * r, y, Math.sin(a) * r * Math.cos(tilt), 1.6 + rnd.nextDouble() * 2.2});
        }
        this.rifts.add(new Vec3(1.0, 0.25, 0.1).normalize());
        this.rifts.add(new Vec3(-0.4, -0.15, -1.0).normalize());
    }

    private static Vec3 randomDir(RandomSource rnd) {
        double z = rnd.nextDouble() * 2 - 1, a = rnd.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - z * z);
        return new Vec3(Math.cos(a) * s, z, Math.sin(a) * s);
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
        for (int x = Math.max(chunk.minX(), this.origin.getX() - REACH); x <= Math.min(chunk.maxX(), this.origin.getX() + REACH); x++) {
            for (int z = Math.max(chunk.minZ(), this.origin.getZ() - REACH); z <= Math.min(chunk.maxZ(), this.origin.getZ() + REACH); z++) {
                for (int y = Math.max(chunk.minY(), this.origin.getY() - REACH); y <= Math.min(chunk.maxY(), this.origin.getY() + REACH); y++) {
                    int dx = x - this.origin.getX(), dy = y - this.origin.getY(), dz = z - this.origin.getZ();
                    BlockState state = this.blockAt(dx, dy, dz);
                    if (state != null) {
                        level.setBlock(p.set(x, y, z), state, 2);
                    }
                }
            }
        }
        // The chest, on the floor of the chamber.
        BlockPos chest = this.origin.offset(3, (int) -CHAMBER + 1, -2);
        if (chunk.isInside(chest)) {
            this.createChest(level, chunk, RandomSource.create(this.seed), chest, LOOT, null);
        }
    }

    private BlockState blockAt(int x, int y, int z) {
        double d = Math.sqrt(x * x + y * y + z * z);
        if (d > REACH) {
            return null;
        }
        double h = this.hash(x, y, z);
        BlockState result = null;
        double surface = CRUST + 2.6 * this.noise(x, y, z, 0) + 1.2 * this.noise(x * 2.3, y * 2.3, z * 2.3, 3);
        if (d <= surface) {
            if (d < 1.6) {
                result = Blocks.SHROOMLIGHT.defaultBlockState();                   // the last ember
            } else if (d < 3.0) {
                result = ModBlocks.SOLARITE_BLOCK.get().defaultBlockState();       // its heart
            } else if (d < CHAMBER) {
                result = Blocks.AIR.defaultBlockState();
            } else if (d < MANTLE) {
                result = h < 0.45 ? Blocks.MAGMA_BLOCK.defaultBlockState() : h < 0.7 ? Blocks.SHROOMLIGHT.defaultBlockState()
                    : h < 0.85 ? ModBlocks.SOLARITE_ORE.get().defaultBlockState() : ModBlocks.RAW_SOLARITE_BLOCK.get().defaultBlockState();
            } else {
                // The crust, cracked through by fissures that glow.
                double crack = Math.abs(this.noise(x * 1.6, y * 1.6, z * 1.6, 6));
                if (crack < 0.06) {
                    result = h < 0.6 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.SHROOMLIGHT.defaultBlockState();
                } else {
                    double depth = surface - d;
                    result = depth < 1.5 ? (h < 0.5 ? ModBlocks.CHARRED_END_STONE.get().defaultBlockState()
                            : h < 0.8 ? Blocks.BLACKSTONE.defaultBlockState() : ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState())
                        : (h < 0.5 ? Blocks.BLACKSTONE.defaultBlockState() : h < 0.8 ? Blocks.BASALT.defaultBlockState()
                            : Blocks.MAGMA_BLOCK.defaultBlockState());
                }
            }
            // The rifts: two ways in, from the surface to the chamber.
            for (Vec3 rift : this.rifts) {
                double along = x * rift.x + y * rift.y + z * rift.z;
                if (along > 0) {
                    double off = new Vec3(x, y, z).subtract(rift.scale(along)).length();
                    if (off < 2.2 + 0.6 * this.noise(along, along, along, 2) && d > 3.0) {
                        result = Blocks.AIR.defaultBlockState();
                    }
                }
            }
        }
        // Loops of fire, arching off the surface.
        for (Vec3[] loop : this.loops) {
            Vec3 q = new Vec3(x, y, z).subtract(loop[0]);
            double up = q.dot(loop[1]);
            Vec3 flat = q.subtract(loop[1].scale(up));
            double tube = Math.sqrt(Math.pow(flat.length() - loop[2].x, 2) + up * up);
            if (tube < 1.3 && d > surface - 0.5) {
                result = h < 0.35 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.SHROOMLIGHT.defaultBlockState();
            }
        }
        // The ring of broken rock.
        for (double[] rock : this.rocks) {
            double rd = Math.sqrt(Math.pow(x - rock[0], 2) + Math.pow(y - rock[1], 2) + Math.pow(z - rock[2], 2));
            double r = rock[3] * (1 + 0.25 * this.noise(x * 3, y * 3, z * 3, 4));
            if (rd <= r) {
                result = rd < r - 1.4 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : h < 0.6 ? ModBlocks.CHARRED_END_STONE.get().defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            }
        }
        return result;
    }
}
