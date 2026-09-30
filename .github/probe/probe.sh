#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src && cd /tmp/src
f() { find . -name "$1" | head -1; }
grep -rn -E 'hideGui' --include=*.java net/minecraft/client | head -8
x=$(f Screenshot.java); sed -n 25,84p $x
grep -n -E 'public .*RenderTarget [a-zA-Z]+\(|public .* getWindow\(\)|public .* levelRenderer|public void stop\(\)' net/minecraft/client/Minecraft.java | head
grep -rn -E 'STRIPPABLES|getStripped|AXE_STRIP|getToolModifiedState' --include=*.java . | head -10
x=$(f PlayerInteractEvent.java); grep -n -E 'class RightClickBlock|BUS|public .* get(Pos|Level|ItemStack|Entity|Hand|Face)\(' $x | head -20
grep -n -E 'AmbientLeavesBlockSoundPlayer' net/minecraft/world/level/block/Blocks.java | head -2; find . -name 'AmbientLeavesBlockSoundPlayer.java'
x=$(f WeepingVinesBlock.java); grep -v '^import' $x
x=$(f GrowingPlantHeadBlock.java); grep -n -E 'protected abstract|public abstract|abstract ' $x
grep -n -E 'public static .* (ofFullCopy|ofLegacyCopy)' net/minecraft/world/level/block/state/BlockBehaviour.java
grep -n -A12 'private static BlockBehaviour.Properties leavesProperties' net/minecraft/world/level/block/Blocks.java
grep -n -A10 'private static BlockBehaviour.Properties netherStemProperties' net/minecraft/world/level/block/Blocks.java
grep -n -A6 'private static BlockBehaviour.Properties buttonProperties' net/minecraft/world/level/block/Blocks.java
grep -n -E 'public .* getGenerator\(\)|public ChunkGenerator' net/minecraft/server/level/ServerChunkCache.java | head -3
grep -n -E 'writeToFile' $(f NativeImage.java) | head -3
