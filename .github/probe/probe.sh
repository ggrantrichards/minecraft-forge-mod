#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
cd /tmp/src
f() { find . -name "$1" | head -1; }
echo "=== SCREENSHOT"; grep -n -E 'public static .*\(' $(f Screenshot.java) | head
grep -n -E 'public .* getMainRenderTarget|public final File gameDirectory|public .* gameDirectory|public .* getSingleplayerServer|quickPlay|public .* createWorldOpenFlows' net/minecraft/client/Minecraft.java | head
grep -rn -E 'quickPlay[A-Za-z]*' net/minecraft/client/main/Main.java | head -8
grep -n -E 'public boolean hideGui|renderDistance|public OptionInstance<Integer> fov|framerateLimit|public .* renderDistance\(' net/minecraft/client/Options.java | head
grep -n -E 'public .* (countRenderedSections|hasRenderedAllSections|isSectionCompiled|getLastViewDistance|allChanged|isChunkCompiled)\(' net/minecraft/client/renderer/LevelRenderer.java | head
echo "=== TELEPORT"; sed -n 13,60p net/minecraft/world/level/portal/TeleportTransition.java
grep -n -E 'public boolean setGameMode|public .* teleportTo\(' net/minecraft/server/level/ServerPlayer.java | head
grep -n -E 'class ClientTickEvent|class .*Post|BUS' net/minecraftforge/event/TickEvent.java | head -30
grep -rn -E 'class ScreenEvent|Init.Post' net/minecraftforge/client/event/ScreenEvent.java | head -5
echo "=== BLOCK CLASSES"
for c in DoorBlock TrapDoorBlock FenceGateBlock ButtonBlock PressurePlateBlock FenceBlock LeavesBlock UntintedParticleLeavesBlock TintedParticleLeavesBlock SaplingBlock HugeMushroomBlock WeepingVinesBlock WeepingVinesPlantBlock FungusBlock; do
  x=$(f $c.java); echo "--- $c ($x)"; grep -n -E "public $c\(|protected $c\(|abstract|MapCodec<" "$x" | head -5; done
grep -n -E 'BlockSetType (CRIMSON|WARPED|OAK)|public static final BlockSetType [A-Z_]+ =' $(f BlockSetType.java) | head -12
grep -n -E 'WoodType (CRIMSON|WARPED|OAK) ' $(f WoodType.java) | head
grep -n -E 'public TreeGrower\(|public static final TreeGrower OAK' $(f TreeGrower.java)
for b in CRIMSON_STEM STRIPPED_CRIMSON_STEM CRIMSON_HYPHAE CRIMSON_PLANKS CRIMSON_DOOR CRIMSON_TRAPDOOR CRIMSON_FENCE_GATE CRIMSON_FENCE CRIMSON_BUTTON CRIMSON_PRESSURE_PLATE CRIMSON_STAIRS CRIMSON_SLAB OAK_LEAVES DARK_OAK_LEAVES OAK_SAPLING SHROOMLIGHT RED_MUSHROOM_BLOCK MUSHROOM_STEM WEEPING_VINES WEEPING_VINES_PLANT; do
  echo "--- $b"; grep -n -A7 " $b = register" net/minecraft/world/level/block/Blocks.java | head -9; done
grep -n -E 'static Block log\(|static Block leaves\(|private static Block (netherStem|woodenButton|stoneButton)' -A8 net/minecraft/world/level/block/Blocks.java | head -60
echo "=== STRIP"; grep -rn -E 'STRIPPABLES|getToolModifiedState|AXE_STRIP' --include=*.java net/minecraft/world/item/AxeItem.java net/minecraftforge | head -10
D=/tmp/assets/data/minecraft; A=/tmp/assets/assets/minecraft
c() { echo "=== DATA ${1#/tmp/assets/}"; tr -s ' \n' ' ' < "$1" 2>/dev/null | head -c ${2:-1800} || echo MISSING; echo; }
for b in crimson_door crimson_trapdoor crimson_fence_gate crimson_fence crimson_button crimson_pressure_plate crimson_stem crimson_hyphae oak_leaves oak_sapling weeping_vines weeping_vines_plant shroomlight red_mushroom_block; do c $A/blockstates/$b.json 2500; c $A/items/$b.json 400; done
for m in crimson_door_bottom_left crimson_door_top_right_open crimson_trapdoor_bottom crimson_trapdoor_open crimson_fence_gate crimson_fence_gate_wall_open crimson_fence_post crimson_fence_side crimson_fence_inventory crimson_button crimson_button_pressed crimson_button_inventory crimson_pressure_plate crimson_pressure_plate_down oak_leaves oak_sapling crimson_stem crimson_hyphae weeping_vines red_mushroom_block_inventory; do c $A/models/block/$m.json 600; done
c $A/models/item/crimson_door.json 300; c $A/models/item/oak_sapling.json 300; c $A/items/oak_leaves.json 400
for l in crimson_door oak_leaves crimson_stem crimson_slab oak_sapling red_mushroom_block; do c $D/loot_table/blocks/$l.json 2500; done
for r in crimson_planks crimson_door crimson_trapdoor crimson_fence crimson_fence_gate crimson_button crimson_pressure_plate crimson_hyphae crimson_stairs stripped_crimson_hyphae; do c $D/recipe/$r.json 700; done
ls $D/tags/block $D/tags/item | grep -E 'wood|plank|log|stem|door|fence|button|pressure|leaves|sapling|flammable|axe|climb' | sort -u | tr '\n' ' '; echo
c $D/tags/item/non_flammable_wood.json; c $D/tags/block/crimson_stems.json; c $D/tags/item/planks.json 300; c $D/tags/block/logs.json; c $D/tags/block/mineable/axe.json 400
for n in continentalness erosion jagged surface patch gravel; do c $D/worldgen/noise/$n.json 300; done
