#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
echo "ASCII_B64_START"; unzip -p "$J" assets/minecraft/textures/font/ascii.png | base64 -w0; echo; echo "ASCII_B64_END"
unzip -p "$J" assets/minecraft/font/include/space.json
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src
cd /tmp/src
sed -n 200,216p net/minecraft/client/gui/Hud.java
grep -n "Hud" net/minecraft/client/Minecraft.java | head
exit 1
