#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
find . -iname "*enderman*.java" | head
F=$(find . -iname "EnderMan.java" -o -iname "Enderman.java" | head -1); echo "== $F"
grep -n "teleport\|EntityTeleportEvent\|ForgeEventFactory" $F | head -20
F=$(find . -name LivingEntity.java | head -1); sed -n 3803,3840p $F | grep -n "Event\|post" 
echo "== slime bounce"
grep -rn "isSuppressingBounce\|bounceUp\|updateEntityMovementAfterFallOn" net/minecraft/world/level/block/Block.java net/minecraft/world/level/block/BedBlock.java net/minecraft/world/entity/Entity.java | head
grep -rn "SLIME_BLOCK\|BlockTags.BOUNCE\|bounce" net/minecraft/world/entity/Entity.java | head
grep -rn "class .*Block.*bounce\|BOUNCY\|bounciness" net/minecraft/tags/BlockTags.java net/minecraft/world/level/block/state/BlockBehaviour.java | head
echo "== speedFactor / jumpFactor"
grep -n "speedFactor\|jumpFactor" net/minecraft/world/level/block/state/BlockBehaviour.java | head -4
echo "== effect apply / getGravity override"
grep -rn "LEVITATION" net/minecraft/world/entity/LivingEntity.java | head
exit 0
