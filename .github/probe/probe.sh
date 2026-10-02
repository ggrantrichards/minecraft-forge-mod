#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
for f in ChorusPlantBlock ChorusFlowerBlock ChorusPlantFeature; do F=$(find . -name "$f.java" | head -1); echo "== $F"; grep -n "canSurvive\|END_STONE\|SUPPORT\|BlockTags\|place(" $F | head -12; done
grep -n "CHORUS" net/minecraft/tags/BlockTags.java
grep -n "chorus" net/minecraft/data/worldgen/placement/EndPlacements.java net/minecraft/data/worldgen/features/EndFeatures.java 2>/dev/null | head
echo "== forge onLoad"; grep -n "onLoad\|onChunkUnloaded" net/minecraftforge/common/extensions/IForgeBlockEntity.java
echo "== teleport events rest"; F=$(find . -path "*minecraftforge*" -name "EntityTeleportEvent.java"); grep -n "class " $F
grep -rn "EntityTeleportEvent" --include=*.java net/minecraft/world/entity/monster | head
F=$(find . -name EnderMan.java | head -1); echo $F; grep -n "teleport" $F | head
echo "== loot condition id"; grep -rn "loot_table_id\|LOOT_TABLE_ID" net/minecraftforge --include=*.java | head -5
echo "== brewing"; F=$(find . -name PotionBrewing.java | head -1); echo $F; grep -n "public.*addMix\|public static\|Builder\b" $F | head
grep -rn "class.*Brewing.*Event" net/minecraftforge --include=*.java | head
echo "== potion registry"; grep -n "register(" net/minecraft/world/item/alchemy/Potions.java | head -3
echo "== creeper hurtAndBreak"; F=$(find . -name Creeper.java | head -1); grep -n "hurtAndBreak\|CREEPER_IGNITERS" $F
grep -n "COPPER_TORCH\b\|COPPER_WALL_TORCH" net/minecraft/world/item/Items.java net/minecraft/world/level/block/Blocks.java | head -3
echo "== jukebox song json"; F=$(find . -name JukeboxSong.java | head -1); sed -n 1,40p $F | grep -n "record\|Codec"
echo "== trim pattern"; F=$(find . -name TrimPattern.java | head -1); grep -n "record\|Codec" $F | head -5
F=$(find . -name SmithingTemplateItem.java); sed -n 95,112p $F
echo "== banner"; F=$(find . -name BannerPattern.java | head -1); grep -n "record" $F
grep -n "FLOW_BANNER_PATTERN" -A4 net/minecraft/world/item/Items.java | head -6
grep -n "PATTERN_ITEM_FLOW\|FLOW" net/minecraft/tags/BannerPatternTags.java | head
exit 0
