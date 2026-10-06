#!/usr/bin/env python3
"""The Nebula Fields' palette after beta.4: an emission nebula - dusty rose and magenta, the red of
glowing hydrogen, with cold pale blue, the oxygen glow, at the tips. Recolours the existing textures
(their shapes and shading stay as drawn), mapping each pixel's brightness onto the new ramps:

    python3 tools/textures/nebula_textures.py

Purple and violet pixels become rose-magenta; teal, cyan and near-white highlights become the pale
blue glow. Run it once on the purple originals (it remembers them in tools/textures/nebula_src/).
"""
import colorsys
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "src/main/resources/assets/astralend/textures/block"
SRC = Path(__file__).resolve().parent / "nebula_src"

ROSE = ["#2a0d1e", "#4a1631", "#6e2246", "#93305c", "#b84a76", "#d76c94", "#eb98b6"]
GLOW = ["#2c4f6e", "#4f86ad", "#7fb9dc", "#b0dcf4", "#e2f4ff"]

# Each texture, and how bright the result should sit on the rose ramp (the moss a little darker, so the
# plants stand out on it).
TEXTURES = {"chorus_moss.png": 0.85, "nebula_tufts.png": 1.0, "astral_vine.png": 1.0,
            "astral_vine_plant.png": 1.0, "nebula_tendril.png": 1.0, "nebula_tendril_plant.png": 1.0}


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def ramp(pal, t):
    cols = [hexc(c) for c in pal]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    return tuple(round(cols[i][k] + (cols[i + 1][k] - cols[i][k]) * f) for k in range(3))


def recolour(img, gain):
    out = img.copy()
    px = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            hue = h * 360
            # cyan/teal/blue-green or very pale: the glow; everything else (violets, purples, greys): rose
            glowing = (150 <= hue <= 215 and s > 0.15) or l > 0.82
            if glowing:
                px[x, y] = ramp(GLOW, (l - 0.25) / 0.7) + (a,)
            else:
                px[x, y] = ramp(ROSE, l * 1.25 * gain) + (a,)
    return out


def main():
    SRC.mkdir(exist_ok=True)
    for name, gain in TEXTURES.items():
        original = SRC / name
        if not original.exists():
            shutil.copy(TEX / name, original)
        recolour(Image.open(original).convert("RGBA"), gain).save(TEX / name)
    print("Recoloured", ", ".join(TEXTURES))


if __name__ == "__main__":
    main()
