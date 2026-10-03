#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
mkdir -p /tmp/music/files && cd /tmp/music/files
for u in OutThere_0.ogg ObservingTheStar.ogg "Heavenly%20Loop_0.ogg" Ambient-Loop-isaiah658_0.ogg Space_Music.ogg; do
  curl -sL "https://opengameart.org/sites/default/files/$u" -o "$(echo $u | sed 's/%20/_/g')"
done
file * ; ls -la
cd "$GITHUB_WORKSPACE"
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src && cd /tmp/src
grep -rn "BACKGROUND_MUSIC\|AMBIENT_SOUNDS\|MUSIC_VOLUME" net/minecraft/world/attribute/EnvironmentAttributes.java | head
F=$(grep -rl "class BackgroundMusic" net/minecraft | head -1); echo $F; sed -n 1,60p $F | grep -n "record\|fieldOf\|Codec"
F=$(grep -rl "record AmbientSounds\|class AmbientSounds" net/minecraft | head -1); echo $F; sed -n 1,60p $F | grep -n "record\|fieldOf"
grep -rn "Musics.END\b\|END_BOSS\|createGameMusic" net/minecraft/sounds/Musics.java | head -5
grep -n "background_music\|BACKGROUND_MUSIC" -r net/minecraft/data/worldgen/biome/EndBiomes.java | head
exit 0
