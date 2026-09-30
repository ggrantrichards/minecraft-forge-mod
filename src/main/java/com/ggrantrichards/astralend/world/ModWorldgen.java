package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Registers the biome source type "astralend:astral_end", which
// data/minecraft/dimension/the_end.json uses for the End.
public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
        DeferredRegister.create(Registries.BIOME_SOURCE, AstralEnd.MODID);

    public static final RegistryObject<MapCodec<? extends BiomeSource>> ASTRAL_END =
        BIOME_SOURCES.register("astral_end", () -> AstralEndBiomeSource.CODEC);

    private ModWorldgen() {}
}
