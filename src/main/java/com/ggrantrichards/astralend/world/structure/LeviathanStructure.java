package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Leviathan: the whole skeleton of a Star Leviathan, 320 blocks from tail to chin, adrift over the
// void - and the citadel the End's builders raised on it, with the dragon egg's reliquary in its skull.
// LeviathanPiece builds it. Like the Rift Maw it only forms out over open void.
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
        int y0 = minY + 28 + context.random().nextInt(10);
        double a = Math.PI / 4 + turn * Math.PI / 2, c = Math.cos(a), s = Math.sin(a);
        // Of eighteen points under it, at most three may have land that reaches up into it.
        int land = 0;
        for (int w : new int[] {-140, -80, -20, 40, 100, 150}) {
            for (int u : new int[] {-16, 0, 16}) {
                int x = cx + (int) Math.round(w * c - u * s), z = cz + (int) Math.round(w * s + u * c);
                int h = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (h > minY + 1 && h > y0 + 4) {
                    land++;
                }
            }
        }
        if (land > 3) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(cx, y0, cz);
        long seed = context.seed() ^ (cx * 341873128712L + cz * 132897987541L) ^ 0x1E7AL;
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new LeviathanPiece(origin, turn, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.LEVIATHAN.get();
    }
}
