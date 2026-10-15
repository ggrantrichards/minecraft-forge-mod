package com.ggrantrichards.astralend.world.structure;

import net.minecraft.util.Mth;

// The shape of one Leviathan (after beta.4), in its own frame: w along it (the head toward +w), u across
// it, v up from the structure's origin. Every block is decided here from shapes, so each chunk is built
// on its own and they all agree; LeviathanPiece builds the body from it and LeviathanHeadPiece the skull.
// (Designed in a Python mirror, tools-side, and previewed before it was ported: keep the two in step.)
//
// Some 200 blocks from the tail's tip to the snout's, and no two alike - the seed decides:
//   - how its tail curls and droops, and where it snapped off (a gap, then the rest floating on);
//   - which ribs are whole, which broken off short, which gone;
//   - which lung has rotted away (if either);
//   - how many End City towers its builders raised on its back, and where; which carries the
//     Constellation Maze; where the hall along its spine starts, and where its roof has fallen in.
// What's always there, each with one job:
//   - the skull (the Maw): a boxy cranium with two eye sockets a side under a heavy brow, a long squared
//     snout with nostrils and blocky fangs, the lower jaw hanging open, and the throat - the wormhole -
//     closing the back of the mouth; inside the cranium, the first explorers' camp and a ledge up to the eyes;
//   - the reliquary in the ribcage: the Curator's arena, the four End Chimes pylons, the egg's pedestal;
//   - over the pedestal, hung from the spine, the heart - black and purple, still beating;
//   - the stomach, low at the back: a sac of Voidgel with what it swallowed;
//   - the towers' loot, and the maze on its tower.
// The colours: ivory bone; the builders' purpur, End Stone bricks, purple glass and End Rods; black-purple
// flesh with violet veins that pulse. Nothing red, nothing from the Nether.
public final class LeviathanShape {
    public static final int TAIL = -120, SKULL_W = 20, SNOUT = 80;
    // The frame's middle (where the structure's origin is): the skeleton reaches about 100 blocks each way.
    public static final int W0 = -20;
    static final int[] SLOTS = {-58, -40, -14, 6};
    static final int MAZE_HEIGHT = 24;

    public enum Material { BONE, SPINE, WEATHERED, WEATHERED_SPINE, LICHENED, LICHENED_SPINE, PURPUR, PURPUR_SLAB, PURPUR_PILLAR, END_BRICK, END_BRICK_WALL, GLASS, OBSIDIAN, THROAT, FLESH, HEART,
        BREATHCOMB, VOIDGEL, AIR }

    final long seed;
    final double tailAmp, tailDroop;
    final int tailBreak, hallFrom, collapse, mazeTower;
    final int[] towers;
    final boolean[] lungs = new boolean[2];
    final double[] ribReach = new double[20];   // per rib (10 a side, interleaved -/+): degrees reached, 0 = gone

    public LeviathanShape(long seed) {
        this.seed = seed;
        this.tailAmp = (this.r(1) * 2 - 1) * 16;
        this.tailDroop = 4 + this.r(2) * 8;
        this.tailBreak = -96 - (int) (this.r(3) * 12);
        int hall = -64 + (int) (this.r(4) * 16);
        this.collapse = -40 + (int) (this.r(5) * 40);
        this.mazeTower = this.r(6) < 0.5 ? SLOTS[0] : SLOTS[1];
        int[] picked = new int[SLOTS.length];
        int n = 0;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != this.mazeTower && (this.r(10 + i) < 0.6 || SLOTS[i] == -14)) {
                picked[n++] = SLOTS[i];
            }
        }
        this.towers = java.util.Arrays.copyOf(picked, n);
        this.hallFrom = Math.min(hall, this.mazeTower - 6);
        this.lungs[0] = this.r(7) > 0.25;
        this.lungs[1] = this.r(8) > 0.25;
        for (int i = 0; i < 10; i++) {
            for (int side = 0; side < 2; side++) {
                double q = this.r(100 + i * 2 + side);
                this.ribReach[i * 2 + side] = q < 0.15 ? 0 : q < 0.4 ? 70 + (q - 0.15) / 0.25 * 60 : 165;
            }
        }
    }

    // SplitMix64's finaliser: a well-spread number in [0, 1) for any input.
    static double mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * 0x1.0p-53;
    }

    double r(int k) {
        return mix(this.seed + (k + 1) * 0x9E3779B97F4A7C15L);
    }

    double hash(int a, int b, int c) {
        return mix(this.seed ^ (a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + c * 0x165667B19E3779F9L));
    }

    public static double sv(double w) {
        return 44 + 2.5 * Math.sin((w + 120) / 35.0);
    }

    // The bottom of the cranium.
    public static int b() {
        return Mth.floor(sv(SKULL_W)) - 12;
    }

    // The reliquary's floor.
    public static int floor() {
        return Mth.floor(sv(-24)) - 19;
    }

    // And its ceiling, for the Curator's second phase, when gravity turns over and the fight goes on
    // upside down (Upwelling lifts no further than 20 blocks).
    public static int ceiling() {
        return floor() + 16;
    }

    public int towerHeight(int t) {
        return t == this.mazeTower ? MAZE_HEIGHT : 16 + (int) (this.r(200 + t) * 6);
    }

    public int[] towers() {
        return this.towers;
    }

    public int mazeTower() {
        return this.mazeTower;
    }

    public int hallFrom() {
        return this.hallFrom;
    }

    // What's at the block (u, v, w), or null to leave it as it is. Bone comes in patches of its variants,
    // as vanilla's stone bricks come mossy and cracked: weathered bone (more of it toward the old tail),
    // and on bone that faces up, the End's lichen.
    public Material block(int u, int v, int w) {
        Material m = this.raw(u, v, w);
        if (m != Material.BONE && m != Material.SPINE) {
            return m;
        }
        boolean spine = m == Material.SPINE;
        Material above = this.raw(u, v + 1, w);
        if ((above == null || above == Material.AIR) && this.patches(u, v, w, 31, 5.0) > 0.58) {
            return spine ? Material.LICHENED_SPINE : Material.LICHENED;
        }
        // Weathered bone in broad patches with ragged, speckled edges (more of it toward the old tail).
        double weather = 0.7 * this.patches(u, v, w, 17, 8.0) + 0.3 * this.hash(u, v, w + 7919);
        if (weather > (w < -60 ? 0.5 : 0.56)) {
            return spine ? Material.WEATHERED_SPINE : Material.WEATHERED;
        }
        return m;
    }

    // Smooth noise in [0, 1), in patches about `scale` blocks across.
    double patches(int u, int v, int w, int salt, double scale) {
        double x = u / scale, y = v / scale, z = w / scale;
        int x0 = Mth.floor(x), y0 = Mth.floor(y), z0 = Mth.floor(z);
        double fx = x - x0, fy = y - y0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        double c00 = Mth.lerp(fx, this.hash(x0, y0, z0 + salt * 1000), this.hash(x0 + 1, y0, z0 + salt * 1000));
        double c10 = Mth.lerp(fx, this.hash(x0, y0 + 1, z0 + salt * 1000), this.hash(x0 + 1, y0 + 1, z0 + salt * 1000));
        double c01 = Mth.lerp(fx, this.hash(x0, y0, z0 + 1 + salt * 1000), this.hash(x0 + 1, y0, z0 + 1 + salt * 1000));
        double c11 = Mth.lerp(fx, this.hash(x0, y0 + 1, z0 + 1 + salt * 1000), this.hash(x0 + 1, y0 + 1, z0 + 1 + salt * 1000));
        return Mth.lerp(fz, Mth.lerp(fy, c00, c10), Mth.lerp(fy, c01, c11));
    }

    private Material raw(int u, int v, int w) {
        if (w >= SKULL_W - 1) {
            Material res = this.skull(u, v, w - SKULL_W);
            if (res != null || w >= SKULL_W + 3) {
                return res;
            }
        }
        return this.body(u, v, w);
    }

    // ------------------------------------------------------------------ the body

    private Material body(int u, int v, int w) {
        if (w < TAIL) {
            return null;
        }
        double sv = sv(w);
        // the citadel on its back: square towers (crowned, but the maze's), and the hall along the spine
        for (int i = -1; i < this.towers.length; i++) {
            int t = i < 0 ? this.mazeTower : this.towers[i];
            boolean maze = t == this.mazeTower;
            int rho = Math.max(Math.abs(u), Math.abs(w - t));
            int height = this.towerHeight(t);
            int top = maze ? height + 1 : height + 3;
            int wide = maze || v < sv + height - 5 ? 4 : 5;
            if (rho <= wide && v >= sv + 4 && v <= sv + top) {
                return this.tower(u, v - sv, w - t, rho, height, maze);
            }
        }
        if (w >= this.hallFrom && w <= SKULL_W + 1 && Math.abs(u) <= 5 && v >= sv + 4 && v < sv + 13) {
            Material hall = this.hall(u, v - sv, w);
            if (hall != null) {
                return hall;
            }
        }
        // the spine; the tail curls and droops past w = -60
        if (w > this.tailBreak - 3 && w < this.tailBreak) {
            return null;                                    // where the tail snapped off
        }
        double su = 0, svv = sv;
        if (w < -60) {
            double t = (-60 - w) / 60.0;
            su += this.tailAmp * t * t;
            svv -= this.tailDroop * t * t;
        }
        double du = u - su, dv = v - svv;
        int k = w - 6 * Math.round(w / 6.0F);
        int rc = w > -60 ? 2 : 1;
        // each vertebra a block of bone five long, a thinner neck of bone between them
        if (w > -90) {
            if (Math.abs(k) <= 2 && Math.abs(du) <= 1 && Math.abs(dv) <= 1 || Math.round(du) == 0 && Math.round(dv) == 0) {
                return Material.SPINE;
            }
        } else if (Math.round(du) == 0 && Math.round(dv) == 0) {
            return Material.SPINE;
        }
        if (Math.abs(k) <= 1 && Math.abs(du) <= rc && Math.abs(dv) <= rc) {
            return Material.SPINE;
        }
        boolean underHall = w >= this.hallFrom && w <= SKULL_W;
        int top = underHall ? 3 : w > -60 ? 7 : 3;
        if (dv > rc && dv <= rc + top && Math.abs(du) < 0.5) {
            int lean = Mth.floor((dv - rc) * 0.45);         // the spines lean back toward the tail
            if (k == -lean || k == -lean + 1 && dv <= rc + 2) {
                return Material.BONE;                       // the spinous process
            }
        }
        if (k == 0 && Math.abs(dv) < 0.5 && Math.abs(du) > rc && Math.abs(du) <= rc + 3 && w > -90) {
            return Material.BONE;                           // the side processes
        }
        // the ribs, and the reliquary they hold
        if (w >= -64 && w <= 9) {
            if (this.rib(u, v, w, sv)) {
                return Material.BONE;
            }
            if (this.insideCage(u, v, w, sv)) {
                Material organ = this.organ(u, v, w, sv);
                if (organ != null) {
                    return organ;
                }
                int floor = floor();
                if (v == floor && Math.abs(u) <= 9 && w >= -54 && w <= 6) {
                    return this.gap(u, w) ? Material.AIR
                        : Math.abs(u) == 9 || w == -54 || w == 6 ? Material.END_BRICK : Material.PURPUR;
                }
                if (v == floor + 1 && Math.abs(u) == 9 && w >= -54 && w <= 6 && Math.floorMod(w - 2, 7) != 0) {
                    return Material.END_BRICK_WALL;
                }
                if (v == ceiling() && Math.abs(u) <= 9 && w >= -54 && w <= 6) {
                    if (Math.abs(u - LADDER_U) <= 1 && Math.abs(w - LADDER_W) <= 1) {
                        return Material.AIR;                       // the ladder comes down through it
                    }
                    return this.ceilingGap(u, w) ? Material.AIR
                        : Math.abs(u) == 9 || w == -54 || w == 6 ? Material.END_BRICK : Material.PURPUR;
                }
            }
        }
        return null;
    }

    static double ribHalf(double w) {
        return 9 + 4 * Math.cos((w + 24) / 40.0 * Math.PI / 2);     // widest round the middle
    }

    // Each rib an arc down from the spine, swept back toward the tail as it goes down, as a whale's are -
    // thick at the spine, thinner to the tip.
    private boolean rib(int u, int v, int w, double sv) {
        double drop = Math.max(0.0, sv + 1.5 - v);
        for (int i = 0; i < 10; i++) {
            int rw = -56 + 7 * i;
            if (Math.abs(w - (rw - 0.24 * drop)) > 0.9) {
                continue;
            }
            int side = u < 0 ? 0 : 1;
            double reach = this.ribReach[i * 2 + side];
            if (reach == 0 || u == 0) {
                continue;
            }
            double a = ribHalf(rw), bb = 11.5, cv = sv - 10;
            double e = Math.hypot(u / a, (v - cv) / bb);
            if (Math.abs(e - 1) <= (drop < 9 ? 1.5 : 1.0) / a) {
                double ang = Math.toDegrees(Math.atan2(Math.abs(u) / a, (v - cv) / bb));
                if (ang <= reach) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insideCage(int u, int v, int w, double sv) {
        double a = ribHalf(w), bb = 11.5, cv = sv - 10;
        return w >= -54 && w <= 6 && Math.hypot(u / a, (v - cv) / bb) < 1 - 1.3 / a;
    }

    // Floor gaps in the reliquary: cells 3 blocks square, some open to the void below - never in the
    // arena's middle, by the pylons, the pedestal, the ladder's foot or under the stomach.
    boolean gap(int u, int w) {
        if (Math.abs(u) < 5 && Math.abs(w + 26) < 6 || Math.abs(u) < 5 && w > -8 || Math.abs(u + 3) < 3 && Math.abs(w + 31) < 3 || w < -42) {
            return false;
        }
        for (int[] p : PYLONS) {
            if (Math.abs(u - p[0]) < 3 && Math.abs(w - p[1]) < 3) {
                return false;
            }
        }
        return this.hash(Math.floorDiv(u, 3), Math.floorDiv(w, 3), 77) < 0.15;
    }

    // The ceiling's own gaps, open to the ribs above: never over the arena's middle, by the hanging pylons or
    // the ladder.
    boolean ceilingGap(int u, int w) {
        if (Math.abs(u) < 5 && Math.abs(w - ARENA_W) < 6 || Math.abs(u - LADDER_U) < 3 && Math.abs(w - LADDER_W) < 3 || w < -42 || w > -4) {
            return false;
        }
        for (int[] p : PYLONS) {
            if (Math.abs(u - p[0]) < 3 && Math.abs(w - p[1]) < 3) {
                return false;
            }
        }
        return this.hash(Math.floorDiv(u, 3), Math.floorDiv(w, 3), 78) < 0.12;
    }

    public static final int[][] PYLONS = {{-6, -33}, {6, -33}, {-6, -19}, {6, -19}};
    public static final int ARENA_W = -26, PEDESTAL_W = -2, HEART_W = -2, STOMACH_W = -48, LADDER_U = -3, LADDER_W = -31;

    private Material organ(int u, int v, int w, double sv) {
        // the heart: two lobes hung from the spine over the pedestal
        double hv = sv - 8;
        double h1 = Math.sqrt(u * u + Math.pow(v - hv, 2) + Math.pow(w + 2, 2)) / 3.6;
        double h2 = Math.sqrt(Math.pow(u - 1.5, 2) + Math.pow(v - (hv - 2), 2) + Math.pow(w + 4, 2) * 1.2) / 2.7;
        double h = Math.min(h1, h2);
        if (h <= 1.0) {
            return h <= 0.62 || this.hash(u, v, w) < 0.25 ? Material.HEART : Material.FLESH;
        }
        // its vessels: up into the spine and out to the ribs
        double[][][] vessels = {
            {{0, hv + 2, -2}, {0, sv - 1, 0}}, {{-2, hv - 1, -2}, {-9, sv - 14, 5}}, {{2, hv - 1, -2}, {9, sv - 14, 5}},
            {{-1.5, hv - 2, -4}, {-10, sv - 6, -12}}, {{1.5, hv - 2, -4}, {10, sv - 6, -12}}};
        for (double[][] c : vessels) {
            if (cone(u, v, w, c[0], c[1], 1.0)) {
                return Material.FLESH;
            }
        }
        // the lungs, if they haven't rotted away
        for (int i = 0; i < 2; i++) {
            int s = i == 0 ? -1 : 1;
            if (this.lungs[i] && Math.pow((u - s * 6) / 2.6, 2) + Math.pow((v - (sv - 5)) / 2.6, 2) + Math.pow((w + 26) / 14.0, 2) <= 1) {
                return Material.BREATHCOMB;
            }
        }
        // the stomach, low at the back: a sac of Voidgel with what it swallowed
        double st = Math.sqrt(Math.pow(u / 4.5, 2) + Math.pow((v - (floor() + 4)) / 3.8, 2) + Math.pow((w - STOMACH_W) / 5.0, 2));
        if (st <= 1) {
            return st >= 0.75 ? Material.VOIDGEL : Material.AIR;
        }
        return null;
    }

    static boolean cone(double u, double v, double w, double[] a, double[] b, double r) {
        double ex = b[0] - a[0], ey = b[1] - a[1], ez = b[2] - a[2];
        double len = ex * ex + ey * ey + ez * ez;
        double t = Mth.clamp(((u - a[0]) * ex + (v - a[1]) * ey + (w - a[2]) * ez) / len, 0.0, 1.0);
        double dx = u - (a[0] + ex * t), dy = v - (a[1] + ey * t), dz = w - (a[2] + ez * t);
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    // The End City hall along the spine: a purpur floor, End Stone brick walls with purple glass between purpur
    // pillars standing proud every six blocks, a pitched purpur roof with a ridge - fallen in along one stretch.
    // (null outside its walls: |u| = 5 is only its pillars.)
    private Material hall(int u, double h, int w) {
        boolean fallen = w >= this.collapse && w <= this.collapse + 5;
        boolean bay = Math.floorMod(w - this.hallFrom, 6) == 0;
        if (Math.abs(u) == 5) {
            return bay && h >= 5 && h < 10 ? Material.PURPUR_PILLAR : null;
        }
        if (h < 5) {
            return Material.PURPUR;
        }
        if (fallen && h >= 7) {
            if (h >= 10) {
                return null;
            }
            return Math.abs(u) < 4 || this.hash(u, Mth.floor(h), w) < 0.6 ? Material.AIR : Material.END_BRICK;
        }
        if (h >= 12) {
            return Math.abs(u) <= 1 ? Material.PURPUR_SLAB : null;     // the ridge
        }
        if (h >= 11) {
            return Math.abs(u) <= 2 ? Material.PURPUR : Math.abs(u) <= 3 ? Material.PURPUR_SLAB : null;
        }
        if (h >= 10) {
            return Material.PURPUR;
        }
        if (Math.abs(u) >= 4) {
            if (bay) {
                return Material.PURPUR_PILLAR;
            }
            int m = Math.floorMod(w - this.hallFrom, 6);
            boolean window = m >= 2 && m <= 4 && h >= 7 && h < 9;
            return window ? Material.GLASS : Material.END_BRICK;
        }
        return Material.AIR;
    }

    // An End City tower: purpur walls with pillars at the corners, a band of End Stone brick at every floor,
    // purple glass windows, a floor every six blocks, a door each way onto the hall. And (but the maze's,
    // whose flat roof the maze starts from) the End City's crown: the top floor overhangs a block all round
    // on slab brackets, with windows; battlements round the roof; a little cap in the middle for its beacon.
    private Material tower(int u, double h, int dw, int rho, int height, boolean maze) {
        if (!maze) {
            if (h >= height + 1) {
                if (rho <= 1) {
                    return h < height + 2 ? Material.PURPUR : h < height + 3 ? Material.PURPUR_SLAB : Material.AIR;
                }
                return rho == 5 && h < height + 2 && Math.floorMod(u + dw, 2) == 0 ? Material.PURPUR : Material.AIR;
            }
            if (h >= height) {
                return rho >= 4 ? Material.PURPUR : Material.PURPUR_SLAB;
            }
            if (h >= height - 5) {
                if (rho == 5) {
                    if (h < height - 4) {
                        return Material.PURPUR_SLAB;
                    }
                    boolean corner = Math.abs(u) == 5 && Math.abs(dw) == 5;
                    boolean window = Mth.floor(h) == height - 2 && (u == 0 || dw == 0);
                    return corner ? Material.PURPUR_PILLAR : window ? Material.GLASS : Material.PURPUR;
                }
                if (h >= height - 4) {
                    return h < height - 3 && !(u == 2 && dw == 2) ? Material.PURPUR : Material.AIR;
                }
            }
        }
        if (h >= height) {
            if (h < height + 1) {
                return maze || rho == 4 ? Material.PURPUR : Material.PURPUR_SLAB;
            }
            return Material.AIR;
        }
        if (rho == 4) {
            if (h >= 5 && h < 8 && Math.abs(u) <= 1) {
                return Material.AIR;                        // its doors, onto the hall
            }
            boolean corner = Math.abs(u) == 4 && Math.abs(dw) == 4;
            boolean window = Mth.floor(h) % 6 == 3 && (u == 0 || dw == 0);
            boolean band = Mth.floor(h) % 6 == 5;
            return corner ? Material.PURPUR_PILLAR : window ? Material.GLASS : band ? Material.END_BRICK : Material.PURPUR;
        }
        if (h < 5 || Mth.floor(h) % 6 == 4 && h > 8 && !(u == 2 && dw == 2)) {
            return Material.PURPUR;
        }
        return Material.AIR;
    }

    // ------------------------------------------------------------------ the skull

    // s = w - SKULL_W: the back of the cranium at 0, the snout's tip near 60.
    private Material skull(int u, int v, int s) {
        int b = b();
        int cv = b + 11;
        Material res = null;
        // the cranium: a boxy shell (a squarish superellipse), hollow
        double q = Math.pow(u / 13.0, 4) + Math.pow((v - cv) / 11.0, 4) + Math.pow((s - 12) / 13.0, 4);
        double qi = Math.pow(u / 11.0, 4) + Math.pow((v - cv) / 9.0, 4) + Math.pow((s - 12) / 11.0, 4);
        if (q <= 1) {
            if (qi > 1) {
                res = this.hash(u, v, s) > 0.02 ? Material.BONE : Material.AIR;   // weathered pits
            } else {
                res = v <= b + 3 ? Material.BONE : Material.AIR;
                if (v == cv + 2 && Math.abs(u) >= 8 && s >= 7 && s <= 23) {
                    res = Material.BONE;                    // the ledge up to the eyes
                }
            }
        }
        // brow ridges and cheekbones
        if (Math.abs(u) >= 12 && Math.abs(u) <= 14 && v >= cv + 6 && v <= cv + 7 && s >= 7 && s <= 24) {
            res = Material.BONE;
        }
        if (Math.abs(u) >= 12 && Math.abs(u) <= 13 && v >= cv - 3 && v <= cv - 2 && s >= 12 && s <= 25) {
            res = Material.BONE;
        }
        // a crest down the crown
        if (Math.abs(u) <= 1 && v >= cv + 10 && v <= cv + 12 && s >= 1 && s <= 18) {
            res = Material.BONE;
        }
        // the snout: a squared roof of bone with lips down its sides
        if (s >= 22 && s <= 60) {
            double f = (s - 22) / 38.0;
            double hw = 11 - 5 * f;
            if (s > 56) {
                hw *= Math.sqrt(Math.max(0, 1 - Math.pow((s - 56) / 4.5, 2)));
            }
            double tv = cv + 5 - 6 * f;
            double lip = tv - 6 + 2 * f;
            if (Math.abs(u) <= hw) {
                if (v >= tv - 2 && v <= tv || Math.abs(u) >= hw - 2 && v >= lip && v <= tv) {
                    res = Material.BONE;
                } else if (v > lip && v < tv - 2) {
                    res = Material.AIR;
                }
            }
            // the forehead: the cranium's roof sloping down onto the snout, so the head is one shape
            if (s <= 38) {
                double slope = cv + 11 - (s - 20) * (6 + 6 * (s - 20) / 18.0) / 18.0;
                if (Math.abs(u) <= hw - 1 && v > tv && v <= slope) {
                    res = Material.BONE;
                }
            }
            // the nostrils
            if (s >= 49 && s <= 51 && Math.abs(u) >= 2 && Math.abs(u) <= 3 && v >= tv - 2 && v <= tv) {
                res = v == tv - 2 ? Material.OBSIDIAN : Material.AIR;
            }
            // the upper teeth; the fangs near the front
            if (Math.abs(u) <= hw && Math.abs(u) >= hw - 1.5 && Math.floorMod(s - 25, 3) == 0 && s < 58) {
                int length = 3 + (int) (this.hash(u > 0 ? 1 : 0, 1, s) * 3) + (s >= 49 && s <= 52 ? 4 : 0);
                if (v >= lip - length && v < lip) {
                    res = Material.BONE;
                }
            }
        }
        // the throat: a disc of dark stone closing the back of the mouth, in a wall of bone
        double t = u * u + Math.pow(v - (b + 6), 2);
        if (s >= 22 && s <= 23 && v >= b + 1 && v <= cv + 1 && Math.abs(u) <= 11) {
            res = t <= 4.5 * 4.5 ? Material.THROAT : Material.BONE;
        }
        // the tongue-bone: a ledge in the mouth to stand on (where the Maw's travellers come out)
        if (Math.abs(u) <= 3 && v >= b - 1 && v <= b && s >= 24 && s <= 36) {
            res = Material.BONE;
        }
        // the lower jaw, hanging open, its teeth up
        if (s >= 1 && s <= 58) {
            double jv = b + 1 - 0.32 * (s - 1);
            double hwj = 11 - 4 * (s - 1) / 57.0;
            if (v >= jv - 4 && v <= jv && (Math.abs(u) >= hwj - 3 && Math.abs(u) <= hwj || s >= 54 && Math.abs(u) <= hwj)) {
                res = Material.BONE;
            }
            if (Math.abs(u) >= hwj - 1.5 && Math.abs(u) <= hwj && Math.floorMod(s - 8, 3) == 0 && s < 54) {
                int length = 2 + (int) (this.hash(u > 0 ? 1 : 0, 2, s) * 3);
                if (v > jv && v <= jv + length) {
                    res = Material.BONE;
                }
            }
        }
        // the eye sockets, two each side, rimmed in obsidian
        int[][] sockets = {{16, 22, cv + 1, cv + 5}, {9, 13, cv + 1, cv + 4}};
        for (int[] k : sockets) {
            if (Math.abs(u) >= 9 && Math.abs(u) <= 13) {
                if (s >= k[0] && s <= k[1] && v >= k[2] && v <= k[3]) {
                    res = Material.AIR;
                } else if (s >= k[0] - 1 && s <= k[1] + 1 && v >= k[2] - 1 && v <= k[3] + 1 && Math.abs(u) >= 11 && q <= 1.0
                    && res == Material.BONE) {
                    res = Material.OBSIDIAN;
                }
            }
        }
        // the way in at the back, where the spine and the hall come in
        if (s <= 3 && Math.abs(u) <= 2) {
            double svv = sv(SKULL_W);
            if (v >= svv - 2 && v <= svv + 1 || v >= svv + 4 && v <= svv + 8) {
                res = Material.AIR;
            }
        }
        return res;
    }

    // The four eyes, in the frame: {u, v, w}, the front socket's then the back one's, each side.
    public static int[][] eyes() {
        int cv = b() + 11;
        return new int[][] {{-10, cv + 3, SKULL_W + 19}, {10, cv + 3, SKULL_W + 19}, {-10, cv + 2, SKULL_W + 11}, {10, cv + 2, SKULL_W + 11}};
    }

    public static int[] throat() {
        return new int[] {0, b() + 6, SKULL_W + 22};
    }
}
