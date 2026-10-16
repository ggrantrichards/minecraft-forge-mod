package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Leviathan: the skeleton of a Star Leviathan, some 200 blocks from tail to snout, adrift high over the
// void - no two alike (LeviathanShape) - with the End City its builders raised on its back, the reliquary
// in its ribcage, its heart still beating, and its skull, the Maw. LeviathanPiece builds the body,
// LeviathanHeadPiece the skull. It floats well above the islands, and only where no land comes near it.
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
        int minY = context.heightAccessor().getMinBuildHeight();
        int y0 = minY + 100 + context.random().nextInt(14);
        int[] cs = LeviathanPiece.CS[turn];
        // Under the whole of it - tail to snout, across its width - at most two points may have land, and
        // none may come within 8 blocks of its lowest part (its jaw, some 8 blocks over its origin).
        int land = 0;
        for (int w = LeviathanShape.TAIL; w <= LeviathanShape.SNOUT; w += 15) {
            for (int u : new int[] {-20, 0, 20}) {
                int dw = w - LeviathanShape.W0;
                int x = cx + dw * cs[0] - u * cs[1], z = cz + dw * cs[1] + u * cs[0];
                int h = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (h > y0) {
                    return Optional.empty();
                }
                if (h > minY + 1 && h > y0 - 25) {
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
