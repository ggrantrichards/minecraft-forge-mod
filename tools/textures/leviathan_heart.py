#!/usr/bin/env python3
"""The Leviathan's living tissue (after beta.4: black and purple, not crimson - no gore, and the End's own
colours): the Heart is near-black muscle in a net of violet veins, animated in a slow double beat - the
veins flush bright twice, close together, then it rests; the Flesh round it is black-purple fibre whose
veins pulse more faintly, like the Nether's stems. Writes textures/block/leviathan_heart.png and
leviathan_flesh.png (16 x 16 frames, stacked) and their .mcmeta.

    python3 tools/textures/leviathan_heart.py
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures/block/leviathan_heart.png"
FLESH = [(16, 8, 24), (28, 13, 40), (42, 19, 58), (58, 26, 80)]
VEIN = [(96, 36, 156), (140, 58, 210), (196, 112, 255), (238, 196, 255)]
# The beat, frame by frame (0 = at rest .. 1 = full flush): lub, dub, then a long rest.
BEAT = [0.0, 0.55, 1.0, 0.45, 0.25, 0.8, 0.5, 0.2, 0.08] + [0.0] * 11


def tile_noise(rnd, cell):
    n = 16 // cell
    g = [[rnd.random() for _ in range(n)] for _ in range(n)]

    def at(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx) % n, int(gy) % n
        x1, y1 = (x0 + 1) % n, (y0 + 1) % n
        tx, ty = gx - int(gx), gy - int(gy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0][x0] + (g[y0][x1] - g[y0][x0]) * tx
        b = g[y1][x0] + (g[y1][x1] - g[y1][x0]) * tx
        return a + (b - a) * ty
    return at


def main():
    rnd = random.Random(1907)
    base = tile_noise(rnd, 4)
    # Veins: a few wandering paths that wrap round the tile, so the net carries on across blocks.
    vein = [[0.0] * 16 for _ in range(16)]
    for k in range(3):
        x, y = rnd.uniform(0, 16), rnd.uniform(0, 16)
        ang = rnd.uniform(0, math.tau)
        width = 0.5 if k == 0 else 0.25
        for _ in range(30):
            ang += rnd.uniform(-0.6, 0.6)
            x, y = (x + math.cos(ang) * 0.7) % 16, (y + math.sin(ang) * 0.7) % 16
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    px, py = int(x + dx) % 16, int(y + dy) % 16
                    d = math.hypot(px + 0.5 - x, py + 0.5 - y) if abs(px - x) < 2 and abs(py - y) < 2 else 9
                    vein[py][px] = max(vein[py][px], max(0.0, 1.0 - d / (width + 0.6)))
    frames = []
    for b in BEAT:
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                f = base(x, y)
                v = vein[y][x]
                if v > 0.5:
                    i = min(3, int((v - 0.5) / 0.5 * 1.5 + b * 2.2))
                    c = VEIN[i]
                else:
                    i = min(3, int(f * 3.0 + b * 0.6 * f))
                    c = FLESH[i]
                img.putpixel((x, y), c + (255,))
        frames.append(img)
    sheet = Image.new("RGBA", (16, 16 * len(frames)))
    for k, fr in enumerate(frames):
        sheet.paste(fr, (0, 16 * k))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    OUT.with_suffix(".png.mcmeta").write_text(json.dumps({"animation": {"frametime": 3, "interpolate": True}}, indent=2) + "\n")
    print("Wrote", OUT, "and its .mcmeta")


if __name__ == "__main__":
    main()


def flesh():
    """Leviathan Flesh: black-purple fibres, with veins of violet running along them that swell and fade
    with the heart's beat, fainter (an animation, like the Nether's stems)."""
    tones = [(12, 6, 18), (22, 10, 32), (32, 15, 46), (44, 20, 62), (60, 28, 84)]
    glow = [(70, 28, 116), (104, 44, 168), (150, 80, 228), (200, 150, 255)]
    rnd = random.Random(2203)
    base = [[0] * 16 for _ in range(16)]
    veins = set()
    for y in range(16):
        band = y % 4
        for x in range(16):
            t = (0, 2, 3, 2)[band]
            if band and rnd.random() < 0.18:
                t += rnd.choice((-1, 1))
            base[y][x] = max(0, min(4, t))
            if band == 2 and (x + y * 3) % 5 in (0, 1):
                veins.add((x, y))
    frames = []
    for b in BEAT:
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                if (x, y) in veins:
                    img.putpixel((x, y), glow[min(3, int(b * 3.2))] + (255,))
                else:
                    img.putpixel((x, y), tones[base[y][x]] + (255,))
        frames.append(img)
    sheet = Image.new("RGBA", (16, 16 * len(frames)))
    for k, fr in enumerate(frames):
        sheet.paste(fr, (0, 16 * k))
    out = OUT.with_name("leviathan_flesh.png")
    sheet.save(out)
    out.with_suffix(".png.mcmeta").write_text(json.dumps({"animation": {"frametime": 3, "interpolate": True}}, indent=2) + "\n")
    print("Wrote", out, "and its .mcmeta")


if __name__ == "__main__":
    flesh()
