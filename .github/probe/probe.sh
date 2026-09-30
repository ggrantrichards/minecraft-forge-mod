#!/usr/bin/env bash
# Temporary: third Phase 3 probe.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
D=/tmp/assets/data/minecraft/worldgen
for f in density_function/end/islands.json density_function/overworld/base_3d_noise.json biome/basalt_deltas.json biome/small_end_islands.json; do echo "=== DATA $f"; tr -s ' \n' ' ' < $D/$f | head -c 3000; echo; done
cd /tmp/src
showf() { f=$(find . -name "$1" | head -1); echo "=== FILE $f"; grep -v '^import' "$f" | head -${2:-200}; echo "=== END $1"; }
showf Feature.java 120
grep -n -E 'FEATURE_TYPE|feature_type|FEATURE =' net/minecraft/core/registries/Registries.java net/minecraft/core/registries/BuiltInRegistries.java
f=$(grep -rl 'EndIslandFeature.CODEC' --include=*.java . | head -1); echo "=== REG $f"; grep -n -B2 -A2 'EndIslandFeature.CODEC\|static .* register(' "$f" | head -30
for c in DistanceToPointFunction EndIslandFunction GradientFunction LerpFunction RangeChoiceFunction NoiseFunction ClampFunction; do
  f=$(find . -name "$c.java" | head -1); echo "=== CODEC $c"; grep -n -A8 'MapCodec<.*CODEC' "$f" | head -14; done
showf EndPlatformFeature.java 60
grep -n -E 'getEntitiesOfClass|getEntities\(' net/minecraft/world/level/EntityGetter.java net/minecraft/world/level/Level.java | head
f=$(find . -name IRenderCallback.java | head -1); echo "=== $f"; grep -v '^import' "$f"
grep -rn 'interface BlockEntityTicker' -A3 --include=*.java net/minecraft | head
grep -n -E 'public (static )?.*(isSpectator|animateTick)' net/minecraft/world/level/block/Block.java | head
cd -
EB=$(find ~/.gradle/caches -name 'eventbus-*.jar' ! -name '*sources*' | head -3); echo "EB=$EB"
for j in $EB; do for c in net/minecraftforge/eventbus/api/bus/CancellableEventBus net/minecraftforge/eventbus/api/bus/EventBus; do javap -cp "$j" "$c" 2>/dev/null; done; done
FML=$(find ~/.gradle/caches .gradle -name 'javafmllanguage*.jar' ! -name '*sources*' | head -1); echo "FML=$FML"; javap -cp "$FML" net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext 2>/dev/null
