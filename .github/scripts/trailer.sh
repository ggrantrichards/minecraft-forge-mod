#!/usr/bin/env bash
# Films the trailer in the world the survey generated (client/ClientTrailer.java), then cuts it
# together with the titles and the score (tools/trailer/compose.py). The video lands in trailer/.
set -u
LOG=run/logs/latest.log
rm -rf "$LOG" trailer-console.log run/screenshots/trailer trailer

# The narration: your own recordings, if there are any (tools/trailer/voice/; narrate.py). No synthetic voice.
sudo apt-get install -y -qq ffmpeg > /dev/null
python3 -m pip install -q --break-system-packages numpy pillow
python3 tools/trailer/narrate.py trailer || { echo "::error::the narration recordings could not be prepared"; exit 1; }
mkdir -p run/saves && rm -rf run/saves/survey-world && cp -r run/survey-world run/saves/survey-world
cp tools/trailer/scenes.txt run/astralend-trailer.txt
# A long view, the HUD at the size most people play at, no chat, no tutorial hints, no pausing when the
# window isn't focused; the view bobs as you walk (the fight is played in first person).
printf 'renderDistance:16\nsimulationDistance:10\nguiScale:3\nchatVisibility:2\nnarrator:0\nonboardAccessibility:false\ntutorialStep:none\nbobView:true\npauseOnLostFocus:false\nparticles:0\nentityShadows:true\nfov:0.0\n' > run/options.txt
# [trailer:<part>] in the commit message: film only the shots with <part> in their names.
ONLY=$(printf '%s' "${COMMIT_MESSAGE:-}" | grep -o '\[trailer:[^]]*\]' | head -1 | sed 's/\[trailer:\(.*\)\]/\1/')
[ -n "$ONLY" ] && echo "filming only the shots with \"$ONLY\" in their names"

export LIBGL_ALWAYS_SOFTWARE=1 SDL_VIDEODRIVER=x11 SDL_VIDEO_FORCE_EGL=1
export VK_ICD_FILENAMES=$(ls /usr/share/vulkan/icd.d/lvp_icd*.json 2>/dev/null | head -1)
setsid xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" ./gradlew runClient -PastralendTrailer \
    ${ONLY:+-PastralendTrailerOnly=$ONLY} > trailer-console.log 2>&1 &
PGID=$!
mkdir -p run/screenshots/trailer
python3 tools/trailer/squash.py run/screenshots/trailer &
SQUASH=$!
for i in $(seq 1 1600); do
    sleep 5
    if grep -q "ASTRALEND TRAILER DONE" "$LOG" 2>/dev/null; then
        sleep 10       # let the last frames finish saving
        break
    fi
    if grep -q -E "Crash report saved|Minecraft Crash Report|BUILD FAILED" trailer-console.log "$LOG" 2>/dev/null; then
        echo "::error::the client crashed while filming"
        grep -h -E "ERROR|Exception" "$LOG" trailer-console.log 2>/dev/null | head -20
        break
    fi
    if (( i % 24 == 0 )); then
        echo "...$((i * 5))s: $(ls run/screenshots/trailer 2>/dev/null | wc -l) frames, $(df -h . | tail -1 | awk '{print $4}') free; $(grep 'AstralEnd trailer' "$LOG" 2>/dev/null | tail -1 | sed 's/.*\] //')"
    fi
done
kill -TERM -- "-$PGID" 2>/dev/null; sleep 3; kill -KILL -- "-$PGID" 2>/dev/null; pkill -KILL -f 'net.minecraftforge' 2>/dev/null
touch run/screenshots/trailer/stop; wait $SQUASH; rm -f run/screenshots/trailer/stop
cp "$LOG" trailer.log 2>/dev/null
grep "AstralEnd trailer" trailer.log | sed 's/.*\[AstralEnd trailer\] //'
echo "$(ls run/screenshots/trailer/*.jpg 2>/dev/null | wc -l) frames filmed"
ls run/screenshots/trailer/*.jpg > /dev/null 2>&1 || { echo "::error::no frames were filmed"; tail -40 trailer.log; exit 1; }

# Cut it together: the game's own font for the titles, the game's own sounds and the mod's music for the
# soundtrack, ffmpeg for the video.
JAR=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
unzip -p "$JAR" assets/minecraft/textures/font/ascii.png > /tmp/ascii.png
unzip -p "$JAR" assets/minecraft/sounds.json > /tmp/sounds.json 2>/dev/null || true
ASSETS=$(dirname "$(dirname "$(find ~/.gradle .gradle -path '*assets/indexes/*.json' 2>/dev/null | head -1)")" 2>/dev/null)
echo "the game's sounds: ${ASSETS:-not found}"
export TRAILER_ASSETS="$ASSETS" TRAILER_SOUNDS_JSON=/tmp/sounds.json
python3 tools/trailer/compose.py run/screenshots/trailer /tmp/ascii.png trailer
# and the README's moving teaser: fifteen seconds of the Eclipse revealed
python3 tools/trailer/teaser.py trailer/the_astral_end_trailer.mp4 trailer/teaser.webp || true
ls -la trailer
