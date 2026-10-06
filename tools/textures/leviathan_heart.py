#!/usr/bin/env python3
"""The Leviathan Heart's texture: crimson flesh in a net of veins, animated in a slow double beat - the
veins flush bright twice, close together, then it rests. Writes
textures/block/leviathan_heart.png (16 x 16 frames, stacked) and its .mcmeta.

    python3 tools/textures/leviathan_heart.py
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures/block/leviathan_heart.png"
FLESH = [(38, 5, 12), (56, 8, 18), (76, 12, 24), (96, 18, 30)]
VEIN = [(122, 22, 34), (164, 32, 44), (212, 60, 62), (250, 120, 104)]
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
    """Leviathan Flesh: the heart's muscle and its great vessels - crimson in long fibres, still."""
    tones = [(70, 12, 22), (104, 20, 32), (136, 28, 40), (168, 42, 52), (196, 72, 74)]
    rnd = random.Random(2203)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        band = y % 4                       # fibres four pixels deep: a dark seam, then lighter toward the middle
        base = (0, 2, 3, 2)[band]
        for x in range(16):
            t = base
            if band and rnd.random() < 0.18:
                t += rnd.choice((-1, 1))
            if band == 2 and (x + y * 3) % 7 == 0:
                t = 4                      # a sheen along the fibre
            img.putpixel((x, y), tones[max(0, min(4, t))] + (255,))
    out = OUT.with_name("leviathan_flesh.png")
    img.save(out)
    print("Wrote", out)


if __name__ == "__main__":
    flesh()
