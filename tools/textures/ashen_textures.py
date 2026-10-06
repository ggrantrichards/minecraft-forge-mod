#!/usr/bin/env python3
"""The Ashen Barrens' palette after beta.4: bone and ash. Recolours the old aurora-teal Shimmer Grass
and Shimmer Reed (now Ashen Grass and Ashen Reed) to dead, pale ash-grey, keeping their shapes:

    python3 tools/textures/ashen_textures.py

Runs on the originals, kept in tools/textures/ashen_src/ the first time.
"""
import colorsys
import shutil
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures/block"
SRC = Path(__file__).resolve().parent / "ashen_src"
ASH = ["#2b2926", "#45423d", "#635f58", "#847f76", "#a8a296", "#cbc5b7", "#e6e0d2"]
TEXTURES = ["shimmer_grass.png", "shimmer_reed_bottom.png", "shimmer_reed_top.png"]


def hexc(h):
    return tuple(int(h.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4))


def ramp(t):
    cols = [hexc(c) for c in ASH]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    return tuple(round(cols[i][k] + (cols[i + 1][k] - cols[i][k]) * f) for k in range(3))


def main():
    SRC.mkdir(exist_ok=True)
    for name in TEXTURES:
        if not (SRC / name).exists():
            shutil.copy(TEX / name, SRC / name)
        img = Image.open(SRC / name).convert("RGBA")
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                if a:
                    _, l, _ = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
                    px[x, y] = ramp(l * 1.05) + (a,)
        img.save(TEX / name)
    print("Recoloured", ", ".join(TEXTURES))


if __name__ == "__main__":
    main()
