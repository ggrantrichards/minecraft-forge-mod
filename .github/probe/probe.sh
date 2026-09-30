#!/usr/bin/env bash
# Temporary: prints vanilla 26.3 worldgen data formats Phase 2 depends on.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
W=/tmp/assets/data/minecraft/worldgen
c() { echo "=== DATA $1"; tr -s ' \n' ' ' < "$W/$1" 2>/dev/null || echo MISSING; echo; }
for f in feature/chorus_plant.json feature/flower_default.json feature/brown_mushroom.json feature/sugar_cane.json feature/end_island.json feature/twisting_vines.json feature/moss_patch.json feature/cave_vine.json feature/glow_lichen.json feature/end_gateway_return.json placed_feature/end_island_decorated.json placed_feature/twisting_vines.json placed_feature/cave_vines.json placed_feature/patch_crimson_roots.json feature/patch_crimson_roots.json feature/crimson_roots.json material_rule/end.json material_rule/nether.json world_preset/normal.json biome/crimson_forest.json biome/warped_forest.json biome/mushroom_fields.json biome/the_end.json; do c "$f"; done
echo "=== LIST feature"; ls $W/feature | tr '\n' ' ' | head -c 6000; echo
echo "=== LIST material_rule"; ls $W/material_rule; echo "=== LIST material_condition"; ls $W/material_condition | head -40
echo "=== LIST block_state_provider"; ls $W/block_state_provider | head -40
for f in $(ls $W/material_condition | head -3); do c material_condition/$f; done
for f in $(ls $W/block_state_provider | head -2); do c block_state_provider/$f; done
echo "=== MATERIAL RULE TYPES"; grep -rhn -E 'register\(registry, "' /tmp/src/net/minecraft/world/level/levelgen/SurfaceRules.java | head -30
ls /tmp/src/net/minecraft/world/level/levelgen/ | tr '\n' ' '; echo
grep -rn -E 'LEVEL_STEM|"dimension"' /tmp/src/net/minecraft/resources/RegistryDataLoader.java /tmp/src/net/minecraft/core/registries/Registries.java | head -8
echo "=== REGISTRAR"; grep -rn -E 'FEATURE =|PLACED_FEATURE =|CONFIGURED_FEATURE|MATERIAL_RULE|MATERIAL_CONDITION|BLOCK_STATE_PROVIDER' /tmp/src/net/minecraft/core/registries/Registries.java | head
echo "=== FEATURES LIST"; grep -n -E 'public static final Feature<[A-Za-z]+> (BLOCK_COLUMN|SIMPLE_BLOCK|VEGETATION_PATCH|RANDOM_PATCH|END_ISLAND|CHORUS_PLANT|TWISTING_VINES|MULTIFACE_GROWTH)' /tmp/src/net/minecraft/world/level/levelgen/feature/Feature.java
echo "=== CLIMATE SAMPLER"; grep -n -E 'record Sampler|DensityFunction (temperature|erosion)|sampleValue' /tmp/src/net/minecraft/world/level/biome/Climate.java /tmp/src/net/minecraft/world/level/levelgen/DensityFunction.java | head
echo "=== CONFIG REG"; grep -rn -E 'registerConfig' /tmp/src/net/minecraftforge --include=*.java | grep -E 'public|default' | head -5
