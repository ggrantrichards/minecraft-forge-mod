package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

// Keys for the mod's biomes and biome tags. The biomes themselves are defined in JSON under
// data/astralend/worldgen/biome/.
public final class ModBiomes {
    public static final ResourceKey<Biome> NEBULA_FIELDS = key("nebula_fields");
    public static final ResourceKey<Biome> ASHEN_BARRENS = key("ashen_barrens");
    // The Eclipse: the golden meadow, the one living, lit place in the End (after beta.4; its hell half,
    // the Corona, and the Seam between them are gone).
    public static final ResourceKey<Biome> ECLIPSE = key("eclipse");
    public static final ResourceKey<Biome> GLIMMERING_CRAGS = key("glimmering_crags");
    // The Deep Hollows: the dark Nullwood forests on ring islands (after beta.4 they took in the Hollow
    // Isles, which they had been a variant of).
    public static final ResourceKey<Biome> DEEP_HOLLOWS = key("deep_hollows");
    // The Glimmering Crags' variant: broken into fragments.
    public static final ResourceKey<Biome> SHATTERED_CRAGS = key("shattered_crags");
    // The caves (after beta.3): under the Deep Hollows, and under the Shattered Crags.
    public static final ResourceKey<Biome> HOLLOW_ROOTS = key("hollow_roots");
    public static final ResourceKey<Biome> CRYSTAL_VAULTS = key("crystal_vaults");

    // Biomes with low gravity. Datapacks can empty this tag to turn low gravity off.
    public static final TagKey<Biome> LOW_GRAVITY = TagKey.create(Registries.BIOME, id("low_gravity"));
    // Biomes where Upwell Cores flip gravity. Datapacks can empty this tag to turn Upwells off.
    public static final TagKey<Biome> UPWELLS = TagKey.create(Registries.BIOME, id("upwells"));

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, path);
    }

    private ModBiomes() {}
}
