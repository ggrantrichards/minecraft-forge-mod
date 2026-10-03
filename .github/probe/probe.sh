#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
MJ=$(find .gradle ~/.gradle -name '*.jar' 2>/dev/null | grep -i '26.3' | grep -v sources | head -40)
for j in $MJ; do unzip -l "$j" 2>/dev/null | grep -q 'worldgen/biome/basalt_deltas.json' && echo "has data: $j"; done
echo "jars: $MJ"
for j in $MJ $(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1); do
  if unzip -l "$j" | grep -q "data/minecraft/worldgen/biome/basalt_deltas.json"; then
    for b in basalt_deltas soul_sand_valley the_end crimson_forest deep_dark; do echo "== $b"; unzip -p "$j" data/minecraft/worldgen/biome/$b.json | head -c 2500; echo; done
    break
  fi
done
echo "== sound events ambient"; for j in $MJ; do unzip -l "$j" | grep -i "assets/minecraft/sounds.json" && unzip -p "$j" assets/minecraft/sounds.json | python3 -c "import json,sys;d=json.load(sys.stdin);print({k:v for k,v in d.items() if k.startswith('ambient.basalt') or k.startswith('ambient.cave')})" | head -c 1500; done
exit 0
