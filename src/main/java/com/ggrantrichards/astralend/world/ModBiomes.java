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
    public static final ResourceKey<Biome> NEBULA_FIELDS = ResourceKey.create(Registries.BIOME, id("nebula_fields"));

    // Biomes with low gravity. Datapacks can empty this tag to turn low gravity off.
    public static final TagKey<Biome> LOW_GRAVITY = TagKey.create(Registries.BIOME, id("low_gravity"));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, path);
    }

    private ModBiomes() {}
}
