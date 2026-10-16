"""(1.21.1 port) Marks a world's experimental-settings warning as already confirmed (level.dat's
confirmedExperimentalSettings), so the client opens it straight away: 1.21.1 asks first about any world
whose world generation comes from data, as the mod's End does.

    python3 .github/scripts/confirm-world.py run/saves/survey-world
"""
import gzip, sys
from pathlib import Path

dat = Path(sys.argv[1]) / "level.dat"
raw = gzip.decompress(dat.read_bytes())
key = b"\x01\x00\x1dconfirmedExperimentalSettings"
i = raw.find(key)
if i < 0:
    print("confirm-world: no confirmedExperimentalSettings in", dat)
    sys.exit(0)
j = i + len(key)
raw = raw[:j] + b"\x01" + raw[j + 1:]
dat.write_bytes(gzip.compress(raw))
print("confirm-world: confirmed", dat)
