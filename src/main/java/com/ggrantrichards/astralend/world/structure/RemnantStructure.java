package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.EclipseLayout;
import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Remnant: what is left of the star that burned out here - a cinder the size of a hill, still
// glowing through its cracks, hanging high over the Eclipse's hell half (the Corona) with its broken
// pieces circling it. One per Eclipse, over the heart of that half (EclipseLayout.REMNANT): every chunk
// is asked (its structure set spaces starts one chunk apart) and only that one says yes. The sky shows
// it from far off (AstralSkyRenderer); come close and it's really there. RemnantPiece builds it.
public class RemnantStructure extends Structure {
    public static final MapCodec<RemnantStructure> CODEC = simpleCodec(RemnantStructure::new);
    public static final int HEIGHT = 165;     // its centre, this far above the bottom of the world

    public RemnantStructure(StructureSettings settings) {
        super(settings);
    }

    // Where the Remnant of the Eclipse nearest this spot hangs (its chunk's middle), or null.
    public static BlockPos anchor(net.minecraft.world.level.biome.Climate.Sampler nebula, double x, double z, int rings) {
        EclipseLayout.Eclipse e = EclipseLayout.nearest(x, z, nebula, rings);
        if (e == null) {
            return null;
        }
        int ax = (int) Math.floor(e.worldX(EclipseLayout.REMNANT[0], EclipseLayout.REMNANT[1]));
        int az = (int) Math.floor(e.worldZ(EclipseLayout.REMNANT[0], EclipseLayout.REMNANT[1]));
        return new BlockPos((ax & ~15) + 8, HEIGHT, (az & ~15) + 8);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        BlockPos anchor = anchor(com.ggrantrichards.astralend.world.EclipseLayout.sampler(context.randomState()), cx, cz, 0);
        if (anchor == null || anchor.getX() != cx || anchor.getZ() != cz) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(cx, context.heightAccessor().getMinY() + HEIGHT + context.random().nextInt(10), cz);
        long seed = context.seed() ^ (cx * 341873128712L + cz * 132897987541L) ^ 0x5A4DL;
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new RemnantPiece(origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.REMNANT.get();
    }
}
