#!/usr/bin/env bash
# Temporary: prints Minecraft/Forge 26.3 source and vanilla data that Phase 3 depends on.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
echo "SRC=$SRC"; find .gradle/mavenizer/repo -name '*-sources.jar' | head
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
cd /tmp/src
show() { f=$(find . -path "*$1" | head -1); echo "=== FILE $f"; grep -v '^import' "$f"; echo "=== END $1"; }
# --- spectator in-wall overlay
show /ScreenEffectRenderer.java
grep -rn -E 'RenderBlockScreenEffectEvent|renderBlockOverlay|onRenderBlockOverlay' --include=*.java . | head -20
f=$(find . -name 'RenderBlockScreenEffectEvent.java' | head -1); [ -n "$f" ] && grep -v '^import' "$f"
grep -rn -B3 -A25 'static boolean renderBlockOverlay\|public static boolean renderBlockOverlay\|renderFireOverlay(' net/minecraftforge/client/ForgeHooksClient.java | head -80
grep -rn 'noPhysics' net/minecraft/world/entity/player/Player.java net/minecraft/client/player/LocalPlayer.java net/minecraft/world/entity/Entity.java | head
grep -rn -n 'isSpectator\|noPhysics\|renderScreenEffect\|ScreenEffectRenderer' net/minecraft/client/renderer/GameRenderer.java net/minecraft/client/renderer/LevelRenderer.java net/minecraft/client/Camera.java | head -30
# --- End portal travel
show /EndPortalBlock.java
grep -n -E 'END_SPAWN_POINT|makeObsidianPlatform|getDragonFight|dragonFight' net/minecraft/server/level/ServerLevel.java | head
grep -n -E 'public .*\(' net/minecraft/world/level/dimension/end/EndDragonFight.java | head -40
grep -n -E 'public .* (teleport|changeDimension|canUsePortal|canTeleport|setAsInsidePortal|handlePortal|isOnPortalCooldown)\(' net/minecraft/world/entity/Entity.java
grep -n -E 'public (record|static|TeleportTransition)' net/minecraft/world/level/portal/TeleportTransition.java | head -20
grep -n -E 'findNearestMapStructure|EYE_OF_ENDER' net/minecraft/server/level/ServerLevel.java net/minecraft/world/level/chunk/ChunkGenerator.java net/minecraft/tags/StructureTags.java | head
grep -n -E 'public .*\(' net/minecraft/world/level/chunk/ChunkGeneratorStructureState.java | head
# --- density functions & features (Java)
show /levelgen/DensityFunction.java
grep -n -E 'register\(|MapCodec<|KeyDispatchDataCodec|class EndIslandDensityFunction|HOLDER_HELPER_CODEC|static .* makeCodec' net/minecraft/world/level/levelgen/DensityFunctions.java | head -80
grep -n -A40 'class EndIslandDensityFunction' net/minecraft/world/level/levelgen/DensityFunctions.java | head -70
grep -n -E 'DENSITY_FUNCTION|FEATURE =|BLOCK_ENTITY_TYPE =' net/minecraft/core/registries/Registries.java
grep -n -E 'public Feature\(|public abstract boolean place|public .* place\(' net/minecraft/world/level/levelgen/feature/Feature.java
grep -n -E 'public .*\(' net/minecraft/world/level/levelgen/feature/FeaturePlaceContext.java
grep -n 'CODEC\|INSTANCE' net/minecraft/world/level/levelgen/feature/configurations/NoneFeatureConfiguration.java
grep -n -E 'public static final Feature<' net/minecraft/world/level/levelgen/feature/Feature.java | head -80
# --- blocks we copy
grep -n -A3 -E ' (SUNFLOWER|MOSS_CARPET|BLUE_ICE|SHORT_GRASS|DEAD_BUSH|GLOW_LICHEN|PALE_MOSS_CARPET|SHORT_DRY_GRASS|CRIMSON_ROOTS|SPAWNER|QUARTZ_PILLAR|CALCITE|TUFF) = register' net/minecraft/world/level/block/Blocks.java
grep -n -E 'public CarpetBlock|public DoublePlantBlock|public TallFlowerBlock|public TallGrassBlock|public BaseEntityBlock|MapCodec<.*codec' net/minecraft/world/level/block/CarpetBlock.java net/minecraft/world/level/block/DoublePlantBlock.java net/minecraft/world/level/block/TallFlowerBlock.java net/minecraft/world/level/block/TallGrassBlock.java net/minecraft/world/level/block/BaseEntityBlock.java
show /block/TallGrassBlock.java
grep -n -E 'interface EntityBlock|default|BlockEntity newBlockEntity|getTicker' net/minecraft/world/level/block/EntityBlock.java
grep -n -E 'public BlockEntityType\(|public static .*Builder|interface BlockEntitySupplier|MOB_SPAWNER =|BELL =' net/minecraft/world/level/block/entity/BlockEntityType.java | head
grep -n -E 'public BlockEntity\(|protected void (save|load)Additional|public .* getType\(' net/minecraft/world/level/block/entity/BlockEntity.java | head
grep -n -E 'static <T extends BlockEntity> BlockEntityTicker|createTickerHelper' net/minecraft/world/level/block/BaseEntityBlock.java
grep -n -A3 'GRAVITY =' net/minecraft/world/entity/ai/attributes/Attributes.java
grep -n -E 'getEffectiveGravity|getGravity\(\)' -A6 net/minecraft/world/entity/LivingEntity.java | head -20
grep -rn -E 'registerConfig|public .* getModBusGroup' net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext.java | head
find . -name ForgeConfigSpec.java -o -name ModConfig.java | head
grep -n -E 'public .* (SpawnEggItem|spawnEgg)\(|SpawnEggItem' net/minecraft/world/item/Items.java | head -5
# --- vanilla data/assets we mirror
D=/tmp/assets/data/minecraft; A=/tmp/assets/assets/minecraft
c() { echo "=== DATA $1"; tr -s ' \n' ' ' < "$1" 2>/dev/null | head -c ${2:-2500} || echo MISSING; echo; }
for f in feature/ore_iron.json placed_feature/ore_iron_upper.json placed_feature/ore_iron_middle.json feature/ore_ancient_debris_large.json feature/forest_rock.json placed_feature/forest_rock.json feature/disk_sand.json placed_feature/disk_sand.json feature/ice_spike.json feature/blue_ice.json placed_feature/blue_ice.json feature/glow_lichen.json placed_feature/glow_lichen.json feature/basalt_pillar.json placed_feature/basalt_pillar.json feature/end_spike.json feature/patch_sunflower.json feature/sunflower.json placed_feature/patch_sunflower.json placed_feature/patch_grass_plain.json feature/patch_grass.json feature/grass.json feature/dripstone_cluster.json placed_feature/patch_dead_bush_2.json feature/patch_dead_bush.json feature/pile_hay.json feature/seagrass_simple.json placed_feature/end_island_decorated.json; do c $D/worldgen/$f; done
echo "=== LIST feature"; ls $D/worldgen/feature | tr '\n' ' '; echo
echo "=== LIST placed"; ls $D/worldgen/placed_feature | tr '\n' ' '; echo
c $D/worldgen/material_rule/overworld.json 6000
echo "=== LIST material_condition"; ls $D/worldgen/material_condition | tr '\n' ' '; echo
for f in $(ls $D/worldgen/material_condition | head -12); do c $D/worldgen/material_condition/$f 400; done
c $D/worldgen/noise_settings/end.json 5000
ls $D/worldgen/density_function/end; c $D/worldgen/density_function/end/sloped_cheese.json; c $D/worldgen/density_function/end/base_3d_noise.json
ls $D/worldgen/noise | tr '\n' ' '; echo; c $D/worldgen/noise/temperature.json; c $D/worldgen/noise/vegetation.json; c $D/worldgen/noise/ridge.json
c $D/worldgen/biome/the_end.json; c $D/worldgen/biome/end_highlands.json 4000
for f in sunflower short_grass moss_carpet blue_ice glow_lichen dead_bush; do c $D/loot_table/blocks/$f.json 1500; c $A/blockstates/$f.json 1500; c $A/items/$f.json 600; done
for f in sunflower_top sunflower_bottom double_plant_top? tall_grass_top short_grass moss_carpet carpet dead_bush cross tinted_cross; do c $A/models/block/$f.json 800; done
ls $A/models/block | grep -E 'template|carpet|double|tall' | tr '\n' ' '; echo
ls $A/particle | tr '\n' ' '; echo
ls $A/textures/particle | tr '\n' ' '; echo
echo "=== BLOCK TAGS"; ls $D/tags/block | tr '\n' ' '; echo
c $D/tags/block/ice.json; c $D/tags/block/replaceable_by_trees.json; c $D/tags/block/sword_efficient.json 400; c $D/tags/block/features_cannot_replace.json
