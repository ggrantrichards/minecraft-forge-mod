package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// A structure that only starts where the ground suits it, then hands over to an ordinary structure
// (a jigsaw) to build. The End is mostly void, and a jigsaw on its own would happily drop a crater
// into empty air at the bottom of the world. The ground is sampled at the start chunk's centre and
// `reach` blocks out in each direction; every sample must lie between min_surface and max_surface
// (the bottom of the world counts as "no ground"). Used as "type": "astralend:island".
public class IslandStructure extends Structure {
    public static final MapCodec<IslandStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        settingsCodec(instance),
        Structure.DIRECT_CODEC.fieldOf("structure").forGetter(s -> s.inner),
        Codec.INT.optionalFieldOf("min_surface", -4096).forGetter(s -> s.minSurface),
        Codec.INT.optionalFieldOf("max_surface", 4096).forGetter(s -> s.maxSurface),
        Codec.INT.optionalFieldOf("reach", 0).forGetter(s -> s.reach)
    ).apply(instance, IslandStructure::new));

    private final Structure inner;
    private final int minSurface;
    private final int maxSurface;
    private final int reach;

    public IslandStructure(StructureSettings settings, Structure inner, int minSurface, int maxSurface, int reach) {
        super(settings);
        this.inner = inner;
        this.minSurface = minSurface;
        this.maxSurface = maxSurface;
        this.reach = reach;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        // (and the chunk's corner, where the jigsaw stands its start and takes the surface height from)
        int[][] offsets = reach == 0 ? new int[][] {{0, 0}, {-8, -8}}
            : new int[][] {{0, 0}, {-8, -8}, {reach, 0}, {-reach, 0}, {0, reach}, {0, -reach}};
        for (int[] o : offsets) {
            int y = context.chunkGenerator().getBaseHeight(cx + o[0], cz + o[1], Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
            boolean ground = y > context.heightAccessor().getMinBuildHeight() + 1;
            if (ground ? y < minSurface || y > maxSurface : minSurface > context.heightAccessor().getMinBuildHeight()) {
                return Optional.empty();
            }
        }
        return inner.findValidGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.ISLAND.get();
    }
}
