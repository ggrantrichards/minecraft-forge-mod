#!/usr/bin/env bash
# Temporary: Phase 8 probe 2 - armour layers, tooltips, effects, loot params, item model properties, reference textures.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
T=assets/minecraft/textures
for f in item/diamond_sword item/diamond_pickaxe item/diamond_axe item/diamond_shovel item/diamond_hoe item/netherite_sword item/netherite_pickaxe \
         item/bow item/crossbow_standby item/trident item/mace item/diamond_helmet item/diamond_chestplate item/diamond_leggings item/diamond_boots \
         item/netherite_helmet item/netherite_chestplate item/netherite_leggings item/netherite_boots \
         trims/items/helmet_trim trims/items/chestplate_trim trims/items/leggings_trim trims/items/boots_trim \
         trims/entity/humanoid/sentry trims/entity/humanoid_leggings/sentry trims/color_palettes/trim_palette trims/color_palettes/amethyst \
         entity/equipment/humanoid/diamond entity/equipment/humanoid_leggings/diamond \
         item/netherite_upgrade_smithing_template item/ender_pearl item/netherite_ingot block/smithing_table_top block/smithing_table_front block/smithing_table_side \
         gui/sprites/container/smithing/error gui/sprites/container/slot/smithing_template_netherite_upgrade gui/sprites/container/slot/ingot; do
  echo "=== b64 $f"; unzip -p "$J" $T/$f.png | base64 -w0; echo
done
unzip -l "$J" | grep -E 'atlases/' | head -20
echo "=== atlases/armor_trims.json"; unzip -p "$J" assets/minecraft/atlases/armor_trims.json | head -60
echo "=== atlases/items.json"; unzip -p "$J" assets/minecraft/atlases/items.json | head -80
cd /tmp/src
show() { f=$(find . -path "*$1" | head -1); echo "=================== $f"; grep -v '^import' "$f" | head -${2:-200}; }
show renderer/entity/layers/EquipmentLayerRenderer.java 160
show model/geom/builders/ArmorModelSet.java 60
grep -rn 'HumanoidArmorLayer(' --include=*.java net/minecraft/client/renderer/entity | head -5
grep -n 'PLAYER_ARMOR\|ARMOR' net/minecraft/client/model/geom/ModelLayers.java | head
f=$(find . -path '*minecraftforge/client/event/EntityRenderersEvent.java'); grep -n 'class AddLayers' -A60 "$f" | grep -n 'public\|BUS'
show event/entity/player/ItemTooltipEvent.java 60
grep -n 'enum ImpactResult' -A10 $(find . -path '*event/entity/ProjectileImpactEvent.java')
grep -n 'record LivingDropsEvent' -A8 $(find . -path '*living/LivingDropsEvent.java')
grep -n 'class BreakEvent' -A40 $(find . -path '*event/level/BlockEvent.java') | grep 'public\|BUS'
grep -n 'class Added' -A30 $(find . -path '*living/MobEffectEvent.java') | grep 'public'
grep -n 'duration\b\|private int duration\|setDuration\|public .*update(\|mapDuration' $(find . -name MobEffectInstance.java) | head -20
show gui/screens/inventory/CyclingSlotBackground.java 60
grep -n 'appendHoverText' -A8 net/minecraft/world/item/Item.java | head -12
grep -n 'appendHoverText' -A25 net/minecraft/world/item/SmithingTemplateItem.java | head -30
show item/properties/select/ComponentContents.java 80
grep -n 'register\|ID_MAPPER.put' $(find . -name ItemModels.java) | head -20
grep -n 'ID_MAPPER.put' $(find . -name ItemTintSources.java) | head
grep -n 'public static int getItemEnchantmentLevel\|public static int getEnchantmentLevel' net/minecraft/world/item/enchantment/EnchantmentHelper.java
grep -n 'public static final ContextKey' $(find . -name LootContextParams.java) | head -20
grep -n 'public .* assemble' $(find . -path '*crafting/AbstractCookingRecipe.java') $(find . -path '*crafting/SingleItemRecipe.java') | head
grep -n 'public float getAttackStrengthScale\|public void attack(\|sweepAttack\|doSweepAttack\|isSweepAttack' net/minecraft/world/entity/player/Player.java | head
grep -n 'protected void onBelowWorld' -A12 net/minecraft/world/entity/LivingEntity.java
grep -n 'protected double getDefaultGravity' -A3 $(find . -path '*projectile/arrow/AbstractArrow.java') $(find . -name ThrownTrident.java) | head
grep -n 'class FrostedIceBlock' -A60 $(find . -name FrostedIceBlock.java) | grep 'public\|protected\|AGE' | head -20
grep -n 'public .*getMaxLocalRawBrightness\|public int getRawBrightness' net/minecraft/world/level/Level.java net/minecraft/world/level/BlockAndTintGetter.java net/minecraft/world/level/LevelReader.java | head
grep -rn 'FMLClientSetupEvent' --include=*.java net/minecraftforge/fml 2>/dev/null | head -3; find . -name FMLClientSetupEvent.java | head -2
grep -n 'fell_out_of_world\|FELL_OUT_OF_WORLD' net/minecraft/world/damagesource/DamageTypes.java
grep -n 'public .*fellOutOfWorld\|public DamageSource magic\|public DamageSource playerAttack\|public DamageSource generic' net/minecraft/world/damagesource/DamageSources.java
grep -n 'public .*getLastClimbablePos\|lastPos\|public .*onGround()' net/minecraft/world/entity/Entity.java | head -5
grep -n 'AvatarRenderer\|class PlayerRenderer' -r --include=*.java net/minecraft/client/renderer/entity/player 2>/dev/null | head -3; ls net/minecraft/client/renderer/entity/player 2>/dev/null
exit 1
