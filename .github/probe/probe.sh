#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
grep -n "GLFW_KEY_F1\|KEY_F1\|case 290\|hideGui\|hudHidden\|toggleGui" net/minecraft/client/KeyboardHandler.java | head
grep -rn "hudHidden\|isHudHidden\|hideHud\|guiHidden" --include=*.java net/minecraft/client | head
grep -n "public boolean \|public final boolean" net/minecraft/client/Options.java | head -40
f=$(find . -path '*minecraftforge/client/event/CustomizeGuiOverlayEvent.java'); grep -n "class \|BUS\|public" "$f" | head -20
f=$(find . -path '*minecraftforge/client/event/AddGuiOverlayLayersEvent.java'); grep -n "class \|BUS\|public" "$f" | head -20
grep -n "public void render(" -A30 net/minecraft/client/gui/Gui.java | head -50
grep -n "renderCrosshair\|SPECTATOR\|isSpectator" net/minecraft/client/gui/Gui.java | head
grep -n "public static final .* LIVE_COMET\|DRIFT_WHALE" -r --include=*.java . | head -2
exit 1
