#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
echo "jar: $J"
unzip -l "$J" | awk '{print $4}' | grep -iE "monster/.*ender|KeyDispatch|densityfunction/[A-Za-z]+\.class$" | head -60
JAVAP=$(dirname $(readlink -f $(which java)))/javap
for c in net.minecraft.world.level.levelgen.densityfunction.DensityFunction 'net.minecraft.world.level.levelgen.densityfunction.DensityFunction$Visitor' 'net.minecraft.world.level.levelgen.densityfunction.DensityFunction$ContextProvider' 'net.minecraft.world.level.levelgen.densityfunction.DensityFunction$SinglePointContext' 'net.minecraft.world.level.levelgen.densityfunction.DensityFunction$SimpleFunction'; do
  echo "== $c"; $JAVAP -cp "$J" "$c" 2>&1 | head -40
done
