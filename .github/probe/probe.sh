#!/usr/bin/env bash
# Temporary: Phase 8 probe - menus, screens, data components, damage events, loot modifiers.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
for f in diamond_sword diamond_helmet diamond_boots bow crossbow trident mace diamond_pickaxe; do
  echo "=== items/$f.json"; unzip -p "$J" assets/minecraft/items/$f.json | head -60
done
echo "=== smithing sprites"; unzip -l "$J" | grep -E 'gui/container/smithing|sprites/container/smithing|sprites/container/slot/' | head -40
for f in assets/minecraft/textures/gui/container/smithing.png; do echo "=== b64 $f"; unzip -p "$J" $f | base64 -w0; echo; done
cd /tmp/src
show() { f=$(find . -path "*$1" | head -1); echo "=================== $f"; grep -v '^import' "$f" | head -${2:-200}; }
show inventory/SmithingMenu.java 160
show inventory/ItemCombinerMenu.java 200
show inventory/ItemCombinerMenuSlotDefinition.java 80
show screens/inventory/SmithingScreen.java 200
show screens/inventory/ItemCombinerScreen.java 100
show item/SmithingTemplateItem.java 120
grep -rn 'class MenuScreens\|public static .* void register' $(find . -name MenuScreens.java) | head
find . -path '*minecraftforge*' -name '*.java' | xargs grep -ln 'MenuScreens\|RegisterMenuScreens' | head
find . -path '*minecraftforge*' -name 'IForgeMenuType.java' | xargs grep -n 'static' | head
grep -n 'public MenuType\|public static <T' $(find . -name MenuType.java) | head
show core/component/DataComponents.java 40
grep -n 'public static <T> Builder\|public Builder<T> \|networkSynchronized\|persistent(' $(find . -path '*core/component/DataComponentType.java') | head
show component/ItemAttributeModifiers.java 140
grep -n 'GRAVITY\|STEP_HEIGHT\|MAX_ABSORPTION\|SAFE_FALL' $(find . -path '*attributes/Attributes.java') | head
grep -n 'enum Operation' -A6 $(find . -path '*attributes/AttributeModifier.java')
grep -n 'public static final EquipmentSlotGroup' $(find . -name EquipmentSlotGroup.java)
echo "== forge living events"; ls $(dirname $(find . -path '*minecraftforge/event/entity/living/LivingDeathEvent.java'))
ls $(dirname $(find . -path '*minecraftforge/event/entity/ProjectileImpactEvent.java')) $(dirname $(find . -path '*minecraftforge/event/level/BlockEvent.java'))
for c in LivingAttackEvent LivingHurtEvent LivingDamageEvent LivingIncomingDamageEvent LivingDeathEvent MobEffectEvent LivingDropsEvent ProjectileImpactEvent ItemTooltipEvent; do
  f=$(find . -path "*minecraftforge/event/*/$c.java" | head -1); [ -n "$f" ] && { echo "=== $f"; grep -n 'public \|BUS\|Cancelable\|HasResult' "$f" | head -30; }
done
f=$(find . -path '*minecraftforge/event/level/BlockEvent.java'); grep -n 'class BreakEvent' -A30 "$f" | grep 'public\|BUS'
find . -path '*minecraftforge/common/loot/*.java' | head; show common/loot/LootModifier.java 80; show common/loot/IGlobalLootModifier.java 60
grep -rn 'global_loot_modifiers\|loot_modifiers' --include=*.java . | head -5
grep -rn 'GLOBAL_LOOT_MODIFIER' --include=ForgeRegistries.java . | head
grep -n 'public .*getRecipeFor' $(find . -path '*crafting/RecipeManager.java') $(find . -path '*crafting/RecipeAccess.java') 2>/dev/null | head
grep -n 'public .*recipeAccess' $(find . -path '*server/level/ServerLevel.java') | head
grep -n 'public .*setAbsorptionAmount\|public .*getMaxAbsorption\|public .*addEffect(\|public .*forceAddEffect\|protected void onBelowWorld\|void onBelowWorld\|public .*getWeaponItem' $(find . -path '*world/entity/LivingEntity.java') $(find . -path '*world/entity/Entity.java') $(find . -path '*projectile/arrow/AbstractArrow.java' -o -path '*projectile/AbstractArrow.java') | head -20
grep -n 'getGravity\|getDefaultGravity\|applyGravity' $(find . -path '*world/entity/Entity.java') | head
grep -n 'public MobEffectInstance(\|public int getDuration\|public .*getCategory\|isBeneficial' $(find . -name MobEffectInstance.java) $(find . -path '*effect/MobEffect.java') | head
grep -n 'public .*overrideStackedOnOther\|public .*overrideOtherStackedOnMe' $(find . -path '*world/item/Item.java')
grep -n 'dropAllDeathLoot\|shouldDropLoot\|tickDeath' $(find . -name EnderDragon.java) | head
show entity/layers/HumanoidArmorLayer.java 120
grep -n 'public void renderLayers\|private void \|public .*renderLayers' $(find . -name EquipmentLayerRenderer.java) | head
grep -n 'trim\|Trim' $(find . -name EquipmentLayerRenderer.java) | head -30
grep -n 'register\|"component"' $(find . -name SelectItemModelProperties.java) | head -30
grep -n 'public .*addToTooltip\|addDetailsToTooltip' -A3 $(find . -path '*world/item/ItemStack.java') | head -40
grep -n 'void appendHoverText\|TooltipDisplay' $(find . -path '*world/item/Item.java') | head
exit 1
