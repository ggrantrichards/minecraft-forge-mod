package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

// The shape of an armour texture in white, made from the armour's own texture when it's first needed,
// for the Starforge glow to light: drawn additively, a dark texture (netherite) would add almost no
// light, so the glow goes through this instead and is as strong on netherite as on diamond. The
// armour's shading is kept, softened, so the glow still shows its plates.
final class GlowMasks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, Identifier> MASKS = new HashMap<>();

    static Identifier of(Identifier armour) {
        return MASKS.computeIfAbsent(armour, GlowMasks::make);
    }

    private static Identifier make(Identifier armour) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier mask = Identifier.fromNamespaceAndPath(AstralEnd.MODID,
            "starforge_glow/" + armour.getNamespace() + "/" + armour.getPath().replace(".png", ""));
        try (InputStream in = minecraft.getResourceManager().getResource(armour).orElseThrow().open();
             NativeImage source = NativeImage.read(in)) {
            NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), true);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getPixel(x, y);
                    int alpha = argb >>> 24;
                    int luma = (((argb >> 16) & 0xFF) * 3 + ((argb >> 8) & 0xFF) * 6 + (argb & 0xFF)) / 10;
                    int v = alpha < 16 ? 0 : 150 + luma * 105 / 255;
                    image.setPixel(x, y, alpha << 24 | v << 16 | v << 8 | v);
                }
            }
            minecraft.getTextureManager().register(mask, new DynamicTexture(mask::toString, image));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("[AstralEnd] couldn't make the Starforge glow for {}", armour, e);
            return armour;
        }
        return mask;
    }

    private GlowMasks() {}
}
