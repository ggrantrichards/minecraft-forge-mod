package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Whalefall: the skeleton of a Star Leviathan that died out in the void, drifting there still -
// and long since built into by those who came after. A landmark, very rare, about 100 blocks high,
// 100 wide and 300 long (WhalefallPiece builds it). It only forms over open void, never on an
// island: the middle and both ends must have nothing under them.
public class WhalefallStructure extends Structure {
    public static final MapCodec<WhalefallStructure> CODEC = simpleCodec(WhalefallStructure::new);

    public WhalefallStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        boolean alongZ = context.random().nextBoolean();
        int minY = context.heightAccessor().getMinY();
        // Over open void: of fifteen points under it, at most two may have land (a small island it
        // has drifted into).
        int land = 0;
        for (int w : new int[] {-120, -60, 0, 60, 130}) {
            for (int u : new int[] {-30, 0, 30}) {
                int x = alongZ ? cx + u : cx + w, z = alongZ ? cz + w : cz - u;
                if (this.height(context, x, z) > minY + 1) {
                    land++;
                }
            }
        }
        if (land > 2) {
            return Optional.empty();
        }
        int y0 = minY + 42 + context.random().nextInt(24);   // at the height of the islands around it
        BlockPos origin = new BlockPos(cx, y0, cz);
        long seed = context.seed() ^ context.chunkPos().toLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new WhalefallPiece(origin, alongZ, seed))));
    }

    private int height(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.WHALEFALL.get();
    }
}
