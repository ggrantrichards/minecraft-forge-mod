package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

// Keys for the mod's biomes and biome tags. The biomes themselves are defined in JSON under
// data/astralend/worldgen/biome/.
public final class ModBiomes {
    public static final ResourceKey<Biome> NEBULA_FIELDS = key("nebula_fields");
    public static final ResourceKey<Biome> ASHEN_BARRENS = key("ashen_barrens");
    public static final ResourceKey<Biome> HOLLOW_ISLES = key("hollow_isles");
    // The Eclipse: its heaven half ("eclipse") and its hell half, the old Corona ("corona").
    public static final ResourceKey<Biome> ECLIPSE = key("eclipse");
    public static final ResourceKey<Biome> CORONA = key("corona");
    public static final ResourceKey<Biome> CHORUS_THICKET = key("chorus_thicket");
    public static final ResourceKey<Biome> GLIMMERING_CRAGS = key("glimmering_crags");
    // The distillation's variants: denser, darker Hollow Isles, and the Crags broken into fragments.
    public static final ResourceKey<Biome> DEEP_HOLLOWS = key("deep_hollows");
    public static final ResourceKey<Biome> SHATTERED_CRAGS = key("shattered_crags");

    // Biomes with low gravity. Datapacks can empty this tag to turn low gravity off.
    public static final TagKey<Biome> LOW_GRAVITY = TagKey.create(Registries.BIOME, id("low_gravity"));
    // Biomes where Upwell Cores flip gravity. Datapacks can empty this tag to turn Upwells off.
    public static final TagKey<Biome> UPWELLS = TagKey.create(Registries.BIOME, id("upwells"));

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, path);
    }

    private ModBiomes() {}
}
