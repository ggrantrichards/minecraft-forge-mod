#!/usr/bin/env python3
"""The Glimmering Crags' palette after beta.4: cold light. Glimmer Crystal and its clusters were a
dark smoky violet that read black in the game; recoloured to a cold white-lavender, bright at the
facets, keeping their shapes:

    python3 tools/textures/crags_textures.py

Runs on the originals, kept in tools/textures/crags_src/ the first time.
"""
import colorsys
import shutil
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures/block"
SRC = Path(__file__).resolve().parent / "crags_src"
ICE = ["#3a3f5c", "#5d6488", "#8a90b8", "#b4b8de", "#d8d8f6", "#f2f0ff", "#ffffff"]
TEXTURES = {"glimmer_crystal.png": 1.25, "glimmer_cluster.png": 1.35}


def hexc(h):
    return tuple(int(h.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4))


def ramp(t):
    cols = [hexc(c) for c in ICE]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    return tuple(round(cols[i][k] + (cols[i + 1][k] - cols[i][k]) * f) for k in range(3))


def main():
    SRC.mkdir(exist_ok=True)
    for name, gain in TEXTURES.items():
        if not (SRC / name).exists():
            shutil.copy(TEX / name, SRC / name)
        img = Image.open(SRC / name).convert("RGBA")
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                if a:
                    _, l, _ = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
                    px[x, y] = ramp(l * gain) + (a,)
        img.save(TEX / name)
    print("Recoloured", ", ".join(TEXTURES))


if __name__ == "__main__":
    main()
