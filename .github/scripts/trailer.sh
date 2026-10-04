#!/usr/bin/env bash
# Films the trailer in the world the survey generated (client/ClientTrailer.java), then cuts it
# together with the titles and the score (tools/trailer/compose.py). The video lands in trailer/.
set -u
LOG=run/logs/latest.log
rm -rf "$LOG" trailer-console.log run/screenshots/trailer trailer

# The narration first (tools/trailer/narrate.py), so a problem with it shows before the long filming.
sudo apt-get install -y -qq ffmpeg > /dev/null
python3 -m pip install -q --break-system-packages numpy pillow kokoro-onnx soundfile
mkdir -p /tmp/kokoro
for f in kokoro-v1.0.onnx voices-v1.0.bin; do
    [ -s /tmp/kokoro/$f ] || curl -sSL --retry 4 -o /tmp/kokoro/$f "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/$f"
done
python3 tools/trailer/narrate.py /tmp/kokoro/kokoro-v1.0.onnx /tmp/kokoro/voices-v1.0.bin trailer \
    || { echo "::error::the narration could not be spoken"; exit 1; }
mkdir -p run/saves && rm -rf run/saves/survey-world && cp -r run/survey-world run/saves/survey-world
cp tools/trailer/scenes.txt run/astralend-trailer.txt
# A longer view than the screenshots, no chat, no tutorial hints.
printf 'renderDistance:12\nsimulationDistance:8\nchatVisibility:2\nnarrator:0\nonboardAccessibility:false\ntutorialStep:none\nbobView:false\n' > run/options.txt

export LIBGL_ALWAYS_SOFTWARE=1 SDL_VIDEODRIVER=x11 SDL_VIDEO_FORCE_EGL=1
export VK_ICD_FILENAMES=$(ls /usr/share/vulkan/icd.d/lvp_icd*.json 2>/dev/null | head -1)
setsid xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew runClient -PastralendTrailer \
    > trailer-console.log 2>&1 &
PGID=$!
for i in $(seq 1 1000); do
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
        echo "...$((i * 5))s: $(ls run/screenshots/trailer 2>/dev/null | wc -l) frames; $(grep 'AstralEnd trailer' "$LOG" 2>/dev/null | tail -1 | sed 's/.*\] //')"
    fi
done
kill -TERM -- "-$PGID" 2>/dev/null; sleep 3; kill -KILL -- "-$PGID" 2>/dev/null; pkill -KILL -f 'net.minecraftforge' 2>/dev/null
cp "$LOG" trailer.log 2>/dev/null
grep "AstralEnd trailer" trailer.log | sed 's/.*\[AstralEnd trailer\] //'
echo "$(ls run/screenshots/trailer 2>/dev/null | wc -l) frames filmed"
ls run/screenshots/trailer/*.png > /dev/null 2>&1 || { echo "::error::no frames were filmed"; tail -40 trailer.log; exit 1; }

# Cut it together: the game's own font for the titles, numpy for the score, ffmpeg for the video.
JAR=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
unzip -p "$JAR" assets/minecraft/textures/font/ascii.png > /tmp/ascii.png
python3 tools/trailer/compose.py run/screenshots/trailer /tmp/ascii.png trailer
# and the README's moving teaser: fifteen seconds of the Eclipse revealed
python3 tools/trailer/teaser.py trailer/the_astral_end_trailer.mp4 trailer/teaser.webp || true
ls -la trailer
