"""Keeps the trailer's frames from filling the disk while it's filmed: every PNG in <dir> that the game
finished writing more than a few seconds ago becomes a high-quality JPEG (opening.py reads either).
Runs until <dir>/stop exists, then converts what's left.

    python3 tools/trailer/squash.py <frames dir> &
"""
import sys
import time
from pathlib import Path

from PIL import Image


def squash(folder, older_than):
    now = time.time()
    done = 0
    for png in sorted(folder.glob("*.png")):
        try:
            if now - png.stat().st_mtime < older_than:
                continue
            Image.open(png).convert("RGB").save(png.with_suffix(".jpg"), quality=95, subsampling=0)
            png.unlink()
            done += 1
        except (OSError, ValueError):
            pass   # still being written; next time
    return done


def main(folder):
    folder = Path(folder)
    folder.mkdir(parents=True, exist_ok=True)
    while not (folder / "stop").exists():
        if squash(folder, 8.0) == 0:
            time.sleep(4.0)
    squash(folder, 0.0)


if __name__ == "__main__":
    main(sys.argv[1])
