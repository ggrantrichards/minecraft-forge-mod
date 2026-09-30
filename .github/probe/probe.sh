#!/usr/bin/env bash
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src && cd /tmp/src
f() { find . -name "$1" | head -1; }
x=$(f WorldOpenFlows.java); grep -n -E 'experimental|Lifecycle|isExperimental|askForBackup|confirm|skip|forge|Forge' $x | head -40
grep -rn -E 'Lifecycle.experimental|Lifecycle.stable|worldGenSettingsLifecycle|isExperimental' --include=*.java net/minecraft/world/level/storage net/minecraft/resources/RegistryDataLoader.java net/minecraft/server/WorldLoader.java net/minecraft/core/*.java | head -30
grep -rn -i -E 'experimental|lifecycle' --include=*.java net/minecraftforge | head -30
x=$(f RegistryDataLoader.java); grep -n -E 'Lifecycle|RegistrationInfo|knownPack|KnownPack' $x | head -20
x=$(f RegistrationInfo.java); grep -v '^import' $x | head -30
