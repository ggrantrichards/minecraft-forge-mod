#!/usr/bin/env bash
# Temporary: trailer probe.
set +e
which ffmpeg; ffmpeg -version | head -1; python3 -c "import numpy; print('numpy', numpy.__version__)"; python3 -c "import PIL; print('PIL', PIL.__version__)"
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
echo "client-extra: $J"; unzip -l "$J" | grep -E "textures/font/|font/.*json" | head -20
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
grep -rn "hideGui" --include=*.java net/minecraft/client | head -20
grep -n "public .*(" net/minecraft/server/ServerTickRateManager.java net/minecraft/world/TickRateManager.java | head -30
grep -n "public static .* grab" -A3 net/minecraft/client/Screenshot.java
grep -n "public int getFps\|fps" net/minecraft/client/Minecraft.java | head -5
grep -n "renderCrosshair\|canRenderCrosshairForSpectator" -A12 net/minecraft/client/gui/Gui.java | head -50
grep -n "public void snapTo\|public final void snapTo\|setOldPosAndRot" net/minecraft/world/entity/Entity.java | head
grep -n "bossEvent\|BossEvent" $(find . -name OrrerySentinel.java) net/minecraft/world/entity/boss/wither/WitherBoss.java | head -10
f=$(find . -path '*minecraftforge/client/event/ViewportEvent.java'); grep -n "class \|public" "$f" | head -40
f=$(find . -path '*minecraftforge/client/event/RenderGuiEvent.java'); grep -n "class \|public\|Cancel" "$f" | head -20
ls $(dirname $(find . -path '*minecraftforge/client/event/ViewportEvent.java')) | head -80
grep -n "ENTITY_INTERACTION_RANGE\|BLOCK_INTERACTION_RANGE" net/minecraft/world/entity/ai/attributes/Attributes.java
grep -n "class ClientTickEvent\|Pre\|Post" $(find . -path '*minecraftforge/event/TickEvent.java') | head
grep -n "public .*resize\|setWindowed\|setWidth\|public void setTitle\|public int getWidth" net/minecraft/client/Minecraft.java com/mojang/blaze3d/platform/Window.java | head
grep -n "\"width\"\|\"height\"\|accepts(\"width\|optionSpec.*width" net/minecraft/client/main/Main.java | head
grep -n "smoothCamera\|cinematic" net/minecraft/client/Options.java | head
exit 1
