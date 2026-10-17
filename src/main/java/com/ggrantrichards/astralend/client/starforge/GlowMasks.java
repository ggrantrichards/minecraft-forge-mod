package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

// The outline of an armour texture in white, made from the armour's own texture when it's first
// needed: the Starforge glow lights only the armour's edges, and leaves the rest of it as it is.
// (1.21.1 port: NativeImage pixels are ABGR here; only the alpha is read, and the mask is white.)
final class GlowMasks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, ResourceLocation> MASKS = new HashMap<>();

    static ResourceLocation of(ResourceLocation armour) {
        return MASKS.computeIfAbsent(armour, GlowMasks::make);
    }

    private static ResourceLocation make(ResourceLocation armour) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation mask = ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID,
            "starforge_glow/" + armour.getNamespace() + "/" + armour.getPath().replace(".png", ""));
        try (InputStream in = minecraft.getResourceManager().getResource(armour).orElseThrow().open();
             NativeImage source = NativeImage.read(in)) {
            int w = source.getWidth(), h = source.getHeight();
            int[] alpha = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    alpha[y * w + x] = source.getPixelRGBA(x, y) >>> 24;
                }
            }
            boolean[] edge = edges(alpha, w, h);
            NativeImage image = new NativeImage(w, h, true);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    image.setPixelRGBA(x, y, edge[y * w + x] ? 0xFFFFFFFF : 0);
                }
            }
            minecraft.getTextureManager().register(mask, new DynamicTexture(image));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("[AstralEnd] couldn't make the Starforge glow for {}", armour, e);
            return armour;
        }
        return mask;
    }

    // The faces of the humanoid armour model's boxes on its 64x32 texture: u, v, width, height and depth
    // of the head, the helmet's outer layer, the body, an arm and a leg (the left ones mirror the right).
    private static final int[][] BOXES = {{0, 0, 8, 8, 8}, {32, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {40, 16, 4, 12, 4}, {0, 16, 4, 12, 4}};

    // The armour's outline: every drawn pixel at the border of its face (so each box's edges light up)
    // or beside a hole in the armour.
    static boolean[] edges(int[] alpha, int w, int h) {
        int scale = Math.max(1, w / 64);
        int[] face = new int[w * h];
        Arrays.fill(face, -1);
        int id = 0;
        for (int[] b : BOXES) {
            int u = b[0], v = b[1], bw = b[2], bh = b[3], d = b[4];
            int[][] faces = {{u + d, v, bw, d}, {u + d + bw, v, bw, d}, {u, v + d, d, bh}, {u + d, v + d, bw, bh},
                {u + d + bw, v + d, d, bh}, {u + d + bw + d, v + d, bw, bh}};
            for (int[] f : faces) {
                for (int y = f[1] * scale; y < Math.min(h, (f[1] + f[3]) * scale); y++) {
                    for (int x = f[0] * scale; x < Math.min(w, (f[0] + f[2]) * scale); x++) {
                        face[y * w + x] = id;
                    }
                }
                id++;
            }
        }
        boolean[] edge = new boolean[w * h];
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (alpha[i] < 16) {
                    continue;
                }
                for (int k = 1; k <= scale && !edge[i]; k++) {
                    for (int[] step : steps) {
                        int nx = x + step[0] * k, ny = y + step[1] * k;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h || face[ny * w + nx] != face[i] || alpha[ny * w + nx] < 16) {
                            edge[i] = true;
                            break;
                        }
                    }
                }
            }
        }
        return edge;
    }

    private GlowMasks() {}
}
