package com.ggrantrichards.astralend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

// The fog eases from one biome's colour to the next over about fifty blocks, instead of the few the
// game blends over. Round the camera, the fog colour of each biome on a coarse grid (BiomeFogs) is
// averaged, weighted toward the middle; the game's own fog colour - which has everything else in it,
// blindness and the like - is then scaled by how that average differs from the colour of the biome
// the camera is in.
public final class FogBlend {
    private static final int STEP = 12;
    private static final int RADIUS = 4;          // grid points each way: 48 blocks
    private static final double SIGMA = 26.0;
    private static long lastTick = -1;
    private static BlockPos lastPos;
    private static float[] ratio = {1.0F, 1.0F, 1.0F};

    public static void register() {
        ViewportEvent.ComputeFogColor.BUS.addListener(FogBlend::onFogColor);
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.END) {
            return;
        }
        Vec3 at = event.getCamera().position();
        BlockPos pos = BlockPos.containing(at);
        // Worked out at most once a tick, or when the camera has moved a block.
        if (level.getGameTime() != lastTick || !pos.equals(lastPos)) {
            lastTick = level.getGameTime();
            lastPos = pos;
            ratio = ratio(level, pos);
        }
        event.setRed(Math.min(1.0F, event.getRed() * ratio[0]));
        event.setGreen(Math.min(1.0F, event.getGreen() * ratio[1]));
        event.setBlue(Math.min(1.0F, event.getBlue() * ratio[2]));
    }

    private static float[] ratio(ClientLevel level, BlockPos pos) {
        Integer own = fog(level, pos);
        if (own == null) {
            return new float[] {1.0F, 1.0F, 1.0F};
        }
        double r = 0, g = 0, b = 0, total = 0;
        for (int i = -RADIUS; i <= RADIUS; i++) {
            for (int j = -RADIUS; j <= RADIUS; j++) {
                BlockPos p = pos.offset(i * STEP, 0, j * STEP);
                if (!level.hasChunkAt(p)) {
                    continue;
                }
                Integer c = fog(level, p);
                if (c == null) {
                    continue;
                }
                double w = Math.exp(-(i * i + j * j) * STEP * STEP / (2.0 * SIGMA * SIGMA));
                r += w * ((c >> 16) & 255);
                g += w * ((c >> 8) & 255);
                b += w * (c & 255);
                total += w;
            }
        }
        if (total <= 0) {
            return new float[] {1.0F, 1.0F, 1.0F};
        }
        return new float[] {scale(r / total, (own >> 16) & 255), scale(g / total, (own >> 8) & 255), scale(b / total, own & 255)};
    }

    private static float scale(double average, int own) {
        return (float) Math.max(0.25, Math.min(4.0, (average + 4.0) / (own + 4.0)));
    }

    private static Integer fog(ClientLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(k -> BiomeFogs.FOG.get(k.identifier().toString())).orElse(null);
    }

    private FogBlend() {}
}
