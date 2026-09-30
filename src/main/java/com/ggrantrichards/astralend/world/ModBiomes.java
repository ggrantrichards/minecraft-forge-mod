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
    public static final ResourceKey<Biome> AURORA_SHALLOWS = key("aurora_shallows");
    public static final ResourceKey<Biome> COMET_WAKE = key("comet_wake");
    public static final ResourceKey<Biome> DARK_MATTER_BELT = key("dark_matter_belt");
    public static final ResourceKey<Biome> SHATTERED_OBSERVATORY = key("shattered_observatory");
    public static final ResourceKey<Biome> CORONA = key("corona");

    // Biomes with low gravity. Datapacks can empty this tag to turn low gravity off.
    public static final TagKey<Biome> LOW_GRAVITY = TagKey.create(Registries.BIOME, id("low_gravity"));
    // Biomes where Gravity Anomalies flip gravity. Datapacks can empty this tag to turn inversion off.
    public static final TagKey<Biome> GRAVITY_INVERSION = TagKey.create(Registries.BIOME, id("gravity_inversion"));

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, path);
    }

    private ModBiomes() {}
}
