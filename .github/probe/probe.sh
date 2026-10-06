#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
JAVAP=$(dirname $(readlink -f $(which java)))/javap
echo "== viewport events"; unzip -l "$J" | awk '{print $4}' | grep -E "client/event/ViewportEvent" | head -20
for c in 'net.minecraftforge.client.event.ViewportEvent$ComputeFogColor' 'net.minecraftforge.client.event.ViewportEvent$RenderFog' net.minecraft.client.renderer.fog.FogRenderer; do echo "== $c"; $JAVAP -p -cp "$J" "$c" 2>&1 | grep -vE "^\s*$" | head -40; done
echo "== fog classes"; unzip -l "$J" | awk '{print $4}' | grep -iE "renderer/fog/" | head -30
echo "== BiomeSource.getNoiseBiome"; $JAVAP -p -cp "$J" net.minecraft.world.level.biome.BiomeSource | grep -i "noiseBiome\|getNoise"
echo "== RandomState sampler"; $JAVAP -p -cp "$J" net.minecraft.world.level.levelgen.RandomState | grep -i "sampler"
out=$(head -c 30000 /tmp/probe.txt | sed 's/%/%25/g; s/\r//g')
out=${out//$'\n'/%0A}
echo "::notice title=probe::${out}"
