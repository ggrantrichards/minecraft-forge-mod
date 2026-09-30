#!/usr/bin/env bash
# Temporary: prints Minecraft/Forge 26.3 source the Phase 1 code depends on.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
show() { echo "=== FILE $1"; cat "$1"; echo "=== END $1"; }
echo "=== ITEM PROPERTIES"; awk '/public static class Properties/,0' net/minecraft/world/item/Item.java | grep -n -E 'public .*\(|^    }' | head -90
echo "=== ITEM CTOR"; grep -n -E 'public Item\(|public BlockItem\(' net/minecraft/world/item/Item.java net/minecraft/world/item/BlockItem.java
show net/minecraft/world/food/FoodProperties.java
grep -n -E 'public static final FoodProperties (BEETROOT|CARROT|SWEET_BERRIES)' -A1 net/minecraft/world/food/Foods.java
echo "=== CREATIVE TAB"; grep -n -E 'public (static )?[A-Za-z.<>]+ [a-zA-Z]+\(' net/minecraft/world/item/CreativeModeTab.java | head -40
grep -n -E 'interface Output|void accept|interface DisplayItemsGenerator' -A3 net/minecraft/world/item/CreativeModeTab.java | head -30
grep -rn -E 'CREATIVE_MODE_TAB' net/minecraft/core/registries/Registries.java | head -3
grep -rn -E 'CreativeModeTab.builder|withTabsBefore' net/minecraftforge | head -5
echo "=== HEIGHT"; grep -n -E 'default int (getMaxY|getMinY|getHeight|getMaxBuildHeight)\(' net/minecraft/world/level/LevelHeightAccessor.java
echo "=== PARTICLES"; grep -n -E 'void addParticle\(' net/minecraft/world/level/Level.java | head -5
grep -n -E 'END_ROD|WHITE_ASH|FALLING_DUST|GLOW ' net/minecraft/core/particles/ParticleTypes.java | head
echo "=== CANSUSTAIN"; grep -rn -A25 'default boolean canSustainPlant' net/minecraftforge/common/extensions/IForgeBlock.java | head -45
echo "=== REGISTER HELPERS"; grep -n -A8 -E 'private static Block register(Slab|Wall|LegacyStair)\(' net/minecraft/world/level/block/Blocks.java | head -40
grep -n -A10 -E ' (BEETROOTS|TWISTING_VINES|TWISTING_VINES_PLANT|DANDELION|GLASS|TINTED_GLASS|BONE_BLOCK|RED_MUSHROOM) = ' net/minecraft/world/level/block/Blocks.java | head -110
echo "=== SOUNDS"; grep -n -o -E 'public static final SoundType (MOSS|MOSS_CARPET|SAND|GLASS|BONE_BLOCK|DEEPSLATE|POLISHED_DEEPSLATE|DEEPSLATE_BRICKS|NETHER_SPROUTS|WEEPING_VINES|FUNGUS|GRASS|CROP|METAL|AMETHYST|AMETHYST_CLUSTER|STONE|CALCITE|TUFF|SCULK|NETHER_ORE|ANCIENT_DEBRIS)\b' net/minecraft/world/level/block/SoundType.java
echo "=== MAPCOLOR"; grep -n -o -E 'public static final MapColor (COLOR_PURPLE|COLOR_BLACK|TERRACOTTA_PURPLE|TERRACOTTA_BLACK|TERRACOTTA_WHITE|COLOR_LIGHT_BLUE|COLOR_CYAN|QUARTZ|METAL|RAW_IRON|SNOW|WOOL|PLANT|COLOR_MAGENTA|DEEPSLATE|SAND)\b' net/minecraft/world/level/material/MapColor.java
echo "=== BLOCKTAGS"; grep -n -o -E 'public static final TagKey<Block> (MINEABLE_WITH_PICKAXE|MINEABLE_WITH_SHOVEL|MINEABLE_WITH_HOE|MINEABLE_WITH_AXE|NEEDS_IRON_TOOL|NEEDS_DIAMOND_TOOL|SUPPORTS_VEGETATION|END_STONE)' net/minecraft/tags/BlockTags.java
echo "=== TAGDIRS"; unzip -l "$OLDPWD/$(find $OLDPWD/.gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1 | sed "s#$OLDPWD/##")" 2>/dev/null | grep -o 'data/minecraft/tags/[a-z_/]*' | sort -u | head -40
