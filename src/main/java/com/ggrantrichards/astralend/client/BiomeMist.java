package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.world.ModBiomes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;

// The air of a few biomes, drawn round you while you're in them - only particles, nothing placed. Each
// is in fixed cloud shapes in the world (a slow 3D noise) drifting on a faint wind, so you walk into
// a bank of it, through it and out again:
//   - the Nebula Fields: their gas hangs low over the moss - rose haze, the red of glowing hydrogen,
//     pricked with cold pale-blue motes, the oxygen glow;
//   - the Ashen Barrens (after beta.4): a thick, pale, suffocating mist of ash lying over the shelves,
//     the only pale air in the End;
//   - the Shattered Crags (after beta.4): starlight falling - sparks drifting down between the shards
//     and on into the void;
//   - the Eclipse (after beta.4, the golden meadow): gold dust hanging in the air, like pollen in low
//     sun, with now and then a teal glint.
public final class BiomeMist {
    private record Profile(ResourceKey<Biome> biome, ParticleOptions[] haze, ParticleOptions glow, float glowShare,
                           double scale, double threshold, int tries, double minAbove, double maxAbove,
                           boolean overVoid, double fall, double rise) {}

    private static final List<Profile> PROFILES = List.of(
        new Profile(ModBiomes.NEBULA_FIELDS,
            new ParticleOptions[] {new DustParticleOptions(0xF08CBC, 3.0F), new DustParticleOptions(0xC04C88, 3.6F)},
            ParticleTypes.GLOW, 0.4F, 18.0, 0.56, 120, 1.0, 12.0, false, 0.0, 0.0),
        new Profile(ModBiomes.ASHEN_BARRENS,
            new ParticleOptions[] {new DustParticleOptions(0xDCD6C8, 4.0F), new DustParticleOptions(0xB8B2A4, 3.4F)},
            ParticleTypes.WHITE_ASH, 0.15F, 22.0, 0.44, 150, 0.0, 9.0, false, 0.0, 0.0),
        new Profile(ModBiomes.ECLIPSE,
            new ParticleOptions[] {new DustParticleOptions(0xF2C45A, 1.1F), new DustParticleOptions(0xE0A23A, 0.8F)},
            ParticleTypes.GLOW, 0.1F, 20.0, 0.42, 90, 1.0, 10.0, false, 0.0, 0.0),
        new Profile(ModBiomes.SHATTERED_CRAGS,
            new ParticleOptions[0],
            ParticleTypes.END_ROD, 1.0F, 14.0, 0.5, 26, -40.0, 30.0, true, -0.05, 0.0));

    public static void register() {
        com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, BiomeMist::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused() || level.dimension() != Level.END) {
            return;
        }
        for (Profile profile : PROFILES) {
            if (level.getBiome(player.blockPosition()).is(profile.biome())) {
                mist(level, player, profile);
                return;
            }
        }
    }

    private static void mist(ClientLevel level, LocalPlayer player, Profile profile) {
        RandomSource random = level.getRandom();
        double wind = level.getGameTime() * 0.015;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < profile.tries(); i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 64.0;
            double y = player.getY() + (random.nextDouble() - 0.35) * 22.0;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 64.0;
            if (cloud(x - wind, y, z - wind * 0.6, profile.scale()) < profile.threshold()) {
                continue;
            }
            p.set(x, y, z);
            if (!level.getBlockState(p).isAir() || !level.getBiome(p).is(profile.biome())) {
                continue;
            }
            if (!profile.overVoid()) {
                // Low over the land, never out over the void.
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
                if (ground <= level.getMinBuildHeight() + 1 || y < ground + profile.minAbove() || y > ground + profile.maxAbove()) {
                    continue;
                }
            }
            if (profile.haze().length == 0 || random.nextFloat() < profile.glowShare()) {
                level.addParticle(profile.glow(), x, y, z, (random.nextDouble() - 0.5) * 0.01,
                    profile.fall() != 0.0 ? profile.fall() : 0.004, (random.nextDouble() - 0.5) * 0.01);
            } else {
                level.addParticle(profile.haze()[random.nextInt(profile.haze().length)], x, y, z, 0.01, profile.rise(), 0.006);
            }
        }
    }

    // A smooth 3D value noise in [0, 1), about `scale` blocks across (flattened a little in y, so the
    // clouds lie wider than they stand tall).
    private static double cloud(double x, double y, double z, double scale) {
        x /= scale;
        y /= scale * 0.6;
        z /= scale;
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

    private BiomeMist() {}
}
