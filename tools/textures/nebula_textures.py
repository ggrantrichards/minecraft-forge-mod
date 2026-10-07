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


def bladder():
    """A Drift Bladder: a see-through rose membrane with darker veins round its edge, and a cold
    pale-blue glow at its heart (the block is translucent: the membrane is half see-through)."""
    import random
    rnd = random.Random(23)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5 / 10.6            # 0 at the middle, ~1 at the corners
            if d < 0.28:                                      # the glowing heart
                c = ramp(GLOW, 1.0 - d * 2.2)
                a = 235
            elif d < 0.42:
                c = ramp(GLOW, 0.35)
                a = 190
            else:
                c = ramp(ROSE, 0.85 - 0.45 * d + (rnd.random() - 0.5) * 0.12)
                a = 150 + int(70 * d)
            if x in (0, 15) or y in (0, 15):                  # the membrane's edge, a little darker
                c = ramp(ROSE, 0.42 + rnd.random() * 0.1)
                a = 235
            img.putpixel((x, y), c + (a,))
    # a few darker veins running in from the edge
    for vx, vy, sx, sy in ((1, 4, 1, 0), (14, 11, -1, 0), (5, 1, 0, 1), (10, 14, 0, -1)):
        for k in range(4):
            img.putpixel((vx + sx * k, vy + sy * k), ramp(ROSE, 0.3) + (225,))
    return img


def clouds():
    """The Nebula Fields' sky: four clouds of glowing gas (white, tinted when drawn), each a 64-pixel
    square of a 128-pixel sheet - wispy, uneven filaments (layered noise, stretched one way), densest
    in a lopsided core and fading to nothing well before the edge."""
    import math
    import random

    def noise(rnd, cells):
        grid = [[rnd.random() for _ in range(cells + 1)] for _ in range(cells + 1)]

        def at(x, y):
            gx, gy = x * cells, y * cells
            x0, y0 = min(int(gx), cells - 1), min(int(gy), cells - 1)
            fx, fy = gx - x0, gy - y0
            fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            a = grid[y0][x0] + (grid[y0][x0 + 1] - grid[y0][x0]) * fx
            b = grid[y0 + 1][x0] + (grid[y0 + 1][x0 + 1] - grid[y0 + 1][x0]) * fx
            return a + (b - a) * fy
        return at

    img = Image.new("RGBA", (128, 128), (255, 255, 255, 0))
    for q in range(4):
        rnd = random.Random(301 + q)
        octaves = [(noise(rnd, c), w) for c, w in ((3, 0.5), (6, 0.28), (12, 0.15), (24, 0.07))]
        angle = rnd.uniform(0, math.pi)
        cx, cy = rnd.uniform(0.4, 0.6), rnd.uniform(0.4, 0.6)
        ox, oy = (q % 2) * 64, (q // 2) * 64
        for y in range(64):
            for x in range(64):
                u, v = x / 63.0, y / 63.0
                # stretched along a slant, so the gas lies in streaks
                su = (u - cx) * math.cos(angle) + (v - cy) * math.sin(angle)
                sv = -(u - cx) * math.sin(angle) + (v - cy) * math.cos(angle)
                r = math.hypot(su / 0.48, sv / 0.26)
                fall = max(0.0, 1.0 - r) ** 1.4
                nu = min(0.999, max(0.0, 0.1 + u * 0.7 + su * 0.2))
                nv = min(0.999, max(0.0, 0.1 + v * 0.7 + sv * 0.2))
                n = sum(f(nu, nv) * w for f, w in octaves)
                density = max(0.0, (n - 0.28) / 0.72) ** 1.2 * fall
                img.putpixel((ox + x, oy + y), (255, 255, 255, int(min(1.0, density * 2.2) * 255)))
    return img


def main():
    bladder().save(TEX / "nebula_bladder.png")
    clouds().save(TEX.parent / "environment" / "nebula_cloud.png")
    SRC.mkdir(exist_ok=True)
    for name, gain in TEXTURES.items():
        original = SRC / name
        if not original.exists():
            shutil.copy(TEX / name, original)
        recolour(Image.open(original).convert("RGBA"), gain).save(TEX / name)
    print("Recoloured", ", ".join(TEXTURES))


if __name__ == "__main__":
    main()
