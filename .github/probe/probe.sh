#!/usr/bin/env bash
# Temporary: Phase 9 probe.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
for f in assets/minecraft/items/compass.json assets/minecraft/models/block/lantern.json assets/minecraft/models/block/lantern_hanging.json \
         assets/minecraft/blockstates/lantern.json assets/minecraft/models/block/copper_grate.json assets/minecraft/blockstates/copper_grate.json \
         assets/minecraft/models/block/moss_carpet.json assets/minecraft/models/item/mushroom_stew.json \
         data/minecraft/advancement/recipes/building_blocks/end_stone_bricks.json data/minecraft/advancement/adventure/adventuring_time.json \
         data/minecraft/advancement/end/kill_dragon.json data/minecraft/advancement/nether/obtain_ancient_debris.json data/minecraft/recipe/lantern.json \
         data/minecraft/recipe/waxed_copper_block_from_honeycomb.json data/minecraft/recipe/cut_copper.json; do
  echo "=== $f"; unzip -p "$J" $f | head -50
done
cd /tmp/src
show() { f=$(find . -path "*$1" | head -1); echo "=================== $f"; grep -v '^import' "$f" | head -${2:-200}; }
f=$(find . -path '*minecraftforge/event/BuildCreativeModeTabContentsEvent.java'); echo "== $f"; grep -n 'public\|BUS' "$f" | head -30
show world/level/gamerules/GameRules.java 60
grep -n 'public static final GameRule<Boolean> MOB_GRIEFING' -A4 net/minecraft/world/level/gamerules/GameRules.java
grep -n 'private static .* register\|public static .* register\|registerBoolean' net/minecraft/world/level/gamerules/GameRules.java | head
ls net/minecraft/world/level/gamerules/
grep -n 'GAME_RULE' net/minecraft/core/registries/Registries.java net/minecraft/core/registries/BuiltInRegistries.java
grep -n 'public record LodestoneTracker' -A3 $(find . -name LodestoneTracker.java)
grep -n 'MUSHROOM_STEW\|SUSPICIOUS_STEW' -A3 net/minecraft/world/item/Items.java | head -12
grep -n 'public static final Consumable\|public static Consumable.Builder' net/minecraft/world/item/component/Consumables.java | head
grep -n 'public ApplyStatusEffectsConsumeEffect' $(find . -name ApplyStatusEffectsConsumeEffect.java)
grep -n 'public .*Builder onConsume\|public .*Builder' net/minecraft/world/item/component/Consumable.java | head
grep -n 'MUSHROOM_STEW' net/minecraft/world/food/Foods.java
grep -n 'public LanternBlock\|HANGING\|WATERLOGGED' net/minecraft/world/level/block/LanternBlock.java | head
grep -rn 'class .*Grate\|class WaterloggedTransparentBlock' --include=*.java net/minecraft/world/level/block | head
grep -n 'COPPER_GRATE = ' -A3 net/minecraft/world/level/block/Blocks.java | head -5
grep -n 'public ShulkerBullet(' net/minecraft/world/entity/projectile/*.java net/minecraft/world/entity/projectile/*/*.java 2>/dev/null | head
grep -n 'MOSS_CARPET = ' -A3 net/minecraft/world/level/block/Blocks.java | head -5
grep -n 'class CarpetBlock\|class WoolCarpetBlock' -r --include=*.java net/minecraft/world/level/block | head
grep -n 'public static final .*HONEYCOMB\b' net/minecraft/world/item/Items.java
grep -n 'useItemOn' -A25 net/minecraft/world/item/HoneycombItem.java | head -40
exit 1
