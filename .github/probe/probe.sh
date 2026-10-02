#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
grep -n "getEntityBounciness" -A12 net/minecraft/world/entity/Entity.java | head -30
grep -rn "bounciness\|BOUNC" net/minecraft/world/level/block/state/BlockBehaviour.java net/minecraft/world/level/block/Blocks.java net/minecraft/tags/BlockTags.java | head
grep -n "public void bounceOn" -A8 net/minecraft/world/level/block/Block.java net/minecraft/world/level/block/SlimeBlock.java net/minecraft/world/level/block/BedBlock.java | head -30
grep -n "SLIME_BLOCK = register" -A4 net/minecraft/world/level/block/Blocks.java
exit 0
