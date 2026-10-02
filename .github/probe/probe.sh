#!/usr/bin/env bash
set +e
ROOT=$PWD
exec > >(tee /tmp/probe.txt) 2>&1
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
echo "SRC=$SRC"
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
F=$(find . -name HappyGhast.java | head -1); echo "== $F"
grep -n "getControllingPassenger\|getRiddenInput\|tickRidden\|getRiddenSpeed\|travel\|protected void positionRider\|getPassengerAttachmentPoint\|mobInteract\|InteractionResult\|isVehicle\|canAddPassenger\|startRiding" "$F" | head -40
sed -n "$(grep -n 'getRiddenInput' $F | head -1 | cut -d: -f1),+25p" "$F"
sed -n "$(grep -n 'tickRidden' $F | head -1 | cut -d: -f1),+20p" "$F"
sed -n "$(grep -n 'void travel' $F | head -1 | cut -d: -f1),+20p" "$F"
echo "== ItemSteerable / FoodOnAStick"
find . -name "ItemSteerable.java" -o -name "FoodOnAStickItem.java" | xargs -I{} sh -c 'echo {}; cat {} | head -70'
grep -n "boost\|ItemBasedSteering" net/minecraft/world/entity/monster/Strider.java 2>/dev/null | head -20 || grep -rln "class Strider" . | head
find . -name "ItemBasedSteering.java" | xargs sed -n 1,80p
grep -rn "getRiddenInput\|tickRidden\|getRiddenSpeed\|getControllingPassenger" net/minecraft/world/entity/LivingEntity.java net/minecraft/world/entity/Mob.java net/minecraft/world/entity/Entity.java | head
grep -n "WARPED_FUNGUS_ON_A_STICK\|CARROT_ON_A_STICK" net/minecraft/world/item/Items.java
echo "== jukebox / banners / trims"
grep -n "jukeboxPlayable\|public Item.Properties trimMaterial\|component(" net/minecraft/world/item/Item.java | head
grep -n "MUSIC_DISC_RELIC\|MUSIC_DISC_CREATOR \|MUSIC_DISC_PRECIPICE" net/minecraft/world/item/Items.java
grep -n "FLOW_BANNER_PATTERN\|GUSTER_BANNER_PATTERN\|BOLT_ARMOR_TRIM_SMITHING_TEMPLATE\|FLOW_ARMOR_TRIM" net/minecraft/world/item/Items.java
find . -name "JukeboxSongs.java" | xargs sed -n 1,40p
find . -name "TrimPatterns.java" | xargs sed -n 1,60p | grep -n "register\|static\|ResourceKey" | head
grep -rn "PROVIDES_BANNER_PATTERNS\|BannerPatternTags" net/minecraft/world/item/component/*.java net/minecraft/core/component/DataComponents.java | head -5
find . -name "SmithingTemplateItem.java" | xargs grep -n "public static\|createArmorTrim" | head
echo "== bells"
F=$(find . -name BellBlockEntity.java); grep -n "public\|shaking\|ticks\|onHit" $F | head -30
F=$(find . -name BellBlock.java); grep -n "attemptToRing\|public boolean onHit\|GameEvent" $F | head
echo "== teleport events"
find . -path "*minecraftforge*" -name "EntityTeleportEvent.java" | xargs grep -n "class\|BUS\|public" | head -40
echo "== part entities"
find . -path "*minecraftforge*" -name "PartEntity.java" | xargs sed -n 1,60p
grep -n "isMultipartEntity\|getParts" net/minecraft/world/entity/Entity.java | head; find . -name "EnderDragonPart.java" | xargs sed -n 1,60p | head -60
echo "== brewing"
find . -path "*minecraftforge*" -name "*Brewing*Event*.java" | xargs grep -n "class\|BUS\|public" | head -20
grep -n "public Builder\|addMix\|addContainerRecipe\|addStartMix" net/minecraft/world/item/alchemy/PotionBrewing.java | head
echo "== slime"
F=$(find . -name SlimeBlock.java); sed -n 1,80p $F
echo "== loot modifiers"
find . -path "*minecraftforge*" -name "*LootModifier*.java" | head
echo "== end city loot / shulker"
grep -n "END_CITY" net/minecraft/world/level/storage/loot/BuiltInLootTables.java
grep -n "public\b" net/minecraft/world/entity/monster/Shulker.java | head -20
echo "== enderman teleport"
grep -n "teleport" net/minecraft/world/entity/monster/EnderMan.java | head
echo "== mob effect / potions"
grep -n "SLOW_FALLING\|LEVITATION" net/minecraft/world/effect/MobEffects.java
grep -n "getGravity\|GRAVITY" net/minecraft/world/entity/LivingEntity.java | head
grep -n "SAFE_FALL_DISTANCE\|FALL_DAMAGE_MULTIPLIER" net/minecraft/world/entity/ai/attributes/Attributes.java
echo "== loot ids / poi / target goal"
grep -n "getLootTableId\|getQueriedLootTableId" net/minecraft/world/level/storage/loot/LootTable.java net/minecraft/world/level/storage/loot/LootContext.java
find . -path "*minecraftforge*" -name "LootTableIdCondition.java" | xargs grep -n "MapCodec\|loot_table_id\|class" | head
F=$(find . -name PoiType.java); sed -n 1,40p $F | grep -n "record\|public"
grep -n "LODESTONE" net/minecraft/world/entity/ai/village/poi/PoiTypes.java | head
grep -n "targetConditions\|protected\|public NearestAttackableTargetGoal" net/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal.java | head
grep -n "public.*interactAt\|mobInteract" net/minecraft/world/entity/Mob.java | head -3
grep -n "FLINT_AND_STEEL\|isOnFire\|igniteForSeconds" net/minecraft/world/entity/Entity.java | head -5
exit 0
