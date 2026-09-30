#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
for t in blocks_motion blocks_motion_in_heightmap blocks_motion_in_heightmap_no_leaves blocks_motion_no_leaves; do echo "=== TAG $t"; tr -s ' \n' ' ' < /tmp/assets/data/minecraft/tags/block/$t.json | head -c 1500; echo; done
cd /tmp/src
grep -rn -E 'BLOCKS_MOTION[A-Z_]*' --include=*.java net/minecraft | grep -v 'TagKey<Block> BLOCKS' | head -30
