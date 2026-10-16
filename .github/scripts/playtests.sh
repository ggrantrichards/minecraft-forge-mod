#!/usr/bin/env bash
# Plays the scripted playtests (client/ClientPlaytests.java) in the world the survey generated, in
# the real client. Pictures land in run/screenshots/playtest/, the log in playtests.log.
set -u
LOG=run/logs/latest.log
rm -rf "$LOG" playtests-console.log run/screenshots/playtest
mkdir -p run/saves && rm -rf run/saves/survey-world && cp -r run/survey-world run/saves/survey-world
python3 .github/scripts/confirm-world.py run/saves/survey-world   # (1.21.1: no experimental-settings question)
printf 'renderDistance:10\nsimulationDistance:8\nchatVisibility:2\nnarrator:0\nonboardAccessibility:false\ntutorialStep:none\n' > run/options.txt

export LIBGL_ALWAYS_SOFTWARE=1 SDL_VIDEODRIVER=x11 SDL_VIDEO_FORCE_EGL=1
export VK_ICD_FILENAMES=$(ls /usr/share/vulkan/icd.d/lvp_icd*.json 2>/dev/null | head -1)
setsid xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew runClient -PastralendPlaytests \
    > playtests-console.log 2>&1 &
PGID=$!
for i in $(seq 1 300); do
    sleep 5
    if grep -q "ASTRALEND PLAYTESTS DONE" "$LOG" 2>/dev/null; then
        sleep 5
        break
    fi
    if grep -q -E "Crash report saved|Minecraft Crash Report|BUILD FAILED" playtests-console.log "$LOG" 2>/dev/null; then
        echo "::error::the client crashed during the playtests"
        grep -h -E "ERROR|Exception" "$LOG" playtests-console.log 2>/dev/null | head -20
        break
    fi
done
kill -TERM -- "-$PGID" 2>/dev/null; sleep 3; kill -KILL -- "-$PGID" 2>/dev/null; pkill -KILL -f 'net.minecraftforge' 2>/dev/null
cp "$LOG" playtests.log 2>/dev/null
grep "AstralEnd playtest" playtests.log | sed 's/.*\[AstralEnd playtest\] //'
ls run/screenshots/playtest/*.png > /dev/null 2>&1 || { echo "::error::the playtests recorded nothing"; tail -40 playtests.log; exit 1; }
python3 -m pip install -q --break-system-packages pillow 2>/dev/null || true
# One contact sheet per test, in order, so a test reads at a glance.
python3 - <<'PY'
import glob, os, re
from PIL import Image, ImageDraw
files = sorted(glob.glob("run/screenshots/playtest/*.png"))
tests = {}
for f in files:
    m = re.match(r".*/(.+)_(\d+)\.png", f)
    tests.setdefault(m.group(1), []).append((int(m.group(2)), f))
os.makedirs("playtests", exist_ok=True)
for name, shots in tests.items():
    shots.sort()
    W, H, cols = 427, 240, 4
    rows = (len(shots) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * W, rows * (H + 16)), (20, 20, 26))
    d = ImageDraw.Draw(sheet)
    for i, (t, f) in enumerate(shots):
        x, y = (i % cols) * W, (i // cols) * (H + 16)
        sheet.paste(Image.open(f).convert("RGB").resize((W, H)), (x, y))
        d.text((x + 4, y + H + 2), f"{name} t={t}", fill=(220, 220, 230))
    sheet.save(f"playtests/{name}.jpg", quality=82)
    print("sheet", name, len(shots))
PY
