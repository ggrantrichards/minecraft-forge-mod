#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
grep -n "isHidden" net/minecraft/client/gui/Hud.java | head
grep -n "public final Hud\|Hud hud\|public Hud\| Hud " net/minecraft/client/Minecraft.java | head
grep -n "public .*setFOV\|public .*getFOV\|public void setFov\|public float getFov" $(find . -path '*minecraftforge/client/event/ViewportEvent.java')
grep -n "public OptionInstance<Integer> fov\|fov()" net/minecraft/client/Options.java | head -3
grep -n "getFps\|public static int fps\|int getFps" net/minecraft/client/Minecraft.java | head
grep -n "public .*isSpectator\|renderCrosshair" net/minecraft/client/gui/Hud.java | head
exit 1
