package com.ggrantrichards.astralend.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

// The Eclipse's island, as part of the End's terrain (density_function/terrain/shaped lerps toward it).
// Two outputs:
//   "weight" - 1 over the Eclipse and a ring of void round it, easing to 0 beyond: how much of the
//              terrain here is the Eclipse's;
//   "shape"  - the island itself: one great disc of land, gently rolling, thick in the middle and thin
//              at the rim, ringed by void, with a round hole through it at the Wellspring.
// Where it is comes from EclipseLayout, read from the same "nebula" noise the biome source reads.
public record EclipseDensity(DensityFunction nebula, DensityFunction hills, String output) implements DensityFunction {
    public static final MapCodec<EclipseDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("nebula").forGetter(EclipseDensity::nebula),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("hills").forGetter(EclipseDensity::hills),
        Codec.STRING.fieldOf("output").forGetter(EclipseDensity::output)
    ).apply(i, EclipseDensity::new));
    public static final KeyDispatchDataCodec<EclipseDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    public static final double TOP = 62.0;       // the Eclipse's ground level
    private static final double VOID_RING = 40.0;

    // The last cell looked up, per thread (the terrain asks thousands of times per chunk).
    private record Last(DensityFunction nebula, long i, long j, EclipseLayout.Eclipse eclipse) {}

    private static final ThreadLocal<Last> LAST = new ThreadLocal<>();

    private EclipseLayout.Eclipse eclipse(int x, int z) {
        long i = Math.floorDiv(x, EclipseLayout.CELL), j = Math.floorDiv(z, EclipseLayout.CELL);
        Last last = LAST.get();
        if (last == null || last.nebula() != this.nebula || last.i() != i || last.j() != j) {
            last = new Last(this.nebula, i, j, EclipseLayout.inCell(i, j, this.nebula));
            LAST.set(last);
        }
        return last.eclipse();
    }

    @Override
    public double compute(FunctionContext context) {
        EclipseLayout.Eclipse e = this.eclipse(context.blockX(), context.blockZ());
        if (e == null) {
            return this.output.equals("weight") ? 0.0 : -1.0;
        }
        double d = e.distance(context.blockX(), context.blockZ());
        double r = EclipseLayout.RADIUS;
        if (this.output.equals("weight")) {
            return Mth.clamp((r + VOID_RING - d) / 24.0, 0.0, 1.0);
        }
        if (d > r) {
            return -2.0;
        }
        double wx = e.worldX(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double wz = e.worldZ(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double toWell = Math.sqrt((context.blockX() - wx) * (context.blockX() - wx) + (context.blockZ() - wz) * (context.blockZ() - wz));
        if (toWell < EclipseLayout.WELLSPRING_RADIUS * r) {
            return -2.0;    // the Wellspring's hole, open to the void (its glass floor and water are a feature)
        }
        double t = d / r;
        double h = this.hills.compute(context);
        double top = TOP + 2.5 * h - 6.0 * t * t * t * t;
        double thickness = 10.0 + 34.0 * Math.sqrt(Math.max(0.0, 1.0 - t * t)) + 6.0 * h;
        double y = context.blockY();
        return 0.4 * Math.min(top - y, y - (top - thickness));
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new EclipseDensity(this.nebula.mapAll(visitor), this.hills.mapAll(visitor), this.output));
    }

    @Override
    public double minValue() {
        return -100.0;
    }

    @Override
    public double maxValue() {
        return 100.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
