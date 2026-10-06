package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.world.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;

// The Nebula Fields' clouds: the biome is an emission nebula you can walk in, and its gas hangs low
// over the moss in drifting clouds - rose haze, the red of glowing hydrogen, pricked with cold
// pale-blue motes, the oxygen glow. The clouds are fixed shapes in the world (a slow 3D noise) that
// drift with a faint wind, so you can walk into one, through it and out again; nothing is placed, it's
// only particles, drawn round you while you're in the biome.
public final class NebulaMist {
    private static final DustParticleOptions ROSE = new DustParticleOptions(0xD06A98, 2.2F);
    private static final DustParticleOptions DEEP_ROSE = new DustParticleOptions(0x9A3A6E, 2.8F);
    private static final double SCALE = 18.0;          // cloud size, in blocks
    private static final double THRESHOLD = 0.6;       // how much of the air is cloud (higher: less)
    private static final int TRIES = 70;               // points tried round the player each tick

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(NebulaMist::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused() || level.dimension() != Level.END
                || !level.getBiome(player.blockPosition()).is(ModBiomes.NEBULA_FIELDS)) {
            return;
        }
        RandomSource random = level.getRandom();
        double wind = level.getGameTime() * 0.015;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < TRIES; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 64.0;
            double y = player.getY() + (random.nextDouble() - 0.35) * 22.0;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 64.0;
            if (cloud(x - wind, y, z - wind * 0.6) < THRESHOLD) {
                continue;
            }
            p.set(x, y, z);
            if (!level.getBlockState(p).isAir() || !level.getBiome(p).is(ModBiomes.NEBULA_FIELDS)) {
                continue;
            }
            // Low over the land: from a block above the ground to a dozen up, never out over the void.
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            if (ground <= level.getMinY() + 1 || y < ground + 1.0 || y > ground + 12.0) {
                continue;
            }
            if (random.nextFloat() < 0.22F) {
                level.addParticle(ParticleTypes.GLOW, x, y, z, (random.nextDouble() - 0.5) * 0.01, 0.004, (random.nextDouble() - 0.5) * 0.01);
            } else {
                level.addParticle(random.nextBoolean() ? ROSE : DEEP_ROSE, x, y, z, 0.01, 0.0, 0.006);
            }
        }
    }

    // A smooth 3D value noise in [0, 1), about SCALE blocks across (flattened a little in y, so the
    // clouds lie wider than they stand tall).
    private static double cloud(double x, double y, double z) {
        x /= SCALE;
        y /= SCALE * 0.6;
        z /= SCALE;
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        double fx = smooth(x - x0), fy = smooth(y - y0), fz = smooth(z - z0);
        double c00 = lerp(fx, hash(x0, y0, z0), hash(x0 + 1, y0, z0));
        double c10 = lerp(fx, hash(x0, y0 + 1, z0), hash(x0 + 1, y0 + 1, z0));
        double c01 = lerp(fx, hash(x0, y0, z0 + 1), hash(x0 + 1, y0, z0 + 1));
        double c11 = lerp(fx, hash(x0, y0 + 1, z0 + 1), hash(x0 + 1, y0 + 1, z0 + 1));
        return lerp(fz, lerp(fy, c00, c10), lerp(fy, c01, c11));
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }

    private static double hash(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ 0x6E7B1AL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    private NebulaMist() {}
}
