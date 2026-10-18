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


def veins(rnd, paths, steps, widths):
    """Wandering vein paths that wrap round the tile, so the net carries on across blocks: 0..1 per pixel."""
    v = [[0.0] * 16 for _ in range(16)]
    for k in range(paths):
        x, y = rnd.uniform(0, 16), rnd.uniform(0, 16)
        ang = rnd.uniform(0, math.tau)
        width = widths[min(k, len(widths) - 1)]
        for _ in range(steps):
            ang += rnd.uniform(-0.6, 0.6)
            x, y = (x + math.cos(ang) * 0.7) % 16, (y + math.sin(ang) * 0.7) % 16
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    px, py = int(x + dx) % 16, int(y + dy) % 16
                    d = math.hypot(px + 0.5 - x, py + 0.5 - y) if abs(px - x) < 2 and abs(py - y) < 2 else 9
                    v[py][px] = max(v[py][px], max(0.0, 1.0 - d / (width + 0.6)))
    return v


def flesh():
    """Leviathan Flesh: made the way the Heart is, but quieter - the same near-black muscle, mottled a
    little darker, with fewer and finer veins that only brighten a little with the beat."""
    rnd = random.Random(2422)   # a seed whose veins spread evenly over the tile
    base = tile_noise(rnd, 4)
    grain = tile_noise(rnd, 2)
    vein = veins(rnd, 3, 24, (0.3, 0.25))
    frames = []
    for b in BEAT:
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = vein[y][x]
                if v > 0.5:
                    c = VEIN[min(2, int((v - 0.5) / 0.5 * 1.2 + b * 1.3))]
                else:
                    f = base(x, y) * 0.75 + grain(x, y) * 0.25
                    c = FLESH[min(3, int(f * 3.2))]
                img.putpixel((x, y), c + (255,))
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
