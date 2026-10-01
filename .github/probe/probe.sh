#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
grep -n 'public .*(' net/minecraft/world/level/storage/loot/LootContext.java | head -30
find . -name EntityTypes.java; grep -n 'ZOMBIE\b\|ZOMBIE =' $(find . -name EntityTypes.java) | head -3
grep -n 'hideGui\|public boolean .*[Gg]ui' net/minecraft/client/Options.java | head
grep -rn 'hideGui' net/minecraft/client/Minecraft.java net/minecraft/client/renderer/GameRenderer.java | head -5
exit 1
