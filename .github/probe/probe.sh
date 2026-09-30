#!/usr/bin/env bash
# Temporary: prints Minecraft/Forge 26.3 source and vanilla assets the Phase 1 code depends on.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets
unzip -qo "$SRC" -d /tmp/src
unzip -qo "$ASSETS" -d /tmp/assets
B=/tmp/src/net/minecraft/world/level/block
for f in FallingBlock ColoredFallingBlock StairBlock SlabBlock WallBlock TwistingVinesBlock TwistingVinesPlantBlock GrowingPlantHeadBlock GrowingPlantBodyBlock GrowingPlantBlock FlowerBlock CropBlock BeetrootBlock VegetationBlock BushBlock MushroomBlock TransparentBlock TintedGlassBlock DropExperienceBlock RotatedPillarBlock MossBlock BonemealableBlock; do
  [ -f "$B/$f.java" ] && { echo "=== FILE $f"; cat "$B/$f.java"; echo "=== END $f"; } || echo "=== MISSING $f"
done
echo "=== GREP Blocks"
grep -n -A4 -E ' (END_STONE|DEEPSLATE_DIAMOND_ORE|DIAMOND_ORE|RAW_IRON_BLOCK|IRON_BLOCK|DIAMOND_BLOCK|SAND|GLASS|TINTED_GLASS|TWISTING_VINES|TWISTING_VINES_PLANT|BEETROOTS|MOSS_BLOCK|BONE_BLOCK|COBBLED_DEEPSLATE|COBBLED_DEEPSLATE_STAIRS|COBBLED_DEEPSLATE_SLAB|COBBLED_DEEPSLATE_WALL|POLISHED_DEEPSLATE|DEEPSLATE_BRICKS|CHISELED_DEEPSLATE|DANDELION|RED_MUSHROOM|WITHER_ROSE|SEA_LANTERN|SHROOMLIGHT) = ' /tmp/src/net/minecraft/world/level/block/Blocks.java | head -400
echo "=== GREP BlocksRegisterHelpers"
grep -n -E 'private static Block register|public static Block register|registerStair|registerLegacyStair|static .*Properties .*(flowerPotProperties|wallVariant)' /tmp/src/net/minecraft/world/level/block/Blocks.java | head -30
echo "=== GREP Items"
grep -n -A3 -E ' (BEETROOT_SEEDS|DIAMOND|RAW_IRON|IRON_INGOT|IRON_NUGGET|TWISTING_VINES|WHEAT_SEEDS) = ' /tmp/src/net/minecraft/world/item/Items.java | head -60
grep -n -E 'registerBlock\(|public static Item registerItem\(' /tmp/src/net/minecraft/world/item/Items.java | head -12
echo "=== GREP ItemProperties"
grep -n -E 'public Properties [a-zA-Z]+\(' /tmp/src/net/minecraft/world/item/Item.java | head -60
echo "=== GREP BlockProperties"
grep -n -E 'public (BlockBehaviour\.)?Properties [a-zA-Z]+\(' /tmp/src/net/minecraft/world/level/block/state/BlockBehaviour.java | head -80
echo "=== GREP RenderTypes"
grep -rn -E 'TWISTING_VINES|Blocks\.GLASS|BEETROOTS|DANDELION' /tmp/src/net/minecraft/client/renderer/ItemBlockRenderTypes.java | head
grep -rln -E 'render_type|renderType' /tmp/src/net/minecraftforge 2>/dev/null | head -20
grep -rn '"render_type"' /tmp/src 2>/dev/null | head -10
ls /tmp/src/net/minecraft/client/renderer/chunk/ 2>/dev/null | head -30
grep -rn "enum ChunkSectionLayer" -A12 /tmp/src/net/minecraft 2>/dev/null | head -20
echo "=== MODEL NAMES"
ls /tmp/assets/assets/minecraft/models/block/ | grep -E '^(template_wall|slab|stairs|inner_stairs|outer_stairs|wall_inventory|cross|tinted_cross|crop|cube_all|cube_column|cube_bottom_top|flower_pot_cross|leaves|block)' 
for f in blockstates/twisting_vines.json blockstates/twisting_vines_plant.json blockstates/beetroots.json blockstates/cobbled_deepslate_wall.json blockstates/cobbled_deepslate_stairs.json blockstates/cobbled_deepslate_slab.json blockstates/bone_block.json models/block/twisting_vines.json models/block/beetroots_stage0.json models/block/crop.json models/block/cross.json models/block/cobbled_deepslate_wall_post.json models/block/glass.json models/block/tinted_glass.json models/block/dandelion.json models/block/red_mushroom.json items/twisting_vines.json items/beetroot_seeds.json items/cobbled_deepslate_wall.json items/dandelion.json items/glass.json textures/block/sea_lantern.png.mcmeta textures/block/magma.png.mcmeta; do echo "=== ASSET $f"; cat "/tmp/assets/assets/minecraft/$f"; echo; done
for f in loot_table/blocks/beetroots.json loot_table/blocks/diamond_ore.json loot_table/blocks/iron_ore.json loot_table/blocks/cobbled_deepslate_slab.json loot_table/blocks/twisting_vines.json loot_table/blocks/twisting_vines_plant.json loot_table/blocks/glass.json loot_table/blocks/dandelion.json recipe/cobbled_deepslate_stairs.json recipe/cobbled_deepslate_slab.json recipe/cobbled_deepslate_wall.json recipe/cobbled_deepslate_stairs_from_cobbled_deepslate_stonecutting.json recipe/iron_ingot_from_smelting_raw_iron.json recipe/iron_ingot_from_blasting_raw_iron.json recipe/glass.json recipe/diamond_block.json recipe/diamond.json recipe/polished_deepslate.json recipe/deepslate_bricks.json recipe/chiseled_deepslate.json tags/block/walls.json tags/block/slabs.json tags/item/slabs.json tags/block/mineable/pickaxe.json; do echo "=== DATA $f"; cat "/tmp/assets/data/minecraft/$f" | head -60; echo; done
ls /tmp/assets/data/minecraft/tags/block | head -100
