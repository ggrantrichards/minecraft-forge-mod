package com.ggrantrichards.astralend.maze;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// The constellations, fixed in the End's sky (the sky renderer draws them over the Eclipse; anywhere
// else you find them through a spyglass). The first four answer the Constellation Mazes' bridges;
// five of them - the Leviathan, the Maw, the Watcher, the Dead Star and the Whale - are the Starforge's,
// each sighted from the Eclipse before its upgrade can be forged. Each has a centre in the sky, as a yaw and pitch the
// way Minecraft measures a look (yaw 0 = south, 90 = west; pitch -90 = straight up), and its stars as
// offsets in degrees from that centre (right, up), joined in order by the lines in `lines`.
public final class Constellations {
    public record Constellation(String name, float yaw, float pitch, float[][] stars, int[][] lines) {
        // Which way it lies in the sky, as a unit vector (the same as an entity's look vector).
        public Vec3 direction() {
            return look(this.yaw, this.pitch);
        }
    }

    public static final Constellation[] ALL = {
        // The Leviathan: a long body, a fluked tail.
        new Constellation("leviathan", -135.0F, -52.0F,
            new float[][] {{-15, -1}, {-9, 2}, {-3, 3}, {3, 2}, {9, 0}, {13, -3}, {16, 1}, {-17, -4}},
            new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {4, 6}, {0, 7}}),
        // The Maw: two jaws, open.
        new Constellation("maw", -45.0F, -40.0F,
            new float[][] {{-9, 2}, {-4, 7}, {3, 7}, {9, 3}, {-9, -1}, {-4, -6}, {3, -6}, {9, -2}},
            new int[][] {{0, 1}, {1, 2}, {2, 3}, {4, 5}, {5, 6}, {6, 7}, {0, 4}}),
        // The Rider (from the Mystic Mountain nebula): a knight on a horse, facing you - the horse's long
        // face in the middle, its shoulders, a foreleg and the near hindquarter; the knight's feet hanging
        // either side of its neck; helmed and plumed, his lance raised up and to the left.
        new Constellation("rider", 45.0F, -58.0F,
            new float[][] {{0, 0}, {0, -5.5F}, {-1.5F, 2.4F}, {1.5F, 2.4F}, {-3.6F, -3.2F}, {3.6F, -3.2F}, {3.9F, -8}, {3.5F, -12},
                {-6, -4.5F}, {-6.4F, -11.5F}, {0, 4.2F}, {-3.4F, -0.8F}, {3.4F, -0.8F}, {0, 7.6F}, {0, 9.8F}, {1, 11.8F}, {-8, 14}},
            new int[][] {{0, 1}, {0, 2}, {0, 3}, {4, 5}, {5, 6}, {6, 7}, {4, 8}, {8, 9}, {10, 11}, {10, 12}, {10, 13}, {13, 14},
                {14, 15}, {13, 16}}),
        // The Watcher: a tall, thin figure.
        new Constellation("watcher", 135.0F, -46.0F,
            new float[][] {{0, 10}, {0, 4}, {-5, 2}, {5, 2}, {0, -3}, {-3, -10}, {3, -10}},
            new int[][] {{0, 1}, {1, 2}, {1, 3}, {1, 4}, {4, 5}, {4, 6}}),
        // The Dead Star: a broken ring round a burning heart, high overhead. (Its id is still "remnant".)
        new Constellation("remnant", 0.0F, -70.0F,
            new float[][] {{0, 0}, {0, 7}, {6, 4}, {7, -2}, {3, -7}, {-4, -6}, {-7, 0}},
            new int[][] {{1, 2}, {2, 3}, {3, 4}, {5, 6}, {6, 1}}),
        // The Whale: a long arc of back, a fluke, and its little eye.
        new Constellation("whale", 180.0F, -30.0F,
            new float[][] {{-12, 0}, {-6, 4}, {1, 5}, {8, 3}, {12, -1}, {15, 3}, {15, -5}, {-8, 1}},
            new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {4, 6}}),
    };

    public static int byName(String name) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public static Constellation get(int id) {
        return ALL[Math.floorMod(id, ALL.length)];
    }

    public static Vec3 look(float yaw, float pitch) {
        float y = yaw * Mth.DEG_TO_RAD;
        float p = pitch * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(y) * Mth.cos(p), -Mth.sin(p), Mth.cos(y) * Mth.cos(p));
    }

    // A star of a constellation, as a direction: its centre turned `right` and `up` degrees.
    public static Vec3 star(Constellation c, float right, float up) {
        Vec3 centre = c.direction();
        Vec3 r = centre.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 u = r.cross(centre).normalize();
        return centre.add(r.scale(Math.tan(Math.toRadians(right)))).add(u.scale(Math.tan(Math.toRadians(up)))).normalize();
    }

    private Constellations() {}
}
