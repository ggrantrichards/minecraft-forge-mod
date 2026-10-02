#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
F=$(grep -rl "class EnderMan " --include=*.java . | head -1); echo "== $F"
grep -n "teleport\|EntityTeleportEvent\|ForgeEventFactory" $F | head -20
grep -n "onEnderTeleport\|EntityTeleportEvent" net/minecraftforge/event/ForgeEventFactory.java | head
F=$(find . -name LivingEntity.java | head -1); grep -n "public boolean randomTeleport" -A12 $F | head -30
F=$(find . -name ThrownEnderpearl.java | head -1); grep -n "EntityTeleportEvent\|teleport" $F | head
echo "== bell game event / jukebox song sound json"
F=$(find . -name LanternBlock.java | head -1); grep -n "HANGING\|public static\|canSurvive" $F | head
F=$(find . -name AmethystClusterBlock.java | head -1); grep -n "public AmethystClusterBlock\|FACING\|WATERLOGGED" $F | head -5
F=$(find . -name MobEffect.java | head -1); grep -n "public void onEffectAdded\|onMobRemoved\|onEffectStarted\|applyEffectTick\|shouldApplyEffectTickThisTick\|onMobHurt" $F | head
exit 0
