#!/usr/bin/env bash
# Boots the real Minecraft client on a virtual screen, waits until every texture atlas has been
# built (which means all models and textures were loaded), then fails if the game reported
# anything missing or broken for this mod. Used by CI; also runnable on Linux with Xvfb.
set -u
MOD=astralend
LOG=run/logs/latest.log
rm -f "$LOG" client-console.log

# Software OpenGL (Mesa llvmpipe) on a virtual X screen with the GLX extension enabled.
export LIBGL_ALWAYS_SOFTWARE=1
export SDL_VIDEODRIVER=x11
export SDL_VIDEO_FORCE_EGL=1        # EGL avoids GLX visual matching, which fails on Xvfb
export VK_ICD_FILENAMES=$(ls /usr/share/vulkan/icd.d/lvp_icd*.json 2>/dev/null | head -1)   # software Vulkan fallback
setsid xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew runClient > client-console.log 2>&1 &
PGID=$!

stop_client() {
    kill -TERM -- "-$PGID" 2>/dev/null
    sleep 3
    kill -KILL -- "-$PGID" 2>/dev/null
    pkill -KILL -f 'net.minecraftforge' 2>/dev/null
    true
}

loaded=0
for i in $(seq 1 90); do
    sleep 5
    # (26.3 builds an items atlas last; 1.21.1 puts items on the blocks atlas, so wait for the gui one)
    if grep -q -E "textures/atlas/(items|gui)" "$LOG" 2>/dev/null; then
        echo "Client finished loading textures after ~$((i * 5))s"
        sleep 15   # let any late warnings reach the log
        loaded=1
        break
    fi
    if grep -q -E "Crash report saved|Minecraft Crash Report|BUILD FAILED" client-console.log "$LOG" 2>/dev/null \
       || { grep -q "Failed to create backend OpenGL" "$LOG" 2>/dev/null && grep -q "Failed to create backend Vulkan" "$LOG" 2>/dev/null; }; then
        echo "::error::the client crashed or couldn't start its graphics"
        grep -h -E "ERROR|Exception" "$LOG" client-console.log 2>/dev/null | head -20
        stop_client
        exit 1
    fi
    if (( i % 6 == 0 )); then
        echo "...still loading after $((i * 5))s. Last log line:"
        tail -1 "$LOG" 2>/dev/null || tail -1 client-console.log
    fi
done
stop_client

cp "$LOG" client.log 2>/dev/null || { echo "::error::the client never wrote a log"; tail -80 client-console.log; exit 1; }
if [ "$loaded" != 1 ]; then
    echo "::error::the client didn't finish loading within 7.5 minutes"
    tail -60 client.log
    exit 1
fi

echo "Texture atlases built:"
grep "Created:" client.log | sed 's/.*Created: /  /'
if grep -i "$MOD" client.log | grep -i -E "WARN|ERROR|missing|unable|couldn't|failed"; then
    echo "::error::the client reported missing or broken models/textures for $MOD (see above)"
    exit 1
fi
echo "No missing models or textures reported for $MOD."
