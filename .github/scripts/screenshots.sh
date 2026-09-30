#!/usr/bin/env bash
# Opens the world the survey generated in the real client and photographs every astral biome
# (see dev/ClientScreenshots.java). Screenshots land in run/screenshots/astralend-*.png.
set -u
LOG=run/logs/latest.log
rm -f "$LOG" screenshots-console.log run/screenshots/astralend-*.png
mkdir -p run/saves && rm -rf run/saves/survey-world && cp -r run/survey-world run/saves/survey-world
# Render distance 10 chunks, no chat noise.
printf 'renderDistance:10\nsimulationDistance:6\nchatVisibility:2\nnarrator:0\nonboardAccessibility:false\ntutorialStep:none\n' > run/options.txt

export LIBGL_ALWAYS_SOFTWARE=1 SDL_VIDEODRIVER=x11 SDL_VIDEO_FORCE_EGL=1
export VK_ICD_FILENAMES=$(ls /usr/share/vulkan/icd.d/lvp_icd*.json 2>/dev/null | head -1)
setsid xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew runClient -PastralendScreenshots \
    > screenshots-console.log 2>&1 &
PGID=$!
for i in $(seq 1 200); do
    sleep 5
    if grep -q "ASTRALEND SCREENSHOTS DONE" "$LOG" 2>/dev/null; then
        sleep 5
        break
    fi
    if grep -q -E "Crash report saved|Minecraft Crash Report|BUILD FAILED" screenshots-console.log "$LOG" 2>/dev/null; then
        echo "::error::the client crashed while taking screenshots"
        grep -h -E "ERROR|Exception" "$LOG" screenshots-console.log 2>/dev/null | head -20
        break
    fi
    if (( i % 12 == 0 )); then
        echo "...$((i * 5))s: $(grep -c 'AstralEnd screenshots.*after' "$LOG" 2>/dev/null) screenshots so far"
    fi
done
kill -TERM -- "-$PGID" 2>/dev/null; sleep 3; kill -KILL -- "-$PGID" 2>/dev/null; pkill -KILL -f 'net.minecraftforge' 2>/dev/null
cp "$LOG" screenshots.log 2>/dev/null
grep "AstralEnd screenshots" screenshots.log | sed 's/.*\[AstralEnd screenshots\] //'
ls -la run/screenshots/ 2>/dev/null
ls run/screenshots/astralend-*.png > /dev/null 2>&1 || { echo "::error::no screenshots were taken"; tail -40 screenshots.log; exit 1; }
