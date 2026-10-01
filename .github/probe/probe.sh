#!/usr/bin/env bash
# Temporary: Phase 8 probe 3.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
show() { f=$(find . -path "*$1" | head -1); echo "=================== $f"; grep -v '^import' "$f" | head -${2:-200}; }
show client/model/geom/builders/ArmorModelSet.java 60
find . -name 'ArmorModelSet.java'
grep -n 'PLAYER\|registerArmorSet(' net/minecraft/client/model/geom/ModelLayers.java | head -20
grep -n 'HumanoidArmorLayer\|addLayer\|ArmorModelSet' net/minecraft/client/renderer/entity/player/AvatarRenderer.java | head
grep -n 'addLayer' net/minecraft/client/renderer/entity/LivingEntityRenderer.java
grep -n 'MobEffectInstance with\|public MobEffectInstance [a-zA-Z]*(' net/minecraft/world/effect/MobEffectInstance.java
grep -rn 'getMaxLocalRawBrightness' net/minecraft/world/level/LevelReader.java | head -3
ls net/minecraftforge/event/entity/player/ | head -60
grep -n 'public interface ItemInstance' -A30 $(find . -name ItemInstance.java) | grep -v '^\s*$' | head -30
grep -n 'public static RenderType armorTrim\|public static RenderType armorCutoutNoCull\|public static RenderType armorTranslucent\|public static RenderType entityTranslucent(' net/minecraft/client/renderer/rendertype/RenderTypes.java
grep -n 'submitModel' -A12 $(find . -name SubmitNodeCollector.java) | head -40
grep -n 'public <T> Item.Properties component\|public Item.Properties component' net/minecraft/world/item/Item.java
grep -n 'DATA_COMPONENT_TYPE\|ResourceKey<Registry<MenuType' net/minecraft/core/registries/Registries.java
grep -n 'MENU_TYPES\|DATA_COMPONENT' net/minecraftforge/registries/ForgeRegistries.java | head
grep -n 'public .*openMenu' net/minecraft/world/entity/player/Player.java net/minecraft/server/level/ServerPlayer.java
grep -n 'public static ContainerLevelAccess create' net/minecraft/world/inventory/ContainerLevelAccess.java
grep -n 'protected MenuProvider getMenuProvider\|getMenuProvider' -A6 net/minecraft/world/level/block/SmithingTableBlock.java net/minecraft/world/level/block/CraftingTableBlock.java | head -30
grep -n 'useWithoutItem' -A10 net/minecraft/world/level/block/CraftingTableBlock.java | head -14
grep -n 'class PlayerTickEvent' -A5 net/minecraftforge/event/TickEvent.java | head; grep -n 'Pre\|Post' net/minecraftforge/event/TickEvent.java | head -20
grep -n 'protected void extractLabels\|public void extractRenderState\|protected int imageWidth\|titleLabel\|inventoryLabel' net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java | head
grep -n 'public void text(\|public int text(\|public void drawString\|public .* blitSprite(' net/minecraft/client/gui/GuiGraphicsExtractor.java | head
grep -n 'public .*getAttackStrengthScale' -A3 net/minecraft/world/entity/player/Player.java
grep -n 'class AttackEntityEvent' -A12 net/minecraftforge/event/entity/player/AttackEntityEvent.java
grep -n 'public static .*Codec\|BY_NAME\|fromEnum' net/minecraft/util/StringRepresentable.java | head
grep -n 'public static .*idMapper\|public static .* STRING_UTF8' net/minecraft/network/codec/ByteBufCodecs.java | head
grep -n 'public .*getTagValueOf\|public static .* ofFullCopy\|withPath' net/minecraft/resources/Identifier.java | head -4
grep -n 'register\b\|"trim"\|"has_component"\|ID_MAPPER.put' $(find . -name ConditionalItemModelProperties.java) | head
exit 1
