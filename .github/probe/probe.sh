#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src && cd /tmp/src
f=net/minecraft/world/level/block/state/BlockBehaviour.java
grep -n -E 'legacySolid|calculateSolid|forceSolid|public boolean isSolid|public boolean blocksMotion|initCache|isSolidRender' $f | head -40
grep -n -A25 'private boolean calculateSolid' $f
grep -n -B3 -A12 'public boolean blocksMotion' $f
grep -rn 'rebuildCache\|initCache()' --include=*.java net/minecraft net/minecraftforge | head -20
grep -n -A6 ' END_STONE = register' net/minecraft/world/level/block/Blocks.java
grep -n -A6 ' MOSS_BLOCK = register' net/minecraft/world/level/block/Blocks.java
