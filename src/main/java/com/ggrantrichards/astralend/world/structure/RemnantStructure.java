package com.ggrantrichards.astralend.world.structure;

import com.ggrantrichards.astralend.world.EclipseLayout;
import com.ggrantrichards.astralend.world.ModWorldgen;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// The Remnant: what is left of the star that burned out here - a cinder the size of a hill, still
// glowing through its cracks. It hung over the Eclipse's hell half (the Corona) until it fell (after
// beta.4): now it lies half buried in a crater at the heart of that half (EclipseLayout.REMNANT), and
// the furrow it ploughed runs out to the Seam, the scar between the halves. One per Eclipse: every chunk
// is asked (its structure set spaces starts one chunk apart) and only that one says yes. Where it hung,
// HEIGHT blocks up, the sky still shows a faint ember (AstralSkyRenderer), and the Sunstorms still
// break from there. RemnantPiece builds it.
public class RemnantStructure extends Structure {
    public static final MapCodec<RemnantStructure> CODEC = simpleCodec(RemnantStructure::new);
    public static final int HEIGHT = 165;     // where it hung, this far above the bottom of the world

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
        // On the ground (the Eclipse's terrain is level about there), the furrow pointing at the nearest
        // point of the Seam: on the circle of radius 1/2 round (0, 1/2) in the Eclipse's frame.
        BlockPos origin = new BlockPos(cx, (int) com.ggrantrichards.astralend.world.feature.EclipseTerrainFeature.TOP - 1, cz);
        EclipseLayout.Eclipse e = EclipseLayout.nearest(cx, cz, EclipseLayout.sampler(context.randomState()), 0);
        double u = EclipseLayout.REMNANT[0], v = EclipseLayout.REMNANT[1];
        double len = Math.hypot(u, v - 0.5);
        double su = u / len * 0.5, sv = 0.5 + (v - 0.5) / len * 0.5;
        float heading = (float) Math.atan2(e.worldZ(su, sv) - e.worldZ(u, v), e.worldX(su, sv) - e.worldX(u, v));
        long seed = context.seed() ^ (cx * 341873128712L + cz * 132897987541L) ^ 0x5A4DL;
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new RemnantPiece(origin, seed, heading))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.REMNANT.get();
    }
}
