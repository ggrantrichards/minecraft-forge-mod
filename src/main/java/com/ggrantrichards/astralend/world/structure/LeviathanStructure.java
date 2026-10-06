package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Leviathan: the whole skeleton of a Star Leviathan, some 370 blocks from tail to chin, adrift high
// over the void - the citadel the End's builders raised on it, the reliquary in its ribcage, its organs
// still faintly alive, and its skull, the Maw. LeviathanPiece builds the body, LeviathanHeadPiece the
// skull. It floats well above the islands, and only where no land comes near it.
public class LeviathanStructure extends Structure {
    public static final MapCodec<LeviathanStructure> CODEC = simpleCodec(LeviathanStructure::new);

    public LeviathanStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        int turn = context.random().nextInt(4);
        int minY = context.heightAccessor().getMinY();
        int y0 = minY + 100 + context.random().nextInt(14);
        double a = Math.PI / 4 + turn * Math.PI / 2, c = Math.cos(a), s = Math.sin(a);
        // Under the whole of it - body and skull, across its width - at most two points may have land,
        // and none may come within 15 blocks of its lowest part.
        int land = 0;
        for (int w = -180; w <= 180; w += 30) {
            for (int u : new int[] {-28, 0, 28}) {
                int x = cx + (int) Math.round(w * c - u * s), z = cz + (int) Math.round(w * s + u * c);
                int h = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (h > y0 - 8) {
                    return Optional.empty();
                }
                if (h > minY + 1 && h > y0 - 30) {
                    land++;
                }
            }
        }
        if (land > 2) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(cx, y0, cz);
        long seed = context.seed() ^ (cx * 341873128712L + cz * 132897987541L) ^ 0x1E7AL;
        return Optional.of(new GenerationStub(origin, builder -> {
            builder.addPiece(new LeviathanHeadPiece(origin, turn, seed));
            builder.addPiece(new LeviathanPiece(origin, turn, seed));
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.LEVIATHAN.get();
    }
}
