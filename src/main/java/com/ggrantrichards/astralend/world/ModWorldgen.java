package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.world.feature.CometCoreFeature;
import com.ggrantrichards.astralend.world.feature.CometIceSpireFeature;
import com.ggrantrichards.astralend.world.feature.CraterFeature;
import com.ggrantrichards.astralend.world.feature.GiantLumineshroomFeature;
import com.ggrantrichards.astralend.world.feature.LeviathanRibcageFeature;
import com.ggrantrichards.astralend.world.feature.NullwoodTreeFeature;
import com.ggrantrichards.astralend.world.feature.ObservatoryRingFeature;
import com.ggrantrichards.astralend.world.feature.PetrifiedStalkFeature;
import com.ggrantrichards.astralend.world.feature.TendrilUndersideFeature;
import com.ggrantrichards.astralend.world.feature.InversionPocketFeature;
import com.ggrantrichards.astralend.world.feature.ObservatoryRuinFeature;
import com.ggrantrichards.astralend.world.feature.StardustUnderhangFeature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import com.ggrantrichards.astralend.world.structure.IslandStructure;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Registers the biome source type "astralend:astral_end", which
// data/minecraft/dimension/the_end.json uses for the End, and the feature types coded in Java
// (used by data/astralend/worldgen/feature/*.json), and the structure type "astralend:island".
public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
        DeferredRegister.create(Registries.BIOME_SOURCE, AstralEnd.MODID);

    public static final RegistryObject<MapCodec<? extends BiomeSource>> ASTRAL_END =
        BIOME_SOURCES.register("astral_end", () -> AstralEndBiomeSource.CODEC);

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
        DeferredRegister.create(Registries.FEATURE_TYPE, AstralEnd.MODID);

    static {
        FEATURE_TYPES.register("comet_core", () -> CometCoreFeature.CODEC);
        FEATURE_TYPES.register("stardust_underhang", () -> StardustUnderhangFeature.CODEC);
        FEATURE_TYPES.register("inversion_pocket", () -> InversionPocketFeature.CODEC);
        FEATURE_TYPES.register("observatory_ruin", () -> ObservatoryRuinFeature.CODEC);
        FEATURE_TYPES.register("giant_lumineshroom", () -> GiantLumineshroomFeature.CODEC);
        FEATURE_TYPES.register("tendril_underside", () -> TendrilUndersideFeature.CODEC);
        FEATURE_TYPES.register("nullwood_tree", () -> NullwoodTreeFeature.CODEC);
        FEATURE_TYPES.register("leviathan_ribcage", () -> LeviathanRibcageFeature.CODEC);
        FEATURE_TYPES.register("comet_ice_spire", () -> CometIceSpireFeature.CODEC);
        FEATURE_TYPES.register("crater", () -> CraterFeature.CODEC);
        FEATURE_TYPES.register("petrified_stalk", () -> PetrifiedStalkFeature.CODEC);
        FEATURE_TYPES.register("observatory_ring", () -> ObservatoryRingFeature.CODEC);
    }

    // Phase 6: structures that check the ground before they start (see IslandStructure).
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, AstralEnd.MODID);

    public static final RegistryObject<StructureType<IslandStructure>> ISLAND =
        STRUCTURE_TYPES.register("island", () -> () -> IslandStructure.CODEC);

    private ModWorldgen() {}
}
