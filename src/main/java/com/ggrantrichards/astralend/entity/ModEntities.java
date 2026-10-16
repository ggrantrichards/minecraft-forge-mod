package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// The mobs of the End. Where each one spawns is in its biome's JSON (natural spawns), except the
// Drift Whale and the Watcher, which MobSpawning places by hand so that
// there is never more than one near a player.
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AstralEnd.MODID);

    public static final RegistryObject<EntityType<EndPollinator>> POLLINATOR = ENTITY_TYPES.register("pollinator",
        () -> EntityType.Builder.of(EndPollinator::new, MobCategory.CREATURE).sized(0.5F, 0.55F).eyeHeight(0.3F)
            .clientTrackingRange(8).build("pollinator"));
    public static final RegistryObject<EntityType<UmbralStalker>> UMBRAL_STALKER = ENTITY_TYPES.register("umbral_stalker",
        () -> EntityType.Builder.of(UmbralStalker::new, MobCategory.MONSTER).sized(1.4F, 2.2F).eyeHeight(1.5F)
            .clientTrackingRange(8).build("umbral_stalker"));
    public static final RegistryObject<EntityType<DriftWhale>> DRIFT_WHALE = ENTITY_TYPES.register("drift_whale",
        () -> EntityType.Builder.of(DriftWhale::new, MobCategory.AMBIENT).sized(2.6F, 2.3F).eyeHeight(1.2F).fireImmune()
            .passengerAttachments(3.3F)    // the rider sits on its back (drawn 3.3 blocks up), not inside it
            .clientTrackingRange(16).build("drift_whale"));
    public static final RegistryObject<EntityType<Watcher>> WATCHER = ENTITY_TYPES.register("watcher",
        () -> EntityType.Builder.of(Watcher::new, MobCategory.MISC).sized(1.0F, 2.9F).eyeHeight(2.4F).fireImmune()
            .clientTrackingRange(16).build("watcher"));

    // Phase 7: live comets. Seen from far off and moved every tick, so they're tracked far and often.

    // Phase 10: the Spire Wyrm, mini-boss of the Resonant Spires. A long body, seen from far off.
    public static final RegistryObject<EntityType<SpireWyrm>> SPIRE_WYRM = ENTITY_TYPES.register("spire_wyrm",
        () -> EntityType.Builder.of(SpireWyrm::new, MobCategory.MONSTER).sized(2.0F, 2.0F).eyeHeight(1.2F).fireImmune()
            .clientTrackingRange(16).updateInterval(1).build("spire_wyrm"));

    // ---- The distillation: the Eclipse's mobs.
    public static final RegistryObject<EntityType<Hushback>> HUSHBACK = ENTITY_TYPES.register("hushback",
        () -> EntityType.Builder.of(Hushback::new, MobCategory.CREATURE).sized(1.0F, 1.3F).eyeHeight(1.15F)
            .clientTrackingRange(10).build("hushback"));
    public static final RegistryObject<EntityType<Hydragrip>> HYDRAGRIP = ENTITY_TYPES.register("hydragrip",
        () -> EntityType.Builder.of(Hydragrip::new, MobCategory.CREATURE).sized(1.0F, 0.8F).eyeHeight(0.65F)
            .clientTrackingRange(8).build("hydragrip"));
    public static final RegistryObject<EntityType<MirrorWretch>> MIRROR_WRETCH = ENTITY_TYPES.register("mirror_wretch",
        () -> EntityType.Builder.of(MirrorWretch::new, MobCategory.MONSTER).sized(1.2F, 2.2F).eyeHeight(2.0F)
            .clientTrackingRange(10).build("mirror_wretch"));

    // ---- The distillation: the Leviathan's keepers (placed by the structure; they don't spawn naturally).
    public static final RegistryObject<EntityType<Hollowback>> HOLLOWBACK = ENTITY_TYPES.register("hollowback",
        () -> EntityType.Builder.of(Hollowback::new, MobCategory.MONSTER).sized(1.0F, 1.3F).eyeHeight(1.15F)
            .clientTrackingRange(10).build("hollowback"));
    public static final RegistryObject<EntityType<Curator>> CURATOR = ENTITY_TYPES.register("curator",
        () -> EntityType.Builder.of(Curator::new, MobCategory.MONSTER).sized(2.0F, 4.4F).eyeHeight(3.9F).fireImmune()
            .clientTrackingRange(16).build("curator"));

    // ---- The caves: the Crystal Vaults' hunter.
    public static final RegistryObject<EntityType<Shardback>> SHARDBACK = ENTITY_TYPES.register("shardback",
        () -> EntityType.Builder.of(Shardback::new, MobCategory.MONSTER).sized(1.1F, 1.0F).eyeHeight(0.7F)
            .clientTrackingRange(10).build("shardback"));

    public static void register() {
        com.ggrantrichards.astralend.Events.on(EntityAttributeCreationEvent.class, event -> {
            event.put(POLLINATOR.get(), EndPollinator.createAttributes().build());
            event.put(UMBRAL_STALKER.get(), UmbralStalker.createAttributes().build());
            event.put(DRIFT_WHALE.get(), DriftWhale.createAttributes().build());
            event.put(WATCHER.get(), Watcher.createAttributes().build());
            event.put(SPIRE_WYRM.get(), SpireWyrm.createAttributes().build());
            event.put(HUSHBACK.get(), Hushback.createAttributes().build());
            event.put(HYDRAGRIP.get(), Hydragrip.createAttributes().build());
            event.put(MIRROR_WRETCH.get(), MirrorWretch.createAttributes().build());
            event.put(HOLLOWBACK.get(), Hollowback.createAttributes().build());
            event.put(CURATOR.get(), Curator.createAttributes().build());
            event.put(SHARDBACK.get(), Shardback.createAttributes().build());
        });
        com.ggrantrichards.astralend.Events.on(SpawnPlacementRegisterEvent.class, event -> {
            var replace = SpawnPlacementRegisterEvent.Operation.REPLACE;
            var ground = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            event.register(POLLINATOR.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::onAstralSoil, replace);
            event.register(UMBRAL_STALKER.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::inCave, replace);
            event.register(HUSHBACK.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::onSolidGround, replace);
            event.register(HYDRAGRIP.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::nearWater, replace);
            event.register(MIRROR_WRETCH.get(), SpawnPlacementTypes.ON_GROUND, ground, MirrorWretch::checkWretchSpawnRules, replace);
            event.register(SHARDBACK.get(), SpawnPlacementTypes.ON_GROUND, ground, ModEntities::inCave, replace);
        });
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean onAstralSoil(
        EntityType<T> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(ModBlocks.ASTRAL_SOIL);
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean onSolidGround(
        EntityType<T> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return !level.getBlockState(pos.below()).isAir();
    }

    // The Hydragrip: on the turf, with water within a few blocks.
    private static <T extends net.minecraft.world.entity.Entity> boolean nearWater(
        EntityType<T> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.below()).is(ModBlocks.ASTRAL_SOIL)) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    // The Umbral Stalker: only under a roof of rock, in the dark (the Glimmering Crags' root caves).
    private static boolean inCave(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason,
                                  BlockPos pos, RandomSource random) {
        // (1.21.1: canSeeSky asks for full sky light, which the End never has; so: is there anything overhead?)
        return pos.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())
            && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    private ModEntities() {}
}
