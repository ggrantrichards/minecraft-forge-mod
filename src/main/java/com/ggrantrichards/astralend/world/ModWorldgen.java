package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.world.feature.CometCoreFeature;
import com.ggrantrichards.astralend.world.feature.CraterFeature;
import com.ggrantrichards.astralend.world.feature.GiantLumineshroomFeature;
import com.ggrantrichards.astralend.world.feature.LeviathanRibcageFeature;
import com.ggrantrichards.astralend.world.feature.NullwoodTreeFeature;
import com.ggrantrichards.astralend.world.feature.PetrifiedStalkFeature;
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
        FEATURE_TYPES.register("upwell", () -> UpwellFeature.CODEC);
        FEATURE_TYPES.register("giant_lumineshroom", () -> GiantLumineshroomFeature.CODEC);
        FEATURE_TYPES.register("tendril_underside", () -> TendrilUndersideFeature.CODEC);
        FEATURE_TYPES.register("nullwood_tree", () -> NullwoodTreeFeature.CODEC);
        FEATURE_TYPES.register("leviathan_ribcage", () -> LeviathanRibcageFeature.CODEC);
        FEATURE_TYPES.register("bone_fragment", () -> com.ggrantrichards.astralend.world.feature.BoneFragmentFeature.CODEC);
        FEATURE_TYPES.register("drift_bladder", () -> com.ggrantrichards.astralend.world.feature.DriftBladderFeature.CODEC);
        FEATURE_TYPES.register("bone_spire", () -> com.ggrantrichards.astralend.world.feature.BoneSpireFeature.CODEC);
        FEATURE_TYPES.register("withered_stalk", () -> com.ggrantrichards.astralend.world.feature.WitheredStalkFeature.CODEC);
        FEATURE_TYPES.register("crater", () -> CraterFeature.CODEC);
        FEATURE_TYPES.register("petrified_stalk", () -> PetrifiedStalkFeature.CODEC);
        // Phase 10
        FEATURE_TYPES.register("glimmer_spire", () -> com.ggrantrichards.astralend.world.feature.GlimmerSpireFeature.CODEC);
        // Phase 11
        FEATURE_TYPES.register("solarite_spire", () -> com.ggrantrichards.astralend.world.feature.SolariteSpireFeature.CODEC);
        FEATURE_TYPES.register("scorch_fissure", () -> com.ggrantrichards.astralend.world.feature.ScorchFissureFeature.CODEC);
        FEATURE_TYPES.register("oasis", () -> com.ggrantrichards.astralend.world.feature.OasisFeature.CODEC);
        FEATURE_TYPES.register("underside", () -> com.ggrantrichards.astralend.world.feature.UndersideFeature.CODEC);
        // The distillation
        FEATURE_TYPES.register("root_cave", () -> com.ggrantrichards.astralend.world.feature.RootCaveFeature.CODEC);
        FEATURE_TYPES.register("eclipse_terrain", () -> com.ggrantrichards.astralend.world.feature.EclipseTerrainFeature.CODEC);
        FEATURE_TYPES.register("border_blend", () -> com.ggrantrichards.astralend.world.feature.BorderBlendFeature.CODEC);
        FEATURE_TYPES.register("hollow_roots", () -> com.ggrantrichards.astralend.world.feature.HollowRootsFeature.CODEC);
        FEATURE_TYPES.register("crystal_vault", () -> com.ggrantrichards.astralend.world.feature.CrystalVaultFeature.CODEC);
        FEATURE_TYPES.register("eclipse_landmarks", () -> com.ggrantrichards.astralend.world.feature.EclipseLandmarksFeature.CODEC);
        FEATURE_TYPES.register("memory_moss", () -> com.ggrantrichards.astralend.world.feature.MemoryMossFeature.CODEC);
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

    private ModWorldgen() {}
}
