package com.ggrantrichards.astralend.world;

import net.minecraft.world.level.levelgen.DensityFunction;

// Where the Eclipses are, and the shape of each. Shared by the biome source (which half of an Eclipse a
// spot is in), the terrain (EclipseDensity: the Eclipse's island), the Remnant (which hangs over each
// Eclipse's hell half), the Eclipse's features and the Sunstorms.
//
// The outer End is cut into cells CELL blocks square; three cells in four hold one Eclipse, a disc
// RADIUS blocks across the middle, at a spot and turn of its own. What decides is the "nebula" region
// noise (the noise router's temperature slot), read at a fixed point of each cell: it's seeded with
// the world, so every world has its own Eclipses, and the biome source and the terrain - which see
// the same noise - always agree.
//
// Each Eclipse is a yin-yang, in its own frame (u, v), a unit disc:
//   - the heaven half: u > 0, plus the disc of radius 1/2 round (0, 1/2), minus that round (0, -1/2);
//   - the hell half: the rest - its S-shaped border runs through the middle;
//   - and each holds a seed of the other: a small disc of hell round (0, 1/2) and of heaven round (0, -1/2).
public final class EclipseLayout {
    public static final int CELL = 4096;
    public static final double RADIUS = 180.0;
    public static final double SEED = 0.13;                 // the seeds' radius, in units of RADIUS
    private static final double MARGIN = RADIUS + 320.0;    // keeps an Eclipse well inside its cell
    private static final double CHANCE = 0.75;
    private static final double MIN_DISTANCE = 1700.0;      // from the centre of the End

    // Anchors, in the Eclipse's frame (u, v).
    public static final double[] WELLSPRING = {0.6, -0.1};  // the heart of the heaven half
    public static final double WELLSPRING_RADIUS = 0.15;
    public static final double[] REMNANT = {-0.6, 0.1};     // over the heart of the hell half
    public static final double[] HELL_SEED = {0.0, 0.5};    // in heaven: one burnt spire
    public static final double[] HEAVEN_SEED = {0.0, -0.5}; // in hell: one small living pool

    public enum Part { NONE, HEAVEN, HELL }

    public record Eclipse(double x, double z, double angle) {
        // A point of the Eclipse's frame, in the world.
        public double worldX(double u, double v) {
            return this.x + RADIUS * (u * Math.cos(this.angle) - v * Math.sin(this.angle));
        }

        public double worldZ(double u, double v) {
            return this.z + RADIUS * (u * Math.sin(this.angle) + v * Math.cos(this.angle));
        }

        public double u(double wx, double wz) {
            return ((wx - this.x) * Math.cos(this.angle) + (wz - this.z) * Math.sin(this.angle)) / RADIUS;
        }

        public double v(double wx, double wz) {
            return (-(wx - this.x) * Math.sin(this.angle) + (wz - this.z) * Math.cos(this.angle)) / RADIUS;
        }

        public double distance(double wx, double wz) {
            return Math.sqrt((wx - this.x) * (wx - this.x) + (wz - this.z) * (wz - this.z));
        }

        public Part part(double wx, double wz) {
            return EclipseLayout.part(this.u(wx, wz), this.v(wx, wz));
        }
    }

    // The yin-yang, in the unit disc.
    public static Part part(double u, double v) {
        double r2 = u * u + v * v;
        if (r2 > 1.0) {
            return Part.NONE;
        }
        double top = u * u + (v - 0.5) * (v - 0.5);
        double bottom = u * u + (v + 0.5) * (v + 0.5);
        if (top < SEED * SEED) return Part.HELL;
        if (bottom < SEED * SEED) return Part.HEAVEN;
        if (top < 0.25) return Part.HEAVEN;
        if (bottom < 0.25) return Part.HELL;
        return u > 0 ? Part.HEAVEN : Part.HELL;
    }

    // The Eclipse of the cell holding this spot, or null if that cell has none.
    public static Eclipse at(double x, double z, DensityFunction nebula) {
        return inCell(Math.floorDiv((long) Math.floor(x), CELL), Math.floorDiv((long) Math.floor(z), CELL), nebula);
    }

    public static Eclipse inCell(long i, long j, DensityFunction nebula) {
        double n = nebula.compute(new DensityFunction.SinglePointContext((int) (i * CELL + CELL / 3), 0, (int) (j * CELL + 2 * CELL / 3)));
        long h = mix(i * 0x9E3779B97F4A7C15L ^ j * 0xC2B2AE3D27D4EB4FL ^ Double.doubleToLongBits(n));
        if (unit(h) > CHANCE) {
            return null;
        }
        double x = i * CELL + MARGIN + unit(mix(h + 1)) * (CELL - 2 * MARGIN);
        double z = j * CELL + MARGIN + unit(mix(h + 2)) * (CELL - 2 * MARGIN);
        if (x * x + z * z < MIN_DISTANCE * MIN_DISTANCE) {
            return null;
        }
        return new Eclipse(x, z, unit(mix(h + 3)) * Math.PI * 2.0);
    }

    // The nearest Eclipse's centre to a spot, searching cells out to `rings` away; null if none.
    public static Eclipse nearest(double x, double z, DensityFunction nebula, int rings) {
        long ci = Math.floorDiv((long) Math.floor(x), CELL), cj = Math.floorDiv((long) Math.floor(z), CELL);
        Eclipse best = null;
        double bestD = Double.MAX_VALUE;
        for (long i = ci - rings; i <= ci + rings; i++) {
            for (long j = cj - rings; j <= cj + rings; j++) {
                Eclipse e = inCell(i, j, nebula);
                if (e != null && e.distance(x, z) < bestD) {
                    bestD = e.distance(x, z);
                    best = e;
                }
            }
        }
        return best;
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    private EclipseLayout() {}
}
