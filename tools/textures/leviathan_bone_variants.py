#!/usr/bin/env python3
"""The Leviathan Bone's variants (after beta.4), so a skeleton of thousands of bone blocks isn't one block
repeated - as vanilla breaks up its stone bricks with mossy and cracked ones:

    python3 tools/textures/leviathan_bone_variants.py

- Weathered Leviathan Bone: the same bone, greyer, cracked through and pitted;
- Lichened Leviathan Bone: End lichen grown over it - pale violet crusts with teal-green flecks - thickest
  on top and creeping down its sides from the top edge, as moss creeps down a mossy cobblestone.
Drawn over the Leviathan Bone's own textures, so the grain matches.
"""
import random
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures/block"
LICHEN = [(92, 70, 128), (124, 98, 164), (156, 130, 196), (190, 168, 222)]
FLECK = [(70, 138, 112), (104, 176, 140)]


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


def weathered(src, seed):
    rnd = random.Random(seed)
    img = src.copy()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            grey = (r + g + b) / 3
            # greyer and a little darker: the ivory gone dull
            px[x, y] = (int(grey * 0.55 + r * 0.33), int(grey * 0.55 + g * 0.33), int(grey * 0.55 + b * 0.36), a)
    # two cracks wandering across it
    for _ in range(2):
        x, y = rnd.randrange(16), rnd.randrange(16)
        dx = rnd.choice((-1, 1))
        for _ in range(9):
            px[x % 16, y % 16] = shade(px[x % 16, y % 16], 0.55)
            y += 1
            if rnd.random() < 0.5:
                x += dx
    # and pits
    for _ in range(6):
        x, y = rnd.randrange(16), rnd.randrange(16)
        px[x, y] = shade(px[x, y], 0.62)
    return img


def lichen_patch(seed, cover):
    """Which pixels the lichen covers: a few round crusts, joined up."""
    rnd = random.Random(seed)
    grid = [[False] * 16 for _ in range(16)]
    for _ in range(cover):
        cx, cy, r = rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(1.6, 3.2)
        for y in range(16):
            for x in range(16):
                ddx = min(abs(x + 0.5 - cx), 16 - abs(x + 0.5 - cx))
                ddy = min(abs(y + 0.5 - cy), 16 - abs(y + 0.5 - cy))
                if ddx * ddx + ddy * ddy < r * r:
                    grid[y][x] = True
    return grid


def lichened(src, seed, side):
    rnd = random.Random(seed)
    img = src.copy()
    px = img.load()
    grid = lichen_patch(seed, 5) if not side else [[False] * 16 for _ in range(16)]
    if side:
        # creeping down from the top edge, ragged, a crust or two lower down
        for x in range(16):
            for y in range(rnd.choice((1, 2, 2, 3, 4, 5))):
                grid[y][x] = True
        for _ in range(2):
            cx, cy = rnd.randrange(16), rnd.randrange(7, 15)
            for y in range(cy - 1, cy + 2):
                for x in range(cx - 1, cx + 2):
                    if rnd.random() < 0.7:
                        grid[y % 16][x % 16] = True
    for y in range(16):
        for x in range(16):
            if grid[y][x]:
                edge = not all(grid[(y + dy) % 16][(x + dx) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                c = LICHEN[0 if edge else rnd.choice((1, 2, 2, 3))]
                if rnd.random() < 0.12:
                    c = rnd.choice(FLECK)
                px[x, y] = c + (255,)
    return img


def main():
    side = Image.open(TEX / "leviathan_bone_side.png").convert("RGBA")
    top = Image.open(TEX / "leviathan_bone_top.png").convert("RGBA")
    weathered(side, 11).save(TEX / "weathered_leviathan_bone_side.png")
    weathered(top, 12).save(TEX / "weathered_leviathan_bone_top.png")
    lichened(side, 21, True).save(TEX / "lichened_leviathan_bone_side.png")
    lichened(top, 22, False).save(TEX / "lichened_leviathan_bone_top.png")
    print("Wrote the weathered and lichened Leviathan Bone textures")


if __name__ == "__main__":
    main()
