package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Rift Maw: the fossil skull of a Star Leviathan on its own floating island - a cranium you could
// fit a village in, a jaw 80 blocks long - and inside, the hall where its brain was. RiftMawPiece
// builds it. It only forms out over open void: nothing of the land around may reach up into it.
public class RiftMawStructure extends Structure {
    public static final MapCodec<RiftMawStructure> CODEC = simpleCodec(RiftMawStructure::new);

    public RiftMawStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        boolean alongZ = context.random().nextBoolean();
        int minY = context.heightAccessor().getMinY();
        int y0 = minY + 50 + context.random().nextInt(16);
        // Of fifteen points under it, at most two may have land that comes within 30 blocks of it.
        int land = 0;
        for (int w : new int[] {-60, -25, 10, 45, 80}) {
            for (int u : new int[] {-35, 0, 35}) {
                int x = alongZ ? cx + u : cx + w, z = alongZ ? cz + w : cz - u;
                int h = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (h > minY + 1 && h > y0 - 30) {
                    land++;
                }
            }
        }
        if (land > 2) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(cx, y0, cz);
        long seed = context.seed() ^ (cx * 341873128712L + cz * 132897987541L) ^ 0x5EEDL;
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new RiftMawPiece(origin, alongZ, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.RIFT_MAW.get();
    }
}
