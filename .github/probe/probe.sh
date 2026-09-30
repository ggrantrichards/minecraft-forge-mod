#!/usr/bin/env bash
# Temporary: prints Minecraft/Forge 26.3 source and vanilla data that Phase 2 depends on.
set +e
ROOT=$PWD
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
cd /tmp/src
show() { echo "=== FILE $1"; grep -v '^import' "$1"; echo "=== END $1"; }
show net/minecraft/world/level/biome/TheEndBiomeSource.java
grep -n -E 'abstract|public .*\(|protected .*\(' net/minecraft/world/level/biome/BiomeSource.java | head -40
grep -n -E 'THE_END|END_HIGHLANDS|END_MIDLANDS|SMALL_END_ISLANDS|END_BARRENS' net/minecraft/world/level/biome/Biomes.java
grep -rn -E 'static .* retrieveElement|static .* retrieveGetter' net/minecraft/resources/RegistryOps.java
grep -n -E 'BIOME_SOURCE|DENSITY_FUNCTION|NOISE_SETTINGS' net/minecraft/core/registries/Registries.java
echo "=== CLIMATE"; grep -n -E 'record TargetPoint|public .* erosion\(|public .* temperature\(|interface Sampler|TargetPoint sample' -A2 net/minecraft/world/level/biome/Climate.java | head -40
grep -n -E 'public static (long|float) (quantizeCoord|unquantizeCoord)' net/minecraft/world/level/biome/Climate.java
echo "=== ATTR"; grep -n -E 'GRAVITY|JUMP_STRENGTH|SAFE_FALL_DISTANCE' net/minecraft/world/entity/ai/attributes/Attributes.java
grep -v '^import' net/minecraft/world/entity/ai/attributes/AttributeModifier.java | head -60
grep -n -E 'public .*(Modifier|getValue)\(' net/minecraft/world/entity/ai/attributes/AttributeInstance.java | head -20
grep -n -E 'public .* getAttribute\(' net/minecraft/world/entity/LivingEntity.java
echo "=== LEVEL"; grep -n -E 'ResourceKey<Level> (END|OVERWORLD)|findClosestBiome3d|public .* getBiome\(' net/minecraft/world/level/Level.java net/minecraft/server/level/ServerLevel.java net/minecraft/world/level/LevelReader.java | head
grep -n -E 'public .* randomState\(|public .* getGenerator\(' net/minecraft/server/level/ServerChunkCache.java
grep -n -E 'public .* getBiomeSource\(' net/minecraft/world/level/chunk/ChunkGenerator.java
grep -n -E 'public .* sampler\(' net/minecraft/world/level/levelgen/RandomState.java
grep -n -E 'public .* halt\(|public .* getLevel\(|public Iterable<ServerLevel> getAllLevels' net/minecraft/server/MinecraftServer.java
ls net/minecraft/world/entity/projectile/ | grep -i pearl; ls net/minecraft/world/entity/projectile/throwableitemprojectile 2>/dev/null | grep -i pearl
echo "=== FORGE EVENTS"
find net/minecraftforge -name 'TickEvent.java' -o -name 'LivingEvent.java' -o -name 'EntityJoinLevelEvent.java' -o -name 'ServerStartedEvent.java' -o -name 'ForgeConfigSpec.java' -o -name 'ModConfig.java' -o -name 'EntityTickEvent.java' | head
for f in $(find net/minecraftforge -name 'TickEvent.java' -o -name 'EntityJoinLevelEvent.java' -o -name 'ServerStartedEvent.java' | head -3); do echo "=== FILE $f"; grep -v '^import' "$f" | grep -n -E 'class|record|interface|BUS|public ' | head -40; done
grep -rn -E 'class LivingTickEvent|record LivingTickEvent|LivingTickEvent' net/minecraftforge/event/entity/living/LivingEvent.java | head -5
grep -rn -E 'public .* registerConfig\(' net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext.java net/minecraftforge/fml/ModLoadingContext.java 2>/dev/null | head
grep -rn -E 'public (static )?.*(define|build|push|pop|comment)\(' net/minecraftforge/common/ForgeConfigSpec.java | head -12
cd /tmp/assets/data/minecraft
for f in worldgen/noise_settings/end.json worldgen/biome/end_highlands.json worldgen/biome/small_end_islands.json dimension_type/the_end.json worldgen/configured_feature/chorus_plant.json worldgen/placed_feature/chorus_plant.json worldgen/configured_feature/flower_default.json worldgen/placed_feature/flower_default.json worldgen/configured_feature/patch_brown_mushroom.json worldgen/placed_feature/brown_mushroom_normal.json worldgen/configured_feature/patch_sugar_cane.json worldgen/placed_feature/patch_sugar_cane.json worldgen/configured_feature/moss_patch.json worldgen/noise/temperature.json worldgen/noise/vegetation.json; do echo "=== DATA $f"; cat "$f" 2>/dev/null || echo MISSING; echo; done
ls worldgen/configured_feature | head -300 | tr '\n' ' '; echo
ls /tmp/assets/data/minecraft/worldgen/ ; ls /tmp/assets/data/minecraft | head -50
