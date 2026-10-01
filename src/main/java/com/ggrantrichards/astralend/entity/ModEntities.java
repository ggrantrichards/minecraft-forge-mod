package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// The mobs of the End. Where each one spawns is in its biome's JSON (natural spawns), except the
// Drift Whale, the Watcher and the Black Hole Guardian, which MobSpawning places by hand so that
// there is never more than one near a player.
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AstralEnd.MODID);

    public static final RegistryObject<EntityType<EndPollinator>> POLLINATOR = ENTITY_TYPES.register("pollinator",
        () -> EntityType.Builder.of(EndPollinator::new, MobCategory.CREATURE).sized(0.5F, 0.55F).eyeHeight(0.3F)
            .clientTrackingRange(8).build(ENTITY_TYPES.key("pollinator")));
    public static final RegistryObject<EntityType<VoidWanderer>> VOID_WANDERER = ENTITY_TYPES.register("void_wanderer",
        () -> EntityType.Builder.of(VoidWanderer::new, MobCategory.CREATURE).sized(0.75F, 0.8F).eyeHeight(0.45F)
            .clientTrackingRange(10).build(ENTITY_TYPES.key("void_wanderer")));
    public static final RegistryObject<EntityType<UmbralStalker>> UMBRAL_STALKER = ENTITY_TYPES.register("umbral_stalker",
        () -> EntityType.Builder.of(UmbralStalker::new, MobCategory.MONSTER).sized(1.4F, 2.2F).eyeHeight(1.5F)
            .clientTrackingRange(8).build(ENTITY_TYPES.key("umbral_stalker")));
    public static final RegistryObject<EntityType<CinderMite>> CINDER_MITE = ENTITY_TYPES.register("cinder_mite",
        () -> EntityType.Builder.of(CinderMite::new, MobCategory.MONSTER).sized(0.6F, 0.35F).eyeHeight(0.2F).fireImmune()
            .clientTrackingRange(8).build(ENTITY_TYPES.key("cinder_mite")));
    public static final RegistryObject<EntityType<CoronaSkimmer>> CORONA_SKIMMER = ENTITY_TYPES.register("corona_skimmer",
        () -> EntityType.Builder.of(CoronaSkimmer::new, MobCategory.CREATURE).sized(1.3F, 0.35F).eyeHeight(0.2F).fireImmune()
            .clientTrackingRange(10).build(ENTITY_TYPES.key("corona_skimmer")));
    public static final RegistryObject<EntityType<BlackHoleGuardian>> BLACK_HOLE_GUARDIAN = ENTITY_TYPES.register("black_hole_guardian",
        () -> EntityType.Builder.of(BlackHoleGuardian::new, MobCategory.MONSTER).sized(0.9F, 0.9F).eyeHeight(0.45F).fireImmune()
            .clientTrackingRange(10).build(ENTITY_TYPES.key("black_hole_guardian")));
    public static final RegistryObject<EntityType<DriftWhale>> DRIFT_WHALE = ENTITY_TYPES.register("drift_whale",
        () -> EntityType.Builder.of(DriftWhale::new, MobCategory.AMBIENT).sized(2.6F, 2.3F).eyeHeight(1.2F).fireImmune()
            .clientTrackingRange(16).build(ENTITY_TYPES.key("drift_whale")));
    public static final RegistryObject<EntityType<Watcher>> WATCHER = ENTITY_TYPES.register("watcher",
        () -> EntityType.Builder.of(Watcher::new, MobCategory.MISC).sized(1.0F, 2.9F).eyeHeight(2.4F).fireImmune()
            .clientTrackingRange(16).build(ENTITY_TYPES.key("watcher")));

    // Phase 6: the Orbital Station's guardian.
    public static final RegistryObject<EntityType<StationSentinel>> STATION_SENTINEL = ENTITY_TYPES.register("station_sentinel",
        () -> EntityType.Builder.of(StationSentinel::new, MobCategory.MONSTER).sized(1.6F, 3.2F).eyeHeight(2.7F).fireImmune()
            .clientTrackingRange(16).build(ENTITY_TYPES.key("station_sentinel")));

    // Phase 7: live comets. Seen from far off and moved every tick, so they're tracked far and often.
    public static final RegistryObject<EntityType<com.ggrantrichards.astralend.comet.LiveComet>> LIVE_COMET = ENTITY_TYPES.register("live_comet",
        () -> EntityType.Builder.of(com.ggrantrichards.astralend.comet.LiveComet::new, MobCategory.MISC).sized(2.2F, 2.2F).eyeHeight(1.1F)
            .fireImmune().clientTrackingRange(16).updateInterval(1).build(ENTITY_TYPES.key("live_comet")));

    public static void register() {
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(POLLINATOR.get(), EndPollinator.createAttributes().build());
            event.put(VOID_WANDERER.get(), VoidWanderer.createAttributes().build());
            event.put(UMBRAL_STALKER.get(), UmbralStalker.createAttributes().build());
            event.put(CINDER_MITE.get(), CinderMite.createAttributes().build());
            event.put(CORONA_SKIMMER.get(), CoronaSkimmer.createAttributes().build());
            event.put(BLACK_HOLE_GUARDIAN.get(), BlackHoleGuardian.createAttributes().build());
            event.put(DRIFT_WHALE.get(), DriftWhale.createAttributes().build());
            event.put(WATCHER.get(), Watcher.createAttributes().build());
            event.put(STATION_SENTINEL.get(), StationSentinel.createAttributes().build());
            event.put(LIVE_COMET.get(), com.ggrantrichards.astralend.comet.LiveComet.createAttributes().build());
        });
        SpawnPlacementRegisterEvent.BUS.addListener(event -> {
            var replace = SpawnPlacementRegisterEvent.Operation.REPLACE;
            var ground = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            event.register(POLLINATOR.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::onAstralSoil, replace);
            event.register(VOID_WANDERER.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::onSolidGround, replace);
            event.register(UMBRAL_STALKER.get(), SpawnPlacementTypes.ON_GROUND, ground, Monster::checkMonsterSpawnRules, replace);
            event.register(CINDER_MITE.get(), SpawnPlacementTypes.ON_GROUND, ground, CinderMite::checkCinderMiteSpawnRules, replace);
            event.register(CORONA_SKIMMER.get(), SpawnPlacementTypes.ON_GROUND, ground, CoronaSkimmer::checkSkimmerSpawnRules, replace);
        });
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean onAstralSoil(
        EntityType<T> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(ModBlocks.ASTRAL_SOIL);
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean onSolidGround(
        EntityType<T> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return !level.getBlockState(pos.below()).isAir();
    }

    private ModEntities() {}
}
