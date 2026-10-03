#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
JAVAP=$(dirname $(readlink -f $(which java)))/javap
for c in net.minecraft.world.level.levelgen.densityfunction.SamplerContext 'net.minecraft.world.level.levelgen.densityfunction.DensitySampler$Bound' net.minecraft.world.item.component.SwingAnimation; do
  echo "== $c"; $JAVAP -cp "$J" "$c" 2>&1 | head -40
done
echo "== callers of createClimateSampler / SamplerContext"
mkdir -p /tmp/x && cd /tmp/x && unzip -qo "$OLDPWD/$J" 'net/minecraft/*' && cd "$OLDPWD"
for f in $(grep -rl "createClimateSampler" /tmp/x/net | head -12); do
  c=$(echo $f | sed 's|/tmp/x/||; s|\.class$||; s|/|.|g'); echo "-- $c"
  $JAVAP -c -p -cp /tmp/x "$c" | grep -B8 -A3 "createClimateSampler" | grep -E "invoke|getstatic|new |Method|^  [a-z].*\(" | head -24
done
echo "== Entity.markHurt"; $JAVAP -c -p -cp "$J" net.minecraft.world.entity.Entity | grep -A8 "void markHurt()"
