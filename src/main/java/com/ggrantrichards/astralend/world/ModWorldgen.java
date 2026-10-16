package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.world.feature.CometCoreFeature;
import com.ggrantrichards.astralend.world.feature.CraterFeature;
import com.ggrantrichards.astralend.world.feature.GiantLumineshroomFeature;
import com.ggrantrichards.astralend.world.feature.LeviathanRibcageFeature;
import com.ggrantrichards.astralend.world.feature.NullwoodTreeFeature;
import com.ggrantrichards.astralend.world.feature.TendrilUndersideFeature;
import com.ggrantrichards.astralend.world.feature.UpwellFeature;
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
// (used by data/astralend/worldgen/configured_feature/*.json), and the structure type "astralend:island".
public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
        DeferredRegister.create(Registries.BIOME_SOURCE, AstralEnd.MODID);

    public static final RegistryObject<MapCodec<? extends BiomeSource>> ASTRAL_END =
        BIOME_SOURCES.register("astral_end", () -> AstralEndBiomeSource.CODEC);


    public static final DeferredRegister<Feature<?>> FEATURE_TYPES =
        DeferredRegister.create(Registries.FEATURE, AstralEnd.MODID);

    static {
        FEATURE_TYPES.register("comet_core", CometCoreFeature::new);
        FEATURE_TYPES.register("stardust_underhang", StardustUnderhangFeature::new);
        FEATURE_TYPES.register("upwell", UpwellFeature::new);
        FEATURE_TYPES.register("giant_lumineshroom", GiantLumineshroomFeature::new);
        FEATURE_TYPES.register("tendril_underside", TendrilUndersideFeature::new);
        FEATURE_TYPES.register("nullwood_tree", NullwoodTreeFeature::new);
        FEATURE_TYPES.register("leviathan_ribcage", LeviathanRibcageFeature::new);
        FEATURE_TYPES.register("bone_fragment", com.ggrantrichards.astralend.world.feature.BoneFragmentFeature::new);
        FEATURE_TYPES.register("drift_bladder", com.ggrantrichards.astralend.world.feature.DriftBladderFeature::new);
        FEATURE_TYPES.register("bone_spire", com.ggrantrichards.astralend.world.feature.BoneSpireFeature::new);
        FEATURE_TYPES.register("withered_stalk", com.ggrantrichards.astralend.world.feature.WitheredStalkFeature::new);
        FEATURE_TYPES.register("crater", CraterFeature::new);
        // Phase 10
        FEATURE_TYPES.register("glimmer_spire", com.ggrantrichards.astralend.world.feature.GlimmerSpireFeature::new);
        // Phase 11
        FEATURE_TYPES.register("scorch_fissure", com.ggrantrichards.astralend.world.feature.ScorchFissureFeature::new);
        FEATURE_TYPES.register("oasis", com.ggrantrichards.astralend.world.feature.OasisFeature::new);
        FEATURE_TYPES.register("underside", com.ggrantrichards.astralend.world.feature.UndersideFeature::new);
        // The distillation
        FEATURE_TYPES.register("root_cave", com.ggrantrichards.astralend.world.feature.RootCaveFeature::new);
        FEATURE_TYPES.register("eclipse_terrain", com.ggrantrichards.astralend.world.feature.EclipseTerrainFeature::new);
        FEATURE_TYPES.register("border_blend", com.ggrantrichards.astralend.world.feature.BorderBlendFeature::new);
        FEATURE_TYPES.register("hollow_roots", com.ggrantrichards.astralend.world.feature.HollowRootsFeature::new);
        FEATURE_TYPES.register("crystal_vault", com.ggrantrichards.astralend.world.feature.CrystalVaultFeature::new);
        FEATURE_TYPES.register("eclipse_landmarks", com.ggrantrichards.astralend.world.feature.EclipseLandmarksFeature::new);
        FEATURE_TYPES.register("memory_moss", com.ggrantrichards.astralend.world.feature.MemoryMossFeature::new);
    }

    // Phase 6: structures that check the ground before they start (see IslandStructure).
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, AstralEnd.MODID);

    public static final RegistryObject<StructureType<IslandStructure>> ISLAND =
        STRUCTURE_TYPES.register("island", () -> () -> IslandStructure.CODEC);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, AstralEnd.MODID);

    // Distillation 5: the Leviathan, a whole skeleton with a citadel built on it, by its own piece too.
    public static final RegistryObject<StructureType<com.ggrantrichards.astralend.world.structure.LeviathanStructure>> LEVIATHAN =
        STRUCTURE_TYPES.register("leviathan", () -> () -> com.ggrantrichards.astralend.world.structure.LeviathanStructure.CODEC);
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> LEVIATHAN_PIECE =
        STRUCTURE_PIECES.register("leviathan", () -> com.ggrantrichards.astralend.world.structure.LeviathanPiece::new);
    // Its skull, the Maw (the Rift Maw that was, merged into the one Leviathan).
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> LEVIATHAN_HEAD_PIECE =
        STRUCTURE_PIECES.register("leviathan_head", () -> com.ggrantrichards.astralend.world.structure.LeviathanHeadPiece::new);

    // (1.21.1 port) Density function types 26.3 has and 1.21.1 doesn't.
    public static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>> DENSITY_FUNCTION_TYPES =
        DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AstralEnd.MODID);

    public static final RegistryObject<MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>> DISTANCE_TO_POINT =
        DENSITY_FUNCTION_TYPES.register("distance_to_point", () -> DistanceToPoint.DATA_CODEC);

    private ModWorldgen() {}
}
