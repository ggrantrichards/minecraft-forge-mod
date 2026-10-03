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

    // The distillation: the Eclipse's island, a density function coded in Java (see EclipseDensity).
    public static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>> DENSITY_FUNCTION_TYPES =
        DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AstralEnd.MODID);

    static {
        DENSITY_FUNCTION_TYPES.register("eclipse", () -> EclipseDensity.DATA_CODEC);
    }

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
        FEATURE_TYPES.register("eclipse_landmarks", () -> com.ggrantrichards.astralend.world.feature.EclipseLandmarksFeature.CODEC);
    }

    // Phase 6: structures that check the ground before they start (see IslandStructure).
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, AstralEnd.MODID);

    public static final RegistryObject<StructureType<IslandStructure>> ISLAND =
        STRUCTURE_TYPES.register("island", () -> () -> IslandStructure.CODEC);

    // Phase 10: the colossal Whalefall, built by a piece of its own (too big for a template).
    public static final RegistryObject<StructureType<com.ggrantrichards.astralend.world.structure.WhalefallStructure>> WHALEFALL =
        STRUCTURE_TYPES.register("whalefall", () -> () -> com.ggrantrichards.astralend.world.structure.WhalefallStructure.CODEC);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, AstralEnd.MODID);
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> WHALEFALL_PIECE =
        STRUCTURE_PIECES.register("whalefall", () -> com.ggrantrichards.astralend.world.structure.WhalefallPiece::new);

    // Phase 11: the Rift Maw, a colossal skull with a hall inside, built by its own piece too.
    public static final RegistryObject<StructureType<com.ggrantrichards.astralend.world.structure.RiftMawStructure>> RIFT_MAW =
        STRUCTURE_TYPES.register("rift_maw", () -> () -> com.ggrantrichards.astralend.world.structure.RiftMawStructure.CODEC);
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> RIFT_MAW_PIECE =
        STRUCTURE_PIECES.register("rift_maw", () -> com.ggrantrichards.astralend.world.structure.RiftMawPiece::new);

    // Phase 11: the Remnant, a dead star's cinder hanging over each Corona.
    public static final RegistryObject<StructureType<com.ggrantrichards.astralend.world.structure.RemnantStructure>> REMNANT =
        STRUCTURE_TYPES.register("remnant", () -> () -> com.ggrantrichards.astralend.world.structure.RemnantStructure.CODEC);
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> REMNANT_PIECE =
        STRUCTURE_PIECES.register("remnant", () -> com.ggrantrichards.astralend.world.structure.RemnantPiece::new);

    private ModWorldgen() {}
}
