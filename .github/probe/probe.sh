#!/usr/bin/env bash
# Temporary: second Phase 3 probe.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
showf() { f=$(find . -name "$1" | head -1); echo "=== FILE $f"; grep -v '^import' "$f" | head -${2:-400}; echo "=== END $1"; }
f=$(find . -name LevelExtractor.java | head -1); echo "=== $f"; sed -n 400,500p "$f"
grep -rn -B2 -A12 'getViewBlockingState' --include=*.java net/minecraft/client | head -60
sed -n 100,150p net/minecraft/client/Camera.java
echo "=== ENTITY canUsePortal"; sed -n 3330,3350p net/minecraft/world/entity/Entity.java
grep -n -A8 'public boolean canUsePortal\|public boolean canChangeDimensions' net/minecraft/server/level/ServerPlayer.java net/minecraft/world/entity/player/Player.java
grep -n -B3 -A3 'noPhysics\|isSpectator' net/minecraft/world/entity/Entity.java | sed -n 1,80p
echo "=== SERVERLEVEL dragon"; sed -n 290,305p net/minecraft/server/level/ServerLevel.java
grep -rn 'static void createEndPlatform\|public static void createEndPlatform' --include=*.java . 
find . -name 'DensityFunction.java' -o -name 'DensityFunctions.java' -o -name 'Feature.java' -o -name 'FeaturePlaceContext.java' -o -name 'NoneFeatureConfiguration.java' -o -name 'FMLJavaModLoadingContext.java' -o -name 'BlockEntityType.java' | head -20
showf DensityFunction.java 200
f=$(find . -name DensityFunctions.java | head -1)
grep -n -E 'register\(|HOLDER_HELPER_CODEC|DIRECT_CODEC|class EndIslandDensityFunction|record Clamp|record Ap2|interface PureTransformer|record RangeChoice|static .*Codec.*makeCodec|KeyDispatchDataCodec' "$f" | head -80
grep -n -A45 'class EndIslandDensityFunction' "$f" | head -80
grep -n -A40 'record RangeChoice' "$f" | head -60
f=$(find . -name Feature.java -path '*levelgen*' | head -1); grep -n -E 'public Feature\(|abstract boolean place|public .*place\(|static final Feature<NoneFeatureConfiguration> (END_ISLAND|BLUE_ICE|END_PLATFORM)' "$f"
f=$(find . -name FeaturePlaceContext.java | head -1); grep -v '^import' "$f"
f=$(find . -name EndIslandFeature.java | head -1); grep -v '^import' "$f"
f=$(find . -name NoneFeatureConfiguration.java | head -1); grep -n 'CODEC\|INSTANCE\|package' "$f"
f=$(find . -name FMLJavaModLoadingContext.java | head -1); grep -n 'public' "$f"
grep -rn 'BLOCK_ENTITY_TYPES\b' net/minecraftforge/registries/ForgeRegistries.java
showf BaseEntityBlock.java 80
grep -n -E 'public .* getEntitiesOfClass\(' net/minecraft/world/level/Level.java net/minecraft/world/level/EntityGetter.java
grep -n -E 'record TargetPoint|interface Sampler|record Sampler|DensityFunction (temperature|humidity|continentalness|erosion|depth|weirdness)' net/minecraft/world/level/biome/Climate.java | head
grep -n -E 'public record NoiseRouter|DensityFunction ' net/minecraft/world/level/levelgen/NoiseRouter.java | head -20
grep -rn -E '"(range_choice|end_islands|noise|shifted_noise|y_clamped_gradient|gradient|lerp|clamp|abs|min|max|add|mul|cache_2d|flat_cache|interpolated|spline|constant)"' --include=*.java net/minecraft/world/level/levelgen | head -40
