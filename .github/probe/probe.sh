#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
cd /tmp/src
echo "=== ForgeHooksClient render_type"; grep -n -B15 -A30 '"render_type"' net/minecraftforge/client/ForgeHooksClient.java
echo "=== NamedRenderTypeManager"; grep -v '^import' net/minecraftforge/client/NamedRenderTypeManager.java | head -120
echo "=== who calls getRenderType / ChunkSectionLayer in block model code"
grep -rn "ChunkSectionLayer\.\|byTransparency\|getRenderType(\|renderType()" net/minecraft/client/renderer/block net/minecraft/client/resources/model net/minecraft/client/renderer/chunk/SectionCompiler.java net/minecraftforge/client/model 2>/dev/null | head -60
f=$(find . -name ItemBlockRenderTypes.java | head -1); echo "=== $f"; grep -n "public static\|CUTOUT\|LEAVES\|MOSS_CARPET\|GLOW_LICHEN\|VINE\b" $f | head -40
echo "=== glow_lichen / vine models"; cat /tmp/assets/assets/minecraft/models/block/glow_lichen.json /tmp/assets/assets/minecraft/models/block/vine.json /tmp/assets/assets/minecraft/models/block/leaves.json 2>&1 | head -60
grep -rln '"render_type"' /tmp/assets/assets/minecraft/models | head -5
exit 1
