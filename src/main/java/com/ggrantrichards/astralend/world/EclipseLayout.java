package com.ggrantrichards.astralend.world;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

// Where the Eclipses are, and the shape of each. Shared by the biome source, the terrain
// (EclipseTerrainFeature: the Eclipse's island), its landmarks and the Sunstorms.
//
// The outer End is cut into cells CELL blocks square; three cells in four hold one Eclipse, a disc
// RADIUS blocks across the middle, at a spot and turn of its own. What decides is the "nebula" region
// noise (the noise router's temperature slot), read at a fixed point of each cell: it's seeded with
// the world, so every world has its own Eclipses, and everything that asks - which all read the
// same noise through the same Climate.Sampler - always agrees.
//
// (After beta.4 an Eclipse is one golden meadow: the yin-yang's hell half, the Seam between the halves
// and the fallen Remnant are gone. Its own frame (u, v) is still used to place things in it: the
// Wellspring in the middle, the ponds round it.)
public final class EclipseLayout {
    public static final int CELL = 4096;
    public static final double RADIUS = 180.0;
    private static final double MARGIN = RADIUS + 320.0;    // keeps an Eclipse well inside its cell
    private static final double CHANCE = 0.75;
    private static final double MIN_DISTANCE = 1700.0;      // from the centre of the End

    // In the Eclipse's frame (u, v): the Wellspring at its heart, and the little ponds the springs under
    // the meadow fill round it ({u, v, radius in blocks}).
    public static final double[] WELLSPRING = {0.0, 0.0};
    public static final double WELLSPRING_RADIUS = 0.15;
    public static final double[][] PONDS = {{-0.48, 0.32, 10.0}, {0.42, 0.46, 8.0}, {0.55, -0.38, 11.0}, {-0.3, -0.58, 8.5}, {-0.7, -0.12, 7.0},
        {0.12, 0.72, 7.0}};

    public enum Part { NONE, HEAVEN }

    private static final Map<RandomState, Climate.Sampler> SAMPLERS = Collections.synchronizedMap(new WeakHashMap<>());

    // The world's region noises, for code that isn't handed them (features, structures, the Sunstorms):
    // an uncached sampler, the kind vanilla's own commands use, made once per world.
    public static Climate.Sampler sampler(RandomState state) {
        return SAMPLERS.computeIfAbsent(state, RandomState::sampler);
    }

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

    // Inside the disc, or not.
    public static Part part(double u, double v) {
        return u * u + v * v > 1.0 ? Part.NONE : Part.HEAVEN;
    }

    // The Eclipse of the cell holding this spot, or null if that cell has none.
    public static Eclipse at(double x, double z, Climate.Sampler nebula) {
        return inCell(Math.floorDiv((long) Math.floor(x), CELL), Math.floorDiv((long) Math.floor(z), CELL), nebula);
    }

    public static Eclipse inCell(long i, long j, Climate.Sampler nebula) {
        double n = com.ggrantrichards.astralend.compat.Compat.sample(nebula.temperature(), (int) (i * CELL + CELL / 3), 0, (int) (j * CELL + 2 * CELL / 3));
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
    public static Eclipse nearest(double x, double z, Climate.Sampler nebula, int rings) {
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
